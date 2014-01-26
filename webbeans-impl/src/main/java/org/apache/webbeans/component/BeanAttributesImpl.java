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

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.spi.Bean;

import javax.enterprise.inject.spi.BeanAttributes;
import org.apache.webbeans.util.AnnotationUtil;

public class BeanAttributesImpl<T> implements BeanAttributes<T>
{
    private final Set<Type> types;
    private final Set<Annotation> qualifiers;
    private final Class<? extends Annotation> scope;
    private final String name;
    private final boolean nullable;
    private final Set<Class<? extends Annotation>> stereotypes;
    private final boolean alternative;

    /**
     * TODO This constructor can be removed, when we move to CDI 1.1 since {@link Bean} extends BeanAttributes.
     */
    public BeanAttributesImpl(Bean<T> bean)
    {
        this(bean.getTypes(),
             bean.getQualifiers(),
             bean.getScope(),
             bean.getName(),
             bean.isNullable(),
             bean.getStereotypes(),
             bean.isAlternative());
    }

    public BeanAttributesImpl(BeanAttributes<T> beanAttributes, boolean nullable)
    {
        this(beanAttributes.getTypes(),
             beanAttributes.getQualifiers(),
             beanAttributes.getScope(),
             beanAttributes.getName(),
             nullable,
             beanAttributes.getStereotypes(),
             beanAttributes.isAlternative());
    }

    public BeanAttributesImpl(Set<Type> types)
    {
        this(types, AnnotationUtil.DEFAULT_AND_ANY_ANNOTATION, Dependent.class, null, false, Collections.<Class<? extends Annotation>>emptySet(), false);
    }

    public BeanAttributesImpl(Set<Type> types, Set<Annotation> qualifiers)
    {
        this(types, qualifiers, Dependent.class, null, false, Collections.<Class<? extends Annotation>>emptySet(), false);
    }

    public BeanAttributesImpl(Set<Type> types,
                        Set<Annotation> qualifiers,
                        Class<? extends Annotation> scope)
    {
        this(types, qualifiers, scope, null, false, Collections.<Class<? extends Annotation>>emptySet(), false);
    }

    public BeanAttributesImpl(Set<Type> types,
                        Set<Annotation> qualifiers,
                        Class<? extends Annotation> scope,
                        Set<Class<? extends Annotation>> stereotypes)
    {
        this(types, qualifiers, scope, null, false, stereotypes, false);
    }

    public BeanAttributesImpl(Set<Type> types,
                        Set<Annotation> qualifiers,
                        Class<? extends Annotation> scope,
                        String name,
                        boolean nullable,
                        Set<Class<? extends Annotation>> stereotypes,
                        boolean alternative)
    {
        this.types = types == null? Collections.<Type>emptySet(): Collections.unmodifiableSet(new HashSet<Type>(types));
        this.qualifiers = qualifiers == null? Collections.<Annotation>emptySet(): Collections.unmodifiableSet(new HashSet<Annotation>(qualifiers));
        this.scope = scope;
        this.name = name;
        this.nullable = nullable;
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

    public boolean isNullable()
    {
        return nullable;
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
