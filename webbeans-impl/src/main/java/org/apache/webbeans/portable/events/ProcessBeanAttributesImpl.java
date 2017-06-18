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
package org.apache.webbeans.portable.events;

import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.BeanAttributes;
import javax.enterprise.inject.spi.ProcessBeanAttributes;
import javax.enterprise.inject.spi.configurator.BeanAttributesConfigurator;

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.configurator.BeanAttributesConfiguratorImpl;

public class ProcessBeanAttributesImpl<T> extends EventBase implements ProcessBeanAttributes<T>
{
    private final WebBeansContext webBeansContext;
    private Annotated annotated;
    private BeanAttributes<T> attributes;
    private boolean veto;
    private Throwable definitionError;
    private boolean ignoreFinalMethods;
    private BeanAttributesConfiguratorImpl beanAttributesConfigurator;

    public ProcessBeanAttributesImpl(WebBeansContext webBeansContext, Annotated annotated, BeanAttributes<T> attributes)
    {
        this.webBeansContext = webBeansContext;
        this.annotated = annotated;
        this.attributes = attributes;
    }

    @Override
    public Annotated getAnnotated()
    {
        checkState();
        return annotated;
    }

    @Override
    public BeanAttributes<T> getBeanAttributes()
    {
        checkState();
        return attributes;
    }

    @Override
    public void setBeanAttributes(BeanAttributes<T> tBeanAttributes)
    {
        checkState();
        attributes = tBeanAttributes;
        beanAttributesConfigurator = null;
    }

    @Override
    public void veto()
    {
        checkState();
        veto = true;
    }

    @Override
    public void addDefinitionError(Throwable throwable)
    {
        checkState();
        definitionError = throwable;
    }

    @Override
    public void ignoreFinalMethods()
    {
        ignoreFinalMethods = true;
    }

    @Override
    public BeanAttributesConfigurator<T> configureBeanAttributes()
    {
        if (beanAttributesConfigurator == null)
        {
            beanAttributesConfigurator = new BeanAttributesConfiguratorImpl(webBeansContext, attributes);
        }

        return beanAttributesConfigurator;
    }

    public BeanAttributes<T> getAttributes()
    {
        if (beanAttributesConfigurator != null)
        {
            return beanAttributesConfigurator.getBeanAttributes();
        }
        return attributes;
    }

    public boolean isVeto()
    {
        return veto;
    }

    public boolean isIgnoreFinalMethods()
    {
        return ignoreFinalMethods;
    }

    public Throwable getDefinitionError()
    {
        return definitionError;
    }


}
