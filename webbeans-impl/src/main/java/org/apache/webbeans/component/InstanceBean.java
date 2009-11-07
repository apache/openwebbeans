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
package org.apache.webbeans.component;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.util.TypeLiteral;

import org.apache.webbeans.inject.instance.InstanceFactory;

public class InstanceBean<T> extends AbstractBean<Instance<T>>
{
    public static ThreadLocal<InjectionPoint> local = new ThreadLocal<InjectionPoint>();
    
    public InstanceBean()
    {
        super(WebBeansType.INSTANCE, new TypeLiteral<Instance<T>>(){}.getRawType());        
    }
    
         
    @Override
    protected Instance<T> createInstance(CreationalContext<Instance<T>> creationalContext)
    {
        try
        {
            ParameterizedType injectedType = (ParameterizedType)local.get().getType();
            Instance<T> instance = InstanceFactory.getInstance(injectedType.getActualTypeArguments()[0], local.get().getQualifiers().toArray(new Annotation[0])); 
            
            return instance;
        }
        finally
        {
            local.set(null);
        }
    }
    
}