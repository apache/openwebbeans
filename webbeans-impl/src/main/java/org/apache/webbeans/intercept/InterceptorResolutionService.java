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

import org.apache.webbeans.annotation.AnnotationManager;
import org.apache.webbeans.component.BeanAttributesImpl;
import org.apache.webbeans.component.SelfInterceptorBean;
import org.apache.webbeans.component.creation.BeanAttributesBuilder;
import org.apache.webbeans.component.creation.SelfInterceptorBeanBuilder;
import org.apache.webbeans.config.OpenWebBeansConfiguration;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.container.BeanManagerImpl;
import org.apache.webbeans.context.creational.CreationalContextImpl;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.exception.WebBeansDeploymentException;
import org.apache.webbeans.portable.AnnotatedElementFactory;
import org.apache.webbeans.proxy.InterceptorHandler;
import org.apache.webbeans.util.AnnotationUtil;
import org.apache.webbeans.util.Asserts;
import org.apache.webbeans.util.ClassUtil;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.UnproxyableResolutionException;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedCallable;
import javax.enterprise.inject.spi.AnnotatedConstructor;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Decorator;
import javax.enterprise.inject.spi.InterceptionType;
import javax.enterprise.inject.spi.Interceptor;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.ExcludeClassInterceptors;
import javax.interceptor.Interceptors;
import javax.interceptor.InvocationContext;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Class to calculate interceptor resolution information.
 * It also handles the proxy caching and applying.
 */
public class InterceptorResolutionService
{
    private final WebBeansContext webBeansContext;

    /**
     * Enforcing that interceptor callbacks should not be
     * able to throw checked exceptions is configurable
     */
    private volatile Boolean enforceCheckedException;


    public InterceptorResolutionService(WebBeansContext webBeansContext)
    {
        this.webBeansContext = webBeansContext;
    }


    public <T> BeanInterceptorInfo  calculateInterceptorInfo(Set<Type> beanTypes, Set<Annotation> qualifiers, AnnotatedType<T> annotatedType,
                                                             boolean failOnFinal)
    {
        Asserts.assertNotNull(beanTypes, "beanTypes");
        Asserts.assertNotNull(qualifiers, "qualifiers");
        Asserts.assertNotNull(annotatedType, "AnnotatedType");

        List<AnnotatedMethod> interceptableAnnotatedMethods = getInterceptableBusinessMethods(annotatedType);

        AnnotationManager annotationManager = webBeansContext.getAnnotationManager();
        BeanManagerImpl beanManager = webBeansContext.getBeanManagerImpl();


        // pick up EJB-style interceptors from a class level
        List<Interceptor<?>> classLevelEjbInterceptors = new ArrayList<>();

        collectEjbInterceptors(classLevelEjbInterceptors, annotatedType, false, beanTypes);

        // pick up the decorators
        List<Decorator<?>> decorators = beanManager.unsafeResolveDecorators(beanTypes, AnnotationUtil.asArray(qualifiers));
        if (decorators.isEmpty())
        {
            decorators = Collections.emptyList(); // less to store
        }

        Set<Interceptor<?>> allUsedCdiInterceptors = new HashSet<>();

        // pick up CDI interceptors from a class level
        Set<Annotation> classInterceptorBindings = annotationManager.getInterceptorAnnotations(annotatedType.getAnnotations());
        List<Interceptor<?>> classLevelInterceptors;
        if (classInterceptorBindings.size() > 0)
        {
            classLevelInterceptors = webBeansContext.getBeanManagerImpl().resolveInterceptors(InterceptionType.AROUND_INVOKE, AnnotationUtil.asArray(classInterceptorBindings));
            allUsedCdiInterceptors.addAll(classLevelInterceptors);
        }
        else
        {
            classLevelInterceptors = Collections.EMPTY_LIST;
        }

        Set<Interceptor<?>> allUsedConstructorCdiInterceptors = new HashSet<>();
        addCdiClassLifecycleInterceptors(annotatedType, classInterceptorBindings, allUsedCdiInterceptors, allUsedConstructorCdiInterceptors);

        LinkedHashSet<Interceptor<?>> allUsedEjbInterceptors = new LinkedHashSet<>(); // we need to preserve the order!
        allUsedEjbInterceptors.addAll(classLevelEjbInterceptors);

        Map<Method, BusinessMethodInterceptorInfo> businessMethodInterceptorInfos = new HashMap<>();
        Map<Constructor<?>, BusinessMethodInterceptorInfo> constructorInterceptorInfos = new HashMap<>();

        List<Interceptor<?>> classCdiInterceptors = new ArrayList<>(allUsedCdiInterceptors);
        classCdiInterceptors.sort(new InterceptorComparator(webBeansContext));

        List<Method> nonInterceptedMethods = new ArrayList<>();

        SelfInterceptorBean<T> selfInterceptorBean = resolveSelfInterceptorBean(annotatedType);

        // iterate over all methods and build up the interceptor/decorator stack
        for (AnnotatedMethod annotatedMethod : interceptableAnnotatedMethods)
        {
            BusinessMethodInterceptorInfo methodInterceptorInfo = new BusinessMethodInterceptorInfo();

            calculateEjbMethodInterceptors(methodInterceptorInfo, allUsedEjbInterceptors, classLevelEjbInterceptors, annotatedMethod, failOnFinal);

            calculateCdiMethodInterceptors(methodInterceptorInfo, InterceptionType.AROUND_INVOKE, allUsedCdiInterceptors, annotatedMethod,
                                           classInterceptorBindings, classLevelInterceptors, failOnFinal);

            calculateCdiMethodDecorators(methodInterceptorInfo, decorators, annotatedMethod, failOnFinal);

            if (methodInterceptorInfo.isEmpty() && (selfInterceptorBean == null || !selfInterceptorBean.isAroundInvoke()))
            {
                nonInterceptedMethods.add(annotatedMethod.getJavaMember());
                continue;
            }

            businessMethodInterceptorInfos.put(annotatedMethod.getJavaMember(), methodInterceptorInfo);
        }
        for (AnnotatedConstructor annotatedConstructor : annotatedType.getConstructors())
        {
            BusinessMethodInterceptorInfo constructorInterceptorInfo = new BusinessMethodInterceptorInfo();
            calculateEjbMethodInterceptors(constructorInterceptorInfo, allUsedEjbInterceptors, classLevelEjbInterceptors, annotatedConstructor, failOnFinal);
            if (constructorInterceptorInfo.isEmpty() && (selfInterceptorBean == null || !selfInterceptorBean.isAroundInvoke()))
            {
                continue;
            }

            constructorInterceptorInfos.put(annotatedConstructor.getJavaMember(), constructorInterceptorInfo);
        }

        Map<InterceptionType, LifecycleMethodInfo> lifecycleMethodInterceptorInfos
                = new HashMap<>();

        addLifecycleMethods(
                lifecycleMethodInterceptorInfos,
                annotatedType,
                InterceptionType.POST_CONSTRUCT,
                PostConstruct.class,
                allUsedCdiInterceptors,
                allUsedEjbInterceptors,
                classLevelEjbInterceptors,
                classInterceptorBindings,
                failOnFinal);

        addLifecycleMethods(
                lifecycleMethodInterceptorInfos,
                annotatedType,
                InterceptionType.PRE_DESTROY,
                PreDestroy.class,
                allUsedCdiInterceptors,
                allUsedEjbInterceptors,
                classLevelEjbInterceptors,
                classInterceptorBindings,
                failOnFinal);

        List<Interceptor<?>> cdiInterceptors = new ArrayList<>(allUsedCdiInterceptors);
        cdiInterceptors.sort(new InterceptorComparator(webBeansContext));

        List<Interceptor<?>> cdiConstructorInterceptors = new ArrayList<>(allUsedConstructorCdiInterceptors);
        cdiConstructorInterceptors.sort(new InterceptorComparator(webBeansContext));

        boolean interceptedBean = !annotatedType.getJavaClass().isInterface() && (
                                      allUsedEjbInterceptors.size() > 0 ||
                                      allUsedCdiInterceptors.size() > 0 ||
                                      lifecycleMethodInterceptorInfos.size() > 0
                                  );

        if ((interceptedBean || decorators.size() > 0) && Modifier.isFinal(annotatedType.getJavaClass().getModifiers()))
        {
            throw new WebBeansDeploymentException("Cannot apply Decorators or Interceptors on a final class: "
                                                     + annotatedType.getJavaClass().getName());
        }

        // if we have an interceptedBean, the bean must be proxyable in any case (also @Dependent)
        if (interceptedBean)
        {
            boolean proxyable = false;
            for (AnnotatedConstructor<T> constructor : annotatedType.getConstructors())
            {
                if ((constructor.getParameters().isEmpty() && !isUnproxyable(constructor, failOnFinal)) ||
                     constructor.isAnnotationPresent(Inject.class))
                {
                    proxyable = true;
                    break;
                }
            }

            if (!proxyable)
            {
                throw new WebBeansDeploymentException("Intercepted Bean " + annotatedType.getBaseType() + " must be proxyable");
            }
        }

        return new BeanInterceptorInfo(decorators, allUsedEjbInterceptors,
                                       cdiInterceptors, cdiConstructorInterceptors,
                                       selfInterceptorBean,
                                       constructorInterceptorInfos, businessMethodInterceptorInfos,
                                       nonInterceptedMethods, lifecycleMethodInterceptorInfos,
                                       classCdiInterceptors);
    }

    /**
     * Lifycycle methods like {@link javax.annotation.PostConstruct} and
     * {@link javax.annotation.PreDestroy} must not define a checked Exception
     * regarding to the spec. But this is often unnecessary restrictive so we
     * allow to disable this check application wide.
     *
     * @return <code>true</code> if the spec rule of having no checked exception should be enforced
     */
    private boolean isNoCheckedExceptionEnforced()
    {
        if (enforceCheckedException == null)
        {
            enforceCheckedException = Boolean.parseBoolean(webBeansContext.getOpenWebBeansConfiguration().
                    getProperty(OpenWebBeansConfiguration.INTERCEPTOR_FORCE_NO_CHECKED_EXCEPTIONS, "true"));
        }

        return enforceCheckedException;
    }


    private <T> void addCdiClassLifecycleInterceptors(AnnotatedType<T> annotatedType,
                                                      Set<Annotation> classInterceptorBindings,
                                                      Set<Interceptor<?>> allUsedCdiInterceptors,
                                                      Set<Interceptor<?>> allUsedConstructorCdiInterceptors)
    {
        BeanManagerImpl beanManagerImpl = webBeansContext.getBeanManagerImpl();

        Annotation[] interceptorBindings = null;
        if (classInterceptorBindings.size() > 0)
        {
            interceptorBindings = AnnotationUtil.asArray(classInterceptorBindings);

            allUsedCdiInterceptors.addAll(beanManagerImpl.resolveInterceptors(InterceptionType.POST_CONSTRUCT, interceptorBindings));
            allUsedCdiInterceptors.addAll(beanManagerImpl.resolveInterceptors(InterceptionType.PRE_DESTROY, interceptorBindings));
        }

        AnnotatedConstructor<?> constructorToUse = webBeansContext.getWebBeansUtil().getInjectedConstructor(annotatedType);
        if (constructorToUse == null)
        {
            constructorToUse = annotatedType.getConstructors().stream()
                    .filter(it -> it.getParameters().isEmpty())
                    .findFirst()
                    .orElse(null);
        }
        if (constructorToUse != null)
        {
            Set<Annotation> constructorAnnot = webBeansContext.getAnnotationManager().getInterceptorAnnotations(constructorToUse.getAnnotations());
            for (Annotation classA : classInterceptorBindings)
            {
                boolean overriden = false;
                for (Annotation consA : constructorAnnot)
                {
                    if (classA.annotationType() == consA.annotationType())
                    {
                        overriden = true;
                        break;
                    }
                }
                if (!overriden)
                {
                    constructorAnnot.add(classA);
                }
            }
            if (!constructorAnnot.isEmpty())
            {
                allUsedConstructorCdiInterceptors.addAll(beanManagerImpl.resolveInterceptors(InterceptionType.AROUND_CONSTRUCT, AnnotationUtil.asArray(constructorAnnot)));
            }
        }
        else if (interceptorBindings != null)
        {
            allUsedConstructorCdiInterceptors.addAll(beanManagerImpl.resolveInterceptors(InterceptionType.AROUND_CONSTRUCT, interceptorBindings));
        }
        allUsedCdiInterceptors.addAll(allUsedConstructorCdiInterceptors);
    }

    /**
     * Check whether this class has any method which intercepts the whole bean itself.
     * @return SelfInterceptorBean or <code>null</code> if this bean doesn't intercept itself
     */
    private <T> SelfInterceptorBean<T> resolveSelfInterceptorBean(AnnotatedType<T> annotatedType)
    {
        BeanAttributesImpl<T> beanAttributes = BeanAttributesBuilder.forContext(webBeansContext).newBeanAttibutes(annotatedType).build();
        if (beanAttributes == null)
        {
            // might happen if a proxying rule eefines that this is not a valid bean type.
            return null;
        }

        SelfInterceptorBeanBuilder<T> sibb = new SelfInterceptorBeanBuilder<>(webBeansContext, annotatedType, beanAttributes);
        sibb.defineSelfInterceptorRules();
        if (!sibb.isInterceptorEnabled())
        {
            return null;
        }

        return sibb.getBean();
    }

    private void addLifecycleMethods(Map<InterceptionType, LifecycleMethodInfo> lifecycleMethodInterceptorInfos,
                                     AnnotatedType<?> annotatedType,
                                     InterceptionType interceptionType,
                                     Class<? extends Annotation> lifeycleAnnotation,
                                     Set<Interceptor<?>> allUsedCdiInterceptors,
                                     Set<Interceptor<?>> allUsedEjbInterceptors,
                                     List<Interceptor<?>> classLevelEjbInterceptors,
                                     Set<Annotation> classInterceptorBindings,
                                     boolean failOnFinal)
    {
        List<AnnotatedMethod<?>> foundMethods = new ArrayList<>();
        BusinessMethodInterceptorInfo methodInterceptorInfo = new BusinessMethodInterceptorInfo();

        List<AnnotatedMethod<?>> lifecycleMethodCandidates = webBeansContext.getInterceptorUtil().getLifecycleMethods(annotatedType, lifeycleAnnotation);

        for (AnnotatedMethod<?> lifecycleMethod : lifecycleMethodCandidates)
        {
            verifyLifecycleMethod(lifeycleAnnotation, lifecycleMethod);

            if (lifecycleMethod.getParameters().isEmpty())
            {
                foundMethods.add(lifecycleMethod);
                calculateEjbMethodInterceptors(methodInterceptorInfo, allUsedEjbInterceptors, classLevelEjbInterceptors, lifecycleMethod, failOnFinal);

                calculateCdiMethodInterceptors(methodInterceptorInfo, interceptionType, allUsedCdiInterceptors, lifecycleMethod, classInterceptorBindings, null, failOnFinal);
            }
        }
        for (AnnotatedConstructor<?> lifecycleMethod : annotatedType.getConstructors())
        {
            // TODO: verifyLifecycleMethod(lifeycleAnnotation, lifecycleMethod);
            calculateEjbMethodInterceptors(methodInterceptorInfo, allUsedEjbInterceptors, classLevelEjbInterceptors, lifecycleMethod, failOnFinal);

            calculateCdiMethodInterceptors(methodInterceptorInfo, interceptionType, allUsedCdiInterceptors, lifecycleMethod, classInterceptorBindings, null, failOnFinal);
        }

        if (foundMethods.size() > 0 )
        {
            lifecycleMethodInterceptorInfos.put(interceptionType, new LifecycleMethodInfo(foundMethods, methodInterceptorInfo));
        }
    }

    private void collectEjbInterceptors(List<Interceptor<?>> ejbInterceptors, Annotated annotated, boolean unproxyable, Set<Type> types)
    {
        Interceptors interceptorsAnnot = annotated.getAnnotation(Interceptors.class);
        if (interceptorsAnnot != null)
        {
            if (unproxyable)
            {
                throw new WebBeansConfigurationException(annotated + " is not proxyable, but an Interceptor got defined on it!");
            }

            if (types == null)
            {
                types = Collections.emptySet();
            }

            for (Class interceptorClass : interceptorsAnnot.value())
            {
                if (types.contains(interceptorClass)) // don't create another bean for it
                {
                    continue;
                }

                Interceptor ejbInterceptor = webBeansContext.getInterceptorsManager().getEjbInterceptorForClass(interceptorClass);
                ejbInterceptors.add(ejbInterceptor);
            }
        }
    }

    private void calculateEjbMethodInterceptors(BusinessMethodInterceptorInfo methodInterceptorInfo, Set<Interceptor<?>> allUsedEjbInterceptors,
                                                List<Interceptor<?>> classLevelEjbInterceptors, AnnotatedCallable annotatedMethod,
                                                boolean failOnFinal)
    {
        boolean unproxyable = isUnproxyable(annotatedMethod, failOnFinal);

        List<Interceptor<?>> methodInterceptors = new ArrayList<>();

        if (classLevelEjbInterceptors != null && classLevelEjbInterceptors.size() > 0 && !unproxyable)
        {
            // add the class level defined Interceptors first

            ExcludeClassInterceptors excludeClassInterceptors = annotatedMethod.getAnnotation(ExcludeClassInterceptors.class);
            if (excludeClassInterceptors == null)
            {
                // but only if there is no exclusion of all class-level interceptors
                methodInterceptors.addAll(classLevelEjbInterceptors);
            }
        }

        collectEjbInterceptors(methodInterceptors, annotatedMethod, unproxyable, Collections.<Type>singleton(annotatedMethod.getJavaMember().getDeclaringClass()));
        allUsedEjbInterceptors.addAll(methodInterceptors);

        if (methodInterceptors.size() > 0)
        {
            methodInterceptorInfo.setEjbInterceptors(methodInterceptors);
        }
    }

    private boolean isUnproxyable(AnnotatedCallable annotatedMethod, boolean failOnFinal)
    {
        int modifiers = annotatedMethod.getJavaMember().getModifiers();
        boolean isFinal = Modifier.isFinal(modifiers);
        if (failOnFinal && isFinal)
        {
            throw new UnproxyableResolutionException(annotatedMethod + " is not proxyable");
        }
        return isFinal || Modifier.isPrivate(modifiers);
    }


    private void calculateCdiMethodDecorators(BusinessMethodInterceptorInfo methodInterceptorInfo, List<Decorator<?>> decorators, AnnotatedMethod annotatedMethod,
                                              boolean failOnFinal)
    {
        if (decorators == null || decorators.isEmpty())
        {
            return;
        }

        LinkedHashMap<Decorator<?>, Method> appliedDecorators = new LinkedHashMap<>();

        for (Decorator decorator : decorators)
        {
            if (!webBeansContext.getDecoratorsManager().isDecoratorEnabled(decorator.getBeanClass()))
            {
                continue;
            }

            Method decoratingMethod = getDecoratingMethod(decorator, annotatedMethod);
            if (decoratingMethod != null)
            {
                if (isUnproxyable(annotatedMethod, failOnFinal))
                {
                    throw new WebBeansDeploymentException(annotatedMethod + " is not proxyable, but an Decorator got defined on it!");
                }

                appliedDecorators.put(decorator, decoratingMethod);
            }
        }

        if (appliedDecorators.size() > 0)
        {
            methodInterceptorInfo.setMethodDecorators(appliedDecorators);
        }
    }

    /**
     * @return the Method from the decorator which decorates the annotatedMethod, <code>null</code>
     *         if the given Decorator does <i>not</i> decorate the annotatedMethod
     */
    private Method getDecoratingMethod(Decorator decorator, AnnotatedMethod annotatedMethod)
    {
        Set<Type> decoratedTypes = decorator.getDecoratedTypes();
        for (Type decoratedType : decoratedTypes)
        {
            if (decoratedType instanceof ParameterizedType)
            {
                // TODO handle the case that method parameter types are TypeVariables
                ParameterizedType parameterizedType = (ParameterizedType)decoratedType;
                decoratedType = parameterizedType.getRawType();
            }

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

                    if (methodEquals(decoratorMethod, annotatedMethod.getJavaMember()))
                    {
                        // yikes our method is decorated by this very decorator type.

                        if (Modifier.isAbstract((decorator.getBeanClass().getModifiers())))
                        {
                            // For abstract classes we will only decorate this method if it's really implemented on the decorator itself
                            Class decoratorClass = decorator.getBeanClass();
                            while (decoratorClass != Object.class)
                            {
                                try
                                {
                                    Method m = decoratorClass.getDeclaredMethod(decoratorMethod.getName(), decoratorMethod.getParameterTypes());
                                    if (Modifier.isAbstract(m.getModifiers()))
                                    {
                                        return null;
                                    }
                                    else
                                    {
                                        return decoratorMethod;
                                    }
                                }
                                catch (NoSuchMethodException e)
                                {
                                    // all ok, just continue
                                }

                                decoratorClass = decoratorClass.getSuperclass();
                            }

                            return null;
                        }
                        {
                            return decoratorMethod;
                        }
                    }
                }
            }
        }

        return null;
    }

    private boolean methodEquals(Method method1, Method method2)
    {
        if (method1.getName().equals(method2.getName()))
        {
            Class<?>[] method1Params = method1.getParameterTypes();
            Class<?>[] method2Params = method2.getParameterTypes();
            if (method1Params.length == method2Params.length)
            {
                boolean paramsMatch = true;
                for (int i = 0; i < method1Params.length; i++)
                {
                    if (!method1Params[i].isAssignableFrom(method2Params[i]))
                    {
                        paramsMatch = false;
                        break;
                    }
                }

                if (paramsMatch)
                {
                    return true;
                }
            }
        }

        return false;
    }

    private void calculateCdiMethodInterceptors(BusinessMethodInterceptorInfo methodInterceptorInfo,
                                                InterceptionType interceptionType,
                                                Set<Interceptor<?>> allUsedCdiInterceptors,
                                                AnnotatedCallable annotatedMethod,
                                                Set<Annotation> classInterceptorBindings,
                                                List<Interceptor<?>> classLevelInterceptors,
                                                boolean failOnFinal)
    {
        AnnotationManager annotationManager = webBeansContext.getAnnotationManager();

        boolean unproxyable = isUnproxyable(annotatedMethod, failOnFinal);
        boolean hasMethodInterceptors = false;

        Map<Class<? extends Annotation>, Annotation> cummulatedInterceptorBindings = new HashMap<>();
        for (Annotation interceptorBinding: annotationManager.getInterceptorAnnotations(annotatedMethod.getAnnotations()))
        {
            cummulatedInterceptorBindings.put(interceptorBinding.annotationType(), interceptorBinding);
            hasMethodInterceptors = true;
        }

        if (unproxyable && hasMethodInterceptors)
        {
            throw new WebBeansConfigurationException(annotatedMethod + " is not proxyable, but an Interceptor got defined on it!");
        }

        if (unproxyable)
        {
            // don't apply class level interceptors - instead just return
            return;
        }

        for (Annotation interceptorBinding: classInterceptorBindings)
        {
            if (!cummulatedInterceptorBindings.containsKey(interceptorBinding.annotationType()))
            {
                cummulatedInterceptorBindings.put(interceptorBinding.annotationType(), interceptorBinding);
            }
        }

        if (cummulatedInterceptorBindings.isEmpty())
        {
            return;
        }


        List<Interceptor<?>> methodInterceptors;
        if (hasMethodInterceptors || classLevelInterceptors == null)
        {
            methodInterceptors = webBeansContext.getBeanManagerImpl().resolveInterceptors(interceptionType, AnnotationUtil.asArray(cummulatedInterceptorBindings.values()));
            allUsedCdiInterceptors.addAll(methodInterceptors);
        }
        else
        {
            // if there is no explicit interceptor defined on the method, then we just take all the interceptors from the class
            methodInterceptors = classLevelInterceptors;
        }

        methodInterceptorInfo.setCdiInterceptors(methodInterceptors);
    }

    /**
     * Check that the given lifecycle method has:
     * <ul>
     *     <li>either has no parameter at all (standard case), or</li>
     *     <li>has exactly one InvocationContext parameter (self-interception)</li>
     *     <li>has no return value</li>
     * </ul>
     *
     * @param annotatedMethod
     */
    private <T> void verifyLifecycleMethod(Class<? extends Annotation> lifecycleAnnotation, AnnotatedMethod<T> annotatedMethod)
    {
        List<AnnotatedParameter<T>> params = annotatedMethod.getParameters();
        Method method = annotatedMethod.getJavaMember();
        if (params.size() > 0 && (params.size() > 1 || !params.get(0).getBaseType().equals(InvocationContext.class)))
        {
            throw new WebBeansConfigurationException(lifecycleAnnotation.getName() + " LifecycleMethod "
                                                     + method
                                                     + " must either have no parameter or InvocationContext but has:"
                                                     + Arrays.toString(method.getParameterTypes()));
        }

        if (!method.getReturnType().equals(Void.TYPE))
        {
            throw new WebBeansConfigurationException("@" + lifecycleAnnotation.getName()
                    + " annotated method : " + method.getName()
                    + " in class : " + annotatedMethod.getDeclaringType().getJavaClass().getName()
                    + " must return void type");
        }

        if (isNoCheckedExceptionEnforced() && ClassUtil.isMethodHasCheckedException(method))
        {
            throw new WebBeansConfigurationException("@" + lifecycleAnnotation.getName()
                    + " annotated method : " + method.getName()
                    + " in class : " + annotatedMethod.getDeclaringType().getJavaClass().getName()
                    + " can not throw any checked exception");
        }

        if (Modifier.isStatic(method.getModifiers()))
        {
            throw new WebBeansConfigurationException("@" + lifecycleAnnotation.getName()
                    + " annotated method : " + method.getName()
                    + " in class : " + annotatedMethod.getDeclaringType().getJavaClass().getName()
                    + " can not be static");
        }
    }

    /**
     * @return the list of all non-overloaded non-private and non-static methods
     */
    private List<AnnotatedMethod> getInterceptableBusinessMethods(AnnotatedType annotatedType)
    {
        Class<?> javaClass = annotatedType.getJavaClass();
        List<Method> interceptableMethods = ClassUtil.getNonPrivateMethods(javaClass, false);

        List<AnnotatedMethod> interceptableAnnotatedMethods = new ArrayList<>();

        AnnotatedElementFactory annotatedElementFactory = webBeansContext.getAnnotatedElementFactory();
        Set<AnnotatedMethod> annotatedMethods = (Set<AnnotatedMethod>) annotatedElementFactory.getFilteredAnnotatedMethods(annotatedType);
        if (!javaClass.isAnnotation() && javaClass.isInterface())
        {
            Set<Type> types = new HashSet<>(annotatedType.getTypeClosure());
            types.remove(javaClass);
            types.remove(Object.class);

            if (!types.isEmpty()) // AT only supports 1 parent and ignores interface inheritance so add it manually here
            {
                annotatedMethods = new HashSet<>(annotatedMethods); // otherwise it is not mutable by default
                for (Type c : types)
                {
                    if (!Class.class.isInstance(c))
                    {
                        continue;
                    }
                    Class parent = Class.class.cast(c);
                    AnnotatedType at = annotatedElementFactory.getAnnotatedType(parent);
                    if (at == null)
                    {
                        at = annotatedElementFactory.newAnnotatedType(parent);
                    }
                    if (at != null)
                    {
                        annotatedMethods.addAll((Set<AnnotatedMethod>) annotatedElementFactory.getFilteredAnnotatedMethods(at));
                    }
                }
            }
        }
        for (Method interceptableMethod : interceptableMethods)
        {
            for (AnnotatedMethod<?> annotatedMethod : annotatedMethods)
            {
                if (annotatedMethod.getJavaMember().equals(interceptableMethod))
                {
                    int modifiers = annotatedMethod.getJavaMember().getModifiers();
                    if (Modifier.isPrivate(modifiers) || Modifier.isStatic(modifiers))
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

    public Map<Method, List<Interceptor<?>>> createMethodInterceptors(BeanInterceptorInfo interceptorInfo)
    {
        Map<Method, List<Interceptor<?>>> methodInterceptors = new HashMap<>(interceptorInfo.getBusinessMethodsInfo().size());
        for (Map.Entry<Method, BusinessMethodInterceptorInfo> miEntry : interceptorInfo.getBusinessMethodsInfo().entrySet())
        {
            Method interceptedMethod = miEntry.getKey();
            BusinessMethodInterceptorInfo mii = miEntry.getValue();
            List<Interceptor<?>> activeInterceptors = new ArrayList<>();

            if (mii.getEjbInterceptors() != null)
            {
                Collections.addAll(activeInterceptors, mii.getEjbInterceptors());
            }
            if (mii.getCdiInterceptors() != null)
            {
                Collections.addAll(activeInterceptors, mii.getCdiInterceptors());
            }
            if (interceptorInfo.getSelfInterceptorBean() != null)
            {
                if (interceptedMethod.getAnnotation(AroundInvoke.class) == null) // this check is a dirty hack for now to prevent infinite loops
                {
                    // add self-interception as last interceptor in the chain.
                    activeInterceptors.add(interceptorInfo.getSelfInterceptorBean());
                }
            }

            if (activeInterceptors.size() > 0)
            {
                methodInterceptors.put(interceptedMethod, activeInterceptors);
            }
            else if (mii.getMethodDecorators() != null)
            {
                methodInterceptors.put(interceptedMethod, Collections.EMPTY_LIST);
            }
        }
        return methodInterceptors;
    }

    public <T> Map<Interceptor<?>, Object> createInterceptorInstances(BeanInterceptorInfo interceptorInfo,
                                                                      CreationalContextImpl<T> creationalContextImpl)
    {
        Map<Interceptor<?>,Object> interceptorInstances  = new HashMap<>();
        if (interceptorInfo != null)
        {
            // apply interceptorInfo

            // create EJB-style interceptors
            for (Interceptor interceptorBean : interceptorInfo.getEjbInterceptors())
            {
                creationalContextImpl.putContextual(interceptorBean);
                interceptorInstances.put(interceptorBean, interceptorBean.create(creationalContextImpl));
            }

            // create CDI-style interceptors
            for (Interceptor interceptorBean : interceptorInfo.getCdiInterceptors())
            {
                creationalContextImpl.putContextual(interceptorBean);
                interceptorInstances.put(interceptorBean, interceptorBean.create(creationalContextImpl));
            }
            for (Interceptor interceptorBean : interceptorInfo.getConstructorCdiInterceptors())
            {
                creationalContextImpl.putContextual(interceptorBean);
                interceptorInstances.put(interceptorBean, interceptorBean.create(creationalContextImpl));
            }
        }
        return interceptorInstances;
    }

    public <T> T createProxiedInstance(T instance, CreationalContextImpl<T> creationalContextImpl,
                                       CreationalContext<T> creationalContext,
                                       BeanInterceptorInfo interceptorInfo,
                                       Class<? extends T> proxyClass, Map<Method, List<Interceptor<?>>> methodInterceptors,
                                       String passivationId, Map<Interceptor<?>, Object> interceptorInstances,
                                       Function<CreationalContextImpl<?>, Boolean> isDelegateInjection,
                                       BiFunction<T, List<Decorator<?>>, List<Decorator<?>>> filterDecorators)
    {
        // register the bean itself for self-interception
        if (interceptorInfo.getSelfInterceptorBean() != null)
        {
            interceptorInstances.put(interceptorInfo.getSelfInterceptorBean(), instance);
        }

        T delegate = instance;
        if (interceptorInfo.getDecorators() != null && !isDelegateInjection.apply(creationalContextImpl))
        {
            List<Decorator<?>> decorators = filterDecorators.apply(instance, interceptorInfo.getDecorators());
            Map<Decorator<?>, Object> instances = new HashMap<>();
            for (int i = decorators.size(); i > 0; i--)
            {
                Decorator decorator = decorators.get(i - 1);
                creationalContextImpl.putContextual(decorator);
                creationalContextImpl.putDelegate(delegate);
                Object decoratorInstance = decorator.create(creationalContext);
                instances.put(decorator, decoratorInstance);
                delegate = webBeansContext.getInterceptorDecoratorProxyFactory().createProxyInstance(proxyClass, instance,
                        new DecoratorHandler(interceptorInfo, decorators, instances, i - 1, instance, passivationId));
            }
        }
        InterceptorHandler interceptorHandler = new DefaultInterceptorHandler<>(instance, delegate, methodInterceptors, interceptorInstances, passivationId);

        return webBeansContext.getInterceptorDecoratorProxyFactory().createProxyInstance(proxyClass, instance, interceptorHandler);
    }


    /**
     * static information about interceptors and decorators for a
     * single bean.
     */
    public static class BeanInterceptorInfo
    {

        public BeanInterceptorInfo(List<Decorator<?>> decorators, LinkedHashSet<Interceptor<?>> ejbInterceptors,
                                   List<Interceptor<?>> cdiInterceptors, List<Interceptor<?>> constructorCdiInterceptors,
                                   SelfInterceptorBean<?> selfInterceptorBean,
                                   Map<Constructor<?>, BusinessMethodInterceptorInfo> constructorInterceptorInfos,
                                   Map<Method, BusinessMethodInterceptorInfo> businessMethodsInfo,
                                   List<Method> nonInterceptedMethods,
                                   Map<InterceptionType, LifecycleMethodInfo> lifecycleMethodInterceptorInfos,
                                   List<Interceptor<?>> classCdiInterceptors)
        {
            this.decorators = decorators;
            this.ejbInterceptors = ejbInterceptors;
            this.cdiInterceptors = cdiInterceptors;
            this.classCdiInterceptors = classCdiInterceptors;
            this.constructorCdiInterceptors = constructorCdiInterceptors;
            this.selfInterceptorBean = selfInterceptorBean;
            this.businessMethodsInfo = businessMethodsInfo;
            this.constructorInterceptorInfos = constructorInterceptorInfos;
            this.nonInterceptedMethods = nonInterceptedMethods;
            this.lifecycleMethodInterceptorInfos = lifecycleMethodInterceptorInfos;
        }

        /**
         * All the EJB-style Interceptor Beans which are active on this class somewhere.
         * The Interceptors are sorted according to their definition.
         */
        private LinkedHashSet<Interceptor<?>> ejbInterceptors;

        /**
         * All the CDI-style Interceptor Beans which are active on this class somewhere.
         * This is only used to create the Interceptor instances.
         * The Interceptors are not sorted according to beans.xml .
         */
        private List<Interceptor<?>> cdiInterceptors;

        /**
         * Class only interceptors (for lifecycle methods).
         */
        private List<Interceptor<?>> classCdiInterceptors;

        private final List<Interceptor<?>> constructorCdiInterceptors;

        /**
         * Set if this class intercepts itself.
         */
        private SelfInterceptorBean<?> selfInterceptorBean;

        /**
         * All the Decorator Beans active on this class.
         */
        private List<Decorator<?>> decorators;

        /**
         * For each business method which is either decorated or intercepted we keep an entry.
         * If there is no entry then the method has neither a decorator nor an interceptor.
         */
        private Map<Method, BusinessMethodInterceptorInfo> businessMethodsInfo;
        private Map<Constructor<?>, BusinessMethodInterceptorInfo> constructorInterceptorInfos;

        /**
         * all non-intercepted methods
         */
        private List<Method> nonInterceptedMethods;

        /**
         * Contains info about lifecycle methods.
         * A method can be a 'business method' when invoked via the user but a
         * 'lifecycle method' while invoked by the container!
         */
        private Map<InterceptionType, LifecycleMethodInfo> lifecycleMethodInterceptorInfos;


        public List<Decorator<?>> getDecorators()
        {
            return decorators;
        }

        public List<Interceptor<?>> getClassCdiInterceptors()
        {
            return classCdiInterceptors;
        }

        public LinkedHashSet<Interceptor<?>> getEjbInterceptors()
        {
            return ejbInterceptors;
        }

        public List<Interceptor<?>> getCdiInterceptors()
        {
            return cdiInterceptors;
        }

        public List<Interceptor<?>> getConstructorCdiInterceptors()
        {
            return constructorCdiInterceptors;
        }

        public SelfInterceptorBean<?> getSelfInterceptorBean()
        {
            return selfInterceptorBean;
        }

        public Map<Method, BusinessMethodInterceptorInfo> getBusinessMethodsInfo()
        {
            return businessMethodsInfo;
        }

        public Map<Constructor<?>, BusinessMethodInterceptorInfo> getConstructorInterceptorInfos()
        {
            return constructorInterceptorInfos;
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
        private Interceptor<?>[] ejbInterceptors;
        private Interceptor<?>[] cdiInterceptors;
        private LinkedHashMap<Decorator<?>, Method> methodDecorators;

        public BusinessMethodInterceptorInfo()
        {
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
         * This Map is sorted!
         * Key: the Decorator Bean
         * Value: the decorating method from the decorator instance
         */
        public LinkedHashMap<Decorator<?>, Method> getMethodDecorators()
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

        public void setMethodDecorators(LinkedHashMap<Decorator<?>, Method> methodDecorators)
        {
            if (methodDecorators == null || methodDecorators.isEmpty())
            {
                this.methodDecorators = null;
            }
            else
            {
                this.methodDecorators = methodDecorators;
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
        private List<AnnotatedMethod<?>> methods = new ArrayList<>();
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
