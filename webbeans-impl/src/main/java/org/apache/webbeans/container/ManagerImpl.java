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

import javax.enterprise.context.ContextNotActiveException;
import javax.enterprise.context.spi.Context;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Observer;
import javax.enterprise.inject.AmbiguousResolutionException;
import javax.enterprise.inject.TypeLiteral;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.Decorator;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InterceptionType;
import javax.enterprise.inject.spi.Interceptor;
import javax.enterprise.inject.spi.BeanManager;
import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.Referenceable;
import javax.naming.StringRefAddr;

import org.apache.webbeans.component.AbstractComponent;
import org.apache.webbeans.component.third.ThirdpartyBeanImpl;
import org.apache.webbeans.container.activity.ActivityManager;
import org.apache.webbeans.context.ContextFactory;
import org.apache.webbeans.context.creational.CreationalContextFactory;
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
public class ManagerImpl implements BeanManager, Referenceable
{
    /**Holds the context with key scope*/
    private static Map<Class<? extends Annotation>, List<Context>> contextMap = new ConcurrentHashMap<Class<? extends Annotation>, List<Context>>();

    /**Activity webbeans components*/
    private Set<Bean<?>> components = new CopyOnWriteArraySet<Bean<?>>();

    /**Activity interceptors*/
    private Set<Interceptor<?>> webBeansInterceptors = new CopyOnWriteArraySet<Interceptor<?>>();

    /**Activity decorators*/
    private Set<Decorator<?>> webBeansDecorators = new CopyOnWriteArraySet<Decorator<?>>();

    /**Event notification manager instance*/
    private NotificationManager notificationManager = null;

    /**Injection resolver instance*/
    private InjectionResolver injectionResolver = null;

    /**Proxy map for the webbeans components*/
    private Map<Bean<?>, Object> proxyMap = Collections.synchronizedMap(new IdentityHashMap<Bean<?>, Object>());
    
    /**XML configurator instance*/
    private WebBeansXMLConfigurator xmlConfigurator = null;
    
    /**
     * The parent Manager this child is depending from.
     */
    private ManagerImpl parent;
    
    /**
     * Creates a new {@link BeanManager} instance.
     * Called by the system. Do not use outside of the
     * system.
     */
    public ManagerImpl()
    {
        injectionResolver = new InjectionResolver(this);
        notificationManager = new NotificationManager();
    }    
    
    public ManagerImpl getParent()
    {
        return this.parent;
    }
    
    public synchronized void setParent(ManagerImpl parent)
    {
       this.parent = parent;
    }
    
    
    /**
     * Return manager notification manager.
     * 
     * @return notification manager
     */
    public NotificationManager getNotificationManager()
    {
        return this.notificationManager;
    }
    
    /**
     * Gets injection resolver.
     * 
     * @return injection resolver
     */
    public InjectionResolver getInjectionResolver()
    {
        return this.injectionResolver;
    }

    /**
     * Gets current activity.
     * 
     * @return the current activity
     */
    public static ManagerImpl getManager()
    {
        ActivityManager activityManager = ActivityManager.getInstance();
        
        ManagerImpl currentManager = activityManager.getCurrentActivity();
        
        return currentManager;
    }

    
    /**
     * Sets the xml configurator instance.
     * 
     * @param xmlConfigurator set xml configurator instance.
     * @see WebBeansXMLConfigurator
     */
    public synchronized void setXMLConfigurator(WebBeansXMLConfigurator xmlConfigurator)
    {
        if(this.xmlConfigurator != null)
        {
            throw new IllegalStateException("WebBeansXMLConfigurator is already defined!");
        }
        
        this.xmlConfigurator = xmlConfigurator;
    }
    
    /**
     * Gets the active context for the given scope type.
     * 
     * @param scopeType scope type of the context
     * @throws ContextNotActiveException if no active context
     * @throws IllegalStateException if more than one active context
     */
    public Context getContext(Class<? extends Annotation> scopeType)
    {
        Asserts.assertNotNull(scopeType, "scopeType paramter can not be null");

        List<Context> contexts = new ArrayList<Context>();
        
        Context standardContext = null;

        standardContext = ContextFactory.getStandardContext(scopeType);

        if(standardContext != null)
        {
            if(standardContext.isActive())
            {
                contexts.add(standardContext);   
            }
        }
        
        List<Context> others = ManagerImpl.contextMap.get(scopeType);
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
            throw new ContextNotActiveException("WebBeans context with scope type annotation @" + scopeType.getSimpleName() + " does not exist within current thread");
        }
        
        else if(contexts.size() > 1)
        {
            throw new IllegalStateException("More than one active context exists with scope type annotation @" + scopeType.getSimpleName());
        }

        return contexts.get(0);
    }

    /**
     * Add new webbeans component to the activity.
     * 
     * @param component new webbeans component
     * @return the this activity
     */
    public BeanManager addBean(Bean<?> component)
    {
        if(component instanceof AbstractComponent)
        {
            this.components.add(component);    
        }
        else
        {
            ThirdpartyBeanImpl<?> bean = new ThirdpartyBeanImpl(component);
            this.components.add(bean);
        }
        

        return this;
    }

    public BeanManager addContext(Context context)
    {
        addContext(context.getScopeType(), ContextFactory.getCustomContext(context));

        return this;

    }
    
    
    public void fireEvent(Object event, Annotation... bindings)
    {
        if (ClassUtil.isDefinitionConstainsTypeVariables(event.getClass()))
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
            throw new AmbiguousResolutionException("There are more than one WebBeans with name : " + name);
        }

        component = (AbstractComponent<?>) set.iterator().next();

        object = getInstance(component);

        return object;
    }
    
    public <T> T getInstanceToInject(InjectionPoint injectionPoint, CreationalContext<?> context)
    {
        T instance = null;
        
        if(injectionPoint == null)
        {
            return null;
        }
                
        Annotation[] bindings = new Annotation[injectionPoint.getBindings().size()];
        bindings = injectionPoint.getBindings().toArray(bindings);
        
        //Find the injection point Bean
        Bean<?> bean = injectionResolver.getInjectionPointBean(injectionPoint);
        
        if(context != null && (context instanceof CreationalContextImpl))
        {
            CreationalContextImpl<T> creationalContext = (CreationalContextImpl<T>)context;
            
            instance = (T)creationalContext.get(bean);
            
        }
        
        if(instance == null)
        {
            instance = (T) getInstance(bean);
        }
        
        return instance;
    }
    
    public Object getInstanceToInject(InjectionPoint injectionPoint)
    {        
        return getInstanceToInject(injectionPoint, null);
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
        
        return this.injectionResolver.implResolveByType(apiType, bindingTypes);
    }

    public <T> Set<Bean<T>> resolveByType(TypeLiteral<T> apiType, Annotation... bindingTypes)
    {
        ParameterizedType ptype = (ParameterizedType) apiType.getType();
        ResolutionUtil.resolveByTypeConditions(ptype);

        ResolutionUtil.getInstanceByTypeConditions(bindingTypes);
       
        return this.injectionResolver.implResolveByType(apiType.getType(), bindingTypes);
    }

    public <T> Set<Observer<T>> resolveObservers(T event, Annotation... bindings)
    {
        return this.notificationManager.resolveObservers(event, bindings);
    }

    public Set<Bean<?>> getComponents()
    {
        return getManager().components;
    }

    public BeanManager addDecorator(Decorator decorator)
    {
        getManager().webBeansDecorators.add(decorator);
        return this;
    }

    public BeanManager addInterceptor(Interceptor interceptor)
    {
        getManager().webBeansInterceptors.add(interceptor);
        return this;
    }

    public <T> BeanManager addObserver(Observer<T> observer, Class<T> eventType, Annotation... bindings)
    {
        this.notificationManager.addObserver(observer, eventType, bindings);
        return this;
    }

    public <T> BeanManager addObserver(Observer<T> observer, TypeLiteral<T> eventType, Annotation... bindings)
    {
        this.notificationManager.addObserver(observer, eventType, bindings);
        return this;
    }

    public <T> T getInstance(Bean<T> bean)
    {
        Context context = null;
        T instance = null;
        boolean dependentContext = false;
        try
        {
            if(!ContextFactory.checkDependentContextActive())
            {
                ContextFactory.activateDependentContext();
                dependentContext = true;
            }            

            CreationalContext<T> creationalContext = CreationalContextFactory.getInstance().getCreationalContext(bean);
            
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
                
                //Push proxy instance into the creational context
                creationalContext.push(instance);
                
            }
            /* @ScopeType is not normal */
            else
            {
                context = getContext(bean.getScopeType());
                instance = (T)context.get(bean, creationalContext);                                
            }

        }
        finally
        {
            if(dependentContext)
            {
                ContextFactory.passivateDependentContext();
            }
        }

        return instance;
    }

    public <T> BeanManager removeObserver(Observer<T> observer, Class<T> eventType, Annotation... bindings)
    {
        this.notificationManager.removeObserver(observer, eventType, bindings);
        return this;
    }

    public <T> BeanManager removeObserver(Observer<T> observer, TypeLiteral<T> eventType, Annotation... bindings)
    {
        this.notificationManager.removeObserver(observer, eventType, bindings);
        return this;
    }

    public List<Decorator<?>> resolveDecorators(Set<Type> types, Annotation... bindingTypes)
    {
        WebBeansUtil.checkDecoratorResolverParams(types, bindingTypes);
        Set<Decorator<?>> intsSet = WebBeansDecoratorConfig.findDeployedWebBeansDecorator(types, bindingTypes);
        Iterator<Decorator<?>> itSet = intsSet.iterator();

        List<Decorator<?>> decoratorList = new ArrayList<Decorator<?>>();
        while (itSet.hasNext())
        {
            WebBeansDecorator decorator = (WebBeansDecorator) itSet.next();
            decoratorList.add(decorator);

        }

        Collections.sort(decoratorList, new DecoratorComparator());

        return decoratorList;

    }

    public List<Interceptor<?>> resolveInterceptors(InterceptionType type, Annotation... interceptorBindings)
    {
        WebBeansUtil.checkInterceptorResolverParams(interceptorBindings);

        Set<Interceptor<?>> intsSet = WebBeansInterceptorConfig.findDeployedWebBeansInterceptor(interceptorBindings);
        Iterator<Interceptor<?>> itSet = intsSet.iterator();

        List<Interceptor<?>> interceptorList = new ArrayList<Interceptor<?>>();
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
        return this.components;
    }

    public Set<Interceptor<?>> getInterceptors()
    {
        return this.webBeansInterceptors;
    }

    public Set<Decorator<?>> getDecorators()
    {
        return this.webBeansDecorators;
    }

    private void addContext(Class<? extends Annotation> scopeType, javax.enterprise.context.spi.Context context)
    {
        Asserts.assertNotNull(scopeType, "scopeType parameter can not be null");
        Asserts.assertNotNull(context, "context parameter can not be null");

        List<Context> contextList = ManagerImpl.contextMap.get(scopeType);
        
        if(contextList == null)
        {
            contextList = new CopyOnWriteArrayList<Context>();
            contextList.add(context);
            
            ManagerImpl.contextMap.put(scopeType, contextList);
        }
        else
        {
//X TODO Mark , this brokes the TCK tests!!!!
//            if (context.isActive() && containsActiveContext(contextList))
//            {
//                throw new IllegalStateException("There is already an active Context registered for this scope! Context=" + context.getScopeType());
//            }
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
     * @return {@link BeanManager} instance 
     */
    public BeanManager parse(InputStream xmlStream)
    {
        this.xmlConfigurator.configure(xmlStream);
        
        return this;
    }

    /**
     * Create a new ChildActivityManager.
     */
    public BeanManager createActivity()
    {
        return new ChildActivityManager(this);
    }

    /**
     * Set the activity for the given scope type.
     * 
     * @param scopeType scope type for the context
     */
    public BeanManager setCurrent(Class<? extends Annotation> scopeType)
    {
        if(!WebBeansUtil.isScopeTypeNormal(scopeType))
        {
            throw new IllegalArgumentException("Scope type : " + scopeType.getSimpleName() + " must be normal scope type");
            
        }        
        
        Context context = getContext(scopeType);
        
        ActivityManager.getInstance().addCurrentActivity(context, this);
        
        return this;
    }    
}