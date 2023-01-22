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

import jakarta.enterprise.inject.spi.Annotated;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.enterprise.inject.spi.configurator.InjectionPointConfigurator;
import java.lang.annotation.Annotation;
import java.lang.reflect.Member;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

import org.apache.webbeans.annotation.DefaultLiteral;
import org.apache.webbeans.inject.impl.InjectionPointImpl;

public class InjectionPointConfiguratorImpl implements InjectionPointConfigurator
{

    private Bean<?> ownerBean;
    private Type type;
    private Set<Annotation> qualifiers = new HashSet<>();
    private Annotated annotated;
    private Member member;
    private boolean isDelegate;
    private boolean isTransient;

    public InjectionPointConfiguratorImpl(InjectionPoint originalInjectionPoint)
    {
        this.ownerBean = originalInjectionPoint.getBean();
        this.type = originalInjectionPoint.getType();
        this.qualifiers.addAll(originalInjectionPoint.getQualifiers());
        this.annotated = originalInjectionPoint.getAnnotated();
        this.member = originalInjectionPoint.getMember();
        this.isDelegate = originalInjectionPoint.isDelegate();
        this.isTransient = originalInjectionPoint.isTransient();
    }

    @Override
    public InjectionPointConfigurator type(Type requiredType)
    {
        this.type = requiredType;
        return this;
    }

    @Override
    public InjectionPointConfigurator addQualifier(Annotation qualifier)
    {
        this.qualifiers.add(qualifier);
        return this;
    }

    @Override
    public InjectionPointConfigurator addQualifiers(Annotation... qualifiers)
    {
        for (Annotation qualifier : qualifiers)
        {
            this.qualifiers.add(qualifier);
        }
        return this;
    }

    @Override
    public InjectionPointConfigurator addQualifiers(Set<Annotation> qualifiers)
    {
        this.qualifiers.addAll(qualifiers);
        return this;
    }

    @Override
    public InjectionPointConfigurator qualifiers(Annotation... qualifiers)
    {
        this.qualifiers.clear();
        addQualifiers(qualifiers);
        return this;
    }

    @Override
    public InjectionPointConfigurator qualifiers(Set<Annotation> qualifiers)
    {
        this.qualifiers.clear();
        addQualifiers(qualifiers);
        return this;
    }

    @Override
    public InjectionPointConfigurator delegate(boolean delegate)
    {
        this.isDelegate = delegate;
        return this;
    }

    @Override
    public InjectionPointConfigurator transientField(boolean trans)
    {
        this.isTransient = trans;
        return this;
    }

    public InjectionPoint getInjectionPoint()
    {
        // apply the rules from 3.8 '@Default qualifier'
        if (qualifiers.isEmpty())
        {
            qualifiers.add(DefaultLiteral.INSTANCE);
        }
        else if (qualifiers.size() > 1)
        {
            qualifiers.remove(DefaultLiteral.INSTANCE);
        }

        return new InjectionPointImpl(ownerBean,
                type,
                qualifiers,
                annotated,
                member,
                isDelegate,
                isTransient);
    }
}
