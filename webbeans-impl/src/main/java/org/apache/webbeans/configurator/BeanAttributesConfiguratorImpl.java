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

import javax.enterprise.inject.spi.BeanAttributes;
import javax.enterprise.inject.spi.configurator.BeanAttributesConfigurator;
import javax.enterprise.util.TypeLiteral;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

import org.apache.webbeans.annotation.AnyLiteral;
import org.apache.webbeans.component.BeanAttributesImpl;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.util.GenericsUtil;

public class BeanAttributesConfiguratorImpl<T> implements BeanAttributesConfigurator<T>
{
    private final WebBeansContext webBeansContext;

    private Set<Type> types;
    private Set<Annotation> qualifiers;
    private Class<? extends Annotation> scope;
    private String name;
    private Set<Class<? extends Annotation>> stereotypes;
    private boolean alternative;

    public BeanAttributesConfiguratorImpl(WebBeansContext webBeansContext, BeanAttributes<T> originalBeanAttribute)
    {
        this.webBeansContext = webBeansContext;
        
        this.types = new HashSet<>(originalBeanAttribute.getTypes());
        this.qualifiers = new HashSet<>(originalBeanAttribute.getQualifiers());
        this.scope = originalBeanAttribute.getScope();
        this.name = originalBeanAttribute.getName();
        this.stereotypes = new HashSet<>(originalBeanAttribute.getStereotypes());
        this.alternative = originalBeanAttribute.isAlternative();
    }

    @Override
    public BeanAttributesConfigurator<T> addType(Type type)
    {
        types.add(type);
        return this;
    }

    @Override
    public BeanAttributesConfigurator<T> addType(TypeLiteral typeLiteral)
    {
        types.add(typeLiteral.getType());
        return this;
    }

    @Override
    public BeanAttributesConfigurator<T> addTypes(Type... types)
    {
        for (Type type : types)
        {
            this.types.add(type);
        }
        return this;
    }

    @Override
    public BeanAttributesConfigurator<T> addTypes(Set set)
    {
        types.addAll(set);
        return this;
    }

    @Override
    public BeanAttributesConfigurator<T> addTransitiveTypeClosure(Type type)
    {
        Set<Type> typeClosure = GenericsUtil.getTypeClosure(type, type);
        types.addAll(typeClosure);
        return this;
    }


    @Override
    public BeanAttributesConfigurator<T> types(Type... types)
    {
        this.types.clear();
        addTypes(types);
        return this;
    }

    @Override
    public BeanAttributesConfigurator<T> types(Set<Type> set)
    {
        this.types.clear();
        addTypes(set);
        return this;
    }

    @Override
    public BeanAttributesConfigurator<T> scope(Class<? extends Annotation>  scope)
    {
        this.scope = scope;
        return this;
    }

    @Override
    public BeanAttributesConfigurator<T> addQualifier(Annotation qualifier)
    {
        qualifiers.add(qualifier);
        return this;
    }

    @Override
    public BeanAttributesConfigurator<T> addQualifiers(Annotation... qualifiers)
    {
        for (Annotation qualifier : qualifiers)
        {
            addQualifiers(qualifier);
        }
        return this;
    }

    @Override
    public BeanAttributesConfigurator<T> addQualifiers(Set<Annotation> qualifiers)
    {
        this.qualifiers.addAll(qualifiers);
        return this;
    }

    @Override
    public BeanAttributesConfigurator<T> qualifiers(Annotation... qualifiers)
    {
        this.qualifiers.clear();
        for (Annotation qualifier : qualifiers)
        {
            addQualifier(qualifier);
        }
        return this;
    }

    @Override
    public BeanAttributesConfigurator<T> qualifiers(Set<Annotation> qualifiers)
    {
        this.qualifiers.clear();
        addQualifiers(qualifiers);
        return this;
    }

    @Override
    public BeanAttributesConfigurator<T> addStereotype(Class<? extends Annotation> stereotype)
    {
        stereotypes.add(stereotype);
        return this;
    }

    @Override
    public BeanAttributesConfigurator<T> addStereotypes(Set<Class<? extends Annotation>> stereotypes)
    {
        this.stereotypes.addAll(stereotypes);
        return this;
    }

    @Override
    public BeanAttributesConfigurator<T> stereotypes(Set<Class<? extends Annotation>> stereotypes)
    {
        this.stereotypes.clear();
        addStereotypes(stereotypes);
        return this;
    }

    @Override
    public BeanAttributesConfigurator<T> name(String name)
    {
        this.name = name;
        return this;
    }

    @Override
    public BeanAttributesConfigurator<T> alternative(boolean value)
    {
        this.alternative = value;
        return this;
    }

    public BeanAttributes<T> getBeanAttributes()
    {
        // make sure we always have an @Any Qualifier as well.
        qualifiers.add(AnyLiteral.INSTANCE);

        return new BeanAttributesImpl<T>(types, qualifiers, scope, name, false, stereotypes, alternative);
    }
}
