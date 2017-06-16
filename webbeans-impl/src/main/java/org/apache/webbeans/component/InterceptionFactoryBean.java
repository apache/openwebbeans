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

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.container.InterceptionFactoryImpl;
import org.apache.webbeans.context.creational.CreationalContextImpl;
import org.apache.webbeans.portable.AbstractProducer;
import org.apache.webbeans.util.AnnotationUtil;
import org.apache.webbeans.util.CollectionUtil;

import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InterceptionFactory;
import javax.enterprise.inject.spi.Interceptor;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;

public class InterceptionFactoryBean extends BuiltInOwbBean<InterceptionFactory>
{
    public InterceptionFactoryBean(WebBeansContext webBeansContext)
    {
        super(webBeansContext,
                WebBeansType.INTERCEPTIONFACTORY,
                new BeanAttributesImpl<>(
                        CollectionUtil.<Type>unmodifiableSet(InterceptionFactory.class, Object.class),
                        AnnotationUtil.DEFAULT_AND_ANY_ANNOTATION),
                InterceptionFactory.class,
                false,
                new SimpleProducerFactory<>(new InterceptionFactoryProducer(webBeansContext)));
    }

    @Override
    public boolean isPassivationCapable()
    {
        return true;
    }

    @Override
    public Class<?> proxyableType()
    {
        return InterceptionFactory.class;
    }

    private static class InterceptionFactoryProducer extends AbstractProducer<InterceptionFactory<?>>
    {
        private final WebBeansContext context;

        private InterceptionFactoryProducer(final WebBeansContext webBeansContext)
        {
            this.context = webBeansContext;
        }

        @Override
        protected InterceptionFactory<?> produce(final Map<Interceptor<?>, ?> interceptorInstances,
                                                 final CreationalContextImpl<InterceptionFactory<?>> creationalContext)
        {
            final InjectionPoint ip = creationalContext.getInjectionPoint();
            final AnnotatedType<?> at = context.getBeanManagerImpl().createAnnotatedType(
                    // already validated at startup so let's be brutal at runtime
                    Class.class.cast(ParameterizedType.class.cast(ip.getType()).getActualTypeArguments()[0]));
            return new InterceptionFactoryImpl(context, at, ip.getQualifiers(), creationalContext);
        }
    }
}
