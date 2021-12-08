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
import java.util.Collections;
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

import org.apache.webbeans.component.BeanManagerBean;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.context.creational.CreationalContextImpl;
import org.apache.webbeans.intercept.InterceptorResolutionService.BeanInterceptorInfo;
import org.apache.webbeans.proxy.InterceptorDecoratorProxyFactory;
import org.apache.webbeans.proxy.OwbInterceptorProxy;

public abstract class AbstractProducer<T> implements Producer<T>
{

    protected Set<InjectionPoint> injectionPoints;
    protected Class<? extends T> proxyClass;
    protected String passivationId;
    protected BeanInterceptorInfo interceptorInfo;
    protected InterceptorDecoratorProxyFactory proxyFactory;
    protected Map<Method, List<Interceptor<?>>> methodInterceptors;

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
        if (bean instanceof BeanManagerBean)
        {
            // the BeanManager cannot be decorated nor intercepted
            return;
        }

        interceptorInfo = webBeansContext.getInterceptorResolutionService().
                calculateInterceptorInfo(bean.getTypes(), bean.getQualifiers(), annotatedType, false);
        proxyFactory = webBeansContext.getInterceptorDecoratorProxyFactory();
        if (bean instanceof PassivationCapable)
        {
            PassivationCapable passivationCapable = (PassivationCapable)bean;
            passivationId = passivationCapable.getId();
        }

        methodInterceptors = webBeansContext.getInterceptorResolutionService().createMethodInterceptors(interceptorInfo);

        defineLifecycleInterceptors(bean, annotatedType, webBeansContext);

        if (needsProxy())
        {
            // we only need to create a proxy class for intercepted or decorated Beans
            InterceptorDecoratorProxyFactory pf = webBeansContext.getInterceptorDecoratorProxyFactory();

            ClassLoader classLoader = webBeansContext.getApplicationBoundaryService().getBoundaryClassLoader(annotatedType.getJavaClass());

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
        CreationalContextImpl<T> creationalContextImpl = (CreationalContextImpl<T>) creationalContext;

        Contextual<T> oldContextual = creationalContextImpl.getContextual();

        Map<Interceptor<?>, Object> interceptorInstances = creationalContextImpl.getWebBeansContext()
                .getInterceptorResolutionService().createInterceptorInstances(interceptorInfo, creationalContextImpl);
        creationalContextImpl.putContextual(oldContextual);

        T instance = produce(interceptorInstances, creationalContextImpl);

        if (hasInterceptorInfo() && !(instance instanceof OwbInterceptorProxy))
        {
            instance = creationalContextImpl.getWebBeansContext().getInterceptorResolutionService()
                .createProxiedInstance(instance, creationalContextImpl, creationalContext,
                        interceptorInfo, proxyClass, methodInterceptors, passivationId, interceptorInstances,
                        this::isDelegateInjection, this::filterDecorators);
            creationalContextImpl.putContextual(oldContextual);
        }

        return instance;
    }

    protected List<Decorator<?>> filterDecorators(T instance, List<Decorator<?>> decorators)
    {
        return decorators;
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
        return !methodInterceptors.isEmpty();
    }

    protected boolean hasInterceptorInfo()
    {
        return interceptorInfo != null && proxyClass != null;
    }

    protected boolean isDelegateInjection(CreationalContextImpl<?> cc)
    {
        InjectionPoint ip = cc.getInjectionPoint();
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
