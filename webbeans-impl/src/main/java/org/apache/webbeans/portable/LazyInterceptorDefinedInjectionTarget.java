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
import org.apache.webbeans.container.BeanManagerImpl;
import org.apache.webbeans.context.creational.CreationalContextImpl;

import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.AnnotatedMethod;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.InjectionPoint;
import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LazyInterceptorDefinedInjectionTarget<T> extends InjectionTargetImpl<T>
{
    private volatile boolean init;

    public LazyInterceptorDefinedInjectionTarget(AnnotatedType<T> annotatedType,
                                                 Set<InjectionPoint> injectionPoints,
                                                 WebBeansContext webBeansContext,
                                                 List<AnnotatedMethod<?>> postConstructMethods,
                                                 List<AnnotatedMethod<?>> preDestroyMethods)
    {
        super(annotatedType, injectionPoints, webBeansContext, postConstructMethods, preDestroyMethods);
    }

    @Override
    public T produce(CreationalContext<T> creationalContext)
    {
        if (!init)
        {
            synchronized (this)
            {
                if (!init)
                {
                    CreationalContextImpl<T> creationalContextImpl = (CreationalContextImpl<T>) creationalContext;
                    Bean<T> bean = creationalContextImpl.getBean();
                    if (bean == null) // try to find it
                    {
                        BeanManagerImpl bm = webBeansContext.getBeanManagerImpl();
                        Set<Annotation> annotations = new HashSet<>();
                        for (Annotation a : annotatedType.getAnnotations())
                        {
                            if (bm.isQualifier(a.annotationType()))
                            {
                                annotations.add(a);
                            }
                        }
                        try
                        {
                            Set<Bean<?>> beans = bm.getBeans(annotatedType.getJavaClass(), annotations.toArray(new Annotation[annotations.size()]));
                            bean = (Bean<T>) bm.resolve(beans);
                        }
                        catch (Exception e)
                        {
                            // no-op: whatever can be thrown we don't want it
                        }
                    }
                    if (bean != null)
                    {
                        defineInterceptorStack(bean, annotatedType, webBeansContext);
                    }
                    init = true;
                }
            }
        }
        return super.produce(creationalContext);
    }
}
