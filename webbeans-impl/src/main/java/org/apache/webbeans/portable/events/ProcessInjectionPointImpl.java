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

import org.apache.webbeans.config.WebBeansContext;

import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.ProcessInjectionPoint;
import javax.enterprise.inject.spi.configurator.InjectionPointConfigurator;

/**
 * Fired for every {@link InjectionPoint} of every Java EE component class supporting injection,
 * including every {@link javax.annotation.ManagedBean}, EJB session or message-driven bean, bean, interceptor or decorator.
 *
 * @param <T> bean class
 * @param <X> declared type
 */
public class ProcessInjectionPointImpl<T, X> extends EventBase implements ProcessInjectionPoint<T, X>
{

    private InjectionPoint injectionPoint;


    public ProcessInjectionPointImpl(InjectionPoint injectionPoint)
    {
        this.injectionPoint = injectionPoint;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public InjectionPoint getInjectionPoint()
    {
        checkState();
        return injectionPoint;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setInjectionPoint(InjectionPoint injectionPoint)
    {
        checkState();
        this.injectionPoint = injectionPoint;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addDefinitionError(Throwable t)
    {
        checkState();
        WebBeansContext.getInstance().getBeanManagerImpl().getErrorStack().pushError(t);
    }

    //X TODO OWB-1182 CDI 2.0
    @Override
    public InjectionPointConfigurator configureInjectionPoint()
    {
        throw new UnsupportedOperationException("CDI 2.0 not yet imlemented");
    }
}
