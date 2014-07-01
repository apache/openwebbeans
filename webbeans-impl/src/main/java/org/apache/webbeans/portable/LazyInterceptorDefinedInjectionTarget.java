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

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.context.creational.CreationalContextImpl;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;
import java.util.List;
import java.util.Set;

public class LazyInterceptorDefinedInjectionTarget<T> extends InjectionTargetImpl<T>
{
    private volatile boolean interceptorsDefined;

    public LazyInterceptorDefinedInjectionTarget(final AnnotatedType<T> annotatedType,
                                                 final Set<InjectionPoint> injectionPoints,
                                                 final WebBeansContext webBeansContext,
                                                 final List<AnnotatedMethod<?>> postConstructMethods,
                                                 final List<AnnotatedMethod<?>> preDestroyMethods)
    {
        super(annotatedType, injectionPoints, webBeansContext, postConstructMethods, preDestroyMethods);
        interceptorsDefined = false;
    }

    @Override
    public T produce(final CreationalContext<T> creationalContext)
    {
        final CreationalContextImpl<T> creationalContextImpl = (CreationalContextImpl<T>) creationalContext;
        if (interceptorInfo == null && !interceptorsDefined)
        {
            final Bean<T> bean = creationalContextImpl.getBean();
            if (bean != null)
            {
                synchronized (this)
                {
                    if (!interceptorsDefined)
                    {
                        defineInterceptorStack(bean, annotatedType, webBeansContext);
                        interceptorsDefined = true;
                    }
                }
            }
        }
        return super.produce(creationalContext);
    }
}
