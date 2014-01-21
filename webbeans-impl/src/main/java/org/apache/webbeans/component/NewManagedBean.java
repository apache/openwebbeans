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

import java.util.Set;

import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.InjectionPoint;

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.portable.AbstractProducer;

/**
 * Component definition with {@link javax.enterprise.inject.New} binding annotation.
 * 
 * <p>
 * It is defined as concrete java class component.
 * </p>
 * 
 */
public class NewManagedBean<T> extends ManagedBean<T> implements NewBean<T>
{

    public NewManagedBean(WebBeansContext webBeansContext,
                          WebBeansType webBeansType,
                          AnnotatedType<T> annotatedType,
                          BeanAttributesImpl<T> beanAttributes,
                          Class<T> beanClass,
                          Set<InjectionPoint> injectionPoints)
    {
        super(webBeansContext, webBeansType, annotatedType, beanAttributes, beanClass);
        if (getProducer() instanceof AbstractProducer)
        {
            AbstractProducer<T> producer = (AbstractProducer<T>)getProducer();
            producer.defineInterceptorStack(this, annotatedType, webBeansContext);
        }
    }

    /**
     * always true for New qualifier
     */
    @Override
    public boolean isDependent()
    {
        return true;
    }

}
