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
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.exception.WebBeansDeploymentException;
import org.apache.webbeans.portable.AnnotatedElementFactory;
import org.apache.webbeans.util.AnnotationUtil;
import org.apache.webbeans.util.Asserts;
import org.apache.webbeans.util.ClassUtil;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
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
    private static volatile Boolean enforceCheckedException;


    public InterceptorResolutionService(WebBeansContext webBeansContext)
    {
        this.webBeansContext = webBeansContext;
    }


    public <T> BeanInterceptorInfo  calculateInterceptorInfo(Set<Type> beanTypes, Set<Annotation> qualifiers, AnnotatedType<T> annotatedType)
    {
        Asserts.assertNotNull(beanTypes, "beanTypes must not be null!");
        Asserts.assertNotNull(qualifiers, "qualifiers must not be null!");
        Asserts.assertNotNull(annotatedType, "AnnotatedType must not be null!");

        List<AnnotatedMethod> interceptableAnnotatedMethods = getInterceptableBusinessMethods(annotatedType);

        AnnotationManager annotationManager = webBeansContext.getAnnotationManager();
        BeanManagerImpl beanManager = webBeansContext.getBeanManagerImpl();


        // pick up EJB-style interceptors from a class level
        List<Interceptor<?>> classLevelEjbInterceptors = new ArrayList<Interceptor<?>>();

        collectEjbInterceptors(classLevelEjbInterceptors, annotatedType, false, beanTypes);

        // pick up the decorators
        List<Decorator<?>> decorators = beanManager.unsafeResolveDecorators(beanTypes, AnnotationUtil.asArray(qualifiers));
        if (decorators.size() == 0)
        {
            decorators = Collections.emptyList(); // less to store
        }

        Set<Interceptor<?>> allUsedCdiInterceptors = new HashSet<Interceptor<?>>();

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

        Set<Interceptor<?>> allUsedConstructorCdiInterceptors = new HashSet<Interceptor<?>>();
        addCdiClassLifecycleInterceptors(annotatedType, classInterceptorBindings, allUsedCdiInterceptors, allUsedConstructorCdiInterceptors);

        LinkedHashSet<Interceptor<?>> allUsedEjbInterceptors = new LinkedHashSet<Interceptor<?>>(); // we need to preserve the order!
        allUsedEjbInterceptors.addAll(classLevelEjbInterceptors);

        Map<Method, BusinessMethodInterceptorInfo> businessMethodInterceptorInfos = new HashMap<Method, BusinessMethodInterceptorInfo>();
        Map<Constructor<?>, BusinessMethodInterceptorInfo> constructorInterceptorInfos = new HashMap<Constructor<?>, BusinessMethodInterceptorInfo>();

        List<Method> nonInterceptedMethods = new ArrayList<Method>();

        SelfInterceptorBean<T> selfInterceptorBean = resolveSelfInterceptorBean(annotatedType);

        // iterate over all methods and build up the interceptor/decorator stack
        for (AnnotatedMethod annotatedMethod : interceptableAnnotatedMethods)
        {
            BusinessMethodInterceptorInfo methodInterceptorInfo = new BusinessMethodInterceptorInfo();

            calculateEjbMethodInterceptors(methodInterceptorInfo, allUsedEjbInterceptors, classLevelEjbInterceptors, annotatedMethod);

            calculateCdiMethodInterceptors(methodInterceptorInfo, InterceptionType.AROUND_INVOKE, allUsedCdiInterceptors, annotatedMethod,
                                           classInterceptorBindings, classLevelInterceptors);

            calculateCdiMethodDecorators(methodInterceptorInfo, decorators, annotatedMethod);

            if (methodInterceptorInfo.isEmpty() && (selfInterceptorBean == null || !selfInterceptorBean.isAroundInvoke()))
            {
                nonInterceptedMethods.add(annotatedMethod.getJavaMember());
                continue;
            }

            businessMethodInterceptorInfos.put(annotatedMethod.getJavaMember(), methodInterceptorInfo);
        }
        for (AnnotatedConstructor annotatedConstructor : annotatedType.getConstructors())
        {
            final BusinessMethodInterceptorInfo constructorInterceptorInfo = new BusinessMethodInterceptorInfo();
            calculateEjbMethodInterceptors(constructorInterceptorInfo, allUsedEjbInterceptors, classLevelEjbInterceptors, annotatedConstructor);
            if (constructorInterceptorInfo.isEmpty() && (selfInterceptorBean == null || !selfInterceptorBean.isAroundInvoke()))
            {
                continue;
            }

            constructorInterceptorInfos.put(annotatedConstructor.getJavaMember(), constructorInterceptorInfo);
        }

        Map<InterceptionType, LifecycleMethodInfo> lifecycleMethodInterceptorInfos
                = new HashMap<InterceptionType, LifecycleMethodInfo>();

        addLifecycleMethods(
                lifecycleMethodInterceptorInfos,
                annotatedType,
                InterceptionType.POST_CONSTRUCT,
                PostConstruct.class,
                allUsedCdiInterceptors,
                allUsedEjbInterceptors,
                classLevelEjbInterceptors,
                classInterceptorBindings);

        addLifecycleMethods(
                lifecycleMethodInterceptorInfos,
                annotatedType,
                InterceptionType.PRE_DESTROY,
                PreDestroy.class,
                allUsedCdiInterceptors,
                allUsedEjbInterceptors,
                classLevelEjbInterceptors,
                classInterceptorBindings);

        List<Interceptor<?>> cdiInterceptors = new ArrayList<Interceptor<?>>(allUsedCdiInterceptors);
        Collections.sort(cdiInterceptors, new InterceptorComparator(webBeansContext));

        List<Interceptor<?>> cdiConstructorInterceptors = new ArrayList<Interceptor<?>>(allUsedConstructorCdiInterceptors);
        Collections.sort(cdiConstructorInterceptors, new InterceptorComparator(webBeansContext));

        boolean interceptedBean = allUsedEjbInterceptors.size() > 0 ||
                                  allUsedCdiInterceptors.size() > 0 ||
                                  lifecycleMethodInterceptorInfos.size() > 0;

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
                if ((constructor.getParameters().isEmpty() && !isUnproxyable(constructor)) ||
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
                                       nonInterceptedMethods, lifecycleMethodInterceptorInfos);
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
        final BeanManagerImpl beanManagerImpl = webBeansContext.getBeanManagerImpl();

        Annotation[] interceptorBindings = null;
        if (classInterceptorBindings.size() > 0)
        {
            interceptorBindings = AnnotationUtil.asArray(classInterceptorBindings);

            allUsedCdiInterceptors.addAll(beanManagerImpl.resolveInterceptors(InterceptionType.POST_CONSTRUCT, interceptorBindings));
            allUsedCdiInterceptors.addAll(beanManagerImpl.resolveInterceptors(InterceptionType.PRE_DESTROY, interceptorBindings));
        }

        AnnotatedConstructor<?> constructorToUse = null;
        if (!annotatedType.getConstructors().isEmpty()) // avoid to use class annotations for not used constructors
        {
            for (final AnnotatedConstructor<?> c : annotatedType.getConstructors())
            {
                if (constructorToUse == null && c.getParameters().isEmpty())
                {
                    constructorToUse = c;
                }
                else if (c.getAnnotation(Inject.class) != null)
                {
                    constructorToUse = c;
                    break;
                }
            }
        }
        if (constructorToUse != null)
        {
            final Set<Annotation> constructorAnnot = webBeansContext.getAnnotationManager().getInterceptorAnnotations(constructorToUse.getAnnotations());
            for (final Annotation classA : classInterceptorBindings)
            {
                boolean overriden = false;
                for (final Annotation consA : constructorAnnot)
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
        SelfInterceptorBeanBuilder<T>sibb = new SelfInterceptorBeanBuilder<T>(webBeansContext, annotatedType, beanAttributes);
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
                                     Set<Annotation> classInterceptorBindings)
    {
        List<AnnotatedMethod<?>> foundMethods = new ArrayList<AnnotatedMethod<?>>();
        BusinessMethodInterceptorInfo methodInterceptorInfo = new BusinessMethodInterceptorInfo();

        List<AnnotatedMethod<?>> lifecycleMethodCandidates = webBeansContext.getInterceptorUtil().getLifecycleMethods(annotatedType, lifeycleAnnotation);

        for (AnnotatedMethod<?> lifecycleMethod : lifecycleMethodCandidates)
        {
            verifyLifecycleMethod(lifeycleAnnotation, lifecycleMethod);

            if (lifecycleMethod.getParameters().size() == 0)
            {
                foundMethods.add(lifecycleMethod);
                calculateEjbMethodInterceptors(methodInterceptorInfo, allUsedEjbInterceptors, classLevelEjbInterceptors, lifecycleMethod);

                calculateCdiMethodInterceptors(methodInterceptorInfo, interceptionType, allUsedCdiInterceptors, lifecycleMethod, classInterceptorBindings, null);
            }
        }
        for (AnnotatedConstructor<?> lifecycleMethod : annotatedType.getConstructors())
        {
            // TODO: verifyLifecycleMethod(lifeycleAnnotation, lifecycleMethod);
            calculateEjbMethodInterceptors(methodInterceptorInfo, allUsedEjbInterceptors, classLevelEjbInterceptors, lifecycleMethod);

            calculateCdiMethodInterceptors(methodInterceptorInfo, interceptionType, allUsedCdiInterceptors, lifecycleMethod, classInterceptorBindings, null);
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
                                                List<Interceptor<?>> classLevelEjbInterceptors, AnnotatedCallable annotatedMethod)
    {
        boolean unproxyable = isUnproxyable(annotatedMethod);

        List<Interceptor<?>> methodInterceptors = new ArrayList<Interceptor<?>>();

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

    private boolean isUnproxyable(AnnotatedCallable annotatedMethod)
    {
        int modifiers = annotatedMethod.getJavaMember().getModifiers();
        return Modifier.isFinal(modifiers) || Modifier.isPrivate(modifiers);
    }


    private void calculateCdiMethodDecorators(BusinessMethodInterceptorInfo methodInterceptorInfo, List<Decorator<?>> decorators, AnnotatedMethod annotatedMethod)
    {
        if (decorators == null || decorators.isEmpty())
        {
            return;
        }

        LinkedHashMap<Decorator<?>, Method> appliedDecorators = new LinkedHashMap<Decorator<?>, Method>();

        for (Decorator decorator : decorators)
        {
            if (!webBeansContext.getDecoratorsManager().isDecoratorEnabled(decorator.getBeanClass()))
            {
                continue;
            }

            Method decoratingMethod = getDecoratingMethod(decorator, annotatedMethod);
            if (decoratingMethod != null)
            {
                if (isUnproxyable(annotatedMethod))
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
                                                List<Interceptor<?>> classLevelInterceptors)
    {
        AnnotationManager annotationManager = webBeansContext.getAnnotationManager();

        boolean unproxyable = isUnproxyable(annotatedMethod);
        boolean hasMethodInterceptors = false;

        Map<Class<? extends Annotation>, Annotation> cummulatedInterceptorBindings = new HashMap<Class<? extends Annotation>, Annotation>();
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

        if (cummulatedInterceptorBindings.size() == 0)
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

        List<AnnotatedMethod> interceptableAnnotatedMethods = new ArrayList<AnnotatedMethod>();

        AnnotatedElementFactory annotatedElementFactory = webBeansContext.getAnnotatedElementFactory();
        Set<AnnotatedMethod> annotatedMethods = (Set<AnnotatedMethod>) annotatedElementFactory.getFilteredAnnotatedMethods(annotatedType);
        if (!javaClass.isAnnotation() && javaClass.isInterface())
        {
            Set<Type> types = new HashSet<Type>(annotatedType.getTypeClosure());
            types.remove(javaClass);
            types.remove(Object.class);

            if (!types.isEmpty()) // AT only supports 1 parent and ignores interface inheritance so add it manually here
            {
                annotatedMethods = new HashSet<AnnotatedMethod>(annotatedMethods); // otherwise it is not mutable by default
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
                                   Map<InterceptionType, LifecycleMethodInfo> lifecycleMethodInterceptorInfos)
        {
            this.decorators = decorators;
            this.ejbInterceptors = ejbInterceptors;
            this.cdiInterceptors = cdiInterceptors;
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

        private final List<Interceptor<?>> constructorCdiInterceptors;

        /**
         * Set if this class intercepts itself.
         */
        private SelfInterceptorBean<?> selfInterceptorBean;

        /**
         * All the Decorator Beans active on this class.
         */
        private List<Decorator<?>> decorators = null;

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
        private Interceptor<?>[] ejbInterceptors = null;
        private Interceptor<?>[] cdiInterceptors = null;
        private LinkedHashMap<Decorator<?>, Method> methodDecorators = null;

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
