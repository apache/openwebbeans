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
package org.apache.webbeans.container;

import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;

import javax.context.Context;
import javax.context.ContextNotActiveException;
import javax.context.CreationalContext;
import javax.event.Observer;
import javax.inject.TypeLiteral;
import javax.inject.manager.Bean;
import javax.inject.manager.Decorator;
import javax.inject.manager.InjectionPoint;
import javax.inject.manager.InterceptionType;
import javax.inject.manager.Interceptor;
import javax.inject.manager.Manager;

/**
 * Each Manager can start a new child activity as defined by the JSR-299 spec chapter 11.6
 * 
 * A ChildActivityManager tries to handle the beans, contexts, ... itself 
 * and delegates all other requests to it's parent Manager. 
 *
 */
public class ChildActivityManager implements Manager
{

    /**
     * The parent Manager this child is depending from.
     */
    private Manager parent;
    
    /**
     * All beans added to this instance will be tracked by this very Manager
     */
    private Manager self;
    
    /**
     * the ct will be called by {@code Manager#createActivity()}
     * @param parent is the Manager calling this ct
     */
    public ChildActivityManager(Manager parent)
    {
        this.parent = parent;
        this.self = new ManagerImpl();
    }
    
    /** {@inheritDoc} */
    public Manager addBean(Bean<?> bean)
    {
        //X TODO possibly add checks
        return self.addBean(bean);
    }

    /** {@inheritDoc} */
    public Manager addContext(Context context)
    {
        //X TODO possibly add checks
        return self.addContext(context);
    }

    /** {@inheritDoc} */
    public Manager addDecorator(Decorator decorator)
    {
        throw new UnsupportedOperationException("Decorators may not be registered with a child activity.");
    }

    /** {@inheritDoc} */
    public Manager addInterceptor(Interceptor interceptor)
    {
        throw new UnsupportedOperationException("Interceptors may not be registered with a child activity.");
    }

    /** {@inheritDoc} */
    public <T> Manager addObserver(Observer<T> observer, Class<T> eventType, Annotation... bindings)
    {
        //X TODO possibly add checks
        return self.addObserver(observer, eventType, bindings);
    }

    /** {@inheritDoc} */
    public <T> Manager addObserver(Observer<T> observer, TypeLiteral<T> eventType, Annotation... bindings)
    {
        //X TODO possibly add checks
        return self.addObserver(observer, eventType, bindings);
    }

    /** {@inheritDoc} */
    public Manager createActivity()
    {
        return new ChildActivityManager(this);
    }

    /** {@inheritDoc} */
    public void fireEvent(Object event, Annotation... bindings)
    {
        // TODO Auto-generated method stub

    }

    /** {@inheritDoc} */
    public Context getContext(Class<? extends Annotation> scopeType)
    {
        Context ctx = null;
        //X TODO not 100% sure if this is ok. 'double-definition' case isn't defined by the spec yet!
        try 
        {
            ctx = self.getContext(scopeType);
        }
        catch (ContextNotActiveException cna)
        {
            //dumdidum nothing found, so let's try it in the parent context
        }
        if (ctx == null)
        {
            ctx = parent.getContext(scopeType);
        }
        return ctx;
    }

    /** {@inheritDoc} */
    public <T> T getInstance(Bean<T> bean)
    {
        //X TODO not 100% sure if this is ok. There is some 'double-definition' exception case defined in the spec ...
        T obj = self.getInstance(bean);
        if (obj == null) 
        {
            obj = parent.getInstance(bean);
        }
        return obj;
    }

    /** {@inheritDoc} */
    public Object getInstanceByName(String name)
    {
        //X TODO not 100% sure if this is ok. There is some 'double-definition' exception case defined in the spec ...
        Object obj = self.getInstanceByName(name);
        if (obj == null) 
        {
            obj = parent.getInstanceByName(name);
        }
        return obj;
    }

    /** {@inheritDoc} */
    public <T> T getInstanceByType(Class<T> type, Annotation... bindingTypes)
    {
        //X TODO not 100% sure if this is ok. There is some 'double-definition' exception case defined in the spec ...
        T obj = self.getInstanceByType(type, bindingTypes);
        if (obj == null) 
        {
            obj = parent.getInstanceByType(type, bindingTypes);
        }
        return obj;
    }

    /** {@inheritDoc} */
    public <T> T getInstanceByType(TypeLiteral<T> type, Annotation... bindingTypes)
    {
        //X TODO not 100% sure if this is ok. There is some 'double-definition' exception case defined in the spec ...
        T obj = self.getInstanceByType(type, bindingTypes);
        if (obj == null) 
        {
            obj = parent.getInstanceByType(type, bindingTypes);
        }
        return obj;
    }

    /** {@inheritDoc} */
    public <T> T getInstanceToInject(InjectionPoint injectionPoint, CreationalContext<?> context)
    {
        //X TODO not 100% sure if this is ok. There is some 'double-definition' exception case defined in the spec ...
        T obj = self.<T>getInstanceToInject(injectionPoint, context); //X ugly <T> due to javac bug 6302954
        if (obj == null) 
        {
            obj = parent.<T>getInstanceToInject(injectionPoint, context); //X ugly <T> due to javac bug 6302954
        }
        return obj;
    }

    /** {@inheritDoc} */
    public <T> T getInstanceToInject(InjectionPoint injectionPoint)
    {
        //X TODO not 100% sure if this is ok. There is some 'double-definition' exception case defined in the spec ...
        T obj = self.<T>getInstanceToInject(injectionPoint); //X ugly <T> due to javac bug 6302954
        if (obj == null) 
        {
            obj = parent.<T>getInstanceToInject(injectionPoint); //X ugly <T> due to javac bug 6302954
        }
        return obj;
    }

    /** {@inheritDoc} */
    public Manager parse(InputStream xmlStream)
    {
        self.parse(xmlStream);
        return this;
    }

    /** {@inheritDoc} */
    public <T> Manager removeObserver(Observer<T> observer, Class<T> eventType, Annotation... bindings)
    {
        //X TODO check if the user tries to remove an Observer from the parent -> Exception
        self.removeObserver(observer, eventType, bindings);
        return this;
    }

    /** {@inheritDoc} */
    public <T> Manager removeObserver(Observer<T> observer, TypeLiteral<T> eventType, Annotation... bindings)
    {
        //X TODO check if the user tries to remove an Observer from the parent -> Exception
        self.removeObserver(observer, eventType, bindings);
        return this;
    }

    /** {@inheritDoc} */
    public Set<Bean<?>> resolveByName(String name)
    {
        //X TODO not 100% sure if this is ok. There is some 'double-definition' exception case defined in the spec ...
        Set<Bean<?>> set = self.resolveByName(name);
        if (set == null || set.isEmpty()) 
        {
            set = parent.resolveByName(name);
        }
        return set;
    }

    /** {@inheritDoc} */
    public <T> Set<Bean<T>> resolveByType(Class<T> type, Annotation... bindings)
    {
        //X TODO not 100% sure if this is ok. There is some 'double-definition' exception case defined in the spec ...
        Set<Bean<T>> set = self.resolveByType(type, bindings);
        if (set == null || set.isEmpty()) 
        {
            set = parent.resolveByType(type, bindings);
        }
        return set;
    }

    /** {@inheritDoc} */
    public <T> Set<Bean<T>> resolveByType(TypeLiteral<T> apiType, Annotation... bindingTypes)
    {
        //X TODO not 100% sure if this is ok. There is some 'double-definition' exception case defined in the spec ...
        Set<Bean<T>> set = self.resolveByType(apiType, bindingTypes);
        if (set == null || set.isEmpty()) 
        {
            set = parent.resolveByType(apiType, bindingTypes);
        }
        return set;
    }

    /** {@inheritDoc} */
    public List<Decorator> resolveDecorators(Set<Type> types, Annotation... bindingTypes)
    {
        throw new UnsupportedOperationException("Decorators may not be registered with a child activity.");    
    }

    /** {@inheritDoc} */
    public List<Interceptor> resolveInterceptors(InterceptionType type, Annotation... interceptorBindings)
    {
        throw new UnsupportedOperationException("Interceptors may not be registered with a child activity.");    
    }

    /** {@inheritDoc} */
    public <T> Set<Observer<T>> resolveObservers(T event, Annotation... bindings)
    {
        //X TODO not 100% sure if this is ok. There is some 'double-definition' exception case defined in the spec ...
        Set<Observer<T>> set = self.resolveObservers(event, bindings);
        if (set == null || set.isEmpty()) 
        {
            set = parent.resolveObservers(event, bindings);
        }
        return set;
    }

    /** {@inheritDoc} */
    public Manager setCurrent(Class<? extends Annotation> scopeType)
    {
        //X TODO what about parent? we must not set the current scope type for parents, but what are the implications?
        self.setCurrent(scopeType);
        return this;
    }
}
