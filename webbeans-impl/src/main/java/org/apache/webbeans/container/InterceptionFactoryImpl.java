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
import org.apache.webbeans.configurator.TrackingAnnotatedTypeConfiguratorImpl;
import org.apache.webbeans.context.creational.CreationalContextImpl;
import org.apache.webbeans.intercept.InterceptorResolutionService;
import org.apache.webbeans.proxy.InterceptorDecoratorProxyFactory;
import org.apache.webbeans.util.WebBeansUtil;

import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.InterceptionFactory;
import jakarta.enterprise.inject.spi.configurator.AnnotatedTypeConfigurator;
import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import static java.util.Optional.ofNullable;

public class InterceptionFactoryImpl<T> implements InterceptionFactory<T> /*todo: make it serializable*/
{
    private final CreationalContextImpl<T> creationalContext;
    private final Set<Annotation> qualifiers;
    private final WebBeansContext context;
    private final AnnotatedType<T> at;
    private TrackingAnnotatedTypeConfiguratorImpl<T> configurator;
    private boolean ignoreFinals;
    private volatile boolean called;

    public InterceptionFactoryImpl(WebBeansContext context, AnnotatedType<T> at,
                                   Set<Annotation> qualifiers, CreationalContextImpl<T> cc)
    {
        this.context = context;
        this.configurator = null; // computed later
        this.qualifiers = qualifiers;
        this.creationalContext = cc;
        this.at = at;
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
        if (configurator == null)
        {
            // configurator = new AnnotatedTypeConfiguratorImpl<>(context, at);
            AnnotatedTypeConfiguratorImpl<T> realConfig = new AnnotatedTypeConfiguratorImpl<>(context, at);
            configurator = new TrackingAnnotatedTypeConfiguratorImpl<>(realConfig);
        }
        return configurator;
    }

    @Override
    public T createInterceptedInstance(T originalInstance)
    {
        check();

        final var classLoader = ofNullable(originalInstance.getClass().getClassLoader())
                .orElseGet(WebBeansUtil::getCurrentClassLoader);

        var newAnnotatedType = configurator == null ? at : configurator.getNewAnnotatedType();
        newAnnotatedType.getTypeClosure(); // make sure the toString bellow is accurate
        var passivationId = InterceptionFactory.class.getName() + ">>" + newAnnotatedType + "<<" + ignoreFinals;

        // if configure() has not been called, we need to create a new configurator with the muted annotated type
        if (configurator != null) // meaning app changed dynamically the annotated type configuration
        {
            passivationId = passivationId + ">>" + configurator.getPassivationId();
        }

        var interceptorResolutionService = context.getInterceptorResolutionService();
        var cache = context
                .getWebBeansUtil()
                .getInterceptionFactoryCache()
                .computeIfAbsent(passivationId, () -> {
                    InterceptorResolutionService.BeanInterceptorInfo interceptorInfo =
                            interceptorResolutionService
                                    .calculateInterceptorInfo(newAnnotatedType.getTypeClosure(), qualifiers, newAnnotatedType, !ignoreFinals);
                    InterceptorDecoratorProxyFactory factory = context.getInterceptorDecoratorProxyFactory();
                    return new InterceptionFactoryCacheEntry(
                            factory.createProxyClass(interceptorInfo, newAnnotatedType, classLoader),
                            interceptorInfo);
                });

        var interceptorInstances  = interceptorResolutionService
            .createInterceptorInstances(cache.interceptorInfo, creationalContext);
        var methodInterceptors = interceptorResolutionService.createMethodInterceptors(cache.interceptorInfo);
        return interceptorResolutionService.createProxiedInstance(
                originalInstance, creationalContext, creationalContext, cache.interceptorInfo,
                (Class<? extends T>) cache.proxyClass,
                methodInterceptors, passivationId, interceptorInstances,
                c -> false, (a, d) -> d);
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

    public static class InterceptionFactoryCache
    {
        private final Map<String, InterceptionFactoryCacheEntry> cache = new ConcurrentHashMap<>();

        private InterceptionFactoryCacheEntry computeIfAbsent(
                final String interceptionFactoryCacheKey, final Supplier<InterceptionFactoryCacheEntry> compute)
        {
            var entry = cache.get(interceptionFactoryCacheKey);
            if (entry == null)
            {
                // we do not want to create twice a proxy class,
                // "bottleneck" but quickly cached
                // so "ok"ish
                synchronized (this)
                {
                    entry = cache.get(interceptionFactoryCacheKey);
                    if (entry == null)
                    {
                        entry = compute.get();
                        cache.putIfAbsent(interceptionFactoryCacheKey, entry);
                    }
                }
            }
            return entry;
        }

        public int size()
        {
            return cache.size();
        }
    }

    private static class InterceptionFactoryCacheEntry
    {
        private final Class<?> proxyClass;
        private final InterceptorResolutionService.BeanInterceptorInfo interceptorInfo;

        private InterceptionFactoryCacheEntry(
                final Class<?> proxyClass,
                final InterceptorResolutionService.BeanInterceptorInfo interceptorInfo)
        {
            this.proxyClass = proxyClass;
            this.interceptorInfo = interceptorInfo;
        }
    }
}
