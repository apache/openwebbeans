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
package org.apache.webbeans.portable;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.decorator.Delegate;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.Decorator;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.Interceptor;
import javax.enterprise.inject.spi.PassivationCapable;
import javax.enterprise.inject.spi.Producer;
import javax.interceptor.AroundInvoke;

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.context.creational.CreationalContextImpl;
import org.apache.webbeans.intercept.DecoratorHandler;
import org.apache.webbeans.intercept.DefaultInterceptorHandler;
import org.apache.webbeans.intercept.InterceptorResolutionService.BeanInterceptorInfo;
import org.apache.webbeans.intercept.InterceptorResolutionService.BusinessMethodInterceptorInfo;
import org.apache.webbeans.proxy.InterceptorDecoratorProxyFactory;
import org.apache.webbeans.proxy.InterceptorHandler;
import org.apache.webbeans.proxy.OwbInterceptorProxy;

public abstract class AbstractProducer<T> implements Producer<T>
{

    private Set<InjectionPoint> injectionPoints;
    private Class<? extends T> proxyClass;
    private String passivationId;
    private BeanInterceptorInfo interceptorInfo;
    private InterceptorDecoratorProxyFactory proxyFactory;
    private Map<Method, List<Interceptor<?>>> methodInterceptors;

    public AbstractProducer()
    {
        this(Collections.<InjectionPoint>emptySet());
    }

    public AbstractProducer(Set<InjectionPoint> points)
    {
        // this shares the InjectionPoints with the owning component for now
        injectionPoints = points;
    }

    /**
     *
     * Configure bean instance interceptor stack.
     *
     * This method gets invoked in the ValidateBean phase and will fill all the
     * interceptor information into the given InjectionTargetBean
     *
     */
    public void defineInterceptorStack(Bean<T> bean, AnnotatedType<T> annotatedType, WebBeansContext webBeansContext)
    {
        interceptorInfo = webBeansContext.getInterceptorResolutionService().
                calculateInterceptorInfo(bean.getTypes(), bean.getQualifiers(), annotatedType);
        proxyFactory = webBeansContext.getInterceptorDecoratorProxyFactory();
        if (bean instanceof PassivationCapable)
        {
            PassivationCapable passivationCapable = (PassivationCapable)bean;
            passivationId = passivationCapable.getId();
        }

        methodInterceptors = new HashMap<Method, List<Interceptor<?>>>();
        for (Map.Entry<Method, BusinessMethodInterceptorInfo> miEntry : interceptorInfo.getBusinessMethodsInfo().entrySet())
        {
            Method interceptedMethod = miEntry.getKey();
            BusinessMethodInterceptorInfo mii = miEntry.getValue();
            List<Interceptor<?>> activeInterceptors = new ArrayList<Interceptor<?>>();

            if (mii.getEjbInterceptors() != null)
            {
                Collections.addAll(activeInterceptors, mii.getEjbInterceptors());
            }
            if (mii.getCdiInterceptors() != null)
            {
                Collections.addAll(activeInterceptors, mii.getCdiInterceptors());
            }
            if (interceptorInfo.getSelfInterceptorBean() != null)
            {
                if (interceptedMethod.getAnnotation(AroundInvoke.class) == null) // this check is a dirty hack for now to prevent infinite loops
                {
                    // add self-interception as last interceptor in the chain.
                    activeInterceptors.add(interceptorInfo.getSelfInterceptorBean());
                }
            }

            if (activeInterceptors.size() > 0)
            {
                methodInterceptors.put(interceptedMethod, activeInterceptors);
            }
            else if (mii.getMethodDecorators() != null)
            {
                methodInterceptors.put(interceptedMethod, Collections.EMPTY_LIST);
            }
        }

        defineLifecycleInterceptors(bean, annotatedType, webBeansContext);

        if (needsProxy())
        {
            // we only need to create a proxy class for intercepted or decorated Beans
            InterceptorDecoratorProxyFactory pf = webBeansContext.getInterceptorDecoratorProxyFactory();

            ClassLoader classLoader = annotatedType.getJavaClass().getClassLoader();

            Method[] businessMethods = methodInterceptors.keySet().toArray(new Method[methodInterceptors.size()]);
            Method[] nonInterceptedMethods = interceptorInfo.getNonInterceptedMethods().toArray(new Method[interceptorInfo.getNonInterceptedMethods().size()]);

            proxyClass = (Class<? extends T>) pf.createProxyClass(bean, classLoader, annotatedType.getJavaClass(), businessMethods, nonInterceptedMethods);

            // now we collect the post-construct and pre-destroy interceptors

        }
    }
    
    @Override
    public Set<InjectionPoint> getInjectionPoints()
    {
        return injectionPoints;
    }

    public BeanInterceptorInfo getInterceptorInfo()
    {
        return interceptorInfo;
    }

    @Override
    public T produce(CreationalContext<T> creationalContext)
    {
        final CreationalContextImpl<T> creationalContextImpl = (CreationalContextImpl<T>) creationalContext;

        final Map<Interceptor<?>,Object> interceptorInstances  = new HashMap<Interceptor<?>, Object>();
        final Contextual<T> oldContextual = creationalContextImpl.getContextual();

        if (interceptorInfo != null)
        {
            // apply interceptorInfo

            // create EJB-style interceptors
            for (final Interceptor interceptorBean : interceptorInfo.getEjbInterceptors())
            {
                creationalContextImpl.putContextual(interceptorBean);
                interceptorInstances.put(interceptorBean, interceptorBean.create(creationalContext));
            }

            // create CDI-style interceptors
            for (final Interceptor interceptorBean : interceptorInfo.getCdiInterceptors())
            {
                creationalContextImpl.putContextual(interceptorBean);
                interceptorInstances.put(interceptorBean, interceptorBean.create(creationalContext));
            }
            for (final Interceptor interceptorBean : interceptorInfo.getConstructorCdiInterceptors())
            {
                creationalContextImpl.putContextual(interceptorBean);
                interceptorInstances.put(interceptorBean, interceptorBean.create(creationalContext));
            }
        }
        creationalContextImpl.putContextual(oldContextual);

        T instance = produce(interceptorInstances, creationalContextImpl);

        if (hasInterceptorInfo())
        {
            // register the bean itself for self-interception
            if (interceptorInfo.getSelfInterceptorBean() != null)
            {
                interceptorInstances.put(interceptorInfo.getSelfInterceptorBean(), instance);
            }

            T delegate = instance;
            if (interceptorInfo.getDecorators() != null && !isDelegateInjection(creationalContextImpl))
            {
                List<Decorator<?>> decorators = interceptorInfo.getDecorators();
                Map<Decorator<?>, Object> instances = new HashMap<Decorator<?>, Object>();
                for (int i = decorators.size(); i > 0; i--)
                {
                    Decorator decorator = decorators.get(i - 1);
                    creationalContextImpl.putContextual(decorator);
                    creationalContextImpl.putDelegate(delegate);
                    Object decoratorInstance = decorator.create((CreationalContext) creationalContext);
                    instances.put(decorator, decoratorInstance);
                    delegate = proxyFactory.createProxyInstance(proxyClass, instance, new DecoratorHandler(interceptorInfo, instances, i - 1, instance, passivationId));
                }
            }
            InterceptorHandler interceptorHandler = new DefaultInterceptorHandler<T>(instance, delegate, methodInterceptors, interceptorInstances, passivationId);

            T proxyInstance = proxyFactory.createProxyInstance(proxyClass, instance, interceptorHandler);
            instance = proxyInstance;
            creationalContextImpl.putContextual(oldContextual);
        }

        return instance;
    }

    @Override
    public void dispose(T instance)
    {
    }

    protected abstract T produce(Map<Interceptor<?>, ?> interceptorInstances, CreationalContextImpl<T> creationalContext);
    
    protected InterceptorDecoratorProxyFactory getProxyFactory()
    {
        return proxyFactory;
    }

    protected Map<Method, List<Interceptor<?>>> getMethodInterceptors()
    {
        return methodInterceptors;
    }
    
    protected boolean needsProxy()
    {
        return methodInterceptors.size() != 0;
    }

    protected boolean hasInterceptorInfo()
    {
        return interceptorInfo != null && proxyClass != null;
    }

    protected boolean isDelegateInjection(final CreationalContextImpl<?> cc)
    {
        final InjectionPoint ip = cc.getInjectionPoint();
        if (ip == null)
        {
            return false;
        }
        return ip.getAnnotated().isAnnotationPresent(Delegate.class);
    }

    /**
     * Helper method to unwrap the internal proxy instance.
     * Returns the instance directly if this is not a proxied instance.
     */
    protected T unwrapProxyInstance(T probableProxyInstance)
    {
        if (proxyFactory != null && probableProxyInstance instanceof OwbInterceptorProxy)
        {
            return proxyFactory.unwrapInstance(probableProxyInstance);
        }

        return probableProxyInstance;
    }

    protected void defineLifecycleInterceptors(Bean<T> bean, AnnotatedType<T> annotatedType, WebBeansContext webBeansContext)
    {
        
    }
}
