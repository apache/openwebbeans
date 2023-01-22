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
package org.apache.webbeans.component.creation;


import java.lang.reflect.Method;
import java.util.Map;

import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.InterceptionType;

import org.apache.webbeans.component.BeanAttributesImpl;
import org.apache.webbeans.component.SelfInterceptorBean;
import org.apache.webbeans.config.WebBeansContext;

/**
 * Bean builder for {@link org.apache.webbeans.component.InterceptorBean}s.
 */
public class SelfInterceptorBeanBuilder<T> extends InterceptorBeanBuilder<T, SelfInterceptorBean<T>>
{
    private boolean enabled;

    public SelfInterceptorBeanBuilder(WebBeansContext webBeansContext, AnnotatedType<T> annotatedType, BeanAttributesImpl<T> beanAttributes)
    {
        super(webBeansContext, annotatedType, beanAttributes);
    }

    public void defineSelfInterceptorRules()
    {

        // we do NOT invoke checkInterceptorConditions();
        // self-interceptors have different rules!

        // we do NOT invoke defineInterceptorRules() !

        enabled = defineInterceptorMethods();
    }

    @Override
    public boolean isInterceptorEnabled()
    {
        return enabled;
    }

    @Override
    protected SelfInterceptorBean<T> createBean(Class<T> beanClass, boolean enabled, Map<InterceptionType, Method[]> interceptionMethods)
    {
        return new SelfInterceptorBean<>(webBeansContext, annotatedType, beanAttributes, beanClass, interceptionMethods);
    }
}
