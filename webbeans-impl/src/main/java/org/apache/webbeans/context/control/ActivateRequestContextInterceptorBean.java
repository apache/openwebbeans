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

package org.apache.webbeans.context.control;

import org.apache.webbeans.annotation.EmptyAnnotationLiteral;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.util.AnnotationUtil;

import javax.enterprise.context.Dependent;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.enterprise.context.control.RequestContextController;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InterceptionType;
import javax.enterprise.inject.spi.Interceptor;
import javax.interceptor.InvocationContext;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Set;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;

public class ActivateRequestContextInterceptorBean
        implements Interceptor<ActivateRequestContextInterceptorBean.InterceptorClass>, Serializable
{
    private static final Set<Annotation> BINDING = singleton(new ActivateRequestContextLiteral());
    private static final Set<Type> TYPES = singleton(Object.class);
    private static final InterceptorClass INSTANCE = new InterceptorClass();

    private final WebBeansContext webBeansContext;
    private transient RequestContextController contextController;

    public ActivateRequestContextInterceptorBean(final WebBeansContext webBeansContext)
    {
        this.webBeansContext = webBeansContext;
        this.contextController = new OwbRequestContextController(webBeansContext);
    }

    @Override
    public Set<Annotation> getInterceptorBindings()
    {
        return BINDING;
    }

    @Override
    public boolean intercepts(final InterceptionType type)
    {
        return true;
    }

    @Override
    public Object intercept(final InterceptionType type, final InterceptorClass instance,
                            final InvocationContext ctx) throws Exception
    {
        if (contextController == null) // synchro is not needed since the instance is backed by contextsservice
        {
            contextController = new OwbRequestContextController(webBeansContext);
        }
        final boolean activated = contextController.activate();
        try
        {
            return ctx.proceed();
        }
        finally
        {
            if (activated)
            {
                contextController.deactivate();
            }
        }
    }

    @Override
    public InterceptorClass create(final CreationalContext<InterceptorClass> context)
    {
        return INSTANCE;
    }

    @Override
    public void destroy(final InterceptorClass instance, final CreationalContext<InterceptorClass> context)
    {
        // no-op
    }

    @Override
    public Set<InjectionPoint> getInjectionPoints()
    {
        return emptySet();
    }

    @Override
    public Class<?> getBeanClass()
    {
        return InterceptorClass.class;
    }

    @Override
    public boolean isNullable()
    {
        return false;
    }

    @Override
    public Set<Type> getTypes()
    {
        return TYPES;
    }

    @Override
    public Set<Annotation> getQualifiers()
    {
        return AnnotationUtil.DEFAULT_AND_ANY_ANNOTATION_SET;
    }

    @Override
    public Class<? extends Annotation> getScope()
    {
        return Dependent.class;
    }

    @Override
    public String getName()
    {
        return null;
    }

    @Override
    public Set<Class<? extends Annotation>> getStereotypes()
    {
        return emptySet();
    }

    @Override
    public boolean isAlternative()
    {
        return false;
    }

    public static class ActivateRequestContextLiteral
            extends EmptyAnnotationLiteral<ActivateRequestContext>
            implements ActivateRequestContext
    {
    }

    public static class InterceptorClass implements Serializable
    {
    }
}
