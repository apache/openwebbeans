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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import javax.enterprise.inject.spi.AnnotatedType;

import org.apache.webbeans.component.spi.BeanAttributes;
import org.apache.webbeans.component.spi.InjectionTargetFactory;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.container.InjectionTargetFactoryImpl;

import javax.enterprise.inject.spi.InjectionTarget;
import javax.enterprise.inject.spi.InterceptionType;
import javax.enterprise.inject.spi.Interceptor;
import javax.interceptor.AroundInvoke;

import org.apache.webbeans.intercept.InterceptorResolutionService;
import org.apache.webbeans.portable.InjectionTargetImpl;
import org.apache.webbeans.proxy.InterceptorDecoratorProxyFactory;
import org.apache.webbeans.util.Asserts;
import org.apache.webbeans.util.CDI11s;


/**
 * Abstract class for injection target beans.
 * 
 * @version $Rev$ $Date$
 * @param <T> bean class
 */
public class InjectionTargetBean<T> extends AbstractOwbBean<T>
{    
    /**Annotated type for bean*/
    private AnnotatedType<T> annotatedType;
    private InjectionTarget<T> injectionTarget;

    public InjectionTargetBean(
            WebBeansContext webBeansContext,
            WebBeansType webBeansType,
            AnnotatedType<T> annotatedType,
            BeanAttributes<T> beanAttributes,
            Class<T> beanClass)
    {
        this(webBeansContext, webBeansType, annotatedType, beanAttributes, beanClass, new InjectionTargetFactoryImpl<T>(annotatedType, webBeansContext));
    }

    /**
     * Initializes the InjectionTarget Bean part.
     */
    public InjectionTargetBean(WebBeansContext webBeansContext,
            WebBeansType webBeansType,
            AnnotatedType<T> annotatedType,
            BeanAttributes<T> beanAttributes,
            Class<T> beanClass,
            InjectionTargetFactory<T> factory)
    {
        super(webBeansContext, webBeansType, beanAttributes, beanClass, false);
        Asserts.assertNotNull(annotatedType, "AnnotatedType may not be null");
        this.annotatedType = annotatedType;
        injectionTarget = factory.createInjectionTarget(this);
        setEnabled(true);
    }

    @Override
    public InjectionTarget<T> getProducer()
    {
        return injectionTarget;
    }

    public InjectionTarget<T> getInjectionTarget()
    {
        return injectionTarget;
    }

    /**
     * {@inheritDoc}
     */
    public AnnotatedType<T> getAnnotatedType()
    {
        return annotatedType;
    }

    /**
     *
     * Configure bean instance interceptor stack.
     *
     * This method gets invoked in the ValidateBean phase and will fill all the
     * interceptor information into the given InjectionTargetBean
     *
     */
    public <T> void defineBeanInterceptorStack()
    {
        if (this instanceof InterceptedMarker && getInjectionTarget() instanceof InjectionTargetImpl)
        {
            InjectionTargetImpl<T> injectionTarget = (InjectionTargetImpl<T>) getInjectionTarget();
            InterceptorResolutionService.BeanInterceptorInfo interceptorInfo = webBeansContext.getInterceptorResolutionService().
                    calculateInterceptorInfo(getTypes(), getQualifiers(), getAnnotatedType());

            Map<Method, List<Interceptor<?>>> methodInterceptors = new HashMap<Method, List<Interceptor<?>>>();
            for (Map.Entry<Method, InterceptorResolutionService.BusinessMethodInterceptorInfo> miEntry : interceptorInfo.getBusinessMethodsInfo().entrySet())
            {
                Method interceptedMethod = miEntry.getKey();
                InterceptorResolutionService.BusinessMethodInterceptorInfo mii = miEntry.getValue();
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

            List<Interceptor<?>> postConstructInterceptors
                    = getLifecycleInterceptors(interceptorInfo.getEjbInterceptors(), interceptorInfo.getCdiInterceptors(), InterceptionType.POST_CONSTRUCT);

            List<Interceptor<?>> preDestroyInterceptors
                    = getLifecycleInterceptors(interceptorInfo.getEjbInterceptors(), interceptorInfo.getCdiInterceptors(), InterceptionType.PRE_DESTROY);

            final List<Interceptor<?>> aroundConstruct;
            if (CDI11s.AROUND_CONSTRUCT != null)
            {
                aroundConstruct = getLifecycleInterceptors(interceptorInfo.getEjbInterceptors(), interceptorInfo.getCdiInterceptors(), CDI11s.AROUND_CONSTRUCT);
            }
            else
            {
                aroundConstruct = new ArrayList<Interceptor<?>>();
            }

            Class<? extends T> proxyClass = null;
            if (methodInterceptors.size() > 0 || postConstructInterceptors.size() > 0 || preDestroyInterceptors.size() > 0)
            {
                // we only need to create a proxy class for intercepted or decorated Beans
                InterceptorDecoratorProxyFactory pf = webBeansContext.getInterceptorDecoratorProxyFactory();

                ClassLoader classLoader = getBeanClass().getClassLoader();

                Method[] businessMethods = methodInterceptors.keySet().toArray(new Method[methodInterceptors.size()]);
                Method[] nonInterceptedMethods = interceptorInfo.getNonInterceptedMethods().toArray(new Method[interceptorInfo.getNonInterceptedMethods().size()]);

                proxyClass = (Class<? extends T>) pf.createProxyClass(this, classLoader, getReturnType(), businessMethods, nonInterceptedMethods);

                // now we collect the post-construct and pre-destroy interceptors

            }

            injectionTarget.setInterceptorInfo(interceptorInfo, proxyClass, methodInterceptors, postConstructInterceptors, preDestroyInterceptors, aroundConstruct, getId());
        }

    }


    private List<Interceptor<?>> getLifecycleInterceptors(LinkedHashSet<Interceptor<?>> ejbInterceptors, List<Interceptor<?>> cdiInterceptors, InterceptionType interceptionType)
    {
        List<Interceptor<?>> lifecycleInterceptors = new ArrayList<Interceptor<?>>();

        for (Interceptor<?> ejbInterceptor : ejbInterceptors)
        {
            if (ejbInterceptor.intercepts(interceptionType))
            {
                lifecycleInterceptors.add(ejbInterceptor);
            }
        }
        for (Interceptor<?> cdiInterceptor : cdiInterceptors)
        {
            if (cdiInterceptor.intercepts(interceptionType))
            {
                lifecycleInterceptors.add(cdiInterceptor);
            }
        }

        return lifecycleInterceptors;
    }


}
