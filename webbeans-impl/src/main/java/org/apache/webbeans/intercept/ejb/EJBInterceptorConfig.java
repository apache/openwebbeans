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
package org.apache.webbeans.intercept.ejb;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.interceptor.AroundInvoke;
import javax.interceptor.AroundTimeout;
import javax.interceptor.Interceptors;

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.intercept.InterceptorData;
import org.apache.webbeans.util.Asserts;

/**
 * Configures the EJB related interceptors.
 *
 * @author <a href="mailto:gurkanerdogdu@yahoo.com">Gurkan Erdogdu</a>
 * @since 1.0
 * @deprecated This is not needed at all. EJB interceptors are _solely_ treated by the EJB container!
 */
public final class EJBInterceptorConfig
{

    private final WebBeansContext webBeansContext;

    public EJBInterceptorConfig(WebBeansContext webBeansContext)
    {
        this.webBeansContext = webBeansContext;
    }

    /**
     * Configures the given class for applicable interceptors.
     *
     * @param annotatedType to configure interceptors for
     */
    public void configure(AnnotatedType<?> annotatedType)
    {
        Asserts.assertNotNull(annotatedType);


        Interceptors incs = annotatedType.getAnnotation(Interceptors.class);
        if (incs != null)
        {
            Class<?>[] interceptorClasses = incs.value();

            for (Class<?> intClass : interceptorClasses)
            {
                configureInterceptorAnnots(webBeansContext.getAnnotatedElementFactory().newAnnotatedType(intClass), Collections.EMPTY_LIST, false, null);
            }

        }
        configureBeanAnnots(annotatedType, Collections.EMPTY_LIST);

        Class clazz = annotatedType.getJavaClass();
        webBeansContext.getInterceptorUtil().filterOverridenLifecycleInterceptor(clazz, Collections.EMPTY_LIST);
    }

    /**
     * Configure {@link Interceptors} on bean class.
     * @param annotatedType
     * @param stack interceptor stack of bean
     * @param isMethod if interceptor definition is on the bean
     * @param m if isMethod true, then it is intercepted method
     */
    private void configureInterceptorAnnots(AnnotatedType<?> annotatedType, List<InterceptorData> stack, boolean isMethod, Method m)
    {

        webBeansContext.getWebBeansUtil().configureInterceptorMethods(null, annotatedType, AroundInvoke.class,
                                                                      true, isMethod, stack, m, false);
        webBeansContext.getWebBeansUtil().configureInterceptorMethods(null, annotatedType, AroundTimeout.class,
                                                                      true, isMethod, stack, m, false);
        webBeansContext.getWebBeansUtil().configureInterceptorMethods(null, annotatedType, PostConstruct.class,
                                                                      true, isMethod, stack, m, false);
        webBeansContext.getWebBeansUtil().configureInterceptorMethods(null, annotatedType, PreDestroy.class,
                                                                      true, isMethod, stack, m, false);

    }

    /**
     * Configure bean class defined interceptors.
     * @param annotatedType bean class
     * @param stack interceptor stack
     */
    private void configureBeanAnnots(AnnotatedType annotatedType, List<InterceptorData> stack)
    {
        // 1- Look method intercepor class annotations
        // 2- Look super class around invoke
        // 3- Look bean around invoke

        // 1-
        Set<AnnotatedMethod<?>> annotatedMethods = annotatedType.getMethods();

        for (AnnotatedMethod<?> annotatedMethod : annotatedMethods)
        {
            Interceptors incs = annotatedMethod.getAnnotation(Interceptors.class);
            if (incs != null)
            {
                Method method = annotatedMethod.getJavaMember();
                Class<?>[] intClasses = incs.value();

                for (Class<?> intClass : intClasses)
                {
                    configureInterceptorAnnots(webBeansContext.getAnnotatedElementFactory().newAnnotatedType(intClass), stack, true, method);
                }

            }
        }

        // 3- Bean itself
        webBeansContext.getWebBeansUtil().configureInterceptorMethods(null, annotatedType, AroundInvoke.class,
                                                                      false, false, stack, null, false);
        webBeansContext.getWebBeansUtil().configureInterceptorMethods(null, annotatedType, AroundTimeout.class,
                                                                      false, false, stack, null, false);
        webBeansContext.getWebBeansUtil().configureInterceptorMethods(null, annotatedType, PostConstruct.class,
                                                                      false, false, stack, null, false);
        webBeansContext.getWebBeansUtil().configureInterceptorMethods(null, annotatedType, PreDestroy.class,
                                                                      false, false, stack, null, false);

    }
}
