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
package org.apache.webbeans.component.creation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.ObserverMethod;

import org.apache.webbeans.annotation.NewLiteral;
import org.apache.webbeans.component.InjectionTargetBean;
import org.apache.webbeans.component.NewManagedBean;
import org.apache.webbeans.component.ProducerFieldBean;
import org.apache.webbeans.component.ProducerMethodBean;
import org.apache.webbeans.component.WebBeansType;
import org.apache.webbeans.config.WebBeansContext;

public class NewManagedBeanBuilder<T> extends ManagedBeanBuilder<T, NewManagedBean<T>>
{

    public NewManagedBeanBuilder(WebBeansContext webBeansContext, AnnotatedType<T> annotatedType)
    {
        super(webBeansContext, annotatedType);
    }

    @Override
    public void defineDisposalMethods()
    {
        // not available in @New beans!
    }

    @Override
    public Set<ProducerFieldBean<?>> defineProducerFields(InjectionTargetBean<T> bean)
    {
        // not available in @New beans!
        return Collections.EMPTY_SET;
    }

    @Override
    public Set<ProducerMethodBean<?>> defineProducerMethods(InjectionTargetBean<T> bean)
    {
        // not available in @New beans!
        return Collections.EMPTY_SET;
    }

    @Override
    public Set<ObserverMethod<?>> defineObserverMethods(InjectionTargetBean<T> bean)
    {
        // not available in @New beans!
        return Collections.EMPTY_SET;
    }

    @Override
    protected NewManagedBean<T> createBean(Set<Type> types,
                                           Set<Annotation> qualifiers,
                                           Class<? extends Annotation> scope,
                                           String name,
                                           boolean nullable,
                                           Class<T> beanClass,
                                           Set<Class<? extends Annotation>> stereotypes,
                                           boolean alternative,
                                           boolean enabled)
    {
        Set<Annotation> newQualifiers = new HashSet<Annotation>(qualifiers);
        newQualifiers.add(new NewLiteral(getAnnotated().getJavaClass()));
        return new NewManagedBean<T>(webBeansContext, WebBeansType.MANAGED, getAnnotated(), types, newQualifiers, beanClass, stereotypes);
    }
}
