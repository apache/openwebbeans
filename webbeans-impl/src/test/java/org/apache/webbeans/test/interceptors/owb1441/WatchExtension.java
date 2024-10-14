/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.webbeans.test.interceptors.owb1441;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.AnnotatedConstructor;
import jakarta.enterprise.inject.spi.AnnotatedField;
import jakarta.enterprise.inject.spi.AnnotatedMethod;
import jakarta.enterprise.inject.spi.AnnotatedParameter;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.BeforeBeanDiscovery;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.util.Nonbinding;
import org.apache.webbeans.test.interceptors.interceptorbean.BigBrotherInterceptorBean;
import org.apache.webbeans.test.interceptors.interceptorbean.BigBrothered;

public class WatchExtension implements Extension
{
    public void registerInterceptorBinding(@Observes BeforeBeanDiscovery beforeBeanDiscovery,  BeanManager beanManager) {
        beforeBeanDiscovery.addInterceptorBinding(
            new WatchedAnnotatedType(beanManager.createAnnotatedType(Watched.class)));

    }

    public void registerInterceptor(@Observes AfterBeanDiscovery afterBeanDiscovery) {
        afterBeanDiscovery.addBean(new WatchInterceptorBean(42));
    }

    // To add Nonbinding to @Watched members
    @SuppressWarnings("unchecked")
    static class WatchedAnnotatedType implements AnnotatedType<Watched> {
        private final AnnotatedType<Watched> delegate;
        private final Set<AnnotatedMethod<? super Watched>> methods;

        WatchedAnnotatedType(final AnnotatedType<Watched> delegate) {
            this.delegate = delegate;
            this.methods = new HashSet<>();

            for (AnnotatedMethod<? super Watched> method : delegate.getMethods()) {
                methods.add(new AnnotatedMethod<>() {
                    private final AnnotatedMethod<Watched> delegate = (AnnotatedMethod<Watched>) method;
                    private final Set<Annotation> annotations = Collections.singleton(Nonbinding.Literal.INSTANCE);

                    @Override
                    public Method getJavaMember() {
                        return delegate.getJavaMember();
                    }

                    @Override
                    public List<AnnotatedParameter<Watched>> getParameters() {
                        return delegate.getParameters();
                    }

                    @Override
                    public boolean isStatic() {
                        return delegate.isStatic();
                    }

                    @Override
                    public AnnotatedType<Watched> getDeclaringType() {
                        return delegate.getDeclaringType();
                    }

                    @Override
                    public Type getBaseType() {
                        return delegate.getBaseType();
                    }

                    @Override
                    public Set<Type> getTypeClosure() {
                        return delegate.getTypeClosure();
                    }

                    @Override
                    public <T extends Annotation> T getAnnotation(final Class<T> annotationType) {
                        if (annotationType.equals(Nonbinding.class)) {
                            return (T) annotations.iterator().next();
                        }
                        return null;
                    }

                    @Override
                    public Set<Annotation> getAnnotations() {
                        return annotations;
                    }

                    @Override
                    public boolean isAnnotationPresent(final Class<? extends Annotation> annotationType) {
                        return annotationType.equals(Nonbinding.class);
                    }
                });
            }
        }

        @Override
        public Class<Watched> getJavaClass() {
            return delegate.getJavaClass();
        }

        @Override
        public Set<AnnotatedConstructor<Watched>> getConstructors() {
            return delegate.getConstructors();
        }

        @Override
        public Set<AnnotatedMethod<? super Watched>> getMethods() {
            return this.methods;
        }

        @Override
        public Set<AnnotatedField<? super Watched>> getFields() {
            return delegate.getFields();
        }

        @Override
        public Type getBaseType() {
            return delegate.getBaseType();
        }

        @Override
        public Set<Type> getTypeClosure() {
            return delegate.getTypeClosure();
        }

        @Override
        public <T extends Annotation> T getAnnotation(final Class<T> annotationType) {
            return delegate.getAnnotation(annotationType);
        }

        @Override
        public Set<Annotation> getAnnotations() {
            return delegate.getAnnotations();
        }

        @Override
        public boolean isAnnotationPresent(final Class<? extends Annotation> annotationType) {
            return delegate.isAnnotationPresent(annotationType);
        }
    }

}
