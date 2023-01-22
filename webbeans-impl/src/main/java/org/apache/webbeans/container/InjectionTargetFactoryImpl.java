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

import jakarta.enterprise.inject.spi.InjectionTargetFactory;

import org.apache.webbeans.component.ManagedBean;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.configurator.AnnotatedTypeConfiguratorImpl;
import org.apache.webbeans.portable.InjectionTargetImpl;
import org.apache.webbeans.portable.events.generics.GProcessInjectionTarget;
import org.apache.webbeans.util.Asserts;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.inject.spi.AnnotatedMethod;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.enterprise.inject.spi.InjectionTarget;
import jakarta.enterprise.inject.spi.configurator.AnnotatedTypeConfigurator;
import java.util.List;
import java.util.Set;

public class InjectionTargetFactoryImpl<T> implements InjectionTargetFactory<T>
{
    private final WebBeansContext webBeansContext;

    private AnnotatedType<T> annotatedType;
    private AnnotatedTypeConfiguratorImpl<T> annotatedTypeConfigurator;

    public InjectionTargetFactoryImpl(AnnotatedType<T> annotatedType, WebBeansContext webBeansContext)
    {
        Asserts.assertNotNull(annotatedType, "AnnotatedType");
        Asserts.assertNotNull(webBeansContext, Asserts.PARAM_NAME_WEBBEANSCONTEXT);
        this.webBeansContext = webBeansContext;
        this.annotatedType = annotatedType;
    }

    public InjectionTarget<T> createInjectionTarget()
    {
        return createInjectionTarget(null);
    }

    @Override
    public InjectionTarget<T> createInjectionTarget(Bean<T> bean)
    {
        AnnotatedType<T> at = getAnnotatedType();
        InjectionTargetImpl<T> injectionTarget
            = new InjectionTargetImpl<>(at, createInjectionPoints(bean), webBeansContext, getPostConstructMethods(), getPreDestroyMethods());
        if (ManagedBean.class.isInstance(bean))
        {
            ManagedBean.class.cast(bean).setOriginalInjectionTarget(injectionTarget);
        }
        GProcessInjectionTarget event = webBeansContext.getWebBeansUtil().fireProcessInjectionTargetEvent(injectionTarget, at);
        InjectionTarget it = event.getInjectionTarget();
        event.setStarted();

        // creating the InjectionTarget must only be done once.
        this.annotatedType = null;
        this.annotatedTypeConfigurator = null;

        return it;
    }

    public Set<InjectionPoint> createInjectionPoints(Bean<T> bean)
    {
        return webBeansContext.getInjectionPointFactory().buildInjectionPoints(bean, getAnnotatedType());
    }

    public AnnotatedType<T> getAnnotatedType()
    {
        if (annotatedTypeConfigurator != null)
        {
            return annotatedTypeConfigurator.getNewAnnotatedType();
        }
        return annotatedType;
    }

    protected WebBeansContext getWebBeansContext()
    {
        return webBeansContext;
    }

    protected List<AnnotatedMethod<?>> getPostConstructMethods()
    {
        return webBeansContext.getInterceptorUtil().getLifecycleMethods(annotatedType, PostConstruct.class);
    }

    protected List<AnnotatedMethod<?>> getPreDestroyMethods()
    {
        return webBeansContext.getInterceptorUtil().getLifecycleMethods(annotatedType, PreDestroy.class);
    }

    @Override
    public AnnotatedTypeConfigurator<T> configure()
    {
        if (annotatedType == null)
        {
            throw new IllegalStateException("InjectionTargetFactora can only be used once");
        }
        annotatedTypeConfigurator = new AnnotatedTypeConfiguratorImpl<>(webBeansContext, annotatedType);
        return annotatedTypeConfigurator;
    }
}
