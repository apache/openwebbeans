/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.webbeans.component;

import java.lang.annotation.Annotation;

import javassist.util.proxy.ProxyFactory;

import javax.enterprise.context.spi.CreationalContext;

import org.apache.webbeans.api.ResourceReference;
import org.apache.webbeans.corespi.ServiceLoader;
import org.apache.webbeans.exception.WebBeansException;
import org.apache.webbeans.proxy.JavassistProxyFactory;
import org.apache.webbeans.proxy.ResourceProxyHandler;
import org.apache.webbeans.spi.ResourceInjectionService;

public class ResourceBean<X, T extends Annotation> extends ProducerFieldBean<X>
{
    private X actualResourceReference = null;
    
    private ResourceReference<X,T> resourceReference = null;
    
    public ResourceBean(Class<X> returnType, InjectionTargetBean<?> ownerBean, ResourceReference<X, T> resourceReference)
    {
        super(ownerBean, returnType);
        this.resourceReference = resourceReference;
    }

     
    
    @Override
    @SuppressWarnings("unchecked")
    protected X createInstance(CreationalContext<X> creationalContext)
    {
        X instance = null;
        try
        {
            //X TODO cache proxy class!
            ProxyFactory proxyFactory = JavassistProxyFactory.createProxyFactory(this);
            
            ResourceInjectionService resourceService = ServiceLoader.getService(ResourceInjectionService.class);
            this.actualResourceReference = resourceService.getResourceReference(this.resourceReference);
            proxyFactory.setHandler(new ResourceProxyHandler(this.actualResourceReference));
            
            instance = (X)(JavassistProxyFactory.getProxyClass(proxyFactory).newInstance());
        }
        catch (Exception e)
        {
            throw new WebBeansException(e);
        }
        
        return instance;
    }

    @Override
    protected void destroyInstance(X instance, CreationalContext<X> creationalContext)
    {
        this.actualResourceReference = null;
    }


    public boolean isPassivationCapable()
    {
        return true;
    }
}
