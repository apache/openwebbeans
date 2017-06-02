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

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeanAttributes;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.configurator.BeanConfigurator;
import javax.enterprise.util.TypeLiteral;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class BeanConfiguratorImpl<T> implements BeanConfigurator<T>
{

    @Override
    public BeanConfigurator<T> beanClass(Class<?> beanClass)
    {
        throw new UnsupportedOperationException("TODO implement CDI 2.0");
    }

    @Override
    public BeanConfigurator<T> addInjectionPoint(InjectionPoint injectionPoint)
    {
        throw new UnsupportedOperationException("TODO implement CDI 2.0");
    }

    @Override
    public BeanConfigurator<T> addInjectionPoints(InjectionPoint... injectionPoints)
    {
        throw new UnsupportedOperationException("TODO implement CDI 2.0");
    }

    @Override
    public BeanConfigurator<T> addInjectionPoints(Set<InjectionPoint> injectionPoints)
    {
        throw new UnsupportedOperationException("TODO implement CDI 2.0");
    }

    @Override
    public BeanConfigurator<T> injectionPoints(InjectionPoint... injectionPoints)
    {
        throw new UnsupportedOperationException("TODO implement CDI 2.0");
    }

    @Override
    public BeanConfigurator<T> injectionPoints(Set<InjectionPoint> injectionPoints)
    {
        throw new UnsupportedOperationException("TODO implement CDI 2.0");
    }

    @Override
    public BeanConfigurator<T> id(String id)
    {
        throw new UnsupportedOperationException("TODO implement CDI 2.0");
    }

    @Override
    public <U extends T> BeanConfigurator<U> createWith(Function<CreationalContext<U>, U> callback)
    {
        throw new UnsupportedOperationException("TODO implement CDI 2.0");
    }

    @Override
    public <U extends T> BeanConfigurator<U> produceWith(Function<Instance<Object>, U> callback)
    {
        throw new UnsupportedOperationException("TODO implement CDI 2.0");
    }

    @Override
    public BeanConfigurator<T> destroyWith(BiConsumer<T, CreationalContext<T>> callback)
    {
        throw new UnsupportedOperationException("TODO implement CDI 2.0");
    }

    @Override
    public BeanConfigurator<T> disposeWith(BiConsumer<T, Instance<Object>> callback)
    {
        throw new UnsupportedOperationException("TODO implement CDI 2.0");
    }

    @Override
    public <U extends T> BeanConfigurator<U> read(AnnotatedType<U> type)
    {
        throw new UnsupportedOperationException("TODO implement CDI 2.0");
    }

    @Override
    public BeanConfigurator<T> read(BeanAttributes<?> beanAttributes)
    {
        throw new UnsupportedOperationException("TODO implement CDI 2.0");
    }

    @Override
    public BeanConfigurator<T> addType(Type type)
    {
        throw new UnsupportedOperationException("TODO implement CDI 2.0");
    }

    @Override
    public BeanConfigurator<T> addType(TypeLiteral<?> typeLiteral)
    {
        throw new UnsupportedOperationException("TODO implement CDI 2.0");
    }

    @Override
    public BeanConfigurator<T> addTypes(Type... types)
    {
        throw new UnsupportedOperationException("TODO implement CDI 2.0");
    }

    @Override
    public BeanConfigurator<T> addTypes(Set<Type> types)
    {
        throw new UnsupportedOperationException("TODO implement CDI 2.0");
    }

    @Override
    public BeanConfigurator<T> addTransitiveTypeClosure(Type type)
    {
        throw new UnsupportedOperationException("TODO implement CDI 2.0");
    }

    @Override
    public BeanConfigurator<T> types(Type... types)
    {
        throw new UnsupportedOperationException("TODO implement CDI 2.0");
    }

    @Override
    public BeanConfigurator<T> types(Set<Type> types)
    {
        throw new UnsupportedOperationException("TODO implement CDI 2.0");
    }

    @Override
    public BeanConfigurator<T> scope(Class<? extends Annotation> scope)
    {
        throw new UnsupportedOperationException("TODO implement CDI 2.0");
    }

    @Override
    public BeanConfigurator<T> addQualifier(Annotation qualifier)
    {
        throw new UnsupportedOperationException("TODO implement CDI 2.0");
    }

    @Override
    public BeanConfigurator<T> addQualifiers(Annotation... qualifiers)
    {
        throw new UnsupportedOperationException("TODO implement CDI 2.0");
    }

    @Override
    public BeanConfigurator<T> addQualifiers(Set<Annotation> qualifiers)
    {
        throw new UnsupportedOperationException("TODO implement CDI 2.0");
    }

    @Override
    public BeanConfigurator<T> qualifiers(Annotation... qualifiers)
    {
        throw new UnsupportedOperationException("TODO implement CDI 2.0");
    }

    @Override
    public BeanConfigurator<T> qualifiers(Set<Annotation> qualifiers)
    {
        throw new UnsupportedOperationException("TODO implement CDI 2.0");
    }

    @Override
    public BeanConfigurator<T> addStereotype(Class<? extends Annotation> stereotype)
    {
        throw new UnsupportedOperationException("TODO implement CDI 2.0");
    }

    @Override
    public BeanConfigurator<T> addStereotypes(Set<Class<? extends Annotation>> stereotypes)
    {
        throw new UnsupportedOperationException("TODO implement CDI 2.0");
    }

    @Override
    public BeanConfigurator<T> stereotypes(Set<Class<? extends Annotation>> stereotypes)
    {
        throw new UnsupportedOperationException("TODO implement CDI 2.0");
    }

    @Override
    public BeanConfigurator<T> name(String name)
    {
        throw new UnsupportedOperationException("TODO implement CDI 2.0");
    }

    @Override
    public BeanConfigurator<T> alternative(boolean value)
    {
        throw new UnsupportedOperationException("TODO implement CDI 2.0");
    }
}
