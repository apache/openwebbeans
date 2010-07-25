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

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.PrePassivate;
import javax.ejb.PostActivate;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.InterceptionType;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.AroundTimeout;
import javax.interceptor.ExcludeClassInterceptors;
import javax.interceptor.InvocationContext;

import org.apache.webbeans.component.InjectionTargetBean;
import org.apache.webbeans.context.creational.CreationalContextImpl;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.exception.WebBeansException;
import org.apache.webbeans.logger.WebBeansLogger;
import org.apache.webbeans.util.AnnotationUtil;
import org.apache.webbeans.util.Asserts;
import org.apache.webbeans.util.ClassUtil;
import org.apache.webbeans.util.SecurityUtil;

public final class InterceptorUtil
{
    /**Logger instance*/
    private static final WebBeansLogger logger = WebBeansLogger.getLogger(InterceptorUtil.class);

    private InterceptorUtil()
    {

    }

    /**
     * Check if the given method is a 'business method'
     * in the sense of the Interceptor specification
     * @param method
     * @return <code>true</code> if the given method is an interceptable business method
     */
    public static boolean isWebBeansBusinessMethod(Method method)
    {
        Asserts.nullCheckForMethod(method);

        int modifiers = method.getModifiers();

        if (ClassUtil.isStatic(modifiers) || ClassUtil.isPrivate(modifiers) || ClassUtil.isFinal(modifiers))
        {
            return false;
        }

        Annotation[] anns = method.getDeclaredAnnotations();

        // filter out all container 'special' methods
        for (Annotation ann : anns)
        {
            Class <? extends Annotation> annCls = ann.annotationType();
            if (annCls.equals(Inject.class)        ||
                annCls.equals(PreDestroy.class)    ||
                annCls.equals(PostConstruct.class) ||
                annCls.equals(AroundInvoke.class)  ||
                annCls.equals(PrePassivate.class)  ||   // JSR-299 7.2
                annCls.equals(PostActivate.class)  ||   // JSR-299 7.2
                annCls.equals(AroundTimeout.class))     // JSR-299 7.2
            {
                return false;
            }
        }

        return true;
    }

    public static Class<? extends Annotation> getInterceptorAnnotationClazz(InterceptionType type)
    {
        if (type.equals(InterceptionType.AROUND_INVOKE))
        {
            return AroundInvoke.class;
        }
        else if (type.equals(InterceptionType.POST_ACTIVATE))
        {
            return PostActivate.class;
        }
        else if (type.equals(InterceptionType.POST_CONSTRUCT))
        {
            return PostConstruct.class;
        }
        else if (type.equals(InterceptionType.PRE_DESTROY))
        {
            return PreDestroy.class;
        }
        else if (type.equals(InterceptionType.PRE_PASSIVATE))
        {
            return PrePassivate.class;
        }
        else if (type.equals(InterceptionType.AROUND_TIMEOUT))
        {
            return AroundTimeout.class;
        }
        else
        {
            throw new WebBeansException("Undefined interceotion type");
        }
    }
    
    @SuppressWarnings("unchecked")
    public static <T> boolean isBusinessMethodInterceptor(AnnotatedType<T> annotatedType)
    {
        Set<AnnotatedMethod<? super T>> methods = annotatedType.getMethods();
        for(AnnotatedMethod<? super T> methodA : methods)
        {
            AnnotatedMethod<T> method = (AnnotatedMethod<T>)methodA;
            if(method.isAnnotationPresent(AroundInvoke.class))
            {
                    if (!methodA.getParameters().isEmpty())
                    {
                        List<AnnotatedParameter<T>> parameters = method.getParameters();
                        List<Class<?>> clazzParameters = new ArrayList<Class<?>>();
                        for(AnnotatedParameter<T> parameter : parameters)
                        {
                            clazzParameters.add(ClassUtil.getClazz(parameter.getBaseType()));
                        }
                        
                        Class<?>[] params = clazzParameters.toArray(new Class<?>[0]);
                        if (params.length == 1 && params[0].equals(InvocationContext.class))
                        {
                            if (ClassUtil.getReturnType(method.getJavaMember()).equals(Object.class))
                            {
                                if (!ClassUtil.isMethodHasCheckedException(method.getJavaMember()))
                                {
                                    if (!ClassUtil.isStatic(method.getJavaMember().getModifiers()) && !ClassUtil.isFinal(method.getJavaMember().getModifiers()))
                                    {
                                        return true;
                                    }
                                }
                            }
                        }
                    }                
            }           
        }
        
        return false;
    }
    

    public static boolean isBusinessMethodInterceptor(Class<?> clazz)
    {
        Asserts.nullCheckForClass(clazz);
        Method[] methods = SecurityUtil.doPrivilegedGetDeclaredMethods(clazz);
        for (Method method : methods)
        {
            if (AnnotationUtil.hasMethodAnnotation(method, AroundInvoke.class))
            {
                if (ClassUtil.isMethodHasParameter(method))
                {
                    Class<?>[] params = ClassUtil.getMethodParameterTypes(method);
                    if (params.length == 1 && params[0].equals(InvocationContext.class))
                    {
                        if (ClassUtil.getReturnType(method).equals(Object.class))
                        {
                            if (ClassUtil.isMethodHasException(method))
                            {
                                if (!ClassUtil.isStatic(method.getModifiers()) && !ClassUtil.isFinal(method.getModifiers()))
                                {
                                    return true;
                                }
                            }
                        }
                    }

                }
            }
        }

        return false;
    }

    public static boolean isLifecycleMethodInterceptor(Class<?> clazz)
    {
        Asserts.nullCheckForClass(clazz);
        Method[] methods = SecurityUtil.doPrivilegedGetDeclaredMethods(clazz);
        for (Method method : methods)
        {
            if (AnnotationUtil.hasMethodAnnotation(method, PostConstruct.class) || AnnotationUtil.hasMethodAnnotation(method, PreDestroy.class)
                    || AnnotationUtil.hasMethodAnnotation(method, PostActivate.class)
                    || AnnotationUtil.hasMethodAnnotation(method, PrePassivate.class)
                    )
            {
                if (ClassUtil.isMethodHasParameter(method))
                {
                    Class<?>[] params = ClassUtil.getMethodParameterTypes(method);
                    if (params.length == 1 && params[0].equals(InvocationContext.class))
                    {
                        if (ClassUtil.getReturnType(method).equals(Void.TYPE))
                        {
                            if (!ClassUtil.isMethodHasCheckedException(method))
                            {
                                if (!ClassUtil.isStatic(method.getModifiers()))
                                {
                                    return true;
                                }
                                else
                                {
                                    logger.debug("Static LifeCycle callback method found.");
                                }
                            }
                            else
                            {
                                logger.debug("LifeCycle callback method with checked exception.");
                            }
                        }
                        else
                        {
                            logger.debug("LifeCycle callback method with non-void return type.");
                        }
                    }
                    else
                    {
                        logger.debug("LifeCycle callback method with wrong number or type of parameter(s).");
                    }
                }
                else
                {
                    logger.debug("LifeCycle callback method without any context parameter.");
                }
            }
        }

        return false;
    }
    
    @SuppressWarnings("unchecked")
    public static <T> boolean isLifecycleMethodInterceptor(AnnotatedType<T> annotatedType)
    {
        Set<AnnotatedMethod<? super T>> methods = annotatedType.getMethods();
        for(AnnotatedMethod<? super T> methodA : methods)
        {
            AnnotatedMethod<T> method = (AnnotatedMethod<T>)methodA;
            if(method.isAnnotationPresent(PostConstruct.class) 
                    || method.isAnnotationPresent(PreDestroy.class)
                    || method.isAnnotationPresent(PostActivate.class)
                    || method.isAnnotationPresent(PrePassivate.class))
            {
                    if (!methodA.getParameters().isEmpty())
                    {
                        List<AnnotatedParameter<T>> parameters = method.getParameters();
                        List<Class<?>> clazzParameters = new ArrayList<Class<?>>();
                        for(AnnotatedParameter<T> parameter : parameters)
                        {
                            clazzParameters.add(ClassUtil.getClazz(parameter.getBaseType()));
                        }
                        
                        Class<?>[] params = clazzParameters.toArray(new Class<?>[0]);
                        if (params.length == 1 && params[0].equals(InvocationContext.class))
                        {
                            if (ClassUtil.getReturnType(method.getJavaMember()).equals(Void.TYPE))
                            {
                                if (!ClassUtil.isMethodHasCheckedException(method.getJavaMember()))
                                {
                                    if (!ClassUtil.isStatic(method.getJavaMember().getModifiers()))
                                    {
                                        return true;
                                    }
                                }
                            }
                        }
                    }                
            }           
        }
        
        return false;
    }
    
    
    public static <T> void checkAnnotatedTypeInterceptorConditions(AnnotatedType<T> annotatedType)
    {
        Set<AnnotatedMethod<? super T>> methods = annotatedType.getMethods();
        for(AnnotatedMethod<? super T> methodA : methods)
        {
            if(methodA.isAnnotationPresent(Produces.class))
            {
                throw new WebBeansConfigurationException("Interceptor class : " + annotatedType.getJavaClass().getName()
                                                         + " can not have producer methods but it has one with name : "
                                                         + methodA.getJavaMember().getName());
            }
            
        }
        
        Annotation[] anns = annotatedType.getAnnotations().toArray(new Annotation[0]);
        if (!AnnotationUtil.hasInterceptorBindingMetaAnnotation(anns))
        {
            throw new WebBeansConfigurationException("Interceptor class : " + annotatedType.getJavaClass().getName()
                                                     + " must have at least one @InterceptorBinding annotation");
        }

        checkLifecycleConditions(annotatedType, anns, "Lifecycle interceptor : " + annotatedType.getJavaClass().getName()
                                                      + " interceptor binding type must be defined as @Target{TYPE}");
    }
    

    public static void checkInterceptorConditions(Class<?> clazz)
    {
        Asserts.nullCheckForClass(clazz);
        
        Method[] methods = SecurityUtil.doPrivilegedGetDeclaredMethods(clazz);
        for(Method method : methods)
        {
            if(AnnotationUtil.hasMethodAnnotation(method, Produces.class))
            {
                throw new WebBeansConfigurationException("Interceptor class : " + clazz + " can not have producer methods but it has one with name : "
                                                         + method.getName());
            }
        }
        
        if (!AnnotationUtil.hasInterceptorBindingMetaAnnotation(clazz.getDeclaredAnnotations()))
        {
            throw new WebBeansConfigurationException("WebBeans Interceptor class : " + clazz.getName()
                                                     + " must have at least one @InterceptorBinding annotation");
        }

        checkLifecycleConditions(clazz, clazz.getDeclaredAnnotations(), "Lifecycle interceptor : " + clazz.getName()
                                                                        + " interceptor binding type must be defined as @Target{TYPE}");
    }

    public static <T> void checkLifecycleConditions(Class<T> clazz, Annotation[] annots, String errorMessage)
    {
        Asserts.nullCheckForClass(clazz);

        if (isLifecycleMethodInterceptor(clazz) && !isBusinessMethodInterceptor(clazz))
        {
            Annotation[] anns = AnnotationUtil.getInterceptorBindingMetaAnnotations(annots);

            for (Annotation annotation : anns)
            {
                Target target = annotation.annotationType().getAnnotation(Target.class);
                ElementType[] elementTypes = target.value();

                if (!(elementTypes.length == 1 && elementTypes[0].equals(ElementType.TYPE)))
                {
                    throw new WebBeansConfigurationException(errorMessage);
                }
            }
        }

    }
    
    public static <T> void checkLifecycleConditions(AnnotatedType<T> annotatedType, Annotation[] annots, String errorMessage)
    {
        if (isLifecycleMethodInterceptor(annotatedType) && !isBusinessMethodInterceptor(annotatedType))
        {
            Annotation[] anns = AnnotationUtil.getInterceptorBindingMetaAnnotations(annots);

            for (Annotation annotation : anns)
            {
                Target target = annotation.annotationType().getAnnotation(Target.class);
                ElementType[] elementTypes = target.value();

                if (!(elementTypes.length == 1 && elementTypes[0].equals(ElementType.TYPE)))
                {
                    throw new WebBeansConfigurationException(errorMessage);
                }
            }
        }

    }
    

    public static void checkSimpleWebBeansInterceptorConditions(Class<?> clazz)
    {
        Asserts.nullCheckForClass(clazz);
        Annotation[] anns = clazz.getDeclaredAnnotations();

        boolean hasClassInterceptors = false;
        if (AnnotationUtil.getInterceptorBindingMetaAnnotations(anns).length > 0)
        {
            hasClassInterceptors = true;
        }
        else
        {
            Annotation[] stereoTypes = AnnotationUtil.getStereotypeMetaAnnotations(clazz.getDeclaredAnnotations());
            for (Annotation stero : stereoTypes)
            {
                if (AnnotationUtil.hasInterceptorBindingMetaAnnotation(stero.annotationType().getDeclaredAnnotations()))
                {
                    hasClassInterceptors = true;
                    break;
                }
            }
        }
        
        //Simple webbeans 
        if(ClassUtil.isFinal(clazz.getModifiers()) && hasClassInterceptors)
        {
            throw new WebBeansConfigurationException("Final Simple class with name : " + clazz.getName() + " can not define any InterceptorBindings");
        }

        Method[] methods = SecurityUtil.doPrivilegedGetDeclaredMethods(clazz);

        for (Method method : methods)
        {
            int modifiers = method.getModifiers();
            if (!ClassUtil.isStatic(modifiers) && !ClassUtil.isPrivate(modifiers) && ClassUtil.isFinal(modifiers))
            {
                if (hasClassInterceptors)
                {
                    throw new WebBeansConfigurationException("Simple web bean class : " + clazz.getName()
                                                             + " can not define non-static, non-private final methods. "
                                                             + "Because it is annotated with at least one @InterceptorBinding");
                }
                else
                {
                    if (AnnotationUtil.hasInterceptorBindingMetaAnnotation(method.getDeclaredAnnotations()))
                    {
                        throw new WebBeansConfigurationException("Method : " + method.getName() + "in simple web bean class : "
                                                                 + clazz.getName()
                                                                 + " can not be defined as non-static, non-private and final. "
                                                                 + "Because it is annotated with at least one @InterceptorBinding");
                    }
                }
            }
        }

    }

    /**
     * Gets list of interceptors with the given type.
     * 
     * @param stack interceptor stack
     * @param type interceptor type
     * @return list of interceptor
     */
    @SuppressWarnings("unchecked")
    public static List<InterceptorData> getInterceptorMethods(List<InterceptorData> stack, InterceptorType type)
    {
        List<InterceptorData> ai = new ArrayList<InterceptorData>();
        List<InterceptorData> at = new ArrayList<InterceptorData>();
        List<InterceptorData> pa = new ArrayList<InterceptorData>();
        List<InterceptorData> pc = new ArrayList<InterceptorData>();
        List<InterceptorData> pd = new ArrayList<InterceptorData>();
        List<InterceptorData> pp = new ArrayList<InterceptorData>();
    
        Iterator<InterceptorData> it = stack.iterator();
        while (it.hasNext())
        {
            Method m = null;
            InterceptorData data = it.next();
    
            if (type.equals(InterceptorType.AROUND_INVOKE))
            {
                m = data.getAroundInvoke();
                if (m != null)
                {
                    ai.add(data);
                }
    
            }
            else if (type.equals(InterceptorType.AROUND_TIMEOUT))
            {
                m = data.getAroundTimeout();
                if (m != null)
                {
                    at.add(data);
                }
    
            }
            else if (type.equals(InterceptorType.POST_ACTIVATE))
            {
                m = data.getPostActivate();
                if (m != null)
                {
                    pa.add(data);
                }
    
            }
            else if (type.equals(InterceptorType.POST_CONSTRUCT))
            {
                m = data.getPostConstruct();
                if (m != null)
                {
                    pc.add(data);
                }
    
            }
            else if (type.equals(InterceptorType.PRE_DESTROY))
            {
                m = data.getPreDestroy();
                if (m != null)
                {
                    pd.add(data);
                }
    
            }
            else if (type.equals(InterceptorType.PRE_PASSIVATE))
            {
                m = data.getPrePassivate();
                if (m != null)
                {
                    pp.add(data);
                }
    
            }
        }
    
        if (type.equals(InterceptorType.AROUND_INVOKE))
        {
            return ai;
        }
        else if (type.equals(InterceptorType.AROUND_TIMEOUT))
        {
            return at;
        }
        else if (type.equals(InterceptorType.POST_ACTIVATE))
        {
            return pa;
        }
        else if (type.equals(InterceptorType.POST_CONSTRUCT))
        {
            return pc;
        }
        else if (type.equals(InterceptorType.PRE_DESTROY))
        {
            return pd;
        }
        else if (type.equals(InterceptorType.PRE_PASSIVATE))
        {
            return pp;
    
        }
    
        return Collections.EMPTY_LIST;
    }
    
    /**
     * Returns true if this interceptor data is not related
     * false othwewise.
     * @param id interceptor data
     * @param method called method
     * @return true if this interceptor data is not related
     */
    private static boolean shouldRemoveInterceptorCommon(InterceptorData id, Method method)
    {
        boolean isMethodAnnotatedWithExcludeInterceptorClass = false;
        if (AnnotationUtil.hasMethodAnnotation(method, ExcludeClassInterceptors.class))
        {
            isMethodAnnotatedWithExcludeInterceptorClass = true;
        }

        if (isMethodAnnotatedWithExcludeInterceptorClass)
        {
            // If the interceptor is defined at the class level it should be
            // removed due to ExcludeClassInterceptors method annotation
            if (!id.isDefinedInMethod() && id.isDefinedInInterceptorClass())
            {
                return true;
            }
        }

        // If the interceptor is defined in a different method, remove it
        if (id.isDefinedInMethod() && !id.getInterceptorBindingMethod().equals(method))
        {
            return true;
        }

        return false;
    }

    /**
     * Filter bean interceptor stack.
     * @param stack interceptor stack
     * @param method called method on proxy
     * @param ownerCreationalContext bean creational context
     */
    public static void filterCommonInterceptorStackList(List<InterceptorData> stack, Method method)
    {
        Iterator<InterceptorData> it = stack.iterator();
        while (it.hasNext())
        {
            InterceptorData data = it.next();

            if (shouldRemoveInterceptorCommon(data, method))
            {
                it.remove();
            }
        }
    }
    
    public static Object callAroundInvokes(InjectionTargetBean<?> bean,Object instance, CreationalContextImpl<?> creationalContext,
            Method proceed, Object[] arguments, List<InterceptorData> stack, InvocationContext ejbInvocationContext) throws Exception
    {
        InvocationContextImpl impl = new InvocationContextImpl(bean, instance,
                                                               proceed, arguments, stack, InterceptorType.AROUND_INVOKE);
        if (ejbInvocationContext != null) 
        {
            impl.setEJBInvocationContext(ejbInvocationContext);
        }
        
        impl.setCreationalContext(creationalContext);
        
        return impl.proceed();
    }

}
