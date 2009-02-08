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
import java.lang.reflect.Type;

import javax.context.CreationalContext;
import javax.inject.Instance;

import org.apache.webbeans.inject.instance.InstanceFactory;

public class InstanceComponentImpl<T> extends AbstractComponent<Instance<T>>
{
    private Class<T> injectedType;
    
    private Type[] actualTypeArguments = new Type[0];
    
    public InstanceComponentImpl(Class<Instance<T>> returnType, Type injectedType)
    {
        super(WebBeansType.INSTANCE, returnType);
        configureInjectionClazz(injectedType);
    }
    
    @SuppressWarnings("unchecked")
    private void configureInjectionClazz(Type type)
    {
        if(type instanceof ParameterizedType)
        {
            ParameterizedType pt = (ParameterizedType)type;
            this.injectedType = (Class<T>)pt.getRawType();
            
            this.actualTypeArguments = pt.getActualTypeArguments();
        }
        else
        {
            this.injectedType = (Class<T>)type;
        }
    }

    @Override
    protected Instance<T> createInstance(CreationalContext<Instance<T>> creationalContext)
    {
        Annotation[] anns = new Annotation[getBindings().size()];
        anns = getBindings().toArray(anns);
        
        return InstanceFactory.getInstance(this.injectedType, this.actualTypeArguments, anns);
    }

    @Override
    protected void destroyInstance(Instance<T> instance)
    {
        
    }

}
