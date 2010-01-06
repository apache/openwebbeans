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
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.el.ELResolver;
import javax.el.ExpressionFactory;
import javax.enterprise.context.ContextNotActiveException;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.NormalScope;
import javax.enterprise.context.spi.Context;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Event;
import javax.enterprise.inject.AmbiguousResolutionException;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Stereotype;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Decorator;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.enterprise.inject.spi.InterceptionType;
import javax.enterprise.inject.spi.Interceptor;
import javax.enterprise.inject.spi.ObserverMethod;
import javax.enterprise.util.TypeLiteral;
import javax.inject.Scope;
import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.Referenceable;
import javax.naming.StringRefAddr;

import org.apache.webbeans.component.AbstractBean;
import org.apache.webbeans.component.EnterpriseBeanMarker;
import org.apache.webbeans.component.JmsBeanMarker;
import org.apache.webbeans.component.ManagedBean;
import org.apache.webbeans.component.WebBeansType;
import org.apache.webbeans.component.third.ThirdpartyBeanImpl;
import org.apache.webbeans.config.ManagedBeanConfigurator;
import org.apache.webbeans.config.WebBeansFinder;
import org.apache.webbeans.context.ContextFactory;
import org.apache.webbeans.context.creational.CreationalContextFactory;
import org.apache.webbeans.context.creational.CreationalContextImpl;
import org.apache.webbeans.decorator.DecoratorComparator;
import org.apache.webbeans.decorator.WebBeansDecorator;
import org.apache.webbeans.decorator.WebBeansDecoratorConfig;
import org.apache.webbeans.el.WebBeansELResolver;
import org.apache.webbeans.event.NotificationManager;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.intercept.InterceptorComparator;
import org.apache.webbeans.intercept.WebBeansInterceptorConfig;
import org.apache.webbeans.intercept.webbeans.WebBeansInterceptor;
import org.apache.webbeans.plugins.OpenWebBeansEjbPlugin;
import org.apache.webbeans.plugins.OpenWebBeansJmsPlugin;
import org.apache.webbeans.plugins.PluginLoader;
import org.apache.webbeans.portable.AnnotatedElementFactory;
import org.apache.webbeans.portable.creation.InjectionTargetProducer;
import org.apache.webbeans.proxy.JavassistProxyFactory;
import org.apache.webbeans.util.AnnotationUtil;
import org.apache.webbeans.util.Asserts;
import org.apache.webbeans.util.ClassUtil;
import org.apache.webbeans.util.WebBeansUtil;
import org.apache.webbeans.xml.WebBeansXMLConfigurator;

/**
 * Implementation of the {@link BeanManager} contract of the web beans
 * container.
 * 
 * <p>
 * It is written as thread-safe.
 * </p>
 * 
 * @version $Rev$ $Date$
 * @see BeanManager 
 */
@SuppressWarnings("unchecked")
public class BeanManagerImpl implements BeanManager, Referenceable
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
    private BeanManagerImpl parent;
    
    /**
     * Creates a new {@link BeanManager} instance.
     * Called by the system. Do not use outside of the
     * system.
     */
    public BeanManagerImpl()
    {
        injectionResolver = new InjectionResolver(this);
        notificationManager = new NotificationManager();
    }    
    
    public BeanManagerImpl getParent()
    {
        return this.parent;
    }
    
    public synchronized void setParent(BeanManagerImpl parent)
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
    public static BeanManagerImpl getManager()
    {
        BeanManagerImpl currentManager = (BeanManagerImpl) WebBeansFinder.getSingletonInstance(WebBeansFinder.SINGLETON_MANAGER);
        
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
        
        List<Context> others = BeanManagerImpl.contextMap.get(scopeType);
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
        if(component instanceof AbstractBean)
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
        addContext(context.getScope(), ContextFactory.getCustomContext(context));

        return this;

    }
    
    /**
     * {@inheritDoc}
     */
    public void fireEvent(Object event, Annotation... bindings)
    {                
        if (ClassUtil.isDefinitionConstainsTypeVariables(event.getClass()))
        {
            throw new IllegalArgumentException("Event class : " + event.getClass().getName() + " can not be defined as generic type");
        }

        this.notificationManager.fireEvent(event, bindings);
    }
    
    @Deprecated
    public Object getInstanceByName(String name, CreationalContext<?> creationalContext)
    {
        AbstractBean<?> component = null;
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

        component = (AbstractBean<?>) set.iterator().next();

        object = getInstance(component, creationalContext);

        return object;
    }
    
    
    @Deprecated
    public <T> T getInstanceToInject(InjectionPoint injectionPoint, CreationalContext<?> context)
    {
        return (T)getInjectableReference(injectionPoint, context);
    }
    
    @Deprecated
    public Object getInstanceToInject(InjectionPoint injectionPoint)
    {        
        return getInstanceToInject(injectionPoint, null);
    }

    
    @Deprecated
    public <T> T getInstanceByType(Class<T> type, Annotation... bindingTypes)
    {
        ResolutionUtil.getInstanceByTypeConditions(bindingTypes);
        Set<Bean<?>> set = resolveByType(type, bindingTypes);

        ResolutionUtil.checkResolvedBeans(set, type, bindingTypes);

        Bean<?> bean = set.iterator().next();
        
        return (T)getInstance(bean, createCreationalContext(bean));
    }

    @Deprecated
    public <T> T getInstanceByType(TypeLiteral<T> type, Annotation... bindingTypes)
    {
        ResolutionUtil.getInstanceByTypeConditions(bindingTypes);
        Set<Bean<?>> set = resolveByType(type, bindingTypes);

        ResolutionUtil.checkResolvedBeans(set, type.getRawType(),bindingTypes);

        Bean<?> bean = set.iterator().next();
        
        return (T)getInstance(bean, createCreationalContext(bean));
    }

    @Deprecated
    public Set<Bean<?>> resolveByName(String name)
    {
        return this.injectionResolver.implResolveByName(name);
    }

    @Deprecated
    public Set<Bean<?>> resolveByType(Class<?> apiType, Annotation... bindingTypes)
    {
        ResolutionUtil.getInstanceByTypeConditions(bindingTypes);
        
        return this.injectionResolver.implResolveByType(apiType, bindingTypes);
    }

    @Deprecated
    public Set<Bean<?>> resolveByType(TypeLiteral<?> apiType, Annotation... bindingTypes)
    {
        ParameterizedType ptype = (ParameterizedType) apiType.getType();
        ResolutionUtil.resolveByTypeConditions(ptype);

        ResolutionUtil.getInstanceByTypeConditions(bindingTypes);
       
        return this.injectionResolver.implResolveByType(apiType.getType(), bindingTypes);
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

    
    @Deprecated
    public <T> T getInstance(Bean<T> bean, CreationalContext<?> creationalContext)
    {
        if(creationalContext == null)
        {
            creationalContext = createCreationalContext(bean);
        }
        return (T)getReference(bean, null, creationalContext);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override    
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

    /**
     * {@inheritDoc}
     */
    @Override
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

        List<Context> contextList = BeanManagerImpl.contextMap.get(scopeType);
        
        if(contextList == null)
        {
            contextList = new CopyOnWriteArrayList<Context>();
            contextList.add(context);
            
            BeanManagerImpl.contextMap.put(scopeType, contextList);
        }
        else
        {
            contextList.add(context);
        }

    }

    public Reference getReference() throws NamingException
    {
        return new Reference(BeanManagerImpl.class.getName(), new StringRefAddr("ManagerImpl", "ManagerImpl"), ManagerObjectFactory.class.getName(), null);
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
     * {@inheritDoc}
     */
    @Override
    public <T> AnnotatedType<T> createAnnotatedType(Class<T> type)
    {
        AnnotatedType<T> annotatedType = AnnotatedElementFactory.newAnnotatedType(type);
        
        return annotatedType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> CreationalContext<T> createCreationalContext(Contextual<T> contextual)
    {        
        return CreationalContextFactory.getInstance().getCreationalContext(contextual);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<Bean<?>> getBeans(Type beanType, Annotation... bindings)
    {
        if(ClassUtil.isTypeVariable(beanType))
        {
            throw new WebBeansConfigurationException("Exception in getBeans method. Bean type can not be TypeVariable");
        }
        
        AnnotationUtil.checkQualifierConditions(bindings);
        
        return this.injectionResolver.implResolveByType(beanType, bindings);
        
    }

    @Override
    public Set<Bean<?>> getBeans(String name)
    {        
        return this.injectionResolver.implResolveByName(name);
    }

    @Override
    public ELResolver getELResolver()
    {
        return new WebBeansELResolver();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getInjectableReference(InjectionPoint injectionPoint, CreationalContext<?> creationalContext)
    {
        Object instance = null;
        
        if(injectionPoint == null)
        {
            return null;
        }
                
        Annotation[] bindings = new Annotation[injectionPoint.getQualifiers().size()];
        bindings = injectionPoint.getQualifiers().toArray(bindings);
        
        //Find the injection point Bean
        Bean<?> bean = injectionResolver.getInjectionPointBean(injectionPoint);
        
        if(creationalContext != null && (creationalContext instanceof CreationalContextImpl))
        {
            CreationalContextImpl<?> creationalContextImpl = (CreationalContextImpl<?>)creationalContext;
            
            if(creationalContextImpl.getBean().equals(bean))
            {
                instance = creationalContextImpl.get();   
            }            
            
        }
                
        if(instance == null)
        {
            //Creating a new creational context for target bean instance
            instance = getReference(bean, injectionPoint.getType(), creationalContext);
        }
        
        return instance;

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<Annotation> getInterceptorBindingDefinition(Class<? extends Annotation> binding)
    {
        Annotation[] annotations = AnnotationUtil.getInterceptorBindingMetaAnnotations(binding.getDeclaredAnnotations());
        Set<Annotation> set = new HashSet<Annotation>();
        
        for(Annotation ann : annotations)
        {
            set.add(ann);
        }
        
        return set;
    }

    @Deprecated
    public <X> Bean<? extends X> getMostSpecializedBean(Bean<X> bean)
    {
        Bean<? extends X> specialized = (Bean<? extends X>) WebBeansUtil.getMostSpecializedBean(this, bean);
        
        return specialized;
    }

    @Override
    public Bean<?> getPassivationCapableBean(String id)
    {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getReference(Bean<?> bean, Type beanType, CreationalContext<?> creationalContext)
    {
        Context context = null;
        Object instance = null;
        
        //Check type if bean type is given
        if(beanType != null)
        {
            if(!ResolutionUtil.checkBeanTypeAssignableToGivenType(bean.getTypes(), beanType))
            {
                throw new IllegalArgumentException("Given bean type : " + beanType + " is not applicable for the bean instance : " + bean);
            }
            
        }
        
        //Get bean context
        context = getContext(bean.getScope());
        
        //Scope is normal
        if (WebBeansUtil.isScopeTypeNormal(bean.getScope()))
        {
            instance = getEjbOrJmsProxyReference(bean, beanType,creationalContext);
            
            if(instance != null)
            {
                return instance;
            }            
            //Create Managed Bean Proxy
            else
            {                
                if (this.proxyMap.containsKey(bean))
                {
                    instance = this.proxyMap.get(bean);
                }
                else
                {
                    instance = JavassistProxyFactory.createNewProxyInstance(bean,creationalContext);
                    this.proxyMap.put(bean, instance);                    
                }
            }            
        }
        //Create Pseudo-Scope Bean Instance
        else
        {
            instance = getEjbOrJmsProxyReference(bean, beanType, creationalContext);
            
            if(instance != null)
            {
                return instance;
            }
            
            
            instance = context.get((Bean<Object>)bean, (CreationalContext<Object>)creationalContext);                                
        }
        
        return instance;
    }
    
    private Object getEjbOrJmsProxyReference(Bean<?> bean,Type beanType, CreationalContext<?> creationalContext)
    {
        //Create session bean proxy
        if(bean instanceof EnterpriseBeanMarker)
        {
            OpenWebBeansEjbPlugin ejbPlugin = PluginLoader.getInstance().getEjbPlugin();
            if(ejbPlugin == null)
            {
                throw new IllegalStateException("There is no EJB plugin provider. Injection is failed for bean : " + bean);
            }
            
            return ejbPlugin.getSessionBeanProxy(bean,ClassUtil.getClazz(beanType), creationalContext);
        }
        
        //Create JMS Proxy
        else if(bean instanceof JmsBeanMarker)
        {
            OpenWebBeansJmsPlugin jmsPlugin = PluginLoader.getInstance().getJmsPlugin();
            if(jmsPlugin == null)
            {
                throw new IllegalStateException("There is no JMS plugin provider. Injection is failed for bean : " + bean);
            }            
            
            return jmsPlugin.getJmsBeanProxy(bean, ClassUtil.getClass(beanType));
        }
        
        return null;
    }

    
    @Override
    public Set<Annotation> getStereotypeDefinition(Class<? extends Annotation> stereotype)
    {
        Annotation[] annotations = AnnotationUtil.getStereotypeMetaAnnotations(stereotype.getDeclaredAnnotations());
        Set<Annotation> set = new HashSet<Annotation>();
        
        for(Annotation ann : annotations)
        {
            set.add(ann);
        }
        
        return set;
    }

    @Override
    public boolean isQualifier(Class<? extends Annotation> annotationType)
    {
        return AnnotationUtil.isQualifierAnnotation(annotationType);
    }

    @Override
    public boolean isInterceptorBinding(Class<? extends Annotation> annotationType)
    {
        return AnnotationUtil.isInterceptorBindingAnnotation(annotationType);
    }

    @Override
    public boolean isScope(Class<? extends Annotation> annotationType)
    {
        if(AnnotationUtil.hasAnnotation(annotationType.getDeclaredAnnotations(), Scope.class))
        {
            return true;
        }
     
        return false;
    }
    
    @Override
    public boolean isNormalScope(Class<? extends Annotation> annotationType)
    {
        if(AnnotationUtil.hasAnnotation(annotationType.getDeclaredAnnotations(), NormalScope.class))
        {
            return true;
        }
     
        return false;
    }
    
    @Override
    public boolean isPassivatingScope(Class<? extends Annotation> annotationType)
    {
        if(AnnotationUtil.hasAnnotation(annotationType.getDeclaredAnnotations(), NormalScope.class))
        {
            NormalScope scope = annotationType.getAnnotation(NormalScope.class);            
            return scope.passivating();
        }
     
        return false;
    }    
    

    @Override
    public boolean isStereotype(Class<? extends Annotation> annotationType)
    {
        if(AnnotationUtil.hasAnnotation(annotationType.getDeclaredAnnotations(), Stereotype.class))
        {
            return true;
        }
     
        return false;
    }

    @Override
    public <X> Bean<? extends X> resolve(Set<Bean<? extends X>> beans)
    { 
        Set set = new HashSet<Bean<Object>>();
        for(Bean<? extends X> obj : beans)
        {
            set.add(obj);
        }
        
        set = this.injectionResolver.findByAlternatives(set);
        
        if(set.size() > 1)
        {
            set = this.injectionResolver.findBySpecialization(set);
        }
        
        if(set.size() > 0 && set.size() > 1)
        {
            throw new AmbiguousResolutionException("Ambigious resolution");
        }
        
        return (Bean<? extends X>)set.iterator().next();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validate(InjectionPoint injectionPoint)
    {
        Bean<?> bean = injectionPoint.getBean();
        //Check for correct injection type
        this.injectionResolver.checkInjectionPointType(injectionPoint);
        
        Class<?> rawType = ClassUtil.getRawTypeForInjectionPoint(injectionPoint);
        
        //Comment out while testing TCK Events Test --- WBTCK27 jira./////
        //Hack for EntityManager --> Solve in M3!!!!
        if(rawType.equals(Event.class) || rawType.getSimpleName().equals("EntityManager"))
        {
            return;
        }
        /////////////////////////////////////////////////////////////////
        
        // check for InjectionPoint injection
        if (rawType.equals(InjectionPoint.class))
        {
            Annotated annotated = injectionPoint.getAnnotated();
            if (annotated.getAnnotations().size() == 1 && annotated.isAnnotationPresent(Default.class))
            {
                if (!bean.getScope().equals(Dependent.class))
                {
                    throw new WebBeansConfigurationException("Bean " + bean + "scope can not define other scope except @Dependent to inject InjectionPoint");
                }
            }
        }
        else
        {
            this.injectionResolver.checkInjectionPoints(injectionPoint);
        }        
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> InjectionTarget<T> createInjectionTarget(AnnotatedType<T> type)
    {
        ManagedBean<T> bean = ManagedBeanConfigurator.define(type.getJavaClass(), WebBeansType.MANAGED);
        
        return new InjectionTargetProducer<T>(bean);
    }

    @Override
    public <T> Set<ObserverMethod<? super T>> resolveObserverMethods( T event, Annotation... qualifiers ) 
    {
        return this.notificationManager.resolveObservers(event, qualifiers);
    }

    @Override
    public ExpressionFactory wrapExpressionFactory(ExpressionFactory expressionFactory)
    {
        return null;
    }


}