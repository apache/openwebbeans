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
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Decorator;
import javax.enterprise.inject.spi.InterceptionType;
import javax.enterprise.inject.spi.Interceptor;
import javax.interceptor.ExcludeClassInterceptors;
import javax.interceptor.Interceptors;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
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
 * Class to calculate interceptor resolution information.
 * It also handles the proxy caching and applying.
 */
public class InterceptorResolutionService
{
    private static final Logger logger = WebBeansLoggerFacade.getLogger(InterceptorResolutionService.class);

    private final WebBeansContext webBeansContext;

    private final OpenWebBeansEjbLCAPlugin ejbPlugin;
    private final Class<? extends Annotation> prePassivateClass;
    private final Class<? extends Annotation> postActivateClass;
    private final Class<? extends Annotation> aroundTimeoutClass;


    public InterceptorResolutionService(WebBeansContext webBeansContext)
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


    public <T> BeanInterceptorInfo  calculateInterceptorInfo(Set<Type> beanTypes, Set<Annotation> qualifiers, AnnotatedType<T> annotatedType)
    {
        Asserts.assertNotNull(beanTypes, "beanTypes must not be null!");
        Asserts.assertNotNull(qualifiers, "qualifiers must not be null!");
        Asserts.assertNotNull(annotatedType, "AnnotatedType must not be null!");

        List<AnnotatedMethod> interceptableAnnotatedMethods = getInterceptableBusinessMethods(annotatedType);

        AnnotationManager annotationManager = webBeansContext.getAnnotationManager();
        BeanManager beanManager = webBeansContext.getBeanManagerImpl();

        // pick up CDI interceptors from a class level
        Set<Annotation> classInterceptorBindings = annotationManager.getInterceptorAnnotations(annotatedType.getAnnotations());

        // pick up EJB-style interceptors from a class level
        List<Interceptor<?>> classLevelEjbInterceptors = new ArrayList<Interceptor<?>>();

        collectEjbInterceptors(classLevelEjbInterceptors, annotatedType);

        // pick up the decorators
        List<Decorator<?>> decorators = beanManager.resolveDecorators(beanTypes, AnnotationUtil.asArray(qualifiers));
        if (decorators.size() == 0)
        {
            decorators = null; // less to store
        }

        Set<Interceptor<?>> allUsedCdiInterceptors = new HashSet<Interceptor<?>>();

        LinkedHashSet<Interceptor<?>> allUsedEjbInterceptors = new LinkedHashSet<Interceptor<?>>(); // we need to preserve the order!
        allUsedEjbInterceptors.addAll(classLevelEjbInterceptors);

        Map<Method, BusinessMethodInterceptorInfo> businessMethodInterceptorInfos = new HashMap<Method, BusinessMethodInterceptorInfo>();

        List<Method> nonInterceptedMethods = new ArrayList<Method>();

        // iterate over all methods and build up the interceptor/decorator stack
        for (AnnotatedMethod annotatedMethod : interceptableAnnotatedMethods)
        {
            // this probably needs some more fine tuning if a method is both lifecycle and used as business invocation.
            Set<InterceptionType> interceptionTypes = collectInterceptionTypes(annotatedMethod);
            BusinessMethodInterceptorInfo methodInterceptorInfo = new BusinessMethodInterceptorInfo(interceptionTypes);

            calculateEjbMethodInterceptors(methodInterceptorInfo, allUsedEjbInterceptors, classLevelEjbInterceptors, annotatedMethod);

            calculateCdiMethodInterceptors(methodInterceptorInfo, allUsedCdiInterceptors, annotatedMethod, classInterceptorBindings);

            calculateCdiMethodDecorators(methodInterceptorInfo, decorators, annotatedMethod);

            if (methodInterceptorInfo.isEmpty())
            {
                nonInterceptedMethods.add(annotatedMethod.getJavaMember());
                continue;
            }

            businessMethodInterceptorInfos.put(annotatedMethod.getJavaMember(), methodInterceptorInfo);
        }

        Map<InterceptionType, LifecycleMethodInfo> lifecycleMethodInterceptorInfos
                = new HashMap<InterceptionType, LifecycleMethodInfo>();

        addLifecycleMethods(
                lifecycleMethodInterceptorInfos,
                annotatedType,
                InterceptionType.POST_CONSTRUCT,
                PostConstruct.class,
                allUsedCdiInterceptors,
                classLevelEjbInterceptors,
                classInterceptorBindings,
                true);

        addLifecycleMethods(
                lifecycleMethodInterceptorInfos,
                annotatedType,
                InterceptionType.PRE_DESTROY,
                PreDestroy.class,
                allUsedCdiInterceptors,
                classLevelEjbInterceptors,
                classInterceptorBindings,
                true);

        List<Interceptor<?>> cdiInterceptors = new ArrayList<Interceptor<?>>(allUsedCdiInterceptors);
        Collections.sort(cdiInterceptors, new InterceptorComparator(webBeansContext));


        return new BeanInterceptorInfo(decorators, allUsedEjbInterceptors, cdiInterceptors, businessMethodInterceptorInfos,
                                       nonInterceptedMethods, lifecycleMethodInterceptorInfos);
    }

    private void addLifecycleMethods(Map<InterceptionType, LifecycleMethodInfo> lifecycleMethodInterceptorInfos,
                                     AnnotatedType<?> annotatedType,
                                     InterceptionType interceptionType,
                                     Class<? extends Annotation> annotation,
                                     Set<Interceptor<?>> allUsedInterceptors,
                                     List<Interceptor<?>> classLevelEjbInterceptors,
                                     Set<Annotation> classInterceptorBindings,
                                     boolean parentFirst)
    {
        Set<InterceptionType> it = new HashSet<InterceptionType>();
        it.add(interceptionType);
        List<AnnotatedMethod<?>> foundMethods = new ArrayList<AnnotatedMethod<?>>();
        BusinessMethodInterceptorInfo methodInterceptorInfo = new BusinessMethodInterceptorInfo(it);

        List<AnnotatedMethod<?>> lifecycleMethodCandidates = webBeansContext.getInterceptorUtil().getLifecycleMethods(annotatedType, annotation, parentFirst);

        for (AnnotatedMethod<?> lifecycleMethod : lifecycleMethodCandidates)
        {
            if (lifecycleMethod.getParameters().size() == 0)
            {
                foundMethods.add(lifecycleMethod);
                calculateEjbMethodInterceptors(methodInterceptorInfo, allUsedInterceptors, classLevelEjbInterceptors, lifecycleMethod);

                calculateCdiMethodInterceptors(methodInterceptorInfo, allUsedInterceptors, lifecycleMethod, classInterceptorBindings);
            }
        }

        if (foundMethods.size() > 0 )
        {
            lifecycleMethodInterceptorInfos.put(interceptionType, new LifecycleMethodInfo(foundMethods, methodInterceptorInfo));
        }
    }

    private <T> void collectEjbInterceptors(List<Interceptor<?>> ejbInterceptors, Annotated annotatedType)
    {
        Interceptors interceptorsAnnot = annotatedType.getAnnotation(Interceptors.class);
        if (interceptorsAnnot != null)
        {
            for (Class interceptorClass : interceptorsAnnot.value())
            {
                Interceptor ejbInterceptor = webBeansContext.getInterceptorsManager().getEjbInterceptorForClass(interceptorClass);
                ejbInterceptors.add(ejbInterceptor);
            }
        }
    }

    private void calculateEjbMethodInterceptors(BusinessMethodInterceptorInfo methodInterceptorInfo, Set<Interceptor<?>> allUsedEjbInterceptors,
                                                List<Interceptor<?>> classLevelEjbInterceptors, AnnotatedMethod annotatedMethod)
    {
        List<Interceptor<?>> methodInterceptors = new ArrayList<Interceptor<?>>();

        if (classLevelEjbInterceptors != null && classLevelEjbInterceptors.size() > 0)
        {
            // add the class level defined Interceptors first

            ExcludeClassInterceptors excludeClassInterceptors = annotatedMethod.getAnnotation(ExcludeClassInterceptors.class);
            if (excludeClassInterceptors == null)
            {
                // but only if there is no exclusion of all class-level interceptors
                methodInterceptors.addAll(classLevelEjbInterceptors);
            }
        }

        collectEjbInterceptors(methodInterceptors, annotatedMethod);
        allUsedEjbInterceptors.addAll(methodInterceptors);

        if (methodInterceptors.size() > 0)
        {
            methodInterceptorInfo.setEjbInterceptors(methodInterceptors);
        }
    }


    private void calculateCdiMethodDecorators(BusinessMethodInterceptorInfo methodInterceptorInfo, List<Decorator<?>> decorators, AnnotatedMethod annotatedMethod)
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

    private void calculateCdiMethodInterceptors(BusinessMethodInterceptorInfo methodInterceptorInfo,
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

        InterceptionType interceptionType = methodInterceptorInfo.getInterceptionTypes().isEmpty()
                                                ? InterceptionType.AROUND_INVOKE
                                                : methodInterceptorInfo.getInterceptionTypes().iterator().next(); //X TODO dirty hack for now...

        List<Interceptor<?>> methodInterceptors
                = webBeansContext.getBeanManagerImpl().resolveInterceptors(interceptionType, AnnotationUtil.asArray(cummulatedInterceptorBindings));

        methodInterceptorInfo.setCdiInterceptors(methodInterceptors);

        allUsedCdiInterceptors.addAll(methodInterceptors);
    }


    /**
     * Determine the {@link InterceptionType} of the given AnnotatedMethod
     * of an intercepted method.
     * An empty list means that this is an AroundInvoke method
     */
    private Set<InterceptionType> collectInterceptionTypes(AnnotatedMethod interceptableAnnotatedMethod)
    {
        Set<InterceptionType> interceptionTypes = new HashSet<InterceptionType>();
        for (Annotation annotation : interceptableAnnotatedMethod.getAnnotations())
        {
            if (annotation.equals(PostConstruct.class))
            {
                interceptionTypes.add(InterceptionType.POST_CONSTRUCT);
            }
            if (annotation.equals(PreDestroy.class))
            {
                interceptionTypes.add(InterceptionType.PRE_DESTROY);
            }
            if (null != ejbPlugin && annotation.equals(prePassivateClass))
            {
                interceptionTypes.add(InterceptionType.PRE_PASSIVATE);
            }
            if (null != ejbPlugin && annotation.equals(postActivateClass))
            {
                interceptionTypes.add(InterceptionType.POST_ACTIVATE);
            }
            if (null != ejbPlugin && annotation.equals(aroundTimeoutClass))
            {
                interceptionTypes.add(InterceptionType.AROUND_TIMEOUT);
            }
        }

        return interceptionTypes;
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
        public BeanInterceptorInfo(List<Decorator<?>> decorators,
                                   LinkedHashSet<Interceptor<?>> ejbInterceptors,
                                   List<Interceptor<?>> cdiInterceptors,
                                   Map<Method, BusinessMethodInterceptorInfo> businessMethodsInfo,
                                   List<Method> nonInterceptedMethods,
                                   Map<InterceptionType, LifecycleMethodInfo> lifecycleMethodInterceptorInfos)
        {
            this.decorators = decorators;
            this.ejbInterceptors = ejbInterceptors;
            this.cdiInterceptors = cdiInterceptors;
            this.businessMethodsInfo = businessMethodsInfo;
            this.nonInterceptedMethods = nonInterceptedMethods;
            this.lifecycleMethodInterceptorInfos = lifecycleMethodInterceptorInfos;
        }

        /**
         * All the EJB-style Interceptor Beans which are active on this class somewhere.
         * The Interceptors are sorted according to their definition.
         */
        private LinkedHashSet<Interceptor<?>> ejbInterceptors = null;

        /**
         * All the CDI-style Interceptor Beans which are active on this class somewhere.
         * This is only used to create the Interceptor instances.
         * The Interceptors are not sorted according to beans.xml .
         */
        private List<Interceptor<?>> cdiInterceptors = null;

        /**
         * All the Decorator Beans active on this class.
         */
        private List<Decorator<?>> decorators = null;

        /**
         * For each business method which is either decorated or intercepted we keep an entry.
         * If there is no entry then the method has neither a decorator nor an interceptor.
         */
        private Map<Method, BusinessMethodInterceptorInfo> businessMethodsInfo = new HashMap<Method, BusinessMethodInterceptorInfo>();

        /**
         * all non-intercepted methods
         */
        private List<Method> nonInterceptedMethods = Collections.EMPTY_LIST;

        /**
         * Contains info about lifecycle methods.
         * A method can be a 'business method' when invoked via the user but a
         * 'lifecycle method' while invoked by the container!
         */
        private Map<InterceptionType, LifecycleMethodInfo> lifecycleMethodInterceptorInfos = Collections.EMPTY_MAP;


        public List<Decorator<?>> getDecorators()
        {
            return decorators;
        }

        public LinkedHashSet<Interceptor<?>> getEjbInterceptors()
        {
            return ejbInterceptors;
        }

        public List<Interceptor<?>> getCdiInterceptors()
        {
            return cdiInterceptors;
        }

        public Map<Method, BusinessMethodInterceptorInfo> getBusinessMethodsInfo()
        {
            return businessMethodsInfo;
        }

        public List<Method> getNonInterceptedMethods()
        {
            return nonInterceptedMethods;
        }

        public Map<InterceptionType, LifecycleMethodInfo> getLifecycleMethodInterceptorInfos()
        {
            return lifecycleMethodInterceptorInfos;
        }
    }

    /**
     * We track per method which Interceptors to invoke
     */
    public static class BusinessMethodInterceptorInfo
    {
        private Interceptor<?>[] ejbInterceptors = null;
        private Interceptor<?>[] cdiInterceptors = null;
        private Decorator<?>[]   methodDecorators = null;

        /**
         * lifecycle methods can serve multiple intercepton types :/
         */
        private Set<InterceptionType> interceptionTypes;

        public BusinessMethodInterceptorInfo(Set<InterceptionType> interceptionTypes)
        {
            this.interceptionTypes = interceptionTypes;
        }

        public Set<InterceptionType> getInterceptionTypes()
        {
            return interceptionTypes;
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


    public static class LifecycleMethodInfo
    {
        private List<AnnotatedMethod<?>> methods = new ArrayList<AnnotatedMethod<?>>();
        private BusinessMethodInterceptorInfo methodInterceptorInfo;

        public LifecycleMethodInfo(List<AnnotatedMethod<?>> methods, BusinessMethodInterceptorInfo methodInterceptorInfo)
        {
            this.methods = methods;
            this.methodInterceptorInfo = methodInterceptorInfo;
        }

        public List<AnnotatedMethod<?>> getMethods()
        {
            return methods;
        }

        public BusinessMethodInterceptorInfo getMethodInterceptorInfo()
        {
            return methodInterceptorInfo;
        }
    }

}
