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
package org.apache.webbeans.test.spi.deployer;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.spi.AnnotatedConstructor;
import jakarta.enterprise.inject.spi.AnnotatedField;
import jakarta.enterprise.inject.spi.AnnotatedMethod;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.BeforeBeanDiscovery;
import jakarta.enterprise.inject.spi.Extension;

public class DuplicateAnnotatedTypeExtension implements Extension
{
    public void register(@Observes BeforeBeanDiscovery beforeBeanDiscovery, BeanManager beanManager)
    {
        AnnotatedType<DuplicateAnnotatedTypesAlternativeTest.XmlConfiguredAlternativeBean> delegate
            = beanManager.createAnnotatedType(DuplicateAnnotatedTypesAlternativeTest.XmlConfiguredAlternativeBean.class);

        beforeBeanDiscovery.addAnnotatedType(new AlternativeAnnotatedType<>(delegate), "alternative");
        for (int i = 0; i < DuplicateAnnotatedTypesAlternativeTest.PLAIN_DUPLICATE_COUNT; i++)
        {
            beforeBeanDiscovery.addAnnotatedType(delegate, "plain-" + i);
        }
    }

    private static class AlternativeAnnotatedType<T> implements AnnotatedType<T>
    {
        private final AnnotatedType<T> delegate;
        private final Set<Annotation> annotations;

        private AlternativeAnnotatedType(AnnotatedType<T> delegate)
        {
            this.delegate = delegate;
            this.annotations = new LinkedHashSet<>(delegate.getAnnotations());
            this.annotations.add(Alternative.Literal.INSTANCE);
        }

        @Override
        public Class<T> getJavaClass()
        {
            return delegate.getJavaClass();
        }

        @Override
        public Set<AnnotatedConstructor<T>> getConstructors()
        {
            return delegate.getConstructors();
        }

        @Override
        public Set<AnnotatedMethod<? super T>> getMethods()
        {
            return delegate.getMethods();
        }

        @Override
        public Set<AnnotatedField<? super T>> getFields()
        {
            return delegate.getFields();
        }

        @Override
        public Type getBaseType()
        {
            return delegate.getBaseType();
        }

        @Override
        public Set<Type> getTypeClosure()
        {
            return delegate.getTypeClosure();
        }

        @Override
        public <A extends Annotation> A getAnnotation(Class<A> annotationType)
        {
            for (Annotation annotation : annotations)
            {
                if (annotation.annotationType() == annotationType)
                {
                    return annotationType.cast(annotation);
                }
            }
            return null;
        }

        @Override
        public Set<Annotation> getAnnotations()
        {
            return annotations;
        }

        @Override
        public boolean isAnnotationPresent(Class<? extends Annotation> annotationType)
        {
            return getAnnotation(annotationType) != null;
        }
    }

}
