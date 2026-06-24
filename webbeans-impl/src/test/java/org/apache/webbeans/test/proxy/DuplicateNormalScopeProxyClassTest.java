/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.webbeans.test.proxy;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;

import org.apache.webbeans.custom.CustomProxyPackageMarker;
import org.apache.webbeans.spi.DefiningClassService;
import org.apache.webbeans.test.AbstractUnitTest;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

/**
 * Reproducer for the duplicate proxy-class-name issue surfaced as
 * <a href="https://issues.apache.org/jira/browse/TOMEE-4603">TOMEE-4603</a>.
 *
 * <p>This test is a <strong>failing</strong> reproducer. It is expected to
 * fail on current OpenWebBeans and to pass once the underlying flaw is
 * fixed.</p>
 *
 * <h2>Setup</h2>
 *
 * <p>Two {@code @Produces @NormalScope} methods declared on the same bean
 * class return the same erased type ({@link java.util.Map}). They are
 * tracked as two distinct {@code Bean<?>} instances, so neither
 * {@code BeanManagerImpl#cacheProxies} nor
 * {@code NormalScopeProxyFactory#cachedProxyClasses} is hit when each is
 * resolved for the first time.</p>
 *
 * <p>The generated proxy class name is computed in
 * {@code AbstractProxyFactory#fixPreservedPackages} +
 * {@code NormalScopeProxyFactory#createProxyClass} from the proxied type
 * and its method signatures only &mdash; not from the bean. With
 * {@code useStaticNames=true} (and equivalently under a race in the
 * dynamic-name loop) both producers ask for the same class name, e.g.
 * {@code org.apache.webbeans.custom.Map$$OwbNormalScopeProxy14900025290}.</p>
 *
 * <h2>Why this surfaces in TomEE but not in standalone OWB</h2>
 *
 * <p>The standalone OWB {@code Unsafe.handleLinkageError} catches the
 * {@link LinkageError} raised by the second {@code defineClass} and falls
 * back to {@link Class#forName}. Containers that supply their own
 * {@link DefiningClassService} that goes through the JVM define-class path
 * without such a fallback (TomEE's
 * {@code org.apache.openejb.util.proxy.ClassDefiner} does this) propagate
 * the {@code LinkageError} to the application &mdash; that is the
 * TOMEE-4603 stack trace.</p>
 *
 * <p>This test wires a {@link StrictLookupDefiningService} that mirrors
 * TomEE's behaviour, then resolves both producers and expects both to
 * succeed. Today the second resolution throws
 * {@code java.lang.LinkageError: duplicate class definition for
 * org.apache.webbeans.custom.Map$$OwbNormalScopeProxy...} &mdash; which is
 * the bug. The fix should either share a single proxy class across beans
 * of the same erased type, or add the {@code Class#forName} fallback to
 * {@code AbstractProxyFactory#createProxyClass} so every
 * {@code DefiningClassService} benefits from it.</p>
 *
 * <p>The MyFaces side is innocent: {@code FacesArtifactProducer} simply
 * declares many {@code @FacesScoped} (a {@code @NormalScope}) producer
 * methods of type {@code Map} ({@code requestScope}, {@code param},
 * {@code header}, {@code sessionScope}, ...). The collision is created
 * inside OpenWebBeans.</p>
 */
public class DuplicateNormalScopeProxyClassTest extends AbstractUnitTest
{
    @ApplicationScoped
    public static class MapProducers
    {
        @Produces
        @ApplicationScoped
        @Named("a")
        public Map<String, String> a()
        {
            return new HashMap<>();
        }

        @Produces
        @ApplicationScoped
        @Named("b")
        public Map<String, String> b()
        {
            return new HashMap<>();
        }
    }

    /**
     * Resolve two distinct {@code @NormalScope} producers of the same
     * erased type via a {@link DefiningClassService} that does not
     * silently recover from {@link LinkageError} (mirroring TomEE's
     * {@code ClassDefiner}). Both resolutions are expected to succeed.
     *
     * <p>Today the second resolution throws
     * {@code LinkageError: duplicate class definition for
     * org.apache.webbeans.custom.Map$$OwbNormalScopeProxy...} &mdash;
     * the exact failure reported in TOMEE-4603 &mdash; and the test
     * fails. It will pass once OpenWebBeans no longer relies on
     * {@code Unsafe.handleLinkageError} to mask the proxy-class-name
     * collision.</p>
     */
    @Test
    public void distinctNormalScopedBeansOfSameTypeMustBothResolve()
    {
        // useStaticNames forces the collision deterministically. Without it
        // the dynamic-name loop in AbstractProxyFactory#getUnusedProxyClassName
        // can still collide under concurrency or when the classloader does
        // not return the just-defined class via Class#forName (the actual
        // TomEE-4603 trigger), but useStaticNames keeps this test stable
        // across JVMs and classloaders.
        addConfiguration("org.apache.webbeans.proxy.useStaticNames", "true");
        addService(DefiningClassService.class, StrictLookupDefiningService.class);

        startContainer(MapProducers.class);

        final Map<String, String> a = getInstance("a");
        assertNotNull(a);

        // Today this line propagates a LinkageError out of OpenWebBeans
        // because it picks the same proxy class name as 'a'. That is the
        // bug TOMEE-4603 reports.
        final Map<String, String> b = getInstance("b");
        assertNotNull(b);
    }

    /**
     * Minimal {@link DefiningClassService} mirroring the shape of TomEE's
     * {@code org.apache.openejb.util.proxy.ClassDefiner}: defines the proxy
     * directly via {@link MethodHandles.Lookup#defineClass(byte[])} and
     * propagates whatever the JVM throws (notably {@link LinkageError}).
     * Crucially &mdash; unlike OWB's internal {@code Unsafe} fallback
     * &mdash; there is no silent {@link Class#forName} retry, so the
     * duplicate definition surfaces to the caller.
     */
    public static class StrictLookupDefiningService implements DefiningClassService
    {
        @Override
        public ClassLoader getProxyClassLoader(final Class<?> forClass)
        {
            final ClassLoader classLoader = forClass.getClassLoader();
            return classLoader != null ? classLoader : Thread.currentThread().getContextClassLoader();
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> Class<T> defineAndLoad(final String name, final byte[] bytecode, final Class<T> proxiedClass)
        {
            try
            {
                // The generated proxy lives in org.apache.webbeans.custom, so
                // use the package marker class to obtain a Lookup in that
                // package (same trick OWB's Unsafe uses).
                final MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(
                        CustomProxyPackageMarker.class, MethodHandles.lookup());
                return (Class<T>) lookup.defineClass(bytecode);
            }
            catch (final LinkageError e)
            {
                throw e;
            }
            catch (final Exception e)
            {
                throw new RuntimeException(e);
            }
        }
    }
}
