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
package org.apache.webbeans.component;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;

public abstract class AbstractBean<T> implements Bean<T>
{

    final Set<InjectionPoint> internalInjectionPoints = new HashSet<InjectionPoint>();
    private final Set<InjectionPoint> injectionPoints = Collections.unmodifiableSet(internalInjectionPoints);
    
    private final Set<Type> types;
    private final Set<Annotation> qualifiers;
    private final Class<? extends Annotation> scope;
    private final String name;
    private final boolean nullable;
    private final Class<?> beanClass;
    private final Set<Class<? extends Annotation>> stereotypes;
    private final boolean alternative;

    public AbstractBean(Set<Type> types,
                        Set<Annotation> qualifiers,
                        Class<? extends Annotation> scope,
                        Class<?> beanClass,
                        Set<Class<? extends Annotation>> stereotypes)
    {
        this(types, qualifiers, scope, null, false, beanClass, stereotypes, false);
    }

    public AbstractBean(Set<Type> types,
                        Set<Annotation> qualifiers,
                        Class<? extends Annotation> scope,
                        String name,
                        boolean nullable,
                        Class<?> beanClass,
                        Set<Class<? extends Annotation>> stereotypes,
                        boolean alternative)
    {
        this.types = types == null? Collections.<Type>emptySet(): Collections.unmodifiableSet(new HashSet<Type>(types));
        this.qualifiers = qualifiers == null? Collections.<Annotation>emptySet(): Collections.unmodifiableSet(new HashSet<Annotation>(qualifiers));
        this.scope = scope;
        this.name = name;
        this.nullable = nullable;
        this.beanClass = beanClass;
        this.stereotypes = stereotypes == null
                ? Collections.<Class<? extends Annotation>>emptySet()
                : Collections.unmodifiableSet(new HashSet<Class<? extends Annotation>>(stereotypes));
        this.alternative = alternative;
    }

    @Override
    public Set<Type> getTypes()
    {
        return types;
    }

    @Override
    public Set<Annotation> getQualifiers()
    {
        return qualifiers;
    }

    @Override
    public Class<? extends Annotation> getScope()
    {
        return scope;
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public boolean isNullable()
    {
        return nullable;
    }

    @Override
    public Set<InjectionPoint> getInjectionPoints()
    {
        return injectionPoints;
    }

    @Override
    public Class<?> getBeanClass()
    {
        return beanClass;
    }

    @Override
    public Set<Class<? extends Annotation>> getStereotypes()
    {
        return stereotypes;
    }

    @Override
    public boolean isAlternative()
    {
        return alternative;
    }
}
