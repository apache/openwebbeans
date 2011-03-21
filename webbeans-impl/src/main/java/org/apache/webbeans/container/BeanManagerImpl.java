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
package org.apache.webbeans.container;

import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.el.ELResolver;
import javax.el.ExpressionFactory;
import javax.enterprise.context.ContextNotActiveException;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.NormalScope;
import javax.enterprise.context.spi.Context;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Stereotype;
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
import javax.interceptor.InterceptorBinding;
import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.Referenceable;
import javax.naming.StringRefAddr;
import org.apache.webbeans.component.AbstractOwbBean;
import org.apache.webbeans.component.EnterpriseBeanMarker;
import org.apache.webbeans.component.InjectionTargetBean;
import org.apache.webbeans.component.InjectionTargetWrapper;
import org.apache.webbeans.component.JmsBeanMarker;
import org.apache.webbeans.component.NewBean;
import org.apache.webbeans.component.OwbBean;
import org.apache.webbeans.component.WebBeansType;
import org.apache.webbeans.component.third.ThirdpartyBeanImpl;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.context.creational.CreationalContextImpl;
import org.apache.webbeans.decorator.DecoratorComparator;
import org.apache.webbeans.decorator.WebBeansDecorator;
import org.apache.webbeans.decorator.WebBeansDecoratorConfig;
import org.apache.webbeans.event.NotificationManager;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.exception.definition.DuplicateDefinitionException;
import org.apache.webbeans.exception.inject.DefinitionException;
import org.apache.webbeans.intercept.InterceptorComparator;
import org.apache.webbeans.intercept.webbeans.WebBeansInterceptor;
import org.apache.webbeans.plugins.OpenWebBeansJmsPlugin;
import org.apache.webbeans.portable.AnnotatedElementFactory;
import org.apache.webbeans.portable.creation.InjectionTargetProducer;
import org.apache.webbeans.portable.events.discovery.ErrorStack;
import org.apache.webbeans.spi.ScannerService;
import org.apache.webbeans.spi.adaptor.ELAdaptor;
import org.apache.webbeans.spi.plugins.OpenWebBeansEjbPlugin;
import org.apache.webbeans.util.AnnotationUtil;
import org.apache.webbeans.util.Asserts;
import org.apache.webbeans.util.ClassUtil;
import org.apache.webbeans.util.WebBeansUtil;
import org.apache.webbeans.xml.WebBeansXMLConfigurator;

import static org.apache.webbeans.util.InjectionExceptionUtils.throwAmbiguousResolutionException;
import static org.apache.webbeans.util.InjectionExceptionUtils.throwAmbiguousResolutionExceptionForBeanName;

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
    private static final long serialVersionUID = 2L;

    /**
     * Holds the non-standard contexts with key = scope type
     * This will get used if more than 1 scope exists.
     * Since the contexts will only get added through the
     * {@link org.apache.webbeans.portable.events.discovery.AfterBeanDiscoveryImpl}
     * we don't even need a ConcurrentHashMap.
     * @see #singleContextMap
     */
    private Map<Class<? extends Annotation>, List<Context>> contextMap = new HashMap<Class<? extends Annotation>, List<Context>>();

    /**
     * This will hold non-standard contexts where only one Context implementation got registered
     * for the given key = scope type
     * Since the contexts will only get added through the
     * {@link org.apache.webbeans.portable.events.discovery.AfterBeanDiscoveryImpl}
     * we don't even need a ConcurrentHashMap.
     * @see #contextMap
     */
    private Map<Class<? extends Annotation>, Context> singleContextMap = new HashMap<Class<? extends Annotation>, Context>();

    /**Deployment archive beans*/
    private Set<Bean<?>> deploymentBeans = new CopyOnWriteArraySet<Bean<?>>();

    /**Activity interceptors*/
    private List<Interceptor<?>> webBeansInterceptors = new ArrayList<Interceptor<?>>();
    
    /**Normal scoped cache proxies*/
    private Map<Contextual<?>, Object> cacheProxies = new ConcurrentHashMap<Contextual<?>, Object>();

    /**Activity decorators*/
    private Set<Decorator<?>> webBeansDecorators = new CopyOnWriteArraySet<Decorator<?>>();

    /**Event notification manager instance*/
    private NotificationManager notificationManager = null;

    /**Injection resolver instance*/
    private InjectionResolver injectionResolver = null;

    /**XML configurator instance*/
    private WebBeansXMLConfigurator xmlConfigurator = null;
    
    /**Additional decorator class*/
    private List<Class<?>> additionalDecoratorClasses = new ArrayList<Class<?>>();
    
    /**Additional interceptor class*/
    private List<Class<?>> additionalInterceptorClasses = new ArrayList<Class<?>>();

    /**Additional interceptor binding types we got via Extensions */
    private Map<Class<? extends Annotation>, Set<Annotation>> additionalInterceptorBindingTypes = new HashMap<Class<? extends Annotation>, Set<Annotation>>();

    /**
     * This list contains additional qualifiers which got set via the {@link javax.enterprise.inject.spi.BeforeBeanDiscovery#addQualifier(Class)}
     * event function.
     */
    private List<Class<? extends Annotation>> additionalQualifiers = new ArrayList<Class<? extends Annotation>>();
    
    /**
     * This list contains additional scopes which got set via the 
     * {@link javax.enterprise.inject.spi.BeforeBeanDiscovery#addScope(Class, boolean, boolean)} event function.
     */
    private List<ExternalScope> additionalScopes =  new ArrayList<ExternalScope>();

    private List<AnnotatedType<?>> additionalAnnotatedTypes = new ArrayList<AnnotatedType<?>>();


    private ErrorStack errorStack = new ErrorStack();
    
    /**
     * This map stores all beans along with their unique {@link javax.enterprise.inject.spi.PassivationCapable} id.
     * This is used as a reference for serialization.
     */
    private ConcurrentHashMap<String, Bean<?>> passivationBeans = new ConcurrentHashMap<String, Bean<?>>(); 
    
    private Map<Contextual<?>, InjectionTargetWrapper<?>> injectionTargetWrappers = 
        Collections.synchronizedMap(new IdentityHashMap<Contextual<?>, InjectionTargetWrapper<?>>());
    
    /**InjectionTargets for Java EE component instances that supports injections*/
    private Map<Class<?>, InjectionTargetWrapper<?>> injectionTargetForJavaEeComponents = 
        new ConcurrentHashMap<Class<?>, InjectionTargetWrapper<?>>();

    private AnnotatedElementFactory annotatedElementFactory;

    private final WebBeansContext webBeansContext;

    /**
     * This flag will get set to <code>true</code> if a custom bean
     * (all non-internal beans like {@link org.apache.webbeans.component.BeanManagerBean;} etc)
     * gets set.
     */
    private boolean inUse = false;


    /**
     * Creates a new {@link BeanManager} instance.
     * Called by the system. Do not use outside of the
     * system.
     */
    public BeanManagerImpl(WebBeansContext webBeansContext)
    {
        this.webBeansContext = webBeansContext;
        injectionResolver = new InjectionResolver(webBeansContext);
        notificationManager = new NotificationManager(webBeansContext);
        annotatedElementFactory = webBeansContext.getAnnotatedElementFactory();
    }

    public <T> void putInjectionTargetWrapper(Contextual<T> contextual, InjectionTargetWrapper<T> wrapper)
    {
        Asserts.assertNotNull(contextual);
        Asserts.assertNotNull(wrapper);
        
        this.injectionTargetWrappers.put(contextual, wrapper);
    }
    
    public <T> InjectionTargetWrapper<T> getInjectionTargetWrapper(Contextual<T> contextual)
    {
        Asserts.assertNotNull(contextual);
        return (InjectionTargetWrapper<T>)this.injectionTargetWrappers.get(contextual);
    }
    
    public <T> void putInjectionTargetWrapperForJavaEeComponents(Class<T> javaEeComponentClass, InjectionTargetWrapper<T> wrapper)
    {
        Asserts.assertNotNull(javaEeComponentClass);
        Asserts.assertNotNull(wrapper);
        
        this.injectionTargetForJavaEeComponents.put(javaEeComponentClass, wrapper);
    }
    
    public <T> InjectionTargetWrapper<T> getInjectionTargetWrapper(Class<T> javaEeComponentClass)
    {
        Asserts.assertNotNull(javaEeComponentClass);
        return (InjectionTargetWrapper<T>)this.injectionTargetForJavaEeComponents.get(javaEeComponentClass);
    }    
    
    public ErrorStack getErrorStack()
    {
        return this.errorStack;
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

        Context standardContext = webBeansContext.getContextFactory().getStandardContext(scopeType);

        if(standardContext != null && standardContext.isActive())
        {
            return standardContext;
        }

        // this is by far the most case
        Context singleContext = singleContextMap.get(scopeType);
        if (singleContext != null)
        {
            if (!singleContext.isActive())
            {
                throw new ContextNotActiveException("WebBeans context with scope type annotation @" + scopeType.getSimpleName() + " does not exist within current thread");
            }
            return singleContext;
        }

        // the spec also allows for multiple contexts existing for the same scope type
        // but in this case only one must be active at a time (for the current thread)
        List<Context> others = contextMap.get(scopeType);
        Context found = null;

        if(others != null)
        {
            for(Context otherContext : others)
            {
                if(otherContext.isActive())
                {
                    if (found != null)
                    {
                        throw new IllegalStateException("More than one active context exists with scope type annotation @" + scopeType.getSimpleName());
                    }
                    
                    found = otherContext;
                }
            }
        }
        
        if (found == null)
        {
            throw new ContextNotActiveException("WebBeans context with scope type annotation @" + scopeType.getSimpleName() + " does not exist within current thread");
        }
        
        return found;
    }

    /**
     * Add new bean to the BeanManager.
     * This will also set OWBs {@link #inUse} status.
     * 
     * @param newBean new bean instance
     * @return the this manager
     */
    public BeanManager addBean(Bean<?> newBean)
    {
        inUse = true;
        return addInternalBean(newBean);
    }

    /**
     * This method is reserved for adding 'internal beans'
     * like e.g. a BeanManagerBean,
     * @param newBean
     * @return
     */
    public BeanManager addInternalBean(Bean<?> newBean)
    {
        if(newBean instanceof AbstractOwbBean)
        {
            addPassivationInfo((OwbBean)newBean);
            this.deploymentBeans.add(newBean);
        }
        else
        {
            ThirdpartyBeanImpl<?> bean = new ThirdpartyBeanImpl(newBean, webBeansContext);
            addPassivationInfo(bean);
            this.deploymentBeans.add(bean);
        }

        return this;
    }


    /**
     * Check if the bean is has a passivation id and add it to the id store.
     *
     * @param bean
     * @throws DefinitionException if the id is not unique.
     */
    protected void addPassivationInfo(OwbBean<?> bean) throws DefinitionException
    {
        String id = bean.getId();
        if(id != null)
        {
            Bean<?> oldBean = passivationBeans.putIfAbsent(id, bean);
            if (oldBean != null)
            {
                throw new DuplicateDefinitionException("PassivationCapable bean id is not unique: " + id + " bean:" + bean);
            }
            
        }        
    }

    
    public BeanManager addContext(Context context)
    {
        addContext(context.getScope(), webBeansContext.getContextFactory().getCustomContext(context));

        return this;

    }
    
    public void addCustomInterceptorClass(Class<?> clazz)
    {
        Asserts.nullCheckForClass(clazz);
        this.additionalInterceptorClasses.add(clazz);
    }

    public void addCustomDecoratorClass(Class<?> clazz)
    {
        Asserts.nullCheckForClass(clazz);
        this.additionalDecoratorClasses.add(clazz);
    }
    
    public boolean containsCustomInterceptorClass(Class<?> clazz)
    {
        Asserts.nullCheckForClass(clazz);
        return this.additionalInterceptorClasses.contains(clazz);
    }

    public boolean containsCustomDecoratorClass(Class<?> clazz)
    {
        Asserts.nullCheckForClass(clazz);
        return this.additionalDecoratorClasses.contains(clazz);
    }

    public void addInterceptorBindingType(Class<? extends Annotation> bindingType, Annotation... inheritsArray)
    {
        Set<Annotation> inherits = additionalInterceptorBindingTypes.get(bindingType);
        if (inherits == null)
        {
            inherits = new HashSet<Annotation>();
            additionalInterceptorBindingTypes.put(bindingType, inherits);
        }
        for(Annotation ann : inheritsArray)
        {
            inherits.add(ann);
        }

    }

    public boolean hasInterceptorBindingType(Class<? extends Annotation> bindingType)
    {
        return additionalInterceptorBindingTypes.keySet().contains(bindingType);
    }


    public Set<Annotation> getInterceptorBindingTypeMetaAnnotations(Class<? extends Annotation> interceptorBindingType)
    {
        return Collections.unmodifiableSet(additionalInterceptorBindingTypes.get(interceptorBindingType));
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
        AbstractOwbBean<?> component = null;
        Object object = null;

        Set<Bean<?>> set = this.injectionResolver.implResolveByName(name);
        if (set.isEmpty())
        {
            return null;
        }

        if (set.size() > 1)
        {
            throwAmbiguousResolutionExceptionForBeanName(set, name);
        }

        component = (AbstractOwbBean<?>) set.iterator().next();

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
        webBeansContext.getResolutionUtil().getInstanceByTypeConditions(bindingTypes);
        Set<Bean<?>> set = resolveByType(type, bindingTypes);

        webBeansContext.getResolutionUtil().checkResolvedBeans(set, type, bindingTypes);

        Bean<?> bean = set.iterator().next();
        
        return (T)getInstance(bean, createCreationalContext(bean));
    }

    @Deprecated
    public <T> T getInstanceByType(TypeLiteral<T> type, Annotation... bindingTypes)
    {
        webBeansContext.getResolutionUtil().getInstanceByTypeConditions(bindingTypes);
        Set<Bean<?>> set = resolveByType(type, bindingTypes);

        webBeansContext.getResolutionUtil().checkResolvedBeans(set, type.getRawType(), bindingTypes);

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
        webBeansContext.getResolutionUtil().getInstanceByTypeConditions(bindingTypes);

        return this.injectionResolver.implResolveByType(apiType, bindingTypes);
    }

    @Deprecated
    public Set<Bean<?>> resolveByType(TypeLiteral<?> apiType, Annotation... bindingTypes)
    {
        ParameterizedType ptype = (ParameterizedType) apiType.getType();
        ResolutionUtil.resolveByTypeConditions(ptype);

        webBeansContext.getResolutionUtil().getInstanceByTypeConditions(bindingTypes);

        return this.injectionResolver.implResolveByType(apiType.getType(), bindingTypes);
    }

    
    public Set<Bean<?>> getComponents()
    {
        return deploymentBeans;
    }
    
    
    public BeanManager addDecorator(Decorator decorator)
    {
        webBeansDecorators.add(decorator);
        if (decorator instanceof OwbBean)
        {
            OwbBean<?> owbBean = (OwbBean<?>)decorator;
            
            if(owbBean.isPassivationCapable())
            {
                this.addPassivationInfo((OwbBean)decorator);   
            }
        }
        return this;
    }

    
    public BeanManager addInterceptor(Interceptor interceptor)
    {
        webBeansInterceptors.add(interceptor);
        if (interceptor instanceof OwbBean)
        {
            OwbBean<?> owbBean = (OwbBean<?>)interceptor;
            if(owbBean.isPassivationCapable())
            {
                this.addPassivationInfo((OwbBean)interceptor);    
            }
            
        }       
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
        webBeansContext.getAnnotationManager().checkDecoratorResolverParams(types, bindingTypes);
        Set<Decorator<?>> intsSet = WebBeansDecoratorConfig.findDeployedWebBeansDecorator(this, types, bindingTypes);
        Iterator<Decorator<?>> itSet = intsSet.iterator();

        List<Decorator<?>> decoratorList = new ArrayList<Decorator<?>>();
        while (itSet.hasNext())
        {
            WebBeansDecorator decorator = (WebBeansDecorator) itSet.next();
            decoratorList.add(decorator);

        }

        Collections.sort(decoratorList, new DecoratorComparator(webBeansContext));

        return decoratorList;

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Interceptor<?>> resolveInterceptors(InterceptionType type, Annotation... interceptorBindings)
    {
        webBeansContext.getAnnotationManager().checkInterceptorResolverParams(interceptorBindings);

        Set<Interceptor<?>> intsSet = webBeansContext.getWebBeansInterceptorConfig().findDeployedWebBeansInterceptor(interceptorBindings, webBeansContext);
        Iterator<Interceptor<?>> itSet = intsSet.iterator();

        List<Interceptor<?>> interceptorList = new ArrayList<Interceptor<?>>();
        while (itSet.hasNext())
        {
            WebBeansInterceptor interceptor = (WebBeansInterceptor) itSet.next();

            if (interceptor.intercepts(type))
            {
                interceptorList.add(interceptor);
            }

        }

        Collections.sort(interceptorList, new InterceptorComparator());

        return interceptorList;
    }

    
    public Set<Bean<?>> getBeans()
    {
        return this.deploymentBeans;
    }
    
    public List<Interceptor<?>> getInterceptors()
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

        List<Context> contextList = contextMap.get(scopeType);
        
        if(contextList == null)
        {
            Context singleContext = singleContextMap.get(scopeType);
            if (singleContext == null)
            {
                // first put them into the singleContextMap
                singleContextMap.put(scopeType, context);
            }
            else
            {
                // from the 2nd Context for this scopetype on, we need to maintain a List for them
                contextList = new ArrayList<Context>();
                contextList.add(singleContext);
                contextList.add(context);

                contextMap.put(scopeType, contextList);
            }
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
        AnnotatedType<T> annotatedType = annotatedElementFactory.newAnnotatedType(type);
        
        return annotatedType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> CreationalContext<T> createCreationalContext(Contextual<T> contextual)
    {
        if (contextual instanceof SerializableBean)
        {
            contextual = ((SerializableBean)contextual).getBean();
        }

        return webBeansContext.getCreationalContextFactory().getCreationalContext(contextual);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<Bean<?>> getBeans(Type beanType, Annotation... bindings)
    {
        if(ClassUtil.isTypeVariable(beanType))
        {
            throw new IllegalArgumentException("Exception in getBeans method. Bean type can not be TypeVariable for bean type : " + beanType);
        }

        webBeansContext.getAnnotationManager().checkQualifierConditions(bindings);

        return this.injectionResolver.implResolveByType(beanType, bindings);
        
    }

    @Override
    public Set<Bean<?>> getBeans(String name)
    {        
        Asserts.assertNotNull(name, "name parameter can not be null");
        
        return this.injectionResolver.implResolveByName(name);
    }

    @Override
    public ELResolver getELResolver()
    {
        ELAdaptor elAdaptor = webBeansContext.getService(ELAdaptor.class);
        return elAdaptor.getOwbELResolver();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getInjectableReference(InjectionPoint injectionPoint, CreationalContext<?> ownerCreationalContext)
    {
        Asserts.assertNotNull(injectionPoint, "injectionPoint parameter can not be null");

        //Injected instance
        Object instance = null;
        
        //Injection point is null
        if(injectionPoint == null)
        {
            return null;
        }
        
        //Find the injection point Bean
        Bean<Object> injectedBean = (Bean<Object>)injectionResolver.getInjectionPointBean(injectionPoint);
        
        boolean isSetIPForProducers=false;
        ScannerService scannerService = webBeansContext.getScannerService();
        if ((scannerService != null && scannerService.isBDABeansXmlScanningEnabled()))
        {
            if (injectedBean instanceof AbstractOwbBean<?>)
            {
                AbstractOwbBean<?> aob = (AbstractOwbBean) injectedBean;
                if (aob.getWebBeansType() == WebBeansType.PRODUCERFIELD || aob.getWebBeansType() == WebBeansType.PRODUCERMETHOD)
                {
                    // There is no way to pass the injection point for producers
                    // without significant refactoring, so set injection point
                    // on
                    // InjectionResolver to properly handle alternative
                    // producers
                    // per BDA.
                    isSetIPForProducers = true;
                }
            }
        }
        if (isSetIPForProducers)
        {
            // retrieve injection point from thread local for alternative
            // producers
            InjectionResolver.injectionPoints.set(injectionPoint);
        }
        if(WebBeansUtil.isDependent(injectedBean))
        {        
            //Using owner creational context
            //Dependents use parent creational context
            instance = getReference(injectedBean, injectionPoint.getType(), ownerCreationalContext);
        }
        
        else
        {   
            //New creational context for normal scoped beans
            CreationalContextImpl<Object> injectedCreational = (CreationalContextImpl<Object>)createCreationalContext(injectedBean);            
            instance = getReference(injectedBean, injectionPoint.getType(), injectedCreational);
        }
        if(isSetIPForProducers)
        {
            //remove reference immediate after instance is retrieved
            InjectionResolver.injectionPoints.set(null);
            InjectionResolver.injectionPoints.remove();
        }

        return instance;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<Annotation> getInterceptorBindingDefinition(Class<? extends Annotation> binding)
    {
        Annotation[] annotations = binding.getDeclaredAnnotations();
        Set<Annotation> set = new HashSet<Annotation>();
        
        if(binding.isAnnotationPresent(InterceptorBinding.class))
        {
            for(Annotation ann : annotations)
            {
                set.add(ann);
            }            
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
        return passivationBeans.get(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getReference(Bean<?> bean, Type beanType, CreationalContext<?> creationalContext)
    {
        Asserts.assertNotNull(bean, "bean parameter can not be null");

        Context context = null;
        Object instance = null;

        if (bean instanceof SerializableBean)
        {
            bean = ((SerializableBean)bean).getBean();
        }
        
        //Check type if bean type is given
        if(beanType != null)
        {
            if(!ResolutionUtil.checkBeanTypeAssignableToGivenType(bean.getTypes(), beanType, bean instanceof NewBean))
            {
                throw new IllegalArgumentException("Given bean type : " + beanType + " is not applicable for the bean instance : " + bean);
            }
            
        }
        
        if(!(creationalContext instanceof CreationalContextImpl))
        {
            creationalContext = webBeansContext.getCreationalContextFactory().wrappedCreationalContext(creationalContext, bean);
        }        
        
                
        //Scope is normal
        if (webBeansContext.getWebBeansUtil().isScopeTypeNormal(bean.getScope()))
        {
            instance = getEjbOrJmsProxyReference(bean, beanType,creationalContext);
            
            if(instance != null)
            {
                return instance;
            }
            
            instance = cacheProxies.get(bean);

            if (instance == null)
            {
                //Create Managed Bean Proxy
                instance = webBeansContext.getJavassistProxyFactory().createNormalScopedBeanProxy((AbstractOwbBean<?>)bean,creationalContext);

                //Cached instance
                cacheProxies.put(bean, instance);
            }

        }
        //Create Pseudo-Scope Bean Instance
        else
        {
            //Get bean context
            context = getContext(bean.getScope());
            
            //Get instance for ejb or jms
            instance = getEjbOrJmsProxyReference(bean, beanType, creationalContext);
            
            if(instance != null)
            {
                return instance;
            }
            
            //Get dependent from DependentContex that create contextual instance
            instance = context.get((Bean<Object>)bean, (CreationalContext<Object>)creationalContext);
        }
        
        return instance;
    }
    
    private Object getEjbOrJmsProxyReference(Bean<?> bean,Type beanType, CreationalContext<?> creationalContext)
    {
        //Create session bean proxy
        if(bean instanceof EnterpriseBeanMarker)
        {
            if(webBeansContext.getWebBeansUtil().isScopeTypeNormal(bean.getScope()))
            {
                //Maybe it is cached
                if(this.cacheProxies.containsKey(bean))
                {
                    return this.cacheProxies.get(bean);
                }
            }

            OpenWebBeansEjbPlugin ejbPlugin = webBeansContext.getPluginLoader().getEjbPlugin();
            if(ejbPlugin == null)
            {
                throw new IllegalStateException("There is no EJB plugin provider. Injection is failed for bean : " + bean);
            }
            
            return ejbPlugin.getSessionBeanProxy(bean,ClassUtil.getClazz(beanType), creationalContext);
        }
        
        //Create JMS Proxy
        else if(bean instanceof JmsBeanMarker)
        {
            OpenWebBeansJmsPlugin jmsPlugin = webBeansContext.getPluginLoader().getJmsPlugin();
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
        Annotation[] annotations = stereotype.getDeclaredAnnotations();
        Set<Annotation> set = new HashSet<Annotation>();
        
        if(stereotype.isAnnotationPresent(Stereotype.class))
        {
            for(Annotation ann : annotations)
            {
                set.add(ann);
            }            
        }
        
        return set;
    }

    @Override
    public boolean isQualifier(Class<? extends Annotation> annotationType)
    {
        return webBeansContext.getAnnotationManager().isQualifierAnnotation(annotationType);
    }

    @Override
    public boolean isInterceptorBinding(Class<? extends Annotation> annotationType)
    {
        return webBeansContext.getAnnotationManager().isInterceptorBindingAnnotation(annotationType);
    }

    @Override
    public boolean isScope(Class<? extends Annotation> annotationType)
    {
        if(AnnotationUtil.hasAnnotation(annotationType.getDeclaredAnnotations(), Scope.class) ||
                AnnotationUtil.hasAnnotation(annotationType.getDeclaredAnnotations(), NormalScope.class))
        {
            return true;
        }
        
        for(ExternalScope ext : this.additionalScopes)
        {
            if(ext.getScope().equals(annotationType))
            {
                return true;
            }
        }
     
        return false;
    }
    
    @Override
    public boolean isNormalScope(Class<? extends Annotation> annotationType)
    {
        for (ExternalScope extScope : additionalScopes)
        {
            if (extScope.getScope().equals(annotationType))
            {
                return extScope.isNormal();
            }
        }
        
        return AnnotationUtil.hasAnnotation(annotationType.getDeclaredAnnotations(), NormalScope.class);
    }
    
    @Override
    public boolean isPassivatingScope(Class<? extends Annotation> annotationType)
    {
        for (ExternalScope extScope : additionalScopes)
        {
            if (extScope.getScope().equals(annotationType))
            {
                return extScope.isPassivating();
            }
        }

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
        return AnnotationUtil.hasAnnotation(annotationType.getDeclaredAnnotations(), Stereotype.class);
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
        
        if(set.size() > 1)
        {
            throwAmbiguousResolutionException(set);
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
                
        // check for InjectionPoint injection
        if (rawType.equals(InjectionPoint.class))
        {
            if (AnnotationUtil.hasAnnotation(AnnotationUtil.getAnnotationsFromSet(injectionPoint.getQualifiers()), Default.class))
            {
                if (!bean.getScope().equals(Dependent.class))
                {
                    throw new WebBeansConfigurationException("Bean " + bean.getBeanClass() + "scope can not define other scope except @Dependent to inject InjectionPoint");
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
        InjectionTargetBean<T> bean = webBeansContext.getWebBeansUtil().defineManagedBean(type);

        if (bean == null)
        {
            throw new DefinitionException("Could not create InjectionTargetBean for type " + type.getJavaClass());
        }

        return new InjectionTargetProducer<T>(bean);
    }

    @Override
    public <T> Set<ObserverMethod<? super T>> resolveObserverMethods( T event, Annotation... qualifiers ) 
    {
        if(ClassUtil.isDefinitionConstainsTypeVariables(event.getClass()))
        {
            throw new IllegalArgumentException("Event type can not contain type variables. Event class is : " + event.getClass());
        }
        
        return this.notificationManager.resolveObservers(event, qualifiers);
    }

    @Override
    public ExpressionFactory wrapExpressionFactory(ExpressionFactory expressionFactory)
    {
        ELAdaptor elAdaptor = webBeansContext.getService(ELAdaptor.class);
        return elAdaptor.getOwbWrappedExpressionFactory(expressionFactory);
    }

    public void addAdditionalQualifier(Class<? extends Annotation> qualifier)
    {
        if (!additionalQualifiers.contains(qualifier))
        {
            additionalQualifiers.add(qualifier);
        }
    }
    
    public void addAdditionalAnnotatedType(AnnotatedType<?> annotatedType)
    {
        this.additionalAnnotatedTypes.add(annotatedType);
    }

    public List<Class<? extends Annotation>> getAdditionalQualifiers()
    {
        return additionalQualifiers;
    }
    
    public void addAdditionalScope(ExternalScope additionalScope)
    {
        if (!additionalScopes.contains(additionalScope))
        {
            additionalScopes.add(additionalScope);
        }
    }


    public List<ExternalScope> getAdditionalScopes()
    {
        return additionalScopes;
    }
    
    public List<AnnotatedType<?>> getAdditionalAnnotatedTypes()
    {
        return additionalAnnotatedTypes;
    }



    public void clear()
    {
        this.additionalAnnotatedTypes.clear();
        this.additionalDecoratorClasses.clear();
        this.additionalInterceptorClasses.clear();
        this.additionalInterceptorBindingTypes.clear();
        this.additionalQualifiers.clear();
        this.additionalScopes.clear();
        this.cacheProxies.clear();
        this.singleContextMap.clear();
        this.contextMap.clear();
        this.deploymentBeans.clear();
        this.errorStack.clear();
        this.injectionTargetForJavaEeComponents.clear();
        this.injectionTargetWrappers.clear();
        this.passivationBeans.clear();
        this.webBeansDecorators.clear();
        this.webBeansInterceptors.clear();
    }

    public boolean isInUse()
    {
        return inUse;
    }
}
