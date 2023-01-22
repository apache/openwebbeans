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

import jakarta.enterprise.inject.spi.AnnotatedParameter;
import jakarta.enterprise.inject.spi.configurator.AnnotatedParameterConfigurator;
import java.lang.annotation.Annotation;
import java.util.function.Predicate;

public class AnnotatedParameterConfiguratorImpl<T> implements AnnotatedParameterConfigurator<T>
{
    private final AnnotatedParameter<T> annotatedParameter;

    public AnnotatedParameterConfiguratorImpl(AnnotatedParameter<T> annotatedParameter)
    {
        this.annotatedParameter = annotatedParameter;
    }

    @Override
    public AnnotatedParameter<T> getAnnotated()
    {
        return annotatedParameter;
    }

    @Override
    public AnnotatedParameterConfigurator<T> add(Annotation annotation)
    {
        annotatedParameter.getAnnotations().add(annotation);
        return this;
    }

    @Override
    public AnnotatedParameterConfigurator<T> remove(Predicate predicate)
    {
        annotatedParameter.getAnnotations().removeIf(predicate);
        return this;
    }

    @Override
    public AnnotatedParameterConfigurator<T> removeAll()
    {
        annotatedParameter.getAnnotations().clear();
        return this;
    }
}
