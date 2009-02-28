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
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.context.Context;
import javax.context.ContextNotActiveException;
import javax.context.CreationalContext;
import javax.event.Observer;
import javax.inject.AmbiguousDependencyException;
import javax.inject.TypeLiteral;
import javax.inject.manager.Bean;
import javax.inject.manager.Decorator;
import javax.inject.manager.InjectionPoint;
import javax.inject.manager.InterceptionType;
import javax.inject.manager.Interceptor;
import javax.inject.manager.Manager;
import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.Referenceable;
import javax.naming.StringRefAddr;

import org.apache.webbeans.component.AbstractComponent;
import org.apache.webbeans.config.WebBeansFinder;
import org.apache.webbeans.context.ContextFactory;
import org.apache.webbeans.context.creational.CreationalContextImpl;
import org.apache.webbeans.decorator.DecoratorComparator;
import org.apache.webbeans.decorator.WebBeansDecorator;
import org.apache.webbeans.decorator.WebBeansDecoratorConfig;
import org.apache.webbeans.event.NotificationManager;
import org.apache.webbeans.intercept.InterceptorComparator;
import org.apache.webbeans.intercept.WebBeansInterceptorConfig;
import org.apache.webbeans.intercept.webbeans.WebBeansInterceptor;
import org.apache.webbeans.proxy.JavassistProxyFactory;
import org.apache.webbeans.util.Asserts;
import org.apache.webbeans.util.ClassUtil;
import org.apache.webbeans.util.WebBeansUtil;
import org.apache.webbeans.xml.WebBeansXMLConfigurator;

/**
 * Implementation of the {@link WebBeansManager} contract of the web beans
 * container.
 * 
 * @since 1.0
 * @see java.webbeans.WebBeansManager
 */
@SuppressWarnings("unchecked")
public class ManagerImpl implements Manager, Referenceable
{
    private Map<Class<? extends Annotation>, List<Context>> contextMap = new ConcurrentHashMap<Class<? extends Annotation>, List<Context>>();

    private Set<Bean<?>> components = new CopyOnWriteArraySet<Bean<?>>();

    private Set<Interceptor> webBeansInterceptors = new CopyOnWriteArraySet<Interceptor>();

    private Set<Decorator> webBeansDecorators = new CopyOnWriteArraySet<Decorator>();

    private NotificationManager notificationManager = null;

    private InjectionResolver injectionResolver = null;

    private Map<Bean<?>, Object> proxyMap = Collections.synchronizedMap(new IdentityHashMap<Bean<?>, Object>());
    
    private WebBeansXMLConfigurator xmlConfigurator = null;

    public ManagerImpl()
    {
        injectionResolver = InjectionResolver.getInstance();
        notificationManager = NotificationManager.getInstance();
    }

    public static ManagerImpl getManager()
    {
        ManagerImpl instance = (ManagerImpl) WebBeansFinder.getSingletonInstance(WebBeansFinder.SINGLETON_MANAGER);

        return instance;
    }

    public void setXMLConfigurator(WebBeansXMLConfigurator xmlConfigurator)
    {
        if(this.xmlConfigurator != null)
        {
            throw new IllegalStateException("There is a WebBeansXMLConfigurator defined already");
        }
        
        this.xmlConfigurator = xmlConfigurator;
    }
    
    public Context getContext(Class<? extends Annotation> scopType)
    {
        Asserts.assertNotNull(scopType, "scopeType paramter can not be null");

        List<Context> contexts = new ArrayList<Context>();
        
        Context standardContext = null;

        standardContext = ContextFactory.getStandardContext(scopType);

        if(standardContext != null)
        {
            if(standardContext.isActive())
            {
                contexts.add(standardContext);   
            }
        }
        
        List<Context> others = this.contextMap.get(scopType);
        if(others != null)
        {
            for(Context otherContext : others)
            {
                if(otherContext.isActive())
                {
                    contexts.add(otherContext);
                }
            }
        }
        

        // Still null
        if (contexts.isEmpty())
        {
            throw new ContextNotActiveException("WebBeans context with scope type annotation @" + scopType.getSimpleName() + " does not exist within current thread");
        }
        
        else if(contexts.size() > 1)
        {
            throw new IllegalStateException("More than one active context exists with scope type annotation @" + scopType.getSimpleName());
        }

        return contexts.get(0);
    }

    public Manager addBean(Bean<?> component)
    {
        getManager().components.add(component);

        return this;
    }

    public Manager addContext(Context context)
    {
        addContext(context.getScopeType(), context);

        return this;

    }

    public void fireEvent(Object event, Annotation... bindings)
    {
        if (ClassUtil.isParametrized(event.getClass()))
        {
            throw new IllegalArgumentException("Event class : " + event.getClass().getName() + " can not be defined as generic type");
        }

        this.notificationManager.fireEvent(event, bindings);
    }

    public Object getInstanceByName(String name)
    {
        AbstractComponent<?> component = null;
        Object object = null;

        Set<Bean<?>> set = this.injectionResolver.implResolveByName(name);
        if (set.isEmpty())
        {
            return null;
        }

        if (set.size() > 1)
        {
            throw new AmbiguousDependencyException("There are more than one WebBeans with name : " + name);
        }

        component = (AbstractComponent<?>) set.iterator().next();

        object = getInstance(component);

        return object;
    }
    
    public <T> T getInstanceToInject(InjectionPoint injectionPoint, CreationalContext<T> context)
    {
        T instance = null;
        
        return instance;
    }
    
    public Object getInstanceToInject(InjectionPoint injectionPoint)
    {
        Object instance = null;
        
        return instance;
    }

    public <T> T getInstanceByType(Class<T> type, Annotation... bindingTypes)
    {
        ResolutionUtil.getInstanceByTypeConditions(bindingTypes);
        Set<Bean<T>> set = resolveByType(type, bindingTypes);

        ResolutionUtil.checkResolvedBeans(set, type);

        return getInstance(set.iterator().next());
    }

    public <T> T getInstanceByType(TypeLiteral<T> type, Annotation... bindingTypes)
    {
        ResolutionUtil.getInstanceByTypeConditions(bindingTypes);
        Set<Bean<T>> set = resolveByType(type, bindingTypes);

        ResolutionUtil.checkResolvedBeans(set, type.getRawType());

        return getInstance(set.iterator().next());
    }

    public Set<Bean<?>> resolveByName(String name)
    {
        return this.injectionResolver.implResolveByName(name);
    }

    public <T> Set<Bean<T>> resolveByType(Class<T> apiType, Annotation... bindingTypes)
    {
        ResolutionUtil.getInstanceByTypeConditions(bindingTypes);
        
        return this.injectionResolver.implResolveByType(apiType, new Type[0], bindingTypes);
    }

    public <T> Set<Bean<T>> resolveByType(TypeLiteral<T> apiType, Annotation... bindingTypes)
    {
        ParameterizedType ptype = (ParameterizedType) apiType.getType();
        ResolutionUtil.resolveByTypeConditions(ptype);

        ResolutionUtil.getInstanceByTypeConditions(bindingTypes);
        
        Type[] args = ptype.getActualTypeArguments();
        return this.injectionResolver.implResolveByType(apiType.getRawType(), args, bindingTypes);
    }

    public <T> Set<Observer<T>> resolveObservers(T event, Annotation... bindings)
    {
        return this.notificationManager.resolveObservers(event, bindings);
    }

    public Set<Bean<?>> getComponents()
    {
        return getManager().components;
    }

    public Manager addDecorator(Decorator decorator)
    {
        getManager().webBeansDecorators.add(decorator);
        return this;
    }

    public Manager addInterceptor(Interceptor interceptor)
    {
        getManager().webBeansInterceptors.add(interceptor);
        return this;
    }

    public <T> Manager addObserver(Observer<T> observer, Class<T> eventType, Annotation... bindings)
    {
        this.notificationManager.addObserver(observer, eventType, bindings);
        return this;
    }

    public <T> Manager addObserver(Observer<T> observer, TypeLiteral<T> eventType, Annotation... bindings)
    {
        this.notificationManager.addObserver(observer, eventType, bindings);
        return this;
    }

    public <T> T getInstance(Bean<T> bean)
    {
        Context context = null;
        T instance = null;

        try
        {
            ContextFactory.getDependentContext().setActive(true);

            /* @ScopeType is normal */
            if (WebBeansUtil.isScopeTypeNormal(bean.getScopeType()))
            {
                if (this.proxyMap.containsKey(bean))
                {
                    instance = (T) this.proxyMap.get(bean);
                }
                else
                {
                    instance = (T) JavassistProxyFactory.createNewProxyInstance(bean);

                    this.proxyMap.put(bean, instance);
                }
            }
            /* @ScopeType is not normal */
            else
            {
                context = getContext(bean.getScopeType());
                instance = context.get(bean, new CreationalContextImpl<T>());
            }

        }
        finally
        {
            ContextFactory.getDependentContext().setActive(false);
        }

        return instance;
    }

    public <T> Manager removeObserver(Observer<T> observer, Class<T> eventType, Annotation... bindings)
    {
        this.notificationManager.removeObserver(observer, eventType, bindings);
        return this;
    }

    public <T> Manager removeObserver(Observer<T> observer, TypeLiteral<T> eventType, Annotation... bindings)
    {
        this.notificationManager.removeObserver(observer, eventType, bindings);
        return this;
    }

    public List<Decorator> resolveDecorators(Set<Class<?>> types, Annotation... bindingTypes)
    {
        WebBeansUtil.checkDecoratorResolverParams(types, bindingTypes);
        Set<Decorator> intsSet = WebBeansDecoratorConfig.findDeployedWebBeansDecorator(types, bindingTypes);
        Iterator<Decorator> itSet = intsSet.iterator();

        List<Decorator> decoratorList = new ArrayList<Decorator>();
        while (itSet.hasNext())
        {
            WebBeansDecorator decorator = (WebBeansDecorator) itSet.next();
            decoratorList.add(decorator);

        }

        Collections.sort(decoratorList, new DecoratorComparator());

        return decoratorList;

    }

    public List<Interceptor> resolveInterceptors(InterceptionType type, Annotation... interceptorBindings)
    {
        WebBeansUtil.checkInterceptorResolverParams(interceptorBindings);

        Set<Interceptor> intsSet = WebBeansInterceptorConfig.findDeployedWebBeansInterceptor(interceptorBindings);
        Iterator<Interceptor> itSet = intsSet.iterator();

        List<Interceptor> interceptorList = new ArrayList<Interceptor>();
        while (itSet.hasNext())
        {
            WebBeansInterceptor interceptor = (WebBeansInterceptor) itSet.next();

            if (interceptor.getMethod(type) != null)
            {
                interceptorList.add(interceptor);
            }

        }

        Collections.sort(interceptorList, new InterceptorComparator());

        return interceptorList;
    }

    public Set<Bean<?>> getBeans()
    {
        return getManager().components;
    }

    public Set<Interceptor> getInterceptors()
    {
        return getManager().webBeansInterceptors;
    }

    public Set<Decorator> getDecorators()
    {
        return getManager().webBeansDecorators;
    }

    private void addContext(Class<? extends Annotation> scopeType, javax.context.Context context)
    {
        Asserts.assertNotNull(scopeType, "scopeType parameter can not be null");
        Asserts.assertNotNull(context, "context parameter can not be null");

        List<Context> contextList = this.contextMap.get(scopeType);
        
        if(contextList == null)
        {
            contextList = new CopyOnWriteArrayList<Context>();
            contextList.add(context);
            
            this.contextMap.put(scopeType, contextList);
        }
        else
        {
            if (context.isActive() && containsActiveContext(contextList))
            {
                throw new IllegalStateException("There is already an active Context registered for this scope! Context=" + context);
            }
            contextList.add(context);
        }

    }

    public Reference getReference() throws NamingException
    {
        return new Reference(ManagerImpl.class.getName(), new StringRefAddr("ManagerImpl", "ManagerImpl"), ManagerObjectFactory.class.getName(), null);
    }

    /**
     * Parse the given XML input stream for adding XML defined artifacts.
     * 
     * @param xmlStream beans xml definitions
     * @return {@link Manager} instance 
     */
    public Manager parse(InputStream xmlStream)
    {
        this.xmlConfigurator.configure(xmlStream);
        
        return this;
    }

    /**
     * Check if the given contextList contains an active Context
     * @param contextList
     * @return <code>true</code> if the given contextList contains an active Context, <code>false</code> otherwise
     */
    private boolean containsActiveContext(List<Context> contextList)
    {
        for (Context c : contextList)
        {
            if (c.isActive())
            {
                return true;
            }
        }
        return false;
    }
    
    public void clear()
    {
        this.components.clear();
        this.contextMap.clear();
        this.proxyMap.clear();
        this.webBeansDecorators.clear();
        this.webBeansInterceptors.clear();
    }
}