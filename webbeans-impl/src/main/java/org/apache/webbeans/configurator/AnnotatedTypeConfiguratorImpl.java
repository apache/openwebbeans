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
package org.apache.webbeans.configurator;

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.portable.AnnotatedConstructorImpl;
import org.apache.webbeans.portable.AnnotatedFieldImpl;
import org.apache.webbeans.portable.AnnotatedMethodImpl;
import org.apache.webbeans.portable.AnnotatedTypeImpl;

import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.configurator.AnnotatedConstructorConfigurator;
import jakarta.enterprise.inject.spi.configurator.AnnotatedFieldConfigurator;
import jakarta.enterprise.inject.spi.configurator.AnnotatedMethodConfigurator;
import jakarta.enterprise.inject.spi.configurator.AnnotatedTypeConfigurator;
import java.lang.annotation.Annotation;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class AnnotatedTypeConfiguratorImpl<T> implements AnnotatedTypeConfigurator<T>
{

    private final AnnotatedType<T> originalAnnotatedType;
    private final AnnotatedTypeImpl<T> newAnnotatedType;
    private Set<AnnotatedConstructorConfigurator<T>> annotatedConstructorConfigurators;
    private Set<AnnotatedMethodConfigurator<? super T>> annotatedMethodConfigurators;
    private Set<AnnotatedFieldConfigurator<? super T>> annotatedFieldConfigurators;


    public AnnotatedTypeConfiguratorImpl(WebBeansContext webBeansContext, AnnotatedType<T> originalAnnotatedType)
    {
        this.originalAnnotatedType = originalAnnotatedType;
        this.newAnnotatedType = new AnnotatedTypeImpl<>(webBeansContext, originalAnnotatedType);

        annotatedConstructorConfigurators = newAnnotatedType.getConstructors().stream()
            .map(m -> new AnnotatedConstructorConfiguratorImpl<>((AnnotatedConstructorImpl<T>) m))
            .collect(Collectors.toSet());

        annotatedMethodConfigurators = newAnnotatedType.getMethods().stream()
            .map(m -> new AnnotatedMethodConfiguratorImpl<>((AnnotatedMethodImpl<T>) m))
            .collect(Collectors.toSet());

        annotatedFieldConfigurators = newAnnotatedType.getFields().stream()
            .map(m -> new AnnotatedFieldConfiguratorImpl<>((AnnotatedFieldImpl<T>) m))
            .collect(Collectors.toSet());
    }


    @Override
    public AnnotatedType<T> getAnnotated()
    {
        return originalAnnotatedType;
    }

    @Override
    public AnnotatedTypeConfigurator<T> add(Annotation annotation)
    {
        newAnnotatedType.addAnnotation(annotation);
        return this;
    }

    @Override
    public AnnotatedTypeConfigurator<T> remove(Predicate predicate)
    {
        newAnnotatedType.getAnnotations().removeIf(predicate);
        return this;
    }

    @Override
    public AnnotatedTypeConfigurator<T> removeAll()
    {
        newAnnotatedType.clearAnnotations();
        return this;
    }

    @Override
    public Set<AnnotatedMethodConfigurator<? super T>> methods()
    {
        return annotatedMethodConfigurators;
    }

    @Override
    public Set<AnnotatedFieldConfigurator<? super T>> fields()
    {
        return annotatedFieldConfigurators;
    }

    @Override
    public Set<AnnotatedConstructorConfigurator<T>> constructors()
    {
        return annotatedConstructorConfigurators;
    }


    public AnnotatedTypeImpl<T> getNewAnnotatedType()
    {
        return newAnnotatedType;
    }

}
