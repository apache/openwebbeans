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
import java.util.Set;

import javax.enterprise.inject.spi.AnnotatedType;

import org.apache.webbeans.config.WebBeansContext;

import javax.enterprise.inject.spi.InjectionTarget;

import org.apache.webbeans.util.Asserts;


/**
 * Abstract class for injection target beans.
 * 
 * @version $Rev$ $Date$
 * @param <T> bean class
 */
public abstract class InjectionTargetBean<T> extends AbstractOwbBean<T>
{    
    /**Annotated type for bean*/
    private AnnotatedType<T> annotatedType;

    protected InjectionTargetBean(WebBeansContext webBeansContext,
                                  WebBeansType webBeansType,
                                  AnnotatedType<T> annotatedType,
                                  Set<Type> types,
                                  Set<Annotation> qualifiers,
                                  Class<? extends Annotation> scope,
                                  Class<T> beanClass,
                                  Set<Class<? extends Annotation>> stereotypes)
    {
        this(webBeansContext, webBeansType, annotatedType, types, qualifiers, scope, null, beanClass, stereotypes, false);
        setEnabled(true);
    }

    /**
     * Initializes the InjectionTarget Bean part.
     */
    protected InjectionTargetBean(WebBeansContext webBeansContext,
                                  WebBeansType webBeansType,
                                  AnnotatedType<T> annotatedType,
                                  Set<Type> types,
                                  Set<Annotation> qualifiers,
                                  Class<? extends Annotation> scope,
                                  String name,
                                  Class<T> beanClass,
                                  Set<Class<? extends Annotation>> stereotypes,
                                  boolean alternative)
    {
        super(webBeansContext, webBeansType, types, qualifiers, scope, name, false, beanClass, stereotypes, alternative);
        Asserts.assertNotNull(annotatedType, "AnnotatedType may not be null");
        this.annotatedType = annotatedType;
    }

    public InjectionTarget<T> getInjectionTarget()
    {
        return (InjectionTarget<T>) getProducer();
    }

    /**
     * {@inheritDoc}
     */
    public AnnotatedType<T> getAnnotatedType()
    {
        return annotatedType;
    }
}
