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

import jakarta.enterprise.context.spi.Contextual;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanAttributes;

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.context.creational.CreationalContextImpl;
import org.apache.webbeans.portable.InjectionTargetImpl;

/**
 * Managed bean implementation of the {@link jakarta.enterprise.inject.spi.Bean}.
 * 
 * @version $Rev$ $Date$
 */
public class ManagedBean<T> extends InjectionTargetBean<T> implements InterceptedMarker
{
    // just needed in BeansDeployer
    private InjectionTargetImpl<T> originalInjectionTarget; // don't do = null!

    public ManagedBean(WebBeansContext webBeansContext,
                       WebBeansType webBeansType,
                       AnnotatedType<T> annotated,
                       BeanAttributes<T> beanAttributes,
                       Class<T> beanClass)
    {
        super(webBeansContext, webBeansType, annotated, beanAttributes, beanClass);
    }

    public boolean valid()
    {
        return true;
    }

    public T create(CreationalContext<T> creationalContext)
    {
        if (!(creationalContext instanceof CreationalContextImpl))
        {
            creationalContext = webBeansContext.getCreationalContextFactory().wrappedCreationalContext(creationalContext, this);
        }
        CreationalContextImpl<T> creationalContextImpl = (CreationalContextImpl<T>)creationalContext;
        Bean<T> oldBean = creationalContextImpl.putBean(this);
        Contextual<T> oldContextual = creationalContextImpl.putContextual(this); // otherwise BeanMetaData is broken
        try
        {
            return super.create(creationalContext);
        }
        finally
        {
            creationalContextImpl.putBean(oldBean);
            creationalContextImpl.putContextual(oldContextual);
        }
    }

    public void setOriginalInjectionTarget(final InjectionTargetImpl<T> originalInjectionTarget)
    {
        this.originalInjectionTarget = originalInjectionTarget;
    }

    public InjectionTargetImpl<T> getOriginalInjectionTarget()
    {
        return originalInjectionTarget;
    }
}
