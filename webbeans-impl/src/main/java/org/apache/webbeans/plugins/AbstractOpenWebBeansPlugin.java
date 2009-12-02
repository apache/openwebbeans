/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.webbeans.plugins;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.enterprise.inject.spi.Bean;

import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.exception.WebBeansPassivationException;

/**
 * Abstract imlpementation of the {@link OpenWebBeansPlugin} interface
 * contract.
 * 
 *  <p>
 *  This abstraction provides the empty implementation for the interface. If any
 *  subclass of this class wants to define customize method, it has to override related
 *  method definition.
 *  </p>
 */
public abstract class AbstractOpenWebBeansPlugin implements OpenWebBeansPlugin
{
    protected AbstractOpenWebBeansPlugin()
    {
        
    }

    /** @{inheritDoc} */
    public void checkForValidResources(Type type, Class<?> clazz, String name, Annotation[] annotations)
    {
        
        
    }

    /** @{inheritDoc} */
    public Object injectResource(Type type, Annotation[] annotations)
    {
        return null;
    }

    /** @{inheritDoc} */
    public boolean isResourceAnnotation(Class<? extends Annotation> annotationClass)
    {
        return false;
    }

    /** @{inheritDoc} */
    public boolean isPassivationCapable(Bean<?> component) throws WebBeansPassivationException
    {
        return false;
    }
    
    /** @{inheritDoc} */
    public void isManagedBean(Class<?> clazz) throws WebBeansConfigurationException
    {
        
    }

    /** @{inheritDoc} */
    public void shutDown() throws WebBeansConfigurationException
    {
        
    }

    /** @{inheritDoc} */
    public void startUp() throws WebBeansConfigurationException
    {
        
    }


}
