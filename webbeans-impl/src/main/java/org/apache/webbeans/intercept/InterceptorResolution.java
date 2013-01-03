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
package org.apache.webbeans.intercept;

import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Decorator;
import javax.enterprise.inject.spi.InterceptionType;
import javax.enterprise.inject.spi.Interceptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.webbeans.annotation.AnnotationManager;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.logger.WebBeansLoggerFacade;
import org.apache.webbeans.util.AnnotationUtil;
import org.apache.webbeans.util.ClassUtil;

/**
 * Class to calculate static interceptor resolution information.
 *
 */
public class InterceptorResolution
{
    private static final Logger logger = WebBeansLoggerFacade.getLogger(InterceptorResolution.class);


    private WebBeansContext webBeansContext;

    public InterceptorResolution(WebBeansContext webBeansContext)
    {
        this.webBeansContext = webBeansContext;
    }


    public BeanInterceptorInfo calculateInterceptorInfo(AnnotatedType annotatedType)
    {
        BeanInterceptorInfo interceptorInfo = new BeanInterceptorInfo();

        List<AnnotatedMethod> interceptableAnnotatedMethods = getInterceptableAnnotatedMethods(annotatedType);

        InterceptorUtil interceptorUtils = webBeansContext.getInterceptorUtil();
        AnnotationManager annotationManager = webBeansContext.getAnnotationManager();
        BeanManager bm = webBeansContext.getBeanManagerImpl();

        List<Interceptor> classLevelCdiInterceptors = new ArrayList<Interceptor>();
        List<Interceptor> classLevelEjbInterceptors = new ArrayList<Interceptor>();


        // pick up CDI interceptors from a class level
        //X TODO should work but can surely be improved!
        Set<Annotation> classInterceptorBindings
                = annotationManager.getInterceptorAnnotations(AnnotationUtil.getAnnotationsFromSet(annotatedType.getAnnotations()));

        //X TODO pick up EJB interceptors from a class level
        //X TODO pick up the decorators

        // iterate over all methods and build up the interceptor stack
        for (AnnotatedMethod interceptableAnnotatedMethod : interceptableAnnotatedMethods)
        {
            Set<Annotation> methodInterceptorBindings
                    = annotationManager.getInterceptorAnnotations(AnnotationUtil.getAnnotationsFromSet(interceptableAnnotatedMethod.getAnnotations()));

            List<Annotation> cummulatedInterceptorBindings = new ArrayList<Annotation>();
            cummulatedInterceptorBindings.addAll(methodInterceptorBindings);
            cummulatedInterceptorBindings.addAll(classInterceptorBindings);

        }

        //X TODO sort the CDI interceptors

        return interceptorInfo;
    }

    /**
     * @return the list of all non-overloaded non-private and non-static methods
     */
    private List<AnnotatedMethod> getInterceptableAnnotatedMethods(AnnotatedType annotatedType)
    {
        List<Method> interceptableMethods = ClassUtil.getNonPrivateMethods(annotatedType.getJavaClass());

        List<AnnotatedMethod> interceptableAnnotatedMethods = new ArrayList<AnnotatedMethod>();

        Set<AnnotatedMethod> annotatedMethods = annotatedType.getMethods();
        for (Method interceptableMethod : interceptableMethods)
        {
            for (AnnotatedMethod<?> annotatedMethod : annotatedMethods)
            {
                if (annotatedMethod.getJavaMember() == interceptableMethod)
                {
                    interceptableAnnotatedMethods.add(annotatedMethod);
                }
            }
        }

        return interceptableAnnotatedMethods;
    }


    /**
     * static information about interceptors and decorators for a
     * single bean.
     */
    public static class BeanInterceptorInfo
    {
        /**
         * All the Interceptor Beans which are active on this class somewhere.
         * This is only used to create the Interceptor instances.
         */
        private Set<Interceptor<?>> interceptors = null;

        /**
         * All the Decorator Beans active on this class.
         */
        private Set<Decorator<?>> decorators = null;

        /**
         * For each method which is either decorated or intercepted we keep an entry.
         * If there is no entry then the method has neither a decorator nor an interceptor.
         */
        private Map<Method, MethodInterceptorInfo> methodsInfo = new HashMap<Method, MethodInterceptorInfo>();


    }

    /**
     * We track per method which Interceptors to invoke
     */
    public static class MethodInterceptorInfo
    {
        public MethodInterceptorInfo(InterceptionType interceptionType, List<Interceptor> methodEjbInterceptors, List<Interceptor> methodCdiInterceptors,
                                     List<Decorator> methodDecorators)
        {
            this.interceptionType = interceptionType;
            this.methodCdiInterceptors = methodCdiInterceptors;
            this.methodDecorators = methodDecorators;
            this.methodEjbInterceptors = methodEjbInterceptors;
        }

        private InterceptionType  interceptionType;
        private List<Interceptor> methodEjbInterceptors = null;
        private List<Interceptor> methodCdiInterceptors = null;
        private List<Decorator>   methodDecorators = null;


        /**
         * This is needed for later invoking the correct
         * interceptor method on the Interceptors.
         * (e.g. &#064;AroundInvoke vs &#064;PostConstruct interceptors)
         */
        public InterceptionType getInterceptionType()
        {
            return interceptionType;
        }

        /**
         * The (sorted) EJB-style ({@link javax.interceptor.Interceptors})
         * Interceptor Beans for a specific method or <code>null</code>
         * if no Interceptor exists for this method.
         * They must be called <i>before</i> the {@link #methodCdiInterceptors}!
         */
        public List<Interceptor> getMethodEjbInterceptors()
        {
            return methodEjbInterceptors;
        }

        /**
         * The (sorted) CDI Interceptor Beans for a specific method or <code>null</code>
         * if no Interceptor exists for this method.
         */
        public List<Interceptor> getMethodCdiInterceptors()
        {
            return methodCdiInterceptors;
        }

        /**
         * The (sorted) Decorator Beans for a specific method or <code>null</code>
         * if no Decorator exists for this method.
         */
        public List<Decorator> getMethodDecorators()
        {
            return methodDecorators;
        }

    }
}
