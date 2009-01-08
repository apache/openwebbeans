/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.apache.webbeans.test.mock;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.webbeans.Observer;
import javax.webbeans.TypeLiteral;
import javax.webbeans.manager.Bean;
import javax.webbeans.manager.Context;
import javax.webbeans.manager.Decorator;
import javax.webbeans.manager.InterceptionType;
import javax.webbeans.manager.Interceptor;
import javax.webbeans.manager.Manager;

import org.apache.webbeans.component.AbstractComponent;
import org.apache.webbeans.container.ManagerImpl;

public class MockManager implements Manager
{
    private static MockManager instance = new MockManager();

    private static ManagerImpl manager = ManagerImpl.getManager();

    private List<AbstractComponent<?>> componentList = new ArrayList<AbstractComponent<?>>();

    private MockManager()
    {

    }

    public static MockManager getInstance()
    {
        return instance;
    }

    public void clear()
    {
        componentList.clear();
        manager.getBeans().clear();
    }

    public List<AbstractComponent<?>> getComponents()
    {
        return componentList;
    }

    public AbstractComponent<?> getComponent(int i)
    {
        return componentList.get(i);
    }

    public int getDeployedCompnents()
    {
        return manager.getBeans().size();
    }

    public Manager addBean(Bean<?> bean)
    {
        manager.addBean(bean);
        return this;
    }

    public Manager addContext(Context context)
    {
        return manager.addContext(context);
    }

    public Manager addDecorator(Decorator decorator)
    {
        return manager.addDecorator(decorator);
    }

    public Manager addInterceptor(Interceptor interceptor)
    {
        manager.addInterceptor(interceptor);
        return this;
    }

    public <T> Manager addObserver(Observer<T> observer, Class<T> eventType, Annotation... bindings)
    {
        manager.addObserver(observer, eventType, bindings);
        return this;
    }

    public <T> Manager addObserver(Observer<T> observer, TypeLiteral<T> eventType, Annotation... bindings)
    {
        manager.addObserver(observer, eventType, bindings);
        return this;
    }

    public void fireEvent(Object event, Annotation... bindings)
    {
        manager.fireEvent(event, bindings);

    }

    public Context getContext(Class<? extends Annotation> scopeType)
    {
        return manager.getContext(scopeType);
    }

    public <T> T getInstance(Bean<T> bean)
    {
        return manager.getInstance(bean);
    }

    public Object getInstanceByName(String name)
    {
        return manager.getInstanceByName(name);
    }

    public <T> T getInstanceByType(Class<T> type, Annotation... bindingTypes)
    {
        return manager.getInstanceByType(type, bindingTypes);
    }

    public <T> T getInstanceByType(TypeLiteral<T> type, Annotation... bindingTypes)
    {
        return manager.getInstanceByType(type, bindingTypes);
    }

    public <T> Manager removeObserver(Observer<T> observer, Class<T> eventType, Annotation... bindings)
    {
        return manager.removeObserver(observer, eventType, bindings);
    }

    public <T> Manager removeObserver(Observer<T> observer, TypeLiteral<T> eventType, Annotation... bindings)
    {
        return manager.removeObserver(observer, eventType, bindings);
    }

    public Set<Bean<?>> resolveByName(String name)
    {
        return manager.resolveByName(name);
    }

    public <T> Set<Bean<T>> resolveByType(Class<T> type, Annotation... bindings)
    {
        return manager.resolveByType(type, bindings);
    }

    public <T> Set<Bean<T>> resolveByType(TypeLiteral<T> apiType, Annotation... bindingTypes)
    {
        return manager.resolveByType(apiType, bindingTypes);
    }

    public List<Decorator> resolveDecorators(Set<Class<?>> types, Annotation... bindingTypes)
    {
        return manager.resolveDecorators(types, bindingTypes);
    }

    public List<Interceptor> resolveInterceptors(InterceptionType type, Annotation... interceptorBindings)
    {
        return manager.resolveInterceptors(type, interceptorBindings);
    }

    public <T> Set<Observer<T>> resolveObservers(T event, Annotation... bindings)
    {
        return manager.resolveObservers(event, bindings);
    }
}
