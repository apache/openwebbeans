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
package org.apache.webbeans.component.third;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;

import org.apache.webbeans.component.AbstractComponent;
import org.apache.webbeans.component.WebBeansType;

public class ThirdpartyBeanImpl<T> extends AbstractComponent<T>
{
    private Bean<T> bean = null;
    
    
    public ThirdpartyBeanImpl(Bean<T> bean)
    {
        super(WebBeansType.THIRDPARTY);
        
        this.bean = bean;
        
    }
    
    @Override
    public Set<Annotation> getBindings()
    {
        
        return bean.getBindings();
    }

    @Override
    public Class<? extends Annotation> getDeploymentType()
    {
        
        return bean.getDeploymentType();
    }

    @Override
    public Set<InjectionPoint> getInjectionPoints()
    {
        
        return bean.getInjectionPoints();
    }

    @Override
    public String getName()
    {
        
        return bean.getName();
    }

    @Override
    public Class<? extends Annotation> getScopeType()
    {
        
        return bean.getScopeType();
    }

    @Override
    public Set<Type> getTypes()
    {
        
        return bean.getTypes();
    }

    @Override
    public boolean isNullable()
    {
        
        return bean.isNullable();
    }

    @Override
    public boolean isSerializable()
    {
        
        return bean.isSerializable();
    }

    public T create(CreationalContext<T> context)
    {
        
        return bean.create(context);
    }

    public void destroy(T instance, CreationalContext<T> context)
    {
        bean.destroy(instance,context);
        
    }



    @Override
    protected T createInstance(CreationalContext<T> creationalContext)
    {
        throw new UnsupportedOperationException();
    }



    @Override
    protected void destroyInstance(T instance)
    {
        throw new UnsupportedOperationException();
        
    }

}
