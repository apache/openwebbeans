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

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.configurator.AnnotatedTypeConfiguratorImpl;
import org.apache.webbeans.context.creational.CreationalContextImpl;
import org.apache.webbeans.intercept.InterceptorResolutionService;
import org.apache.webbeans.portable.AnnotatedTypeImpl;
import org.apache.webbeans.proxy.InterceptorDecoratorProxyFactory;
import org.apache.webbeans.util.WebBeansUtil;

import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.InterceptionFactory;
import jakarta.enterprise.inject.spi.Interceptor;
import jakarta.enterprise.inject.spi.configurator.AnnotatedTypeConfigurator;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class InterceptionFactoryImpl<T> implements InterceptionFactory<T> /*todo: make it serializable*/
{
    private final CreationalContextImpl<T> creationalContext;
    private final AnnotatedTypeConfiguratorImpl<T> configurator;
    private final Set<Annotation> qualifiers;
    private final WebBeansContext context;
    private boolean ignoreFinals;
    private volatile boolean called;

    public InterceptionFactoryImpl(WebBeansContext context, AnnotatedType<T> at,
                                   Set<Annotation> qualifiers, CreationalContextImpl<T> cc)
    {
        this.context = context;
        this.configurator = new AnnotatedTypeConfiguratorImpl<>(context, at);
        this.qualifiers = qualifiers;
        this.creationalContext = cc;
    }

    @Override
    public InterceptionFactory<T> ignoreFinalMethods()
    {
        ignoreFinals = true;
        return this;
    }

    @Override
    public AnnotatedTypeConfigurator<T> configure()
    {
        return configurator;
    }

    @Override
    public T createInterceptedInstance(T originalInstance)
    {
        check();

        ClassLoader classLoader = originalInstance.getClass().getClassLoader();
        if (classLoader == null)
        {
            classLoader = WebBeansUtil.getCurrentClassLoader();
        }

        InterceptorDecoratorProxyFactory factory = context.getInterceptorDecoratorProxyFactory();
        AnnotatedTypeImpl<T> newAnnotatedType = configurator.getNewAnnotatedType();
        InterceptorResolutionService.BeanInterceptorInfo interceptorInfo =
                context.getInterceptorResolutionService()
                    .calculateInterceptorInfo(newAnnotatedType.getTypeClosure(), qualifiers, newAnnotatedType, !ignoreFinals);
        Class<T> subClass = factory.getCachedProxyClass(interceptorInfo, newAnnotatedType, classLoader);

        Map<Interceptor<?>,Object> interceptorInstances  = context.getInterceptorResolutionService()
                .createInterceptorInstances(interceptorInfo, creationalContext);

        Map<Method, List<Interceptor<?>>> methodInterceptors =
                context.getInterceptorResolutionService().createMethodInterceptors(interceptorInfo);

        // this is a good question actually, should we even support it?
        String passivationId = InterceptionFactory.class.getName() + ">>" + newAnnotatedType.toString();

        return context.getInterceptorResolutionService().createProxiedInstance(
                originalInstance, creationalContext, creationalContext, interceptorInfo, subClass,
                methodInterceptors, passivationId, interceptorInstances, c -> false, (a, d) -> d);
    }

    private void check()
    {
        boolean ok = false;
        if (!called)
        {
            synchronized (this)
            {
                if (!called)
                {
                    called = true;
                    ok = true;
                }
            }
        }
        if (!ok)
        {
            throw new IllegalStateException("createInterceptedInstance() can be called only once");
        }
    }
}
