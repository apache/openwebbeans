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

import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.configurator.AnnotatedConstructorConfigurator;
import javax.enterprise.inject.spi.configurator.AnnotatedFieldConfigurator;
import javax.enterprise.inject.spi.configurator.AnnotatedMethodConfigurator;
import javax.enterprise.inject.spi.configurator.AnnotatedTypeConfigurator;
import java.lang.annotation.Annotation;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class AnnotatedTypeConfiguratorImpl implements AnnotatedTypeConfigurator
{
    @Override
    public AnnotatedType getAnnotated()
    {
        throw new UnsupportedOperationException("TODO implement CDI 2.0");
    }

    @Override
    public AnnotatedTypeConfigurator add(Annotation annotation)
    {
        throw new UnsupportedOperationException("TODO implement CDI 2.0");
    }

    @Override
    public AnnotatedTypeConfigurator remove(Predicate predicate)
    {
        throw new UnsupportedOperationException("TODO implement CDI 2.0");
    }

    @Override
    public AnnotatedTypeConfigurator removeAll()
    {
        throw new UnsupportedOperationException("TODO implement CDI 2.0");
    }

    @Override
    public Set<AnnotatedMethodConfigurator> methods()
    {
        throw new UnsupportedOperationException("TODO implement CDI 2.0");
    }

    @Override
    public Stream<AnnotatedMethodConfigurator> filterMethods(Predicate predicate)
    {
        throw new UnsupportedOperationException("TODO implement CDI 2.0");
    }

    @Override
    public Set<AnnotatedFieldConfigurator> fields()
    {
        throw new UnsupportedOperationException("TODO implement CDI 2.0");
    }

    @Override
    public Stream<AnnotatedFieldConfigurator> filterFields(Predicate predicate)
    {
        throw new UnsupportedOperationException("TODO implement CDI 2.0");
    }

    @Override
    public Set<AnnotatedConstructorConfigurator> constructors()
    {
        throw new UnsupportedOperationException("TODO implement CDI 2.0");
    }

    @Override
    public Stream<AnnotatedConstructorConfigurator> filterConstructors(Predicate predicate)
    {
        throw new UnsupportedOperationException("TODO implement CDI 2.0");
    }
}
