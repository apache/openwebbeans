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

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Decorator;
import javax.enterprise.inject.spi.InterceptionType;
import javax.enterprise.inject.spi.Interceptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.webbeans.annotation.AnnotationManager;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.logger.WebBeansLoggerFacade;
import org.apache.webbeans.plugins.OpenWebBeansEjbLCAPlugin;
import org.apache.webbeans.util.AnnotationUtil;
import org.apache.webbeans.util.ClassUtil;

/**
 * Class to calculate static interceptor resolution information.
 *
 */
public class InterceptorResolution
{
    private static final Logger logger = WebBeansLoggerFacade.getLogger(InterceptorResolution.class);

    private final WebBeansContext webBeansContext;

    private final OpenWebBeansEjbLCAPlugin ejbPlugin;
    private final Class<? extends Annotation> prePassivateClass;
    private final Class<? extends Annotation> postActivateClass;
    private final Class<? extends Annotation> aroundTimeoutClass;


    public InterceptorResolution(WebBeansContext webBeansContext)
    {
        this.webBeansContext = webBeansContext;
        ejbPlugin = webBeansContext.getPluginLoader().getEjbLCAPlugin();
        if (ejbPlugin != null)
        {
            prePassivateClass = ejbPlugin.getPrePassivateClass();
            postActivateClass = ejbPlugin.getPostActivateClass();
            aroundTimeoutClass = ejbPlugin.getAroundTimeoutClass();
        }
        else
        {
            prePassivateClass = null;
            postActivateClass = null;
            aroundTimeoutClass = null;
        }
    }


    public BeanInterceptorInfo calculateInterceptorInfo(AnnotatedType annotatedType)
    {
        List<AnnotatedMethod> interceptableAnnotatedMethods = getInterceptableAnnotatedMethods(annotatedType);

        InterceptorUtil interceptorUtils = webBeansContext.getInterceptorUtil();
        AnnotationManager annotationManager = webBeansContext.getAnnotationManager();
        BeanManager beanManager = webBeansContext.getBeanManagerImpl();

        List<Interceptor> classLevelEjbInterceptors = new ArrayList<Interceptor>();

        List<MethodInterceptorInfo> methodInterceptorInfos = new ArrayList<MethodInterceptorInfo>();

        // pick up CDI interceptors from a class level
        //X TODO should work but can surely be improved!
        Set<Annotation> classInterceptorBindings
                = annotationManager.getInterceptorAnnotations(AnnotationUtil.getAnnotationsFromSet(annotatedType.getAnnotations()));

        //X TODO pick up EJB interceptors from a class level
        //X TODO pick up the decorators

        Set<Interceptor<?>> allUsedCdiInterceptors = new HashSet<Interceptor<?>>();


        // iterate over all methods and build up the CDI interceptor stack
        for (AnnotatedMethod interceptableAnnotatedMethod : interceptableAnnotatedMethods)
        {
            Set<Annotation> cummulatedInterceptorBindings = new HashSet<Annotation>();
            cummulatedInterceptorBindings.addAll(
                    annotationManager.getInterceptorAnnotations(AnnotationUtil.getAnnotationsFromSet(interceptableAnnotatedMethod.getAnnotations())));

            cummulatedInterceptorBindings.addAll(classInterceptorBindings);

            if (cummulatedInterceptorBindings.size() == 0)
            {
                continue;
            }

            InterceptionType interceptionType = calculateInterceptionType(interceptableAnnotatedMethod);

            MethodInterceptorInfo methodInterceptorInfo = new MethodInterceptorInfo(interceptableAnnotatedMethod.getJavaMember(), interceptionType);

            List<Interceptor<?>> methodInterceptors = beanManager.resolveInterceptors(interceptionType, AnnotationUtil.getAnnotationsFromSet(cummulatedInterceptorBindings));
            methodInterceptorInfo.setMethodCdiInterceptors(methodInterceptors);

            allUsedCdiInterceptors.addAll(methodInterceptors);

            methodInterceptorInfos.add(methodInterceptorInfo);
        }

        return new BeanInterceptorInfo(null, allUsedCdiInterceptors, methodInterceptorInfos.toArray(new MethodInterceptorInfo[methodInterceptorInfos.size()]));
    }


    /**
     * Determine the {@link InterceptionType} of the given AnnotatedMethod
     * of an intercepted method.
     */
    private InterceptionType calculateInterceptionType(AnnotatedMethod interceptableAnnotatedMethod)
    {
        for (Annotation annotation : interceptableAnnotatedMethod.getAnnotations())
        {
            if (annotation.equals(PostConstruct.class))
            {
                return InterceptionType.POST_CONSTRUCT;
            }
            if (annotation.equals(PreDestroy.class))
            {
                return InterceptionType.PRE_DESTROY;
            }
            if (null != ejbPlugin && annotation.equals(prePassivateClass))
            {
                return InterceptionType.PRE_PASSIVATE;
            }
            if (null != ejbPlugin && annotation.equals(postActivateClass))
            {
                return InterceptionType.POST_ACTIVATE;
            }
            if (null != ejbPlugin && annotation.equals(aroundTimeoutClass))
            {
                return InterceptionType.AROUND_TIMEOUT;
            }
        }

        return InterceptionType.AROUND_INVOKE;
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
                if (annotatedMethod.getJavaMember().equals(interceptableMethod))
                {
                    if (!webBeansContext.getInterceptorUtil().isWebBeansBusinessMethod(annotatedMethod))
                    {
                        // we must only intercept business methods
                        continue;
                    }

                    interceptableAnnotatedMethods.add(annotatedMethod);
                }
            }
        }

        return interceptableAnnotatedMethods;
    }

    private boolean isBusinessMethod(Method interceptableMethod)
    {
        return false;  //To change body of created methods use File | Settings | File Templates.
    }


    /**
     * static information about interceptors and decorators for a
     * single bean.
     */
    public static class BeanInterceptorInfo
    {
        public BeanInterceptorInfo(Set<Decorator<?>> decorators, Set<Interceptor<?>> interceptors, MethodInterceptorInfo[] methodsInfo)
        {
            this.decorators = decorators;
            this.interceptors = interceptors;
            this.methodsInfo = methodsInfo;
        }

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
        private MethodInterceptorInfo[] methodsInfo = null;

        public Set<Decorator<?>> getDecorators()
        {
            return decorators;
        }

        public Set<Interceptor<?>> getInterceptors()
        {
            return interceptors;
        }

        public MethodInterceptorInfo[] getMethodsInfo()
        {
            return methodsInfo;
        }
    }

    /**
     * We track per method which Interceptors to invoke
     */
    public static class MethodInterceptorInfo
    {
        private Method               method;
        private InterceptionType     interceptionType;
        private List<Interceptor<?>> methodEjbInterceptors = null;
        private List<Interceptor<?>> methodCdiInterceptors = null;
        private List<Decorator<?>>   methodDecorators = null;

        public MethodInterceptorInfo(Method method, InterceptionType interceptionType)
        {
            this.method = method;
            this.interceptionType = interceptionType;
        }

        /**
         * @return the Method this entry is for.
         */
        public Method getMethod()
        {
            return method;
        }

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
        public List<Interceptor<?>> getMethodEjbInterceptors()
        {
            return methodEjbInterceptors;
        }

        /**
         * The (sorted) CDI Interceptor Beans for a specific method or <code>null</code>
         * if no Interceptor exists for this method.
         */
        public List<Interceptor<?>> getMethodCdiInterceptors()
        {
            return methodCdiInterceptors;
        }

        /**
         * The (sorted) Decorator Beans for a specific method or <code>null</code>
         * if no Decorator exists for this method.
         */
        public List<Decorator<?>> getMethodDecorators()
        {
            return methodDecorators;
        }

        public void setMethodCdiInterceptors(List<Interceptor<?>> methodCdiInterceptors)
        {
            this.methodCdiInterceptors = methodCdiInterceptors;
        }

        public void setMethodDecorators(List<Decorator<?>> methodDecorators)
        {
            this.methodDecorators = methodDecorators;
        }

        public void setMethodEjbInterceptors(List<Interceptor<?>> methodEjbInterceptors)
        {
            this.methodEjbInterceptors = methodEjbInterceptors;
        }
    }
}
