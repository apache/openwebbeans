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
import java.lang.reflect.ParameterizedType;
import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.util.TypeLiteral;
import javax.inject.Provider;

import org.apache.webbeans.inject.instance.InstanceFactory;

public class InstanceBean<T> extends AbstractOwbBean<Provider<T>>
{
    // TODO refactor. public static variables are uterly ugly
    public static ThreadLocal<InjectionPoint> local = new ThreadLocal<InjectionPoint>();
    
    @SuppressWarnings("serial")
    public InstanceBean()
    {
        super(WebBeansType.INSTANCE, new TypeLiteral<Provider<T>>(){}.getRawType());        
    }
    
         
    @Override
    protected Provider<T> createInstance(CreationalContext<Provider<T>> creationalContext)
    {
        try
        {
            ParameterizedType injectedType = (ParameterizedType)local.get().getType();
            Set<Annotation> qualifiers = local.get().getQualifiers();
            Instance<T> instance = InstanceFactory.getInstance(injectedType.getActualTypeArguments()[0],
                                                               qualifiers.toArray(new Annotation[qualifiers.size()]));
            
            return instance;
        }
        finally
        {
            local.set(null);
            local.remove();
        }
    }


    /* (non-Javadoc)
     * @see org.apache.webbeans.component.AbstractOwbBean#isPassivationCapable()
     */
    @Override
    public boolean isPassivationCapable()
    {
        return true;
    }
    
    
    
}