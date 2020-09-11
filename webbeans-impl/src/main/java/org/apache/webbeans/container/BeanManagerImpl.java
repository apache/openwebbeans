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

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.el.ELResolver;
import javax.el.ExpressionFactory;
import javax.enterprise.context.ContextNotActiveException;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.NormalScope;
import javax.enterprise.context.spi.AlterableContext;
import javax.enterprise.context.spi.Context;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Event;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.InjectionException;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Stereotype;
import javax.enterprise.inject.Vetoed;
import javax.enterprise.inject.spi.*;
import javax.inject.Scope;
import javax.interceptor.InterceptorBinding;
import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.Referenceable;
import javax.naming.StringRefAddr;

import org.apache.webbeans.annotation.AnyLiteral;
import org.apache.webbeans.annotation.DefaultLiteral;
import org.apache.webbeans.component.AbstractOwbBean;
import org.apache.webbeans.component.AbstractProducerBean;
import org.apache.webbeans.component.CdiInterceptorBean;
import org.apache.webbeans.component.DecoratorBean;
import org.apache.webbeans.component.EnterpriseBeanMarker;
import org.apache.webbeans.component.InjectionTargetBean;
import org.apache.webbeans.component.JmsBeanMarker;
import org.apache.webbeans.component.ManagedBean;
import org.apache.webbeans.component.NewBean;
import org.apache.webbeans.component.OwbBean;
import org.apache.webbeans.component.ProducerAwareInjectionTargetBean;
import org.apache.webbeans.component.WebBeansType;
import org.apache.webbeans.component.creation.BeanAttributesBuilder;
import org.apache.webbeans.component.creation.CdiInterceptorBeanBuilder;
import org.apache.webbeans.component.creation.DecoratorBeanBuilder;
import org.apache.webbeans.component.creation.FieldProducerFactory;
import org.apache.webbeans.component.creation.MethodProducerFactory;
import org.apache.webbeans.component.third.PassivationCapableThirdpartyBeanImpl;
import org.apache.webbeans.component.third.ThirdpartyBeanImpl;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.context.CustomAlterablePassivatingContextImpl;
import org.apache.webbeans.context.CustomPassivatingContextImpl;
import org.apache.webbeans.context.creational.CreationalContextImpl;
import org.apache.webbeans.decorator.DecoratorComparator;
import org.apache.webbeans.event.EventImpl;
import org.apache.webbeans.event.EventMetadataImpl;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.exception.DuplicateDefinitionException;

import org.apache.webbeans.intercept.InterceptorUtil;
import org.apache.webbeans.plugins.OpenWebBeansJmsPlugin;
import org.apache.webbeans.portable.AnnotatedElementFactory;
import org.apache.webbeans.portable.InjectionTargetImpl;
import org.apache.webbeans.portable.LazyInterceptorDefinedInjectionTarget;
import org.apache.webbeans.portable.events.discovery.ErrorStack;
import org.apache.webbeans.portable.events.generics.GProcessInjectionPoint;
import org.apache.webbeans.portable.events.generics.GProcessInjectionTarget;
import org.apache.webbeans.spi.adaptor.ELAdaptor;
import org.apache.webbeans.spi.plugins.OpenWebBeansEjbPlugin;
import org.apache.webbeans.util.AnnotationUtil;
import org.apache.webbeans.util.Asserts;
import org.apache.webbeans.util.ClassUtil;
import org.apache.webbeans.util.GenericsUtil;
import org.apache.webbeans.util.WebBeansUtil;

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
    private Map<Class<? extends Annotation>, List<Context>> contextMap = new HashMap<>();

    /**
     * This will hold non-standard contexts where only one Context implementation got registered
     * for the given key = scope type
     * Since the contexts will only get added through the
     * {@link org.apache.webbeans.portable.events.discovery.AfterBeanDiscoveryImpl}
     * we don't even need a ConcurrentHashMap.
     * @see #contextMap
     */
    private Map<Class<? extends Annotation>, Context> singleContextMap = new HashMap<>();

    /**Deployment archive beans*/
    private Set<Bean<?>> deploymentBeans = new HashSet<>();

    /**Normal scoped cache proxies*/
    private Map<Contextual<?>, Object> cacheProxies = new ConcurrentHashMap<>();

    /**Injection resolver instance*/
    private InjectionResolver injectionResolver;

    /**
     * This list contains additional qualifiers which got set via the
     * {@link javax.enterprise.inject.spi.BeforeBeanDiscovery#addQualifier(Class)}
     * event function.
     */
    private List<Class<? extends Annotation>> additionalQualifiers = new ArrayList<>();
    private Map<Class<?>, AnnotatedType<? extends Annotation>> additionalAnnotatedTypeQualifiers = new HashMap<>();

    /**
     * This list contains additional scopes which got set via the
     * {@link javax.enterprise.inject.spi.BeforeBeanDiscovery#addScope(Class, boolean, boolean)} event function.
     */
    private List<ExternalScope> additionalScopes = new ArrayList<>();

    /** quick detection if an annotation is a scope-annotation  */
    private Set<Class<? extends Annotation>> scopeAnnotations = new HashSet<>();

    /** quick detection if an annotation is NOT a scope-annotation  */
    private Set<Class<? extends Annotation>> nonscopeAnnotations = new HashSet<>();


    private ConcurrentMap<Class<?>, ConcurrentMap<String, AnnotatedType<?>>> additionalAnnotatedTypes = new ConcurrentHashMap<>();

    private ErrorStack errorStack = new ErrorStack();

    /**
     * This map stores all beans along with their unique {@link javax.enterprise.inject.spi.PassivationCapable} id.
     * This is used as a reference for serialization.
     */
    private ConcurrentMap<String, Bean<?>> passivationBeans = new ConcurrentHashMap<>();

    /**InjectionTargets for Java EE component instances that supports injections*/
    private Map<Class<?>, Producer<?>> producersForJavaEeComponents =
        new ConcurrentHashMap<>();

    private AnnotatedElementFactory annotatedElementFactory;

    private final WebBeansContext webBeansContext;

    /**
     * This flag will get set to <code>true</code> if a custom bean
     * (all non-internal beans like {@link org.apache.webbeans.component.BeanManagerBean;} etc)
     * gets set.
     */
    private boolean inUse;

    /**
     * This flag will get set to handle lifecyle around
     * {@link javax.enterprise.inject.spi.AfterBeanDiscovery}
     */
    private LifecycleState beanDiscoveryState = LifecycleState.BEFORE_DISCOVERY;

    /**
     * This flag will get set to {@code true} after the
     * {@link javax.enterprise.inject.spi.AfterDeploymentValidation} gets fired
     */
    private boolean afterDeploymentValidationFired;

    /**
     * we cache results of calls to {@link #isNormalScope(Class)} because
     * this doesn't change at runtime.
     * We don't need to take special care about classloader
     * hierarchies, because each cl has other classes.
     */
    private static Map<Class<? extends Annotation>, Boolean> isScopeTypeNormalCache =
        new ConcurrentHashMap<>();

    /**
     * Map to be able to lookup always 3rd party beans when user does lookups with custom beans.
     */
    private Map<Bean<?>, Bean<?>> thirdPartyMapping = new HashMap<>();

    /**
     * Creates a new {@link BeanManager} instance.
     * Called by the system. Do not use outside of the
     * system.
     */
    public BeanManagerImpl(WebBeansContext webBeansContext)
    {
        this.webBeansContext = webBeansContext;
        injectionResolver = new InjectionResolver(webBeansContext);
        annotatedElementFactory = webBeansContext.getAnnotatedElementFactory();
    }

    public WebBeansContext getWebBeansContext()
    {
        return webBeansContext;
    }

    public <T> void putProducerForJavaEeComponent(Class<T> javaEeComponentClass, Producer<T> wrapper)
    {
        Asserts.assertNotNull(javaEeComponentClass);
        Asserts.assertNotNull(wrapper);

        producersForJavaEeComponents.put(javaEeComponentClass, wrapper);
    }

    public <T> Producer<T> getProducerForJavaEeComponent(Class<T> javaEeComponentClass)
    {
        Asserts.assertNotNull(javaEeComponentClass);
        return (Producer<T>) producersForJavaEeComponents.get(javaEeComponentClass);
    }

    public ErrorStack getErrorStack()
    {
        return errorStack;
    }

    /**
     * Gets injection resolver.
     *
     * @return injection resolver
     */
    public InjectionResolver getInjectionResolver()
    {
        return injectionResolver;
    }

    /**
     * Gets the active context for the given scope type.
     *
     * @param scopeType scope type of the context
     * @throws ContextNotActiveException if no active context
     * @throws IllegalStateException if more than one active context
     */
    @Override
    public Context getContext(Class<? extends Annotation> scopeType)
    {
        Asserts.assertNotNull(scopeType, "scopeType");

        Context standardContext = webBeansContext.getContextsService().getCurrentContext(scopeType);

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
                throw new ContextNotActiveException("WebBeans context with scope type annotation @"
                                                    + scopeType.getSimpleName()
                                                    + " does not exist within current thread");
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
                        throw new IllegalStateException("More than one active context exists with scope type annotation @"
                                                        + scopeType.getSimpleName());
                    }

                    found = otherContext;
                }
            }
        }

        if (found == null)
        {
            throw new ContextNotActiveException("WebBeans context with scope type annotation @"
                                                + scopeType.getSimpleName() + " does not exist within current thread");
        }

        return found;
    }

    @Override
    public Instance<Object> createInstance()
    {
        return OwbCDI.current().select(DefaultLiteral.INSTANCE);
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
    public <T> BeanManager addInternalBean(Bean<T> newBean)
    {
        if(newBean instanceof AbstractOwbBean)
        {
            addPassivationInfo(newBean);
            deploymentBeans.add(newBean);
        }
        else
        {
            ThirdpartyBeanImpl<?> bean;
            if (!PassivationCapable.class.isInstance(newBean))
            {
                bean = new ThirdpartyBeanImpl<>(webBeansContext, newBean);
            }
            else
            {
                bean = new PassivationCapableThirdpartyBeanImpl<>(webBeansContext, newBean);
            }
            addPassivationInfo(bean);
            deploymentBeans.add(bean);
            thirdPartyMapping.put(newBean, bean);
        }

        return this;
    }


    /**
     * Check if the bean is has a passivation id and add it to the id store.
     *
     * @param bean
     * @throws DefinitionException if the id is not unique.
     */
    public void addPassivationInfo(Bean<?> bean) throws DefinitionException
    {
        String id = null;
        if (bean instanceof OwbBean<?>)
        {
            id = ((OwbBean) bean).getId();
        }
        if (id == null && bean instanceof PassivationCapable)
        {
            id = ((PassivationCapable) bean).getId();
        }

        if(id != null)
        {
            Bean<?> oldBean = passivationBeans.putIfAbsent(id, bean);
            if (oldBean != null)
            {
                throw new DuplicateDefinitionException("PassivationCapable bean id is not unique: " +
                        id + " bean:" + bean + ", existing: " + oldBean);
            }
        }
    }


    public BeanManager addContext(Context context)
    {
        addContext(context.getScope(), wrapCustomContext(context));

        return this;

    }

    /**
     * If the context is passivating then we need to wrap it into a version which
     * uses the {@link SerializableBeanVault }
     */
    public Context wrapCustomContext(Context context)
    {
        if (isPassivatingScope(context.getScope()))
        {
            if (context instanceof AlterableContext)
            {
                return new CustomAlterablePassivatingContextImpl(webBeansContext.getSerializableBeanVault(), (AlterableContext) context);
            }
            else
            {
                return new CustomPassivatingContextImpl(webBeansContext.getSerializableBeanVault(), context);
            }
        }

        return context;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void fireEvent(Object event, Annotation... bindings)
    {
        fireEvent(event, false, bindings);
    }

    @Override
    public Event<Object> getEvent()
    {
        return new EventImpl<>(new EventMetadataImpl(null, Object.class, null, new Annotation[]{AnyLiteral.INSTANCE}, webBeansContext), webBeansContext);
    }

    public void fireEvent(Object event, boolean containerEvent, Annotation... bindings)
    {
        Type type = event.getClass();
        if (GenericsUtil.hasTypeParameters(type))
        {
            type = GenericsUtil.getParameterizedType(type);
        }
        fireEvent(event, new EventMetadataImpl(null, type, null, bindings, webBeansContext), containerEvent);
    }

    /**
     * Fire &#064;Initialized and &#064Destroyed events, but only IF any observers do exist.
     */
    public void fireContextLifecyleEvent(Object payload, Annotation lifecycleQualifier)
    {
        if (webBeansContext.getNotificationManager().hasContextLifecycleObserver(lifecycleQualifier))
        {
            fireEvent(payload, lifecycleQualifier);
        }
    }

    /**
     * Like {@link #fireEvent(Object, java.lang.annotation.Annotation...)} but intended for
     * internal CDI Container lifecycle events. The difference is that those
     * events must only be delivered to CDI Extensions and not to normal beans.
     */
    public void fireLifecycleEvent(Object event, Annotation... bindings)
    {
        fireEvent(event, new EventMetadataImpl(null, event.getClass(), null, bindings, webBeansContext), true);
    }

    public void fireEvent(Object event, EventMetadataImpl metadata, boolean isLifecycleEvent)
    {
        webBeansContext.getNotificationManager().fireEvent(event, metadata, isLifecycleEvent, null);
    }

    public Set<Bean<?>> getComponents()
    {
        return deploymentBeans;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public List<Decorator<?>> resolveDecorators(Set<Type> types, Annotation... bindingTypes)
    {
        webBeansContext.getAnnotationManager().checkDecoratorResolverParams(types, bindingTypes);
        return unsafeResolveDecorators(types, bindingTypes);
    }

    public List<Decorator<?>> unsafeResolveDecorators(Set<Type> types, Annotation[] bindingTypes)
    {
        webBeansContext.getAnnotationManager().checkQualifiersParams(types, bindingTypes); // checkDecoratorResolverParams is too restrictive for repeatable bindings
        Set<Decorator<?>> intsSet = webBeansContext.getDecoratorsManager().findDeployedWebBeansDecorator(types, bindingTypes);
        List<Decorator<?>> decoratorList = new ArrayList<>(intsSet);
        decoratorList.sort(new DecoratorComparator(webBeansContext));
        return decoratorList;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Interceptor<?>> resolveInterceptors(InterceptionType type, Annotation... interceptorBindings)
    {
        webBeansContext.getAnnotationManager().checkInterceptorResolverParams(interceptorBindings);

        return webBeansContext.getInterceptorsManager().resolveInterceptors(type, interceptorBindings);
    }


    public Set<Bean<?>> getBeans()
    {
        return deploymentBeans;
    }

    private void addContext(Class<? extends Annotation> scopeType, javax.enterprise.context.spi.Context context)
    {
        Asserts.assertNotNull(scopeType, "scopeType");
        Asserts.assertNotNull(context, "context");

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
                contextList = new ArrayList<>();
                contextList.add(singleContext);
                contextList.add(context);

                contextMap.put(scopeType, contextList);
                singleContextMap.remove(scopeType);
            }
        }
        else
        {
            contextList.add(context);
        }

    }

    @Override
    public Reference getReference() throws NamingException
    {
        return new Reference(BeanManagerImpl.class.getName(), new StringRefAddr("ManagerImpl", "ManagerImpl"), ManagerObjectFactory.class.getName(), null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> AnnotatedType<T> createAnnotatedType(Class<T> type)
    {
        return annotatedElementFactory.newAnnotatedType(type);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> CreationalContextImpl<T> createCreationalContext(Contextual<T> contextual)
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

        return injectionResolver.implResolveByType(false, beanType, bindings);

    }

    @Override
    public Set<Bean<?>> getBeans(String name)
    {
        Asserts.assertNotNull(name, "name");

        return injectionResolver.implResolveByName(name);
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
        //Injection point is null
        if(injectionPoint == null)
        {
            return null;
        }

        //Injected instance
        Object instance = null;


        //Find the injection point Bean
        Bean<Object> injectedBean = (Bean<Object>)injectionResolver.getInjectionPointBean(injectionPoint);


        if(WebBeansUtil.isDependent(injectedBean))
        {
            if (!(ownerCreationalContext instanceof CreationalContextImpl))
            {
                ownerCreationalContext = webBeansContext.getCreationalContextFactory().wrappedCreationalContext(ownerCreationalContext, injectionPoint.getBean());
            }

            if (injectionPoint.isDelegate() && ((CreationalContextImpl<?>)ownerCreationalContext).getDelegate() != null)
            {
                // this is a dirty hack for Custom Decorators which inject into @Delegate InjectionPoints
                // by using getInjectableReference. Done in the TCK. Never seen this for real though...
                return ((CreationalContextImpl<?>)ownerCreationalContext).getDelegate();
            }

            ((CreationalContextImpl<?>)ownerCreationalContext).putInjectionPoint(injectionPoint);
            //Using owner creational context
            //Dependents use parent creational context
            try
            {
                instance = getReference(injectedBean, injectionPoint.getType(), ownerCreationalContext);
            }
            finally
            {
                ((CreationalContextImpl<?>)ownerCreationalContext).removeInjectionPoint();
            }
        }
        else
        {
            //New creational context for normal scoped beans
            CreationalContextImpl<Object> injectedCreational = (CreationalContextImpl<Object>)createCreationalContext(injectedBean);
            injectedCreational.putInjectionPoint(injectionPoint);
            try
            {
                instance = getReference(injectedBean, injectionPoint.getType(), injectedCreational);
            }
            finally
            {
                injectedCreational.removeInjectionPoint();
            }
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
        Set<Annotation> set = new HashSet<>();

        if(binding.isAnnotationPresent(InterceptorBinding.class))
        {
            Collections.addAll(set, annotations);
        }

        return set;
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
    public Object getReference(Bean<?> providedBean, Type beanType, CreationalContext<?> creationalContext)
    {
        Asserts.assertNotNull(providedBean, "bean parameter");

        Context context = null;
        Object instance = null;

        Bean<?> bean =  !OwbBean.class.isInstance(providedBean) ? thirdPartyMapping.get(providedBean) : providedBean;
        if (bean == null) // more than unlikely but still possible and not invalid (user could create new instance of bean each time, not forbidden)
        {
            bean = providedBean;
        }
        if (bean instanceof SerializableBean)
        {
            bean = ((SerializableBean)bean).getBean();
        }

        if(!(creationalContext instanceof CreationalContextImpl))
        {
            creationalContext = webBeansContext.getCreationalContextFactory().wrappedCreationalContext(creationalContext, bean);
        }

        if (ManagedBean.class.isInstance(bean))
        {
            ManagedBean.class.cast(bean).valid();
        }

        //Check type if bean type is given
        if(beanType != null && beanType != Object.class)
        {
            boolean isProducer = AbstractProducerBean.class.isInstance(bean);
            if(!isProducer && // we have different rules for producers
               !isBeanTypeAssignableToGivenType(bean.getTypes(), beanType, bean instanceof NewBean, isProducer) &&
               !GenericsUtil.satisfiesDependency(false, isProducer, beanType, bean.getBeanClass(), new HashMap<>()) &&
               !GenericsUtil.satisfiesDependencyRaw(false, isProducer, beanType, bean.getBeanClass(), new HashMap<>()))
            {
                throw new IllegalArgumentException("Given bean type : " + beanType + " is not applicable for the bean instance : " + bean);
            }

        }
        else if (bean instanceof OwbBean)
        {
            // we cannot always use getBeanClass() as this will
            // return the containing class for producer methods and fields
            beanType = ((OwbBean) bean).getReturnType();
        }
        else
        {
            beanType = bean.getBeanClass();
        }

        //Scope is normal
        if (isNormalScope(bean.getScope()))
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
                instance = webBeansContext.getNormalScopeProxyFactory().createNormalScopeProxy(bean);

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

    /**
     * {@inheritDoc}
     */
    public BeanAttributes<?> createBeanAttributes(AnnotatedMember<?> member)
    {
        if (member instanceof AnnotatedField)
        {
            return BeanAttributesBuilder.forContext(webBeansContext).newBeanAttibutes((AnnotatedField<?>)member).build();
        }
        else if (member instanceof AnnotatedMethod)
        {
            return BeanAttributesBuilder.forContext(webBeansContext).newBeanAttibutes((AnnotatedMethod<?>)member).build();
        }
        else
        {
            throw new IllegalArgumentException("Unsupported member type " + member.getClass().getName());
        }
    }

    public InjectionPoint createInjectionPoint(AnnotatedField<?> field)
    {
        return webBeansContext.getInjectionPointFactory().buildInjectionPoint(null, field, false);
    }

    public InjectionPoint createInjectionPoint(AnnotatedParameter<?> parameter)
    {
        InjectionPoint injectionPoint = webBeansContext.getInjectionPointFactory().buildInjectionPoint(null, parameter, false);
        if (AnnotatedMethod.class.isInstance(parameter.getDeclaringCallable()))
        {
            try
            {
                validate(injectionPoint);
            }
            catch (Exception e)
            {
                throw new IllegalArgumentException(e);
            }
        } // TODO else constructor rules are a bit different
        GProcessInjectionPoint event = webBeansContext.getWebBeansUtil().fireProcessInjectionPointEvent(injectionPoint);
        injectionPoint = event.getInjectionPoint();
        event.setStarted();
        return injectionPoint;
    }

    public <X> ProducerFactory<X> getProducerFactory(AnnotatedField<? super X> field, Bean<X> bean)
    {
        return new FieldProducerFactory<>(field, bean, webBeansContext);
    }

    public <X> ProducerFactory<X> getProducerFactory(AnnotatedMethod<? super X> method, Bean<X> bean)
    {
        return new MethodProducerFactory<>(method, bean, webBeansContext);
    }

    public <X> InjectionTargetFactory<X> getInjectionTargetFactory(AnnotatedType<X> type)
    {
        return new InjectionTargetFactoryImpl<>(type, webBeansContext);
    }

    public <T> Bean<T> createBean(BeanAttributes<T> attributes, Class<T> type, InjectionTargetFactory<T> factory)
    {
        AnnotatedType annotatedType = InjectionTargetFactoryImpl.class.isInstance(factory) ?
                InjectionTargetFactoryImpl.class.cast(factory).getAnnotatedType() : getOrCreateAnnotatedType(type);

        if (WebBeansUtil.isDecorator(annotatedType))
        {
            DecoratorBeanBuilder<T> dbb = new DecoratorBeanBuilder<T>(webBeansContext, annotatedType, attributes);
            DecoratorBean<T> decorator = null;
            if (dbb.isDecoratorEnabled())
            {
                dbb.defineDecoratorRules();
                decorator = dbb.getBean();
                webBeansContext.getDecoratorsManager().addDecorator(decorator);
            }
            return decorator;
        }
        else if(WebBeansUtil.isCdiInterceptor(annotatedType))
        {
            CdiInterceptorBeanBuilder<T> ibb = new CdiInterceptorBeanBuilder<T>(webBeansContext, annotatedType, attributes);
            CdiInterceptorBean<T> interceptor = null;
            if (ibb.isInterceptorEnabled())
            {
                ibb.defineCdiInterceptorRules();
                interceptor = ibb.getBean();
                webBeansContext.getInterceptorsManager().addCdiInterceptor(interceptor);
            }
            return interceptor;
        }

        InjectionTargetBean<T> bean = new InjectionTargetBean<T>(
                webBeansContext,
                WebBeansType.THIRDPARTY,
                annotatedType,
                attributes, type, factory);

        if (webBeansContext.getOpenWebBeansConfiguration().supportsInterceptionOnProducers())
        {
            bean.defineInterceptorsIfNeeded();
        }

        return bean;
    }

    private <T> AnnotatedType<T> getOrCreateAnnotatedType(Class<T> type)
    {
        AnnotatedType<T> annotatedType = webBeansContext.getAnnotatedElementFactory().getAnnotatedType(type);
        if (annotatedType == null)
        {
            annotatedType = webBeansContext.getAnnotatedElementFactory().newAnnotatedType(type);
        }
        return annotatedType;
    }


    private boolean isBeanTypeAssignableToGivenType(Set<Type> beanTypes, Type givenType, boolean newBean, boolean producer)
    {
        for (Type beanApiType : beanTypes)
        {
            if (GenericsUtil.satisfiesDependency(false, producer, givenType, beanApiType, new HashMap<>()))
            {
                return true;
            }
            else
            {
                //Check for @New
                if (newBean && ClassUtil.isParametrizedType(givenType))
                {
                    Class<?> requiredType = ClassUtil.getClass(givenType);
                    if (ClassUtil.isClassAssignableFrom(requiredType, ClassUtil.getClass(beanApiType)))
                    {
                        return true;
                    }
                }
            }
        }

        return false;
    }


    private Object getEjbOrJmsProxyReference(Bean<?> bean,Type beanType, CreationalContext<?> creationalContext)
    {
        //Create session bean proxy
        if(bean instanceof EnterpriseBeanMarker)
        {
            if(isNormalScope(bean.getScope()))
            {
                //Maybe it is cached
                if(cacheProxies.containsKey(bean))
                {
                    return cacheProxies.get(bean);
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
        Set<Annotation> set = new HashSet<>();

        if(stereotype.isAnnotationPresent(Stereotype.class))
        {
            Collections.addAll(set, annotations);
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
        if (nonscopeAnnotations.contains(annotationType))
        {
            return false;
        }

        if (scopeAnnotations.contains(annotationType))
        {
            return true;
        }

        boolean isScopeAnnotation = annotationType.getAnnotation(Scope.class) != null ||
                annotationType.getAnnotation(NormalScope.class) != null;

        if (!isScopeAnnotation)
        {
            // also check external scopes
            for (ExternalScope es : getAdditionalScopes())
            {
                if (es.getScope().equals(annotationType))
                {
                    isScopeAnnotation = true;
                    break;
                }
            }
        }

        if (isScopeAnnotation)
        {
            scopeAnnotations.add(annotationType);
        }
        else
        {
            nonscopeAnnotations.add(annotationType);
        }

        return isScopeAnnotation;
    }

    @Override
    public boolean isNormalScope(Class<? extends Annotation> scopeType)
    {
        Boolean isNormal = isScopeTypeNormalCache.get(scopeType);

        if (isNormal != null)
        {
            return isNormal;
        }

        for(ExternalScope extScope : additionalScopes)
        {
            if (extScope.getScope().equals(scopeType))
            {
                isScopeTypeNormalCache.put(scopeType, extScope.isNormal());
                return extScope.isNormal();
            }
        }

        isNormal = scopeType.getAnnotation(NormalScope.class) != null;
        isScopeTypeNormalCache.put(scopeType, isNormal);

        return isNormal;
    }

    @Override
    public boolean isPassivatingScope(Class<? extends Annotation> annotationType)
    {
        for(ExternalScope extScope : additionalScopes)
        {
            if (extScope.getScope().equals(annotationType))
            {
                return extScope.isPassivating();
            }
        }

        NormalScope scope = annotationType.getAnnotation(NormalScope.class);

        if(scope != null)
        {
            return scope.passivating();
        }

        return false;
    }


    @Override
    public boolean isStereotype(Class<? extends Annotation> annotationType)
    {
        return AnnotationUtil.hasAnnotation(annotationType.getDeclaredAnnotations(), Stereotype.class);
    }

    public boolean areInterceptorBindingsEquivalent(Annotation annotation1, Annotation annotation2)
    {
        return AnnotationUtil.isCdiAnnotationEqual(annotation1, annotation2);
    }

    public boolean areQualifiersEquivalent(Annotation annotation1, Annotation annotation2)
    {
        return AnnotationUtil.isCdiAnnotationEqual(annotation1, annotation2);
    }

    public int getInterceptorBindingHashCode(Annotation annotation)
    {
        return AnnotationUtil.getCdiAnnotationHashCode(annotation);
    }

    public int getQualifierHashCode(Annotation annotation)
    {
        return AnnotationUtil.getCdiAnnotationHashCode(annotation);
    }

    /**
     * {@inheritDoc}
     */
    public <T> BeanAttributes<T> createBeanAttributes(AnnotatedType<T> type)
    {
        OpenWebBeansEjbPlugin ejbPlugin = webBeansContext.getPluginLoader().getEjbPlugin();
        if (ejbPlugin != null && ejbPlugin.isSessionBean(type.getJavaClass()))
        {
            return ejbPlugin.createBeanAttributes(type);
        }
        return BeanAttributesBuilder.forContext(webBeansContext).newBeanAttibutes(type).build();
    }


    public <T, X> Bean<T> createBean(BeanAttributes<T> attributes, Class<X> type, ProducerFactory<X> factory)
    {
        return new ProducerAwareInjectionTargetBean<>(
            webBeansContext,
            WebBeansType.THIRDPARTY,
            attributes,
            findClass(factory, type),
            factory);
    }

    private Class<?> findClass(ProducerFactory<?> factory, Class<?> type)
    {
        if (MethodProducerFactory.class.isInstance(factory))
        {
            return MethodProducerFactory.class.cast(factory).getReturnType();
        }
        if (FieldProducerFactory.class.isInstance(factory))
        {
            return FieldProducerFactory.class.cast(factory).getReturnType();
        }
        return type;
    }

    public <T extends Extension> T getExtension(Class<T> type)
    {
        T extension = webBeansContext.getExtensionLoader().getExtension(type);
        if (extension == null)
        {
            throw new IllegalArgumentException("extension " + type + " not registered");
        }
        return extension;
    }

    @Override
    public <X> Bean<? extends X> resolve(Set<Bean<? extends X>> beans)
    {
        return injectionResolver.resolve(beans, null);
    }

    @Override
    public <T> InterceptionFactory<T> createInterceptionFactory(CreationalContext<T> creationalContext, Class<T> clazz)
    {
        return new InterceptionFactoryImpl(
                webBeansContext, createAnnotatedType(clazz), AnnotationUtil.DEFAULT_AND_ANY_ANNOTATION_SET,
                // ok, we can need to not cast that brutally
                CreationalContextImpl.class.cast(creationalContext));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validate(InjectionPoint injectionPoint)
    {
        if (injectionPoint == null)
        {
            throw new IllegalArgumentException("InjectionPoint parameter must not be nul");
        }

        Bean<?> bean = injectionPoint.getBean();

        //Check for correct injection type
        injectionResolver.checkInjectionPointType(injectionPoint);

        Class<?> rawType = ClassUtil.getRawTypeForInjectionPoint(injectionPoint);

        // check for InjectionPoint injection
        if (rawType.equals(InjectionPoint.class))
        {
            if (AnnotationUtil.hasAnnotation(AnnotationUtil.asArray(injectionPoint.getQualifiers()), Default.class))
            {
                if (!bean.getScope().equals(Dependent.class))
                {
                    throw new WebBeansConfigurationException("Bean " + bean.getBeanClass() + " scope can not define other scope except @Dependent to inject InjectionPoint");
                }
            }
        }
        else
        {
            injectionResolver.checkInjectionPoint(injectionPoint);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> InjectionTarget<T> createInjectionTarget(AnnotatedType<T> type)
    {
        InjectionTargetFactoryImpl<T> factory = new InjectionTargetFactoryImpl<>(type, webBeansContext);
        InterceptorUtil interceptorUtil = webBeansContext.getInterceptorUtil();
        InjectionTargetImpl<T> injectionTarget = new LazyInterceptorDefinedInjectionTarget<>(
            type,
            factory.createInjectionPoints(null),
            webBeansContext,
            interceptorUtil.getLifecycleMethods(type, PostConstruct.class),
            interceptorUtil.getLifecycleMethods(type, PreDestroy.class));
        if (isAfterBeanDiscoveryDone())
        {
            try
            {
                webBeansContext.getWebBeansUtil().validate(injectionTarget.getInjectionPoints(), null);
            }
            catch (InjectionException | DeploymentException | WebBeansConfigurationException ie)
            {
                throw new IllegalArgumentException(ie);
            }
        }
        GProcessInjectionTarget event = webBeansContext.getWebBeansUtil().fireProcessInjectionTargetEvent(injectionTarget, type);
        InjectionTarget it = event.getInjectionTarget();
        event.setStarted();
        return it;
    }


    @Override
    public <T> Set<ObserverMethod<? super T>> resolveObserverMethods(T event, Annotation... qualifiers)
    {
        return resolveObserverMethods(event, new EventMetadataImpl(null, event.getClass(), null, qualifiers, webBeansContext));
    }

    public <T> Set<ObserverMethod<? super T>> resolveObserverMethods(T event, EventMetadataImpl metadata)
    {
        LinkedList<ObserverMethod<? super Object>> observerMethods
            = new LinkedList<>(webBeansContext.getNotificationManager().resolveObservers(event, metadata, false));

        // new in CDI-2.0: sort observers
        observerMethods.sort(Comparator.comparingInt(ObserverMethod::getPriority));

        return new LinkedHashSet<>(observerMethods);
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

    public void addAdditionalQualifier(AnnotatedType<? extends Annotation> qualifier)
    {
        if (qualifier != null && !additionalQualifiers.contains(qualifier.getJavaClass()))
        {
            additionalAnnotatedTypeQualifiers.put(qualifier.getJavaClass(), qualifier);
            additionalQualifiers.add(qualifier.getJavaClass());
        }
    }

    public void addAdditionalAnnotatedType(Object extension, AnnotatedType<?> annotatedType)
    {
        addAdditionalAnnotatedType(
                extension, annotatedType,
                // ensure this string is stable accross a cluster for web scopes
                extension.getClass().getName() + annotatedType + AnnotatedElementFactory.OWB_DEFAULT_KEY);
    }

    public <T> void addAdditionalAnnotatedType(Object extension, AnnotatedType<T> inAnnotatedType, String id)
    {
        if (id == null)
        {
            addAdditionalAnnotatedType(extension, inAnnotatedType);
            return;
        }

        AnnotatedType<T> annotatedType = new AnnotatedTypeWrapper<>(Extension.class.cast(extension), inAnnotatedType, id);
        if (annotatedType.getAnnotation(Vetoed.class) != null)
        {
            // we could check package here too but would be a lost of time 99.99% of the time
            return;
        }

        webBeansContext.getAnnotatedElementFactory().setAnnotatedType(annotatedType, id);
        ConcurrentMap<String, AnnotatedType<?>> annotatedTypes = additionalAnnotatedTypes.get(annotatedType.getJavaClass());
        if (annotatedTypes == null)
        {
            annotatedTypes = new ConcurrentHashMap<>();
            ConcurrentMap<String, AnnotatedType<?>> oldAnnotatedTypes = additionalAnnotatedTypes.putIfAbsent(annotatedType.getJavaClass(), annotatedTypes);
            if (oldAnnotatedTypes != null)
            {
                annotatedTypes = oldAnnotatedTypes;
            }
        }
        annotatedTypes.put(id, annotatedType);
    }

    public void removeAdditionalAnnotatedType(AnnotatedType<?> annotatedType)
    {
        removeAdditionalAnnotatedType(annotatedType, AnnotatedElementFactory.OWB_DEFAULT_KEY);
    }

    public void removeAdditionalAnnotatedType(AnnotatedType<?> annotatedType, String id)
    {
        ConcurrentMap<String, AnnotatedType<?>> annotatedTypes = additionalAnnotatedTypes.get(annotatedType.getJavaClass());
        if (annotatedTypes == null)
        {
            return;
        }
        if (annotatedType.equals(annotatedTypes.get(id)))
        {
            annotatedTypes.remove(id);
        }
    }

    public List<Class<? extends Annotation>> getAdditionalQualifiers()
    {
        return additionalQualifiers;
    }

    public Map<Class<?>, AnnotatedType<? extends Annotation>> getAdditionalAnnotatedTypeQualifiers()
    {
        return additionalAnnotatedTypeQualifiers;
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

    public Collection<AnnotatedType<?>> getAdditionalAnnotatedTypes()
    {
        Collection<AnnotatedType<?>> annotatedTypes = new ArrayList<>();
        for (ConcurrentMap<String,AnnotatedType<?>> types: additionalAnnotatedTypes.values())
        {
            annotatedTypes.addAll(types.values());
        }
        return annotatedTypes;
    }

    public <T> AnnotatedType<T> getAdditionalAnnotatedType(Class<T> type, String id)
    {
        if (id == null)
        {
            return annotatedElementFactory.getAnnotatedType(type);
        }

        ConcurrentMap<String, AnnotatedType<?>> annotatedTypes = additionalAnnotatedTypes.get(type);
        if (annotatedTypes == null)
        {
            return null;
        }
        return (AnnotatedType<T>)annotatedTypes.get(id);
    }

    public <T> String getId(Class<T> type, AnnotatedType<T> at)
    {
        ConcurrentMap<String, AnnotatedType<?>> additionals = additionalAnnotatedTypes.get(type);
        if (additionals != null)
        {
            for (Map.Entry<String, AnnotatedType<?>> entry : additionals.entrySet())
            {
                if (entry.getValue() == at)
                {
                    return entry.getKey();
                }
            }
        }
        return null;
    }

    public <T> Collection<AnnotatedType<T>> getUserAnnotatedTypes(Class<T> type)
    {
        final ConcurrentMap<String, AnnotatedType<?>> aTypes = additionalAnnotatedTypes.get(type);
        if (aTypes != null)
        {
            Collection<AnnotatedType<T>> types = new ArrayList<>(2);
            for (AnnotatedType at : aTypes.values())
            {
                types.add(at);
            }
            return types;
        }
        return null;
    }

    public <T> Iterable<AnnotatedType<T>> getAnnotatedTypes(Class<T> type)
    {
        final Collection<AnnotatedType<T>> types = new ArrayList<>(2);
        AnnotatedType<T> annotatedType = annotatedElementFactory.getAnnotatedType(type);
        if (annotatedType != null)
        {
            types.add(annotatedType);
        }
        final Collection<AnnotatedType<T>> userAnnotatedTypes = getUserAnnotatedTypes(type);
        if (userAnnotatedTypes != null)
        {
            types.addAll(userAnnotatedTypes);
        }
        return types;
    }

    public void clear()
    {
        additionalAnnotatedTypes.clear();
        additionalQualifiers.clear();
        additionalScopes.clear();
        scopeAnnotations.clear();
        nonscopeAnnotations.clear();
        clearCacheProxies();
        singleContextMap.clear();
        contextMap.clear();
        deploymentBeans.clear();
        errorStack.clear();
        producersForJavaEeComponents.clear();
        passivationBeans.clear();
        webBeansContext.getInterceptorsManager().clear();
        webBeansContext.getDecoratorsManager().clear();
        webBeansContext.getAnnotatedElementFactory().clear();

        injectionResolver.clearCaches();
        webBeansContext.getAnnotationManager().clearCaches();

        // finally destroy all SPI services
        webBeansContext.clear();
    }

    public void clearCacheProxies()
    {
        cacheProxies.clear();
    }

    public boolean isInUse()
    {
        return inUse;
    }

    public boolean isAfterDeploymentValidationFired()
    {
        return afterDeploymentValidationFired;
    }

    public void setAfterDeploymentValidationFired(boolean afterDeploymentValidationFired)
    {
        this.afterDeploymentValidationFired = afterDeploymentValidationFired;
    }

    public void setAfterBeanDiscoveryStart()
    {
        this.beanDiscoveryState = LifecycleState.DISCOVERY;
    }

    public void setAfterBeanDiscoveryDone()
    {
        this.beanDiscoveryState = LifecycleState.AFTER_DISCOVERY;
    }

    public boolean isAfterBeanDiscoveryDone()
    {
        return beanDiscoveryState == LifecycleState.AFTER_DISCOVERY;
    }

    public boolean isAfterBeanDiscovery()
    {
        return beanDiscoveryState == LifecycleState.DISCOVERY;
    }

    private enum LifecycleState
    {
        BEFORE_DISCOVERY, DISCOVERY, AFTER_DISCOVERY
    }
}
