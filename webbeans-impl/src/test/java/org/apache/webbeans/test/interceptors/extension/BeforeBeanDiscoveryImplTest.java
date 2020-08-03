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

import org.apache.webbeans.annotation.EmptyAnnotationLiteral;
import org.apache.webbeans.container.AnnotatedTypeWrapper;
import org.apache.webbeans.test.AbstractUnitTest;
import org.junit.Test;

import javax.annotation.Priority;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.util.Nonbinding;
import javax.interceptor.AroundConstruct;
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
    public void nonBindingByExtension()
    {
        addExtension(new TheExtension());
        addInterceptor(TheInterceptor.class);
        startContainer(Intercepted.class);

        final Intercepted instance = getInstance(Intercepted.class);
        assertEquals("interceptor", instance.value());
        assertEquals("bean", instance.noInterceptor());
    }

    @Test
    public void classAndMethod() {
        addExtension(new OwbExtension());
        addInterceptor(OwbMethodInterceptor.class);
        startContainer(OwbBean.class);

        final OwbBean instance = getInstance(OwbBean.class);
        assertEquals("ok", instance.method());
        assertEquals(0, instance.getCounter());
    }

    @OwbClassBinding
    public static class OwbBean {
        private int counter;

        @OwbMethodBinding
        public String method() {
            throw new UnsupportedOperationException();
        }

        public void incr() {
            counter++;
        }

        public int getCounter() {
            return counter;
        }
    }

    public static class OwbExtension implements Extension {
        private void addInterceptorBindings(@Observes BeforeBeanDiscovery bbd, BeanManager manager) {
            final AnnotatedType<OwbMethodBinding> annotatedType1 = manager.createAnnotatedType(OwbMethodBinding.class);
            final Set<Annotation> annotations = annotatedType1.getAnnotations();
            annotations.add(new EmptyAnnotationLiteral<InterceptorBinding>() {});

            final AnnotatedType<OwbMethodBinding> annotatedType = new AnnotatedTypeWrapper<OwbMethodBinding>(this, annotatedType1, "x") {
                @Override
                public Set<Annotation> getAnnotations()
                {
                    return annotations;
                }

                @Override
                public boolean isAnnotationPresent(final Class<? extends Annotation> aClass)
                {
                    return super.isAnnotationPresent(aClass) || InterceptorBinding.class == aClass;
                }
            };
            bbd.addInterceptorBinding(annotatedType);
        }
    }

    @InterceptorBinding
    @Target({ ElementType.TYPE, ElementType.METHOD })
    @Retention(RetentionPolicy.RUNTIME)
    static @interface OwbClassBinding {
    }

    //@InterceptorBinding
    @Target({ ElementType.TYPE, ElementType.METHOD })
    @Retention(RetentionPolicy.RUNTIME)
    static @interface OwbMethodBinding {
    }

    @OwbMethodBinding
    @Interceptor
    @Priority(1000 + 10)
    static class OwbMethodInterceptor {
        @AroundConstruct
        private Object constructor(InvocationContext context) throws Exception {
            final Object proceed = context.proceed();
            OwbBean.class.cast(context.getTarget()).incr();
            return proceed;
        }

        @AroundInvoke
        private Object method(InvocationContext context) throws Exception {
            if (context.getMethod().getName().equals("method")) {
                return "ok";
            }
            return context.proceed();
        }
    }

    public static class TheExtension implements Extension
    {
        void obs(@Observes final BeforeBeanDiscovery bbd, final BeanManager bm)
        {
            final AnnotatedType<TheBindingType> annotatedType1 = bm.createAnnotatedType(TheBindingType.class);
            final Set<Annotation> annotations = annotatedType1.getAnnotations();
            annotations.add(new EmptyAnnotationLiteral<InterceptorBinding>() {});

            final AnnotatedType<TheBindingType> annotatedType = new AnnotatedTypeWrapper<TheBindingType>(this, annotatedType1, "x") {
                @Override
                public Set<Annotation> getAnnotations()
                {
                    return annotations;
                }

                @Override
                public boolean isAnnotationPresent(final Class<? extends Annotation> aClass)
                {
                    return super.isAnnotationPresent(aClass) || InterceptorBinding.class == aClass;
                }
            };

            final AnnotatedTypeWrapper<TheBindingType> wrapper = new AnnotatedTypeWrapper<TheBindingType>(this, annotatedType, "x") {
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
                                        return (T) new EmptyAnnotationLiteral<Nonbinding>() {};
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

    // @InterceptorBinding: done by the extension
    @Retention(RetentionPolicy.RUNTIME)
    @Target( { ElementType.TYPE, ElementType.METHOD })
    public static @interface TheBindingType
    {
        String shouldBeBound();

        boolean binding();
    }

    @Interceptor
    @TheBindingType(shouldBeBound = "useless normally", binding = true)
    public static class TheInterceptor
    {
        @AroundInvoke
        public Object aroundInvoke(final InvocationContext ctx) throws Exception
        {
            return "interceptor";
        }
    }

    public static class Intercepted
    {
        @TheBindingType(shouldBeBound = "another value", binding = true)
        public String value()
        {
            throw new UnsupportedOperationException("the test failed :(");
        }

        @TheBindingType(shouldBeBound = "useless normally", binding = false)
        public String noInterceptor()
        {
            return "bean";
        }
    }
}
