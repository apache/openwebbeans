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

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Set;

import org.apache.webbeans.spi.ResourceInjectionService;
import org.apache.webbeans.spi.api.ResourceReference;

public class ResourceBean<X, T extends Annotation> extends ProducerFieldBean<X>
{
    
    private ResourceReference<X,T> resourceReference = null;

    public ResourceBean(InjectionTargetBean<?> ownerComponent,
                        ResourceReference<X, T> resourceReference,
                        Set<Type> types,
                        Set<Annotation> qualifiers,
                        Class<? extends Annotation> scope,
                        String name,
                        boolean nullable,
                        Class<X> beanClass,
                        Set<Class<? extends Annotation>> stereotypes,
                        boolean alternative)
    {
        super(ownerComponent, types, qualifiers, scope, name, nullable, beanClass, stereotypes, alternative);
        this.resourceReference = resourceReference;
    }
    
    public ResourceReference<X, T> getReference()
    {
        return resourceReference;
    }
 
    /**
     * Called after deserialization to get a new instance for some type of resource bean instance that are
     * not serializable.
     * 
     * @return a new instance of this resource bean.
     */
    public X getActualInstance() 
    {
        ResourceInjectionService resourceService = getWebBeansContext().getService(ResourceInjectionService.class);
        X instance = resourceService.getResourceReference(resourceReference);
        return instance;
    }
    
    public boolean isPassivationCapable()
    {
        return true;
    }
}
