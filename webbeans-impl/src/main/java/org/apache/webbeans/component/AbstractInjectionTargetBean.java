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
package org.apache.webbeans.component;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Decorator;

import org.apache.webbeans.config.OWBLogConst;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.decorator.WebBeansDecorator;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.exception.WebBeansException;
import org.apache.webbeans.intercept.InterceptorData;
import javax.enterprise.inject.spi.InterceptionType;

import org.apache.webbeans.intercept.InvocationContextImpl;
import org.apache.webbeans.intercept.webbeans.WebBeansInterceptorBeanPleaseRemove;
import org.apache.webbeans.logger.WebBeansLoggerFacade;
import org.apache.webbeans.portable.InjectionTargetImpl;
import org.apache.webbeans.proxy.ProxyFactory;
import org.apache.webbeans.spi.ResourceInjectionService;
import org.apache.webbeans.util.Asserts;
import org.apache.webbeans.util.WebBeansUtil;

/**
 * Abstract class for injection target beans.
 * 
 * @version $Rev$ $Date$
 * @param <T> bean class
 */
public abstract class AbstractInjectionTargetBean<T> extends AbstractOwbBean<T> implements InjectionTargetBean<T>
{    
    /**Annotated type for bean*/
    private AnnotatedType<T> annotatedType;
    
    /**
     * Holds the all of the interceptor related data, contains around-invoke,
     * post-construct and pre-destroy
     * @deprecated old InterceptorData based config
     */
    protected List<InterceptorData> interceptorStack = new ArrayList<InterceptorData>();

    /**
     * Decorators
     * @deprecated will be replaced by InterceptorResolution logic
     */
    protected List<Decorator<?>> decorators = new ArrayList<Decorator<?>>();
    
    protected AbstractInjectionTargetBean(WebBeansContext webBeansContext,
                                          WebBeansType webBeansType,
                                          AnnotatedType<T> annotatedType,
                                          Set<Type> types,
                                          Set<Annotation> qualifiers,
                                          Class<? extends Annotation> scope,
                                          Class<T> beanClass,
                                          Set<Class<? extends Annotation>> stereotypes)
    {
        this(webBeansContext, webBeansType, annotatedType, types, qualifiers, scope, null, beanClass, stereotypes, false);
        setEnabled(true);
    }

    /**
     * Creates a new observer owner component.
     * 
     * @param webBeansType webbean type
     * @param returnType bean class type
     * @param webBeansContext
     */
    protected AbstractInjectionTargetBean(WebBeansContext webBeansContext,
                                          WebBeansType webBeansType,
                                          AnnotatedType<T> annotatedType,
                                          Set<Type> types,
                                          Set<Annotation> qualifiers,
                                          Class<? extends Annotation> scope,
                                          String name,
                                          Class<T> beanClass,
                                          Set<Class<? extends Annotation>> stereotypes,
                                          boolean alternative)
    {
        super(webBeansContext, webBeansType, types, qualifiers, scope, name, false, beanClass, stereotypes, alternative);
        Asserts.assertNotNull(annotatedType, "AnnotatedType may not be null");
        this.annotatedType = annotatedType;
    }

    /**
     * {@inheritDoc}
     */
    protected T createInstance(CreationalContext<T> creationalContext)
    {
        T instance;

        //Default creation phases
        instance = createDefaultInstance(creationalContext);

        return instance;
    }

    /**
     * Returns bean instance.
     * 
     * @param creationalContext creational context
     * @return bean instance
     */
    @SuppressWarnings("unchecked")
    protected T createDefaultInstance(CreationalContext<T> creationalContext)
    {
        beforeConstructor();
        
        //Create actual bean instance
        T instance = createComponentInstance(creationalContext);
        //For dependent instance checks
        T dependentProxy = null;
        boolean isDependentProxy = false;
        if(getScope() == Dependent.class && !(this instanceof EnterpriseBeanMarker))
        {
            final ProxyFactory proxyFactory = getWebBeansContext().getProxyFactory();
            T result = (T) proxyFactory.createDependentScopedBeanProxy(this, instance, creationalContext);
            //Means that Dependent Bean has interceptor/decorator
            if(proxyFactory.isProxyInstance(result))
            {
                //This is a dependent scoped bean instance,
                //Therefore we inject dependencies of this instance
                //Otherwise we loose injection
                injectResources(instance, creationalContext);
                new InjectionTargetImpl<T>(getAnnotatedType(), getInjectionPoints(), webBeansContext).inject(instance, creationalContext);                

                //Dependent proxy
                dependentProxy = result;
                
                //This is a dependent
                isDependentProxy = true;
            }
        }
                        
        
        //If dependent proxy
        if(isDependentProxy)
        {
            return dependentProxy;
        }
        
        return instance;
    }

    /**
     * {@inheritDoc}
     */
    protected void destroyInstance(T instance, CreationalContext<T> creationalContext)
    {
        destroyComponentInstance(instance,creationalContext);
    }

    /**
     * Sub-classes must override this method to create bean instance.
     * 
     * @param creationalContext creational context
     * @return bean instance
     */
    protected T createComponentInstance(CreationalContext<T> creationalContext)
    {
        return null;
    }

    /**
     * Sub-classes must override this method to destroy bean instance.
     * 
     * @param instance object instance.
     */
    protected void destroyComponentInstance(T instance, CreationalContext<T> creationalContext)
    {
        preDestroy(instance, creationalContext);
    }

    /**
     * Called before constructor.
     */
    protected void beforeConstructor()
    {

    }

    /**
     * {@inheritDoc}
     */
    public void postConstruct(T instance, CreationalContext<T> cretionalContext)
    {
        postConstructDefault(instance, cretionalContext);
    }

    /**
     * Default post construct.
     * 
     * @param instance bean instance
     */
    protected void postConstructDefault(T instance, CreationalContext<T> ownerCreationalContext)
    {
        if(getWebBeansType().equals(WebBeansType.MANAGED))
        {
            // Call Post Construct
            if (WebBeansUtil.isContainsInterceptorMethod(getInterceptorStack(), InterceptionType.POST_CONSTRUCT))
            {
                InvocationContextImpl impl = new InvocationContextImpl(getWebBeansContext(), null, instance, null, null,
                        getWebBeansContext().getInterceptorUtil().getInterceptorMethods(getInterceptorStack(),
                                                                                        InterceptionType.POST_CONSTRUCT),
                                                                                        InterceptionType.POST_CONSTRUCT);
                impl.setCreationalContext(ownerCreationalContext);
                try
                {
                    impl.proceed();
                }

                catch (Exception e)
                {
                    getLogger().log(Level.SEVERE, WebBeansLoggerFacade.constructMessage(OWBLogConst.ERROR_0008, "@PostConstruct."), e);
                    throw new WebBeansException(e);
                }
            }            
        }        
    }

    /**
     * {@inheritDoc}
     */
    public void preDestroy(T instance, CreationalContext<T> creationalContext)
    {
        preDestroyDefault(instance, creationalContext);
    }

    /**
     * Default predestroy.
     * 
     * @param instance bean instance
     */
    protected void preDestroyDefault(T instance, CreationalContext<T> creationalContext)
    {
        if(getWebBeansType().equals(WebBeansType.MANAGED) ||
                getWebBeansType().equals(WebBeansType.DECORATOR))                
        {
            if (WebBeansUtil.isContainsInterceptorMethod(getInterceptorStack(), InterceptionType.PRE_DESTROY))
            {                
                InvocationContextImpl impl = new InvocationContextImpl(getWebBeansContext(), null, instance, null, null,
                        getWebBeansContext().getInterceptorUtil().getInterceptorMethods(getInterceptorStack(),
                                                                                        InterceptionType.PRE_DESTROY),
                                                                                        InterceptionType.PRE_DESTROY);
                impl.setCreationalContext(creationalContext);
                try
                {
                    impl.proceed();
                }
                catch (Exception e)
                {
                    getLogger().log(Level.SEVERE, WebBeansLoggerFacade.constructMessage(OWBLogConst.ERROR_0008, "@PreDestroy."), e);
                    throw new WebBeansException(e);
                }
            }            
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public void injectResources(T instance, CreationalContext<T> creationalContext)
    {
        if(getWebBeansType().equals(WebBeansType.MANAGED))
        {
            try
            {
                ResourceInjectionService service = null;
                try
                {
                    service = getWebBeansContext().getService(ResourceInjectionService.class);
                    
                }
                catch(Exception e)
                {
                    // When running in tests
                }
                
                if(service != null)
                {
                    service.injectJavaEEResources(instance);   
                }
            }
            catch (Exception e)
            {
                getLogger().log(Level.SEVERE, OWBLogConst.ERROR_0023, instance);
                throw new WebBeansException(MessageFormat.format(
                        WebBeansLoggerFacade.getTokenString(OWBLogConst.ERROR_0023), instance), e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public List<InterceptorData> getInterceptorStack()
    {
        return interceptorStack;
    }
    
    public List<Decorator<?>> getDecoratorStack()
    {
        return decorators;
    }

    /**
     * {@inheritDoc}
     */
    public AnnotatedType<T> getAnnotatedType()
    {
        return annotatedType;
    }
    
    /* (non-Javadoc)
     * @see org.apache.webbeans.component.AbstractOwbBean#validatePassivationDependencies()
     */
    @Override
    public void validatePassivationDependencies()
    {        
        super.validatePassivationDependencies();
        
        //Check for interceptors and decorators
        for(int i = 0, size = decorators.size(); i < size; i++)
        {
            Decorator<?> dec = decorators.get(i);
            WebBeansDecorator<?> decorator = (WebBeansDecorator<?>)dec;
            if(!decorator.isPassivationCapable())
            {
                throw new WebBeansConfigurationException(MessageFormat.format(
                        WebBeansLoggerFacade.getTokenString(OWBLogConst.EXCEPT_0015), toString()));
            }
            else
            {
                decorator.validatePassivationDependencies();
            }
        }
        
        for(int i = 0, size = interceptorStack.size(); i < size; i++)
        {
            InterceptorData interceptorData = interceptorStack.get(i);
            if(interceptorData.isDefinedWithWebBeansInterceptor())
            {
                WebBeansInterceptorBeanPleaseRemove<?> interceptor = (WebBeansInterceptorBeanPleaseRemove<?>)interceptorData.getWebBeansInterceptor();
                if(!interceptor.isPassivationCapable())
                {
                    throw new WebBeansConfigurationException(MessageFormat.format(
                            WebBeansLoggerFacade.getTokenString(OWBLogConst.EXCEPT_0016), toString()));
                }
                else
                {
                    interceptor.validatePassivationDependencies();
                }
            }
            else
            {
                if(interceptorData.isDefinedInInterceptorClass())
                {
                    Class<?> interceptorClass = interceptorData.getInterceptorClass();
                    if(!Serializable.class.isAssignableFrom(interceptorClass))
                    {
                        throw new WebBeansConfigurationException(MessageFormat.format(
                                WebBeansLoggerFacade.getTokenString(OWBLogConst.EXCEPT_0016), toString()));
                    }               
                    else
                    {
                        if(!getWebBeansContext().getAnnotationManager().checkInjectionPointForInterceptorPassivation(interceptorClass))
                        {
                            throw new WebBeansConfigurationException(MessageFormat.format(
                                    WebBeansLoggerFacade.getTokenString(OWBLogConst.EXCEPT_0017), toString(), interceptorClass));
                        }
                    }
                }
            }
        }
    }    
    
    
    
}
