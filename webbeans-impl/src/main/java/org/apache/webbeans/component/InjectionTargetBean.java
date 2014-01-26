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

import javax.enterprise.inject.spi.AnnotatedType;

import javax.enterprise.inject.spi.BeanAttributes;
import javax.enterprise.inject.spi.InjectionTargetFactory;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.container.InjectionTargetFactoryImpl;

import javax.enterprise.inject.spi.InjectionTarget;

import org.apache.webbeans.util.Asserts;

/**
 * Abstract class for injection target beans.
 * 
 * @version $Rev$ $Date$
 * @param <T> bean class
 */
public class InjectionTargetBean<T> extends AbstractOwbBean<T>
{    
    /**Annotated type for bean*/
    private AnnotatedType<T> annotatedType;
    private InjectionTarget<T> injectionTarget;

    public InjectionTargetBean(
            WebBeansContext webBeansContext,
            WebBeansType webBeansType,
            AnnotatedType<T> annotatedType,
            BeanAttributes<T> beanAttributes,
            Class<T> beanClass)
    {
        this(webBeansContext, webBeansType, annotatedType, beanAttributes, beanClass, new InjectionTargetFactoryImpl<T>(annotatedType, webBeansContext));
    }

    /**
     * Initializes the InjectionTarget Bean part.
     */
    public InjectionTargetBean(WebBeansContext webBeansContext,
            WebBeansType webBeansType,
            AnnotatedType<T> annotatedType,
            BeanAttributes<T> beanAttributes,
            Class<T> beanClass,
            InjectionTargetFactory<T> factory)
    {
        super(webBeansContext, webBeansType, beanAttributes, beanClass, false);
        Asserts.assertNotNull(annotatedType, "AnnotatedType may not be null");
        this.annotatedType = annotatedType;
        injectionTarget = factory.createInjectionTarget(this);
        setEnabled(true);
    }

    @Override
    public InjectionTarget<T> getProducer()
    {
        return injectionTarget;
    }

    public InjectionTarget<T> getInjectionTarget()
    {
        return injectionTarget;
    }

    /**
     * {@inheritDoc}
     */
    public AnnotatedType<T> getAnnotatedType()
    {
        return annotatedType;
    }
}
