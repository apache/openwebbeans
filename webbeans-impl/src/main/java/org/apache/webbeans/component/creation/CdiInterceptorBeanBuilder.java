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


import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.InterceptionType;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

import org.apache.webbeans.component.BeanAttributesImpl;
import org.apache.webbeans.component.CdiInterceptorBean;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.util.ArrayUtil;

/**
 * Bean builder for {@link org.apache.webbeans.component.InterceptorBean}s.
 */
public class CdiInterceptorBeanBuilder<T> extends InterceptorBeanBuilder<T, CdiInterceptorBean<T>>
{

    private Set<Annotation> interceptorBindings;

    public CdiInterceptorBeanBuilder(WebBeansContext webBeansContext, AnnotatedType<T> annotatedType, BeanAttributesImpl<T> beanAttributes)
    {
        super(webBeansContext, annotatedType, beanAttributes);
    }

    public void defineCdiInterceptorRules()
    {
        checkInterceptorConditions();
        defineInterceptorRules();
        defineInterceptorBindings();
    }

    public boolean isInterceptorEnabled()
    {
        return webBeansContext.getInterceptorsManager().isInterceptorClassEnabled(getBeanType());
    }

    protected void defineInterceptorBindings()
    {
        Annotation[] bindings = webBeansContext.getAnnotationManager().getInterceptorBindingMetaAnnotations(getAnnotated().getAnnotations());
        if (bindings == null || bindings.length == 0)
        {
            throw new WebBeansConfigurationException("WebBeans Interceptor class : " + getBeanType()
                    + " must have at least one @InterceptorBinding annotation");
        }

        interceptorBindings = ArrayUtil.asSet(bindings);
    }

    @Override
    protected CdiInterceptorBean<T> createBean(Class<T> beanClass, boolean enabled, Map<InterceptionType, Method[]> interceptionMethods)
    {
        return new CdiInterceptorBean<T>(webBeansContext, getAnnotated(), getBeanAttributes(), beanClass, interceptorBindings, enabled, interceptionMethods);
    }
}
