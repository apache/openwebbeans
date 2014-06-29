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
package org.apache.webbeans.container;

import javax.enterprise.inject.spi.AnnotatedConstructor;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Set;

// totally useless but TCKs check AT references creating a new instance for each call...
class AnnotatedTypeWrapper<T> implements AnnotatedType<T>
{
    private final AnnotatedType<T> original;

    public AnnotatedTypeWrapper(final AnnotatedType<T> annotatedType)
    {
        original = annotatedType;
    }

    @Override
    public Class<T> getJavaClass()
    {
        return original.getJavaClass();
    }

    @Override
    public Set<AnnotatedConstructor<T>> getConstructors()
    {
        return original.getConstructors();
    }

    @Override
    public Set<AnnotatedMethod<? super T>> getMethods()
    {
        return original.getMethods();
    }

    @Override
    public Set<AnnotatedField<? super T>> getFields()
    {
        return original.getFields();
    }

    @Override
    public Type getBaseType()
    {
        return original.getBaseType();
    }

    @Override
    public Set<Type> getTypeClosure()
    {
        return original.getTypeClosure();
    }

    @Override
    public <T extends Annotation> T getAnnotation(Class<T> tClass)
    {
        return original.getAnnotation(tClass);
    }

    @Override
    public Set<Annotation> getAnnotations()
    {
        return original.getAnnotations();
    }

    @Override
    public boolean isAnnotationPresent(final Class<? extends Annotation> aClass)
    {
        return original.isAnnotationPresent(aClass);
    }
}
