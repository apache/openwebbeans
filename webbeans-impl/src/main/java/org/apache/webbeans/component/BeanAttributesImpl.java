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

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanAttributes;
import org.apache.webbeans.util.AnnotationUtil;

public class BeanAttributesImpl<T> implements BeanAttributes<T>
{
    private final Set<Type> types;
    private final Set<Annotation> qualifiers;
    private final Class<? extends Annotation> scope;
    private final String name;
    private final Set<Class<? extends Annotation>> stereotypes;
    private final boolean alternative;

    /**
     * do not remove, this ct is used from within TomEE for example.
     * @deprecated
     */
    public BeanAttributesImpl(Bean<T> bean)
    {
        this(bean.getTypes(),
             bean.getQualifiers(),
             bean.getScope(),
             bean.getName(),
             bean.getStereotypes(),
             bean.isAlternative());
    }

    public BeanAttributesImpl(BeanAttributes<T> beanAttributes)
    {
        this(beanAttributes.getTypes(),
             beanAttributes.getQualifiers(),
             beanAttributes.getScope(),
             beanAttributes.getName(),
             beanAttributes.getStereotypes(),
             beanAttributes.isAlternative());
    }

    public BeanAttributesImpl(Set<Type> types)
    {
        this(types, AnnotationUtil.DEFAULT_AND_ANY_ANNOTATION_SET, Dependent.class, null, Collections.<Class<? extends Annotation>>emptySet(), false);
    }

    public BeanAttributesImpl(Set<Type> types, Set<Annotation> qualifiers)
    {
        this(types, qualifiers, Dependent.class, null, Collections.<Class<? extends Annotation>>emptySet(), false);
    }

    public BeanAttributesImpl(Set<Type> types,
                        Set<Annotation> qualifiers,
                        Class<? extends Annotation> scope)
    {
        this(types, qualifiers, scope, null, Collections.<Class<? extends Annotation>>emptySet(), false);
    }

    public BeanAttributesImpl(Set<Type> types,
                        Set<Annotation> qualifiers,
                        Class<? extends Annotation> scope,
                        Set<Class<? extends Annotation>> stereotypes)
    {
        this(types, qualifiers, scope, null, stereotypes, false);
    }

    public BeanAttributesImpl(Set<Type> types,
                        Set<Annotation> qualifiers,
                        Class<? extends Annotation> scope,
                        String name,
                        Set<Class<? extends Annotation>> stereotypes,
                        boolean alternative)
    {
        this.types = types == null? Collections.<Type>emptySet(): Collections.unmodifiableSet(new HashSet<>(types));
        this.qualifiers = qualifiers == null? Collections.<Annotation>emptySet(): Collections.unmodifiableSet(new HashSet<>(qualifiers));
        this.scope = scope;
        this.name = name;
        this.stereotypes = stereotypes == null
                ? Collections.<Class<? extends Annotation>>emptySet()
                : Collections.unmodifiableSet(new HashSet<>(stereotypes));
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
