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
package org.apache.webbeans.test.interceptors.extension;

import org.apache.webbeans.container.AnnotatedTypeWrapper;
import org.apache.webbeans.test.AbstractUnitTest;
import org.apache.webbeans.test.interceptors.annotation.DependentInterceptorBindingType;
import org.apache.webbeans.util.AnnotationUtil;
import org.junit.Ignore;
import org.junit.Test;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.util.Nonbinding;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InterceptorBinding;
import javax.interceptor.InvocationContext;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class BeforeBeanDiscoveryImplTest extends AbstractUnitTest
{
    @Test
    @Ignore("not yet implemented")
    public void nonBindingByExtension()
    {
        addExtension(new TheExtension());
        addInterceptor(TheInterceptor.class);
        startContainer(TheInterceptor.class, Intercepted.class);
        assertEquals("interceptor", getInstance(Intercepted.class).value());
    }

    public static class TheExtension implements Extension
    {
        void obs(@Observes final BeforeBeanDiscovery bbd, final BeanManager bm)
        {
            final AnnotatedType<TheBindingType> annotatedType = bm.createAnnotatedType(TheBindingType.class);
            final AnnotatedTypeWrapper<TheBindingType> wrapper = new AnnotatedTypeWrapper<TheBindingType>(this, annotatedType) {
                @Override
                public Set<AnnotatedMethod<? super TheBindingType>> getMethods() {
                    final Set<AnnotatedMethod<? super TheBindingType>> methods = super.getMethods();
                    final Set<AnnotatedMethod<? super TheBindingType>> wrapped = new HashSet<AnnotatedMethod<? super TheBindingType>>();
                    for (final AnnotatedMethod<? super TheBindingType> m : methods)
                    {
                        if ("shouldBeBound".equals(m.getJavaMember().getName()))
                        {
                            wrapped.add(new AnnotatedMethod<TheBindingType>() {
                                @Override
                                public Method getJavaMember() {
                                    return m.getJavaMember();
                                }

                                @Override
                                public List<AnnotatedParameter<TheBindingType>> getParameters() {
                                    final List<? extends AnnotatedParameter<? super TheBindingType>> parameters = m.getParameters();
                                    return (List<AnnotatedParameter<TheBindingType>>) parameters;
                                }

                                @Override
                                public boolean isStatic() {
                                    return m.isStatic();
                                }

                                @Override
                                public AnnotatedType<TheBindingType> getDeclaringType() {
                                    final AnnotatedType<? super TheBindingType> declaringType = m.getDeclaringType();
                                    return (AnnotatedType<TheBindingType>) declaringType;
                                }

                                @Override
                                public Type getBaseType() {
                                    return m.getBaseType();
                                }

                                @Override
                                public Set<Type> getTypeClosure() {
                                    return m.getTypeClosure();
                                }

                                @Override
                                public <T extends Annotation> T getAnnotation(final Class<T> annotationType) {
                                    final T annotation = m.getAnnotation(annotationType);
                                    if (annotation == null && Nonbinding.class == annotationType)
                                    {
                                        return (T) new Nonbinding()
                                        {
                                            @Override
                                            public Class<? extends Annotation> annotationType()
                                            {
                                                return Nonbinding.class;
                                            }
                                        };
                                    }
                                    return annotation;
                                }

                                @Override
                                public Set<Annotation> getAnnotations() {
                                    return m.getAnnotations();
                                }

                                @Override
                                public boolean isAnnotationPresent(final Class<? extends Annotation> annotationType) {
                                    return m.isAnnotationPresent(annotationType) || Nonbinding.class == annotationType;
                                }
                            });
                        }
                        else
                        {
                            wrapped.add(m);
                        }
                    }
                    return wrapped;
                }
            };
            bbd.addInterceptorBinding(wrapper);
        }
    }

    @InterceptorBinding
    @Retention(RetentionPolicy.RUNTIME)
    @Target( { ElementType.TYPE, ElementType.METHOD })
    public static @interface TheBindingType
    {
        String shouldBeBound();
    }

    @Interceptor
    @TheBindingType(shouldBeBound = "useless normally")
    public class TheInterceptor
    {
        @AroundInvoke
        public Object aroundInvoke(final InvocationContext ctx) throws Exception
        {
            return "interceptor";
        }
    }

    public static class Intercepted
    {
        @TheBindingType(shouldBeBound = "another value")
        public String value()
        {
            throw new UnsupportedOperationException("shouldn't be called");
        }
    }
}
