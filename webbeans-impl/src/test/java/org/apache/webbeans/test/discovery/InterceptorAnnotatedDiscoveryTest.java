/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.webbeans.test.discovery;

import org.apache.webbeans.corespi.scanner.xbean.CdiArchive;
import org.apache.webbeans.corespi.scanner.xbean.OwbAnnotationFinder;
import org.apache.webbeans.corespi.se.DefaultScannerService;
import org.apache.webbeans.spi.BeanArchiveService;
import org.apache.webbeans.spi.ScannerService;
import org.apache.webbeans.test.AbstractUnitTest;
import org.apache.webbeans.util.WebBeansUtil;
import org.apache.webbeans.xml.DefaultBeanArchiveInformation;
import org.apache.xbean.finder.AnnotationFinder;
import org.junit.Test;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InterceptorBinding;
import jakarta.interceptor.InvocationContext;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class InterceptorAnnotatedDiscoveryTest extends AbstractUnitTest
{
    @Test
    public void discover()
    {
        setClasses(FooInterceptor.class, FooMe.class);

        startContainer();
        assertEquals("foo", getInstance(FooMe.class).foo());
    }

    private void setClasses(final Class<?>... classes) {
        // replace the implicit BDA by an annotated one
        addService(ScannerService.class, new DefaultScannerService()
        {
            @Override
            protected void initFinder()
            {
                // no-op, just to avoid super call
            }

            @Override
            public Map<BeanArchiveService.BeanArchiveInformation, Set<Class<?>>> getBeanClassesPerBda()
            {
                Map<BeanArchiveService.BeanArchiveInformation, Set<Class<?>>> beanInfo = new HashMap<>();

                BeanArchiveService.BeanArchiveInformation bai = new DefaultBeanArchiveInformation("openwebbeans://default")
                {
                    {
                    setBeanDiscoveryMode(BeanArchiveService.BeanDiscoveryMode.ANNOTATED);
                    }
                };

                beanInfo.put(bai, new HashSet<>(Arrays.asList(classes)));

                return beanInfo;
            }
        });
    }

    @Interceptor
    @Foo
    @Priority(0)
    public static class FooInterceptor
    {
        @AroundInvoke
        public Object foo(final InvocationContext ic)
        {
            return "foo";
        }
    }

    @Foo
    @ApplicationScoped
    public static class FooMe {
        String foo()
        {
            return "bar";
        }
    }

    @InterceptorBinding
    @Retention(RUNTIME)
    @Target(TYPE)
    public @interface Foo
    {
    }
}
