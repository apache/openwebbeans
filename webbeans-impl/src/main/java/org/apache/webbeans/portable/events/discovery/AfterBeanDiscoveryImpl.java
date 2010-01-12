/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.apache.webbeans.portable.events.discovery;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.spi.Context;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.ObserverMethod;

import org.apache.webbeans.container.BeanManagerImpl;

/**
 * Event that is fired by the container after it discovers beans.
 * 
 * @version $Rev$ $Date$
 *
 */
public class AfterBeanDiscoveryImpl implements AfterBeanDiscovery
{
    private BeanManagerImpl beanManager = null;
    
    private List<Throwable> errors = new ArrayList<Throwable>();
    
    public AfterBeanDiscoveryImpl()
    {
        this.beanManager = BeanManagerImpl.getManager();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void addBean(Bean<?> bean)
    {
        this.beanManager.addBean(bean);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addContext(Context context)
    {
        this.beanManager.addContext(context);
        
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addDefinitionError(Throwable t)
    {
        // TODO and where do we evaluate and log those errors? ;)
        // -> OWB-227
        this.errors.add(t);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addObserverMethod(ObserverMethod<?> observerMethod)
    {
        //TODO 
        System.out.println("Not implemented Yet!");
    }

}