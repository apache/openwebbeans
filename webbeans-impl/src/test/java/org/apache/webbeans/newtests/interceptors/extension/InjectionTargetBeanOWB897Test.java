/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.webbeans.newtests.interceptors.extension;

import org.apache.webbeans.newtests.AbstractUnitTest;
import org.junit.Test;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AnnotatedConstructor;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.ProcessInjectionTarget;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InterceptorBinding;
import javax.interceptor.InvocationContext;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class InjectionTargetBeanOWB897Test extends AbstractUnitTest
{
    @Inject
    private TheBean bean;

    @Test
    public void doIt()
    {
        addExtension(new AnExtension());
        addInterceptor(InterceptorImpl.class);
        startContainer(Arrays.<Class<?>>asList(TheBean.class), Collections.<String>emptyList(), true);
        assertNotNull(bean);
        assertEquals("ok", bean.intercepted());
        assertEquals("set", bean.getMarker());
        assertTrue(AnExtension.injectCalled);
        assertTrue(AnExtension.postConstructCalled);
        shutDownContainer();
    }

    public static class AnExtension implements Extension {
        private static boolean injectCalled = false;
        private static boolean postConstructCalled = false;

        void pat(final @Observes ProcessAnnotatedType<TheBean> pat) {
            final AnnotatedType<TheBean> at = pat.getAnnotatedType();
            pat.setAnnotatedType(new AnnotatedType<TheBean>() {
                @Override
                public Class<TheBean> getJavaClass()
                {
                    return at.getJavaClass();
                }

                @Override
                public Set<AnnotatedConstructor<TheBean>> getConstructors()
                {
                    return at.getConstructors();
                }

                @Override
                public Set<AnnotatedMethod<? super TheBean>> getMethods()
                {
                    return at.getMethods();
                }

                @Override
                public Set<AnnotatedField<? super TheBean>> getFields()
                {
                    return at.getFields();
                }

                @Override
                public Type getBaseType()
                {
                    return at.getBaseType();
                }

                @Override
                public Set<Type> getTypeClosure()
                {
                    return at.getTypeClosure();
                }

                @Override
                public <T extends Annotation> T getAnnotation(final Class<T> annotationType)
                {
                    return at.getAnnotation(annotationType);
                }

                @Override
                public Set<Annotation> getAnnotations()
                {
                    final Set<Annotation> annotations = new HashSet<Annotation>();
                    annotations.addAll(at.getAnnotations());
                    annotations.add(new AnnotationLiteral<B>()
                    {
                    });
                    return annotations;
                }

                @Override
                public boolean isAnnotationPresent(final Class<? extends Annotation> annotationType)
                {
                    return B.class.equals(annotationType) || at.isAnnotationPresent(annotationType);
                }
            });
        }

        void pij(final @Observes ProcessInjectionTarget<TheBean> pij) {
            pij.setInjectionTarget(new InjectionTarget<TheBean>() {
                @Override
                public void inject(final TheBean instance, final CreationalContext<TheBean> ctx)
                {
                    injectCalled = instance != null;
                    pij.getInjectionTarget().inject(instance, ctx); // to check we don't have an infinite loop
                }

                @Override
                public void postConstruct(final TheBean instance)
                {
                    postConstructCalled = instance != null;
                    pij.getInjectionTarget().postConstruct(instance); // to check we don't have an infinite loop
                }

                @Override
                public void preDestroy(final TheBean instance)
                {
                    assertNotNull(instance);
                }

                @Override
                public TheBean produce(final CreationalContext<TheBean> creationalContext) {
                    final TheBean theBean = pij.getInjectionTarget().produce(creationalContext); // to check we don't have an infinite loop;
                    theBean.setMarker("set");
                    return theBean;
                }

                @Override
                public void dispose(final TheBean instance)
                {
                    assertNotNull(instance);
                }

                @Override
                public Set<InjectionPoint> getInjectionPoints() {
                    return Collections.emptySet();
                }
            });
        }
    }

    public static class TheBean
    {
        private String marker = "not set";

        public String intercepted()
        {
            throw new UnsupportedOperationException();
        }

        public void setMarker(final String marker) {
            this.marker = marker;
        }

        public String getMarker()
        {
            return marker;
        }
    }

    @InterceptorBinding
    @Target( { ElementType.TYPE, ElementType.METHOD })
    @Retention(RetentionPolicy.RUNTIME)
    public static @interface B
    {
    }

    @Interceptor @B
    public static class InterceptorImpl
    {
        @AroundInvoke
        public Object around (final InvocationContext ic) throws Exception
        {
            if (ic.getMethod().getName().equals("intercepted")) {
                return "ok";
            }
            return ic.proceed();
        }
    }
}
