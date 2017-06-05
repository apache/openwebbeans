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

import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.configurator.AnnotatedMethodConfigurator;
import javax.enterprise.inject.spi.configurator.AnnotatedParameterConfigurator;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.apache.webbeans.portable.AnnotatedMethodImpl;


public class AnnotatedMethodConfiguratorImpl<T> implements AnnotatedMethodConfigurator<T>
{
    private final AnnotatedMethodImpl<T> annotatedMethod;

    public AnnotatedMethodConfiguratorImpl(AnnotatedMethodImpl<T> annotatedMethod)
    {
        this.annotatedMethod = annotatedMethod;
    }

    @Override
    public AnnotatedMethod<T> getAnnotated()
    {
        return annotatedMethod;
    }

    @Override
    public AnnotatedMethodConfigurator<T> add(Annotation annotation)
    {
        annotatedMethod.addAnnotation(annotation);
        return this;
    }

    @Override
    public AnnotatedMethodConfigurator<T> remove(Predicate<Annotation> annotation)
    {
        annotatedMethod.getAnnotations().removeIf(annotation);
        return this;
    }

    @Override
    public AnnotatedMethodConfigurator<T> removeAll()
    {
        annotatedMethod.getAnnotations().clear();
        return this;
    }

    @Override
    public List<AnnotatedParameterConfigurator<T>> params()
    {
        throw new UnsupportedOperationException("TODO implement CDI 2.0");
    }

}
