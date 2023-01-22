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

import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.InjectionTarget;
import jakarta.enterprise.inject.spi.ProcessInjectionTarget;

import org.apache.webbeans.config.WebBeansContext;

/**
 * Implementation of the {@link ProcessInjectionTarget}.
 * 
 * @version $Rev$ $Date$
 *
 * @param <X> bean class info
 */
public class ProcessInjectionTargetImpl<X> extends EventBase implements ProcessInjectionTarget<X>
{
    /**Annotated type instance that is used by container to read meta-data*/
    private final AnnotatedType<X> annotatedType;
    
    /**Injection target that is used by container to inject dependencies*/
    private InjectionTarget<X> injectionTarget;
    
    /**Injection target is set or not*/
    private boolean set;
    
    /**
     * Creates a new instance.
     * 
     * @param injectionTarget injection target
     */
    public ProcessInjectionTargetImpl(InjectionTarget<X> injectionTarget, AnnotatedType<X> annotatedType)
    {
        this.injectionTarget = injectionTarget;
        this.annotatedType = annotatedType;
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

    @Override
    public AnnotatedType<X> getAnnotatedType()
    {
        checkState();
        return annotatedType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InjectionTarget<X> getInjectionTarget()
    {
        checkState();
        return injectionTarget;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setInjectionTarget(InjectionTarget<X> injectionTarget)
    {
        checkState();
        this.injectionTarget = injectionTarget;
        set = true;
    }

    /**
     * Returns whether or not injection target is set or not.
     * 
     * @return whether or not injection target is set
     */
    public boolean isSet()
    {
        return set;
    }
}
