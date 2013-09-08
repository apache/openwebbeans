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

import org.apache.webbeans.component.spi.InjectionTargetFactory;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.portable.InjectionTargetImpl;
import org.apache.webbeans.util.Asserts;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InjectionTarget;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class InjectionTargetFactoryImpl<T> implements InjectionTargetFactory<T>
{

    private AnnotatedType<T> annotatedType;
    private WebBeansContext webBeansContext;

    public InjectionTargetFactoryImpl(AnnotatedType<T> annotatedType, WebBeansContext webBeansContext)
    {
        Asserts.assertNotNull(annotatedType, "AnnotatedType may not be null");
        Asserts.assertNotNull(webBeansContext, "WebBeansContext may not be null");
        this.annotatedType = annotatedType;
        this.webBeansContext = webBeansContext;
    }

    public InjectionTarget<T> createInjectionTarget()
    {
        return createInjectionTarget(null);
    }

    @Override
    public InjectionTarget<T> createInjectionTarget(Bean<T> bean)
    {
        final InjectionTargetImpl<T> injectionTarget
            = new InjectionTargetImpl<T>(annotatedType, createInjectionPoints(bean), webBeansContext, getPostConstructMethods(), getPreDestroyMethods());
        return webBeansContext.getWebBeansUtil().fireProcessInjectionTargetEvent(injectionTarget, annotatedType).getCompleteInjectionTarget();
    }

    protected Set<InjectionPoint> createInjectionPoints(Bean<T> bean)
    {
        Set<InjectionPoint> injectionPoints = new HashSet<InjectionPoint>();
        for (InjectionPoint injectionPoint: webBeansContext.getInjectionPointFactory().buildInjectionPoints(bean, annotatedType))
        {
            injectionPoints.add(injectionPoint);
        }
        return injectionPoints;
    }

    protected AnnotatedType<T> getAnnotatedType()
    {
        return annotatedType;
    }

    protected WebBeansContext getWebBeansContext()
    {
        return webBeansContext;
    }

    protected List<AnnotatedMethod<?>> getPostConstructMethods()
    {
        return webBeansContext.getInterceptorUtil().getLifecycleMethods(annotatedType, PostConstruct.class, true);
    }

    protected List<AnnotatedMethod<?>> getPreDestroyMethods()
    {
        return webBeansContext.getInterceptorUtil().getLifecycleMethods(annotatedType, PreDestroy.class, false);
    }
}
