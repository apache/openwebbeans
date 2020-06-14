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
package org.apache.openwebbeans.junit5.internal;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;
import java.lang.annotation.Annotation;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class CdiParametersResolverExtension implements ParameterResolver, AfterEachCallback
{
    private static final ExtensionContext.Namespace NAMESPACE =
            ExtensionContext.Namespace.create(CdiParametersResolverExtension.class.getName());

    @Override
    public boolean supportsParameter(final ParameterContext parameterContext, final ExtensionContext extensionContext)
            throws ParameterResolutionException
    {
        final Instances instances = extensionContext.getStore(NAMESPACE).getOrComputeIfAbsent(
                Instances.class, k -> new Instances(), Instances.class);
        if (instances != null && instances.instances != null &&
                instances.instances.containsKey(parameterContext.getParameter()))
        {
            return false; // already handled
        }
        try
        {
            final Parameter parameter = parameterContext.getParameter();
            final BeanManager bm = CDI.current().getBeanManager();
            final Bean<?> bean = resolveParameterBean(bm, parameter);
            if (bean == null)
            {
                return false;
            }
            final CreationalContext<Object> creationalContext = bm.createCreationalContext(null);
            final Object instance = bm.getReference(bean, parameter.getType(), creationalContext);
            if (instances.instances == null)
            {
                instances.instances = new HashMap<>();
            }
            instances.instances.put(parameter, new Instance(instance, creationalContext));
            return true;
        }
        catch (final IllegalStateException ise) // no cdi container
        {
            return false;
        }
    }

    @Override
    public void afterEach(final ExtensionContext context)
    {
        final Instances instances = context.getStore(NAMESPACE).get(Instances.class, Instances.class);
        if (instances != null && instances.instances != null)
        {
            instances.instances.values().stream().map(i -> i.creationalContext).forEach(CreationalContext::release);
        }
    }

    @Override
    public Object resolveParameter(final ParameterContext parameterContext, final ExtensionContext extensionContext)
            throws ParameterResolutionException
    {
        final Instances instances = extensionContext.getStore(NAMESPACE).get(Instances.class, Instances.class);
        if (instances == null || instances.instances == null)
        {
            throw new ParameterResolutionException("No matching parameter: " + parameterContext.getParameter());
        }
        return instances.instances.get(parameterContext.getParameter()).instance;
    }

    private Bean<?> resolveParameterBean(final BeanManager beanManager, final Parameter parameter)
    {
        final Set<Bean<?>> beans = beanManager.getBeans(parameter.getType(), getQualifiers(beanManager, parameter));
        return beanManager.resolve(beans);
    }

    private Annotation[] getQualifiers(final BeanManager beanManager, final Parameter parameter)
    {
        return Arrays.stream(parameter.getAnnotations())
                .filter(annotation -> beanManager.isQualifier(annotation.annotationType()))
                .toArray(Annotation[]::new);
    }

    private static class Instance
    {
        private final Object instance;
        private final CreationalContext<?> creationalContext;

        private Instance(final Object instance, final CreationalContext<?> creationalContext)
        {
            this.instance = instance;
            this.creationalContext = creationalContext;
        }
    }

    private static class Instances
    {
        private Map<Parameter, Instance> instances;
    }
}
