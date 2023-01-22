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

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.exception.WebBeansException;
import org.apache.webbeans.spi.ResourceInjectionService;
import org.apache.webbeans.spi.api.ResourceReference;

import jakarta.inject.Provider;
import java.io.ObjectStreamException;
import java.io.Serializable;

public class ResourceProvider<T> implements Provider<T>, Serializable
{
    
    private ResourceReference<T, ?> resourceReference;
    private transient WebBeansContext webBeansContext;

    public ResourceProvider(ResourceReference<T, ?> resourceReference, WebBeansContext webBeansContext)
    {
        this.resourceReference = resourceReference;
        this.webBeansContext = webBeansContext;
    }
    
    @Override
    public T get()
    {
        if (webBeansContext == null)
        {
            webBeansContext = WebBeansContext.currentInstance();
        }

        try
        {
            ResourceInjectionService resourceService = webBeansContext.getService(ResourceInjectionService.class);
            return resourceService.getResourceReference(resourceReference);
        }
        catch (Exception e)
        {
            throw new WebBeansException(e);
        }
    }

    Object readResolve() throws ObjectStreamException
    {
        return get();
    }
}
