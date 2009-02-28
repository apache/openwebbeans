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
package org.apache.webbeans.test.tck.mock;

import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Set;

import javax.context.Context;
import javax.context.CreationalContext;
import javax.event.Observer;
import javax.inject.TypeLiteral;
import javax.inject.manager.Bean;
import javax.inject.manager.Decorator;
import javax.inject.manager.InjectionPoint;
import javax.inject.manager.InterceptionType;
import javax.inject.manager.Interceptor;
import javax.inject.manager.Manager;

import org.apache.webbeans.container.ManagerImpl;

public class TCKManager implements Manager
{
    private static TCKManager instance = new TCKManager();
    private ManagerImpl manager = null;
    
    private TCKManager()
    {
        this.manager = ManagerImpl.getManager();
    }
    
    public static TCKManager getInstance()
    {
        return instance;
    }

    public Manager addBean(Bean<?> bean)
    {
        return this.manager.addBean(bean);
    }

    public Manager addContext(Context context)
    {
        
        return this.manager.addContext(context);
    }

    public Manager addDecorator(Decorator decorator)
    {
        
        return this.manager.addDecorator(decorator);
    }

    public Manager addInterceptor(Interceptor interceptor)
    {
        
        return this.manager.addInterceptor(interceptor);
    }

    public <T> Manager addObserver(Observer<T> observer, Class<T> eventType, Annotation... bindings)
    {
        
        return this.manager.addObserver(observer, eventType, bindings);
    }

    public <T> Manager addObserver(Observer<T> observer, TypeLiteral<T> eventType, Annotation... bindings)
    {
        
        return this.manager.addObserver(observer, eventType, bindings);
    }

    public void fireEvent(Object event, Annotation... bindings)
    {
        this.manager.fireEvent(event, bindings);
        
    }

    public Context getContext(Class<? extends Annotation> scopeType)
    {
        
        return this.manager.getContext(scopeType);
    }

    public <T> T getInstance(Bean<T> bean)
    {
        
        return this.manager.getInstance(bean);
    }

    public Object getInstanceByName(String name)
    {
        
        return this.manager.getInstanceByName(name);
    }

    public <T> T getInstanceByType(Class<T> type, Annotation... bindingTypes)
    {
        
        return this.manager.getInstanceByType(type, bindingTypes);
    }

    public <T> T getInstanceByType(TypeLiteral<T> type, Annotation... bindingTypes)
    {
        
        return this.manager.getInstanceByType(type, bindingTypes);
    }

    public <T> T getInstanceToInject(InjectionPoint injectionPoint, CreationalContext<T> context)
    {
        
        return this.manager.getInstanceToInject(injectionPoint, context);
    }

    public Object getInstanceToInject(InjectionPoint injectionPoint)
    {
        
        return this.manager.getInstanceToInject(injectionPoint);
    }

    public Manager parse(InputStream xmlStream)
    {
        
        return this.manager.parse(xmlStream);
    }

    public <T> Manager removeObserver(Observer<T> observer, Class<T> eventType, Annotation... bindings)
    {
        
        return this.manager.removeObserver(observer, eventType, bindings);
    }

    public <T> Manager removeObserver(Observer<T> observer, TypeLiteral<T> eventType, Annotation... bindings)
    {
        
        return this.manager.removeObserver(observer, eventType, bindings);
    }

    public Set<Bean<?>> resolveByName(String name)
    {
        
        return this.manager.resolveByName(name);
    }

    public <T> Set<Bean<T>> resolveByType(Class<T> type, Annotation... bindings)
    {
        
        return this.manager.resolveByType(type, bindings);
    }

    public <T> Set<Bean<T>> resolveByType(TypeLiteral<T> apiType, Annotation... bindingTypes)
    {
        
        return this.manager.resolveByType(apiType, bindingTypes);
    }

    public List<Decorator> resolveDecorators(Set<Class<?>> types, Annotation... bindingTypes)
    {
        
        return this.manager.resolveDecorators(types, bindingTypes);
    }

    public List<Interceptor> resolveInterceptors(InterceptionType type, Annotation... interceptorBindings)
    {
        
        return this.manager.resolveInterceptors(type, interceptorBindings);
    }

    public <T> Set<Observer<T>> resolveObservers(T event, Annotation... bindings)
    {
        
        return this.manager.resolveObservers(event, bindings);
    }

    public void clear()
    {
        this.manager.clear();
    }
}
