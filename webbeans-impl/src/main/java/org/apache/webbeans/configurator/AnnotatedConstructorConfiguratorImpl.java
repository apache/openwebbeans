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

import javax.enterprise.inject.spi.AnnotatedConstructor;
import javax.enterprise.inject.spi.configurator.AnnotatedConstructorConfigurator;
import javax.enterprise.inject.spi.configurator.AnnotatedParameterConfigurator;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.webbeans.portable.AnnotatedConstructorImpl;

public class AnnotatedConstructorConfiguratorImpl<T> implements AnnotatedConstructorConfigurator<T>
{
    private final AnnotatedConstructorImpl<T> annotatedConstructor;
    private final List<AnnotatedParameterConfigurator<T>> annotatedParameterConfigurators;


    public AnnotatedConstructorConfiguratorImpl(AnnotatedConstructorImpl<T> annotatedConstructor)
    {
        this.annotatedConstructor = annotatedConstructor;

        annotatedParameterConfigurators = annotatedConstructor.getParameters().stream()
            .map(m -> new AnnotatedParameterConfiguratorImpl<>(m))
            .collect(Collectors.toList());
    }

    @Override
    public AnnotatedConstructor<T> getAnnotated()
    {
        return annotatedConstructor;
    }

    @Override
    public AnnotatedConstructorConfigurator<T> add(Annotation annotation)
    {
        annotatedConstructor.addAnnotation(annotation);
        return this;
    }

    @Override
    public AnnotatedConstructorConfigurator<T> remove(Predicate predicate)
    {
        annotatedConstructor.getAnnotations().removeIf(predicate);
        return this;
    }

    @Override
    public AnnotatedConstructorConfigurator<T> removeAll()
    {
        annotatedConstructor.getAnnotations().clear();
        return this;
    }

    @Override
    public List<AnnotatedParameterConfigurator<T>> params()
    {
        return annotatedParameterConfigurators;
    }
}
