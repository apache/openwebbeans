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
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Decorator;
import javax.enterprise.inject.spi.InterceptionType;
import javax.enterprise.inject.spi.Interceptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.webbeans.annotation.AnnotationManager;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.logger.WebBeansLoggerFacade;
import org.apache.webbeans.plugins.OpenWebBeansEjbLCAPlugin;
import org.apache.webbeans.util.AnnotationUtil;
import org.apache.webbeans.util.Asserts;
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


    public <T> BeanInterceptorInfo  calculateInterceptorInfo(Bean<T> bean, AnnotatedType<T> annotatedType)
    {
        Asserts.assertNotNull(bean, "Bean must not be null!");
        Asserts.assertNotNull(annotatedType, "AnnotatedType must not be null!");

        List<AnnotatedMethod> interceptableAnnotatedMethods = getInterceptableBusinessMethods(annotatedType);

        AnnotationManager annotationManager = webBeansContext.getAnnotationManager();
        BeanManager beanManager = webBeansContext.getBeanManagerImpl();

        List<Interceptor> classLevelEjbInterceptors = new ArrayList<Interceptor>();

        // pick up CDI interceptors from a class level
        Set<Annotation> classInterceptorBindings = annotationManager.getInterceptorAnnotations(annotatedType.getAnnotations());

        //X TODO pick up EJB-style interceptors from a class level

        // pick up the decorators
        List<Decorator<?>> decorators = beanManager.resolveDecorators(bean.getTypes(), AnnotationUtil.asArray(bean.getQualifiers()));
        if (decorators.size() == 0)
        {
            decorators = null; // less to store
        }

        Set<Interceptor<?>> allUsedCdiInterceptors = new HashSet<Interceptor<?>>();
        Map<Method, MethodInterceptorInfo> businessMethodInterceptorInfos = new HashMap<Method, MethodInterceptorInfo>();


        // iterate over all methods and build up the interceptor/decorator stack
        for (AnnotatedMethod annotatedMethod : interceptableAnnotatedMethods)
        {
            InterceptionType interceptionType = calculateInterceptionType(annotatedMethod);
            MethodInterceptorInfo methodInterceptorInfo = new MethodInterceptorInfo(interceptionType);

            calculateCdiMethodInterceptors(methodInterceptorInfo, allUsedCdiInterceptors, annotatedMethod, classInterceptorBindings);

            calculateCdiMethodDecorators(methodInterceptorInfo, decorators, annotatedMethod);

            if (methodInterceptorInfo.isEmpty())
            {
                continue;
            }

            if (InterceptionType.AROUND_INVOKE.equals(interceptionType))
            {
                businessMethodInterceptorInfos.put(annotatedMethod.getJavaMember(), methodInterceptorInfo);
            }
        }

        return new BeanInterceptorInfo(decorators, allUsedCdiInterceptors, businessMethodInterceptorInfos);
    }


    private void calculateCdiMethodDecorators(MethodInterceptorInfo methodInterceptorInfo, List<Decorator<?>> decorators, AnnotatedMethod annotatedMethod)
    {
        if (decorators == null || decorators.isEmpty())
        {
            return;
        }

        List<Decorator<?>> appliedDecorators = new ArrayList<Decorator<?>>();

        for (Decorator decorator : decorators)
        {
            if (isDecoratorInterceptsMethod(decorator, annotatedMethod))
            {
                appliedDecorators.add(decorator);
            }
        }

        if (appliedDecorators.size() > 0)
        {
            methodInterceptorInfo.setMethodDecorators(new ArrayList<Decorator<?>>(appliedDecorators));
        }
    }

    private boolean isDecoratorInterceptsMethod(Decorator decorator, AnnotatedMethod annotatedMethod)
    {
        String annotatedMethodName = annotatedMethod.getJavaMember().getName();

        Set<Type> decoratedTypes = decorator.getDecoratedTypes();
        for (Type decoratedType : decoratedTypes)
        {
            if (decoratedType instanceof Class)
            {
                Class decoratedClass = (Class) decoratedType;
                Method[] decoratorMethods = decoratedClass.getDeclaredMethods();
                for (Method decoratorMethod : decoratorMethods)
                {
                    int modifiers = decoratorMethod.getModifiers();
                    if (Modifier.isFinal(modifiers) ||
                        Modifier.isPrivate(modifiers) ||
                        Modifier.isStatic(modifiers))
                    {
                        continue;
                    }

                    if (decoratorMethod.getName().equals(annotatedMethodName))
                    {
                        Class<?>[] decoratorMethodParams = decoratorMethod.getParameterTypes();
                        Class<?>[] annotatedMethodParams = annotatedMethod.getJavaMember().getParameterTypes();
                        if (decoratorMethodParams.length == annotatedMethodParams.length)
                        {
                            boolean paramsMatch = true;
                            for (int i = 0; i < decoratorMethodParams.length; i++)
                            {
                                if (!decoratorMethodParams[i].equals(annotatedMethodParams[i]))
                                {
                                    paramsMatch = false;
                                    break;
                                }
                            }

                            if (paramsMatch)
                            {
                                // yikes our method is decorated by this very decorator type.
                                return true;
                            }
                        }
                    }
                }
            }
        }

        return false;

    }

    private void calculateCdiMethodInterceptors(MethodInterceptorInfo methodInterceptorInfo,
                                                Set<Interceptor<?>> allUsedCdiInterceptors,
                                                AnnotatedMethod annotatedMethod,
                                                Set<Annotation> classInterceptorBindings)
    {
        AnnotationManager annotationManager = webBeansContext.getAnnotationManager();

        Set<Annotation> cummulatedInterceptorBindings = new HashSet<Annotation>();
        cummulatedInterceptorBindings.addAll(
                annotationManager.getInterceptorAnnotations(annotatedMethod.getAnnotations()));

        cummulatedInterceptorBindings.addAll(classInterceptorBindings);

        if (cummulatedInterceptorBindings.size() == 0)
        {
            return;
        }

        List<Interceptor<?>> methodInterceptors
                = webBeansContext.getBeanManagerImpl().resolveInterceptors(methodInterceptorInfo.getInterceptionType(),
                                                                           AnnotationUtil.asArray(cummulatedInterceptorBindings));

        methodInterceptorInfo.setCdiInterceptors(methodInterceptors);

        allUsedCdiInterceptors.addAll(methodInterceptors);
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
    private List<AnnotatedMethod> getInterceptableBusinessMethods(AnnotatedType annotatedType)
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


    /**
     * static information about interceptors and decorators for a
     * single bean.
     */
    public static class BeanInterceptorInfo
    {
        public BeanInterceptorInfo(List<Decorator<?>> decorators, Set<Interceptor<?>> interceptors,
                                   Map<Method, MethodInterceptorInfo> businessMethodsInfo)
        {
            this.decorators = decorators;
            this.interceptors = interceptors;
            this.businessMethodsInfo = businessMethodsInfo;
        }

        /**
         * All the Interceptor Beans which are active on this class somewhere.
         * This is only used to create the Interceptor instances.
         */
        private Set<Interceptor<?>> interceptors = null;

        /**
         * All the Decorator Beans active on this class.
         */
        private List<Decorator<?>> decorators = null;

        /**
         * For each business method which is either decorated or intercepted we keep an entry.
         * If there is no entry then the method has neither a decorator nor an interceptor.
         */
        private Map<Method, MethodInterceptorInfo> businessMethodsInfo = new HashMap<Method, MethodInterceptorInfo>();


        public List<Decorator<?>> getDecorators()
        {
            return decorators;
        }

        public Set<Interceptor<?>> getInterceptors()
        {
            return interceptors;
        }

        public Map<Method, MethodInterceptorInfo> getBusinessMethodsInfo()
        {
            return businessMethodsInfo;
        }
    }

    /**
     * We track per method which Interceptors to invoke
     */
    public static class MethodInterceptorInfo
    {
        private InterceptionType interceptionType;
        private Interceptor<?>[] ejbInterceptors = null;
        private Interceptor<?>[] cdiInterceptors = null;
        private Decorator<?>[]   methodDecorators = null;

        public MethodInterceptorInfo(InterceptionType interceptionType)
        {
            this.interceptionType = interceptionType;
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
         * They must be called <i>before</i> the {@link #cdiInterceptors}!
         */
        public Interceptor<?>[] getEjbInterceptors()
        {
            return ejbInterceptors;
        }

        /**
         * The (sorted) CDI Interceptor Beans for a specific method or <code>null</code>
         * if no Interceptor exists for this method.
         */
        public Interceptor<?>[] getCdiInterceptors()
        {
            return cdiInterceptors;
        }

        /**
         * The (sorted) Decorator Beans for a specific method or <code>null</code>
         * if no Decorator exists for this method.
         */
        public Decorator<?>[] getMethodDecorators()
        {
            return methodDecorators;
        }

        public void setCdiInterceptors(List<Interceptor<?>> cdiInterceptors)
        {
            if (cdiInterceptors == null || cdiInterceptors.isEmpty())
            {
                this.cdiInterceptors = null;
            }
            else
            {
                this.cdiInterceptors = cdiInterceptors.toArray(new Interceptor[cdiInterceptors.size()]);
            }
        }

        public void setMethodDecorators(List<Decorator<?>> methodDecorators)
        {
            if (methodDecorators == null || methodDecorators.isEmpty())
            {
                this.methodDecorators = null;
            }
            else
            {
                this.methodDecorators = methodDecorators.toArray(new Decorator[methodDecorators.size()]);
            }
        }

        public void setEjbInterceptors(List<Interceptor<?>> ejbInterceptors)
        {
            if (ejbInterceptors == null || ejbInterceptors.isEmpty())
            {
                this.ejbInterceptors = null;
            }
            else
            {
                this.ejbInterceptors = ejbInterceptors.toArray(new Interceptor[ejbInterceptors.size()]);
            }

        }

        /**
         * Determine if any interceptor information has been set at all.
         */
        public boolean isEmpty()
        {
            return cdiInterceptors == null && ejbInterceptors == null && methodDecorators == null;
        }
    }
}
