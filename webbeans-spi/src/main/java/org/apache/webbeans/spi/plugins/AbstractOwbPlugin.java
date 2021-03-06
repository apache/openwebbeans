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
package org.apache.webbeans.spi.plugins;


/**
 * Abstract implementation of the {@link OpenWebBeansPlugin} interface
 * contract.
 * 
 *  <p>
 *  This abstraction provides the empty implementation for the interface. If any
 *  subclass of this class wants to define customize method, it has to override related
 *  method definition.
 *  </p>
 */
public abstract class AbstractOwbPlugin implements OpenWebBeansPlugin
{
    protected AbstractOwbPlugin()
    {
        
    }

    /** {@inheritDoc} */
    @Override
    public void isManagedBean(Class<?> clazz)
    {
    }
    
    /** {@inheritDoc} */
    @Override
    public boolean supportsJavaEeComponentInjections(Class<?> targetClass)
    {        
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public void shutDown()
    {
    }

    /** {@inheritDoc} */
    @Override
    public void startUp()
    {
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean supportService(Class<?> serviceClass)
    {
        return false;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T getSupportedService(Class<T> serviceClass)
    {
        return null;
    }

}
