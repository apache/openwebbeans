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


import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.BeanAttributes;
import jakarta.enterprise.inject.spi.InterceptionType;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.apache.webbeans.component.CdiInterceptorBean;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.util.ArrayUtil;

import static java.util.Arrays.asList;

/**
 * Bean builder for {@link org.apache.webbeans.component.InterceptorBean}s.
 */
public class CdiInterceptorBeanBuilder<T> extends InterceptorBeanBuilder<T, CdiInterceptorBean<T>>
{

    private Set<Annotation> interceptorBindings;

    public CdiInterceptorBeanBuilder(WebBeansContext webBeansContext, AnnotatedType<T> annotatedType, BeanAttributes<T> beanAttributes)
    {
        super(webBeansContext, annotatedType, beanAttributes);
    }

    public void defineCdiInterceptorRules()
    {
        checkInterceptorConditions();
        defineInterceptorMethods();
        defineInterceptorBindings();
        validateTarget();

        // make sure that CDI interceptors do not have any Producer methods or a method with @Observes
        validateNoProducerOrObserverMethod(annotatedType);

        // make sure that CDI interceptors do not have a Disposes method
        validateNoDisposerWithoutProducer(webBeansContext.getAnnotatedElementFactory().getFilteredAnnotatedMethods(annotatedType),
                Collections.emptySet(), Collections.emptySet(), Collections.emptySet());
    }

    private void validateTarget()
    {
        boolean isLifecycleOnly = ( // spec seems to be more strict but breaks apps + does it really makes sense?
                        interceptionMethods.containsKey(InterceptionType.POST_CONSTRUCT)
                        || interceptionMethods.containsKey(InterceptionType.PRE_DESTROY))
                            &&
                        !(interceptionMethods.containsKey(InterceptionType.AROUND_INVOKE)
                        || interceptionMethods.containsKey(InterceptionType.AROUND_TIMEOUT)
                        || interceptionMethods.containsKey(InterceptionType.AROUND_CONSTRUCT));
        if (isLifecycleOnly && interceptorBindings != null)
        {
            for (Annotation a : interceptorBindings)
            {
                Target target = a.annotationType().getAnnotation(Target.class);
                if (target == null || !asList(target.value()).equals(asList(ElementType.TYPE)))
                {
                    throw new WebBeansConfigurationException(
                            a.annotationType().getName() + " doesn't have strictly @Target(TYPE) but has lifecycle methods. " +
                                    "Interceptor: " + annotatedType.getJavaClass().getName());
                }
            }
        }
    }

    @Override
    public boolean isInterceptorEnabled()
    {
        return webBeansContext.getInterceptorsManager().isInterceptorClassEnabled(annotatedType.getJavaClass());
    }

    protected void defineInterceptorBindings()
    {
        Annotation[] bindings = webBeansContext.getAnnotationManager().getInterceptorBindingMetaAnnotations(annotatedType.getAnnotations());
        if (bindings == null || bindings.length == 0)
        {
            throw new WebBeansConfigurationException("WebBeans Interceptor class : " + annotatedType.getJavaClass()
                    + " must have at least one @InterceptorBinding annotation");
        }

        interceptorBindings = ArrayUtil.asSet(bindings);
    }

    @Override
    protected CdiInterceptorBean<T> createBean(Class<T> beanClass, boolean enabled, Map<InterceptionType, Method[]> interceptionMethods)
    {
        return new CdiInterceptorBean<>(webBeansContext, annotatedType, beanAttributes, beanClass, interceptorBindings, enabled, interceptionMethods);
    }
}
