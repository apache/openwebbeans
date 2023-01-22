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

import jakarta.enterprise.inject.spi.AnnotatedField;
import jakarta.enterprise.inject.spi.configurator.AnnotatedFieldConfigurator;
import java.lang.annotation.Annotation;
import java.util.function.Predicate;

import org.apache.webbeans.portable.AnnotatedFieldImpl;

public class AnnotatedFieldConfiguratorImpl<T> implements AnnotatedFieldConfigurator<T>
{
    private final AnnotatedFieldImpl<T> annotatedField;

    public AnnotatedFieldConfiguratorImpl(AnnotatedFieldImpl<T> annotatedField)
    {
        this.annotatedField = annotatedField;
    }

    @Override
    public AnnotatedField<T> getAnnotated()
    {
        return annotatedField;
    }

    @Override
    public AnnotatedFieldConfigurator<T> add(Annotation annotation)
    {
        annotatedField.addAnnotation(annotation);
        return this;
    }

    @Override
    public AnnotatedFieldConfigurator<T> remove(Predicate annotation)
    {
        annotatedField.getAnnotations().removeIf(annotation);
        return this;
    }

    @Override
    public AnnotatedFieldConfigurator<T> removeAll()
    {
        annotatedField.getAnnotations().clear();
        return this;
    }
}
