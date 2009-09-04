/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.apache.webbeans.intercept;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InterceptionType;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;

import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.exception.WebBeansException;
import org.apache.webbeans.util.AnnotationUtil;
import org.apache.webbeans.util.Asserts;
import org.apache.webbeans.util.ClassUtil;

public final class InterceptorUtil
{
    private InterceptorUtil()
    {

    }

    public static boolean isWebBeansBusinessMethod(Method method)
    {
        Asserts.nullCheckForMethod(method);

        int modifiers = method.getModifiers();

        if (ClassUtil.isStatic(modifiers) || ClassUtil.isPrivate(modifiers) || ClassUtil.isFinal(modifiers))
        {
            return false;
        }

        if (AnnotationUtil.isMethodHasAnnotation(method, Inject.class))
        {
            return false;
        }

        if (AnnotationUtil.isMethodHasAnnotation(method, PreDestroy.class) || AnnotationUtil.isMethodHasAnnotation(method, PostConstruct.class) || AnnotationUtil.isMethodHasAnnotation(method, AroundInvoke.class))
        {
            return false;
        }

        if (AnnotationUtil.isMethodHasAnnotation(method, Produces.class) || AnnotationUtil.isMethodParameterAnnotationExist(method, Disposes.class) || AnnotationUtil.isMethodParameterAnnotationExist(method, Observes.class))
        {
            return true;
        }

        return true;
    }

    public static Class<? extends Annotation> getInterceptorAnnotationClazz(InterceptionType type)
    {
        if (type.equals(InterceptionType.AROUND_INVOKE))
        {
            return AroundInvoke.class;
        }
//        else if (type.equals(InterceptionType.POST_ACTIVATE))
//        {
//            return O;
//        }
        else if (type.equals(InterceptionType.POST_CONSTRUCT))
        {
            return PostConstruct.class;
        }
        else if (type.equals(InterceptionType.PRE_DESTROY))
        {
            return PreDestroy.class;
        }
//        else if (type.equals(InterceptionType.PRE_PASSIVATE))
//        {
//            return PrePassivate.class;
//        }
        else
        {
            throw new WebBeansException("Undefined interceotion type");
        }
    }

    public static boolean isBusinessMethodInterceptor(Class<?> clazz)
    {
        Asserts.nullCheckForClass(clazz);
        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods)
        {
            if (AnnotationUtil.isMethodHasAnnotation(method, AroundInvoke.class))
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
        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods)
        {
            if (AnnotationUtil.isMethodHasAnnotation(method, PostConstruct.class) || AnnotationUtil.isMethodHasAnnotation(method, PreDestroy.class) 
//                    AnnotationUtil.isMethodHasAnnotation(method, PostActivate.class) || 
//                    AnnotationUtil.isMethodHasAnnotation(method, PrePassivate.class)
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
                            }
                        }
                    }

                }
            }
        }

        return false;
    }

    public static void checkInterceptorConditions(Class<?> clazz)
    {
        Asserts.nullCheckForClass(clazz);
        if (!AnnotationUtil.isInterceptorBindingMetaAnnotationExist(clazz.getDeclaredAnnotations()))
        {
            throw new WebBeansConfigurationException("WebBeans Interceptor class : " + clazz.getName() + " must have at least one @InterceptorBinding annotation");
        }

        checkLifecycleConditions(clazz, clazz.getDeclaredAnnotations(), "Lifecycle interceptor : " + clazz.getName() + " interceptor binding type must be defined as @Target{TYPE}");
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
                if (AnnotationUtil.isInterceptorBindingMetaAnnotationExist(stero.annotationType().getDeclaredAnnotations()))
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

        Method[] methods = clazz.getDeclaredMethods();

        for (Method method : methods)
        {
            int modifiers = method.getModifiers();
            if (!ClassUtil.isStatic(modifiers) && !ClassUtil.isPrivate(modifiers) && ClassUtil.isFinal(modifiers))
            {
                if (hasClassInterceptors)
                {
                    throw new WebBeansConfigurationException("Simple web bean class : " + clazz.getName() + " can not define non-static, non-private final methods. Because it is annotated with at least one @InterceptorBinding");
                }
                else
                {
                    if (AnnotationUtil.isInterceptorBindingMetaAnnotationExist(method.getDeclaredAnnotations()))
                    {
                        throw new WebBeansConfigurationException("Method : " + method.getName() + "in simple web bean class : " + clazz.getName() + " can not be defined as non-static, non-private and final . Because it is annotated with at least one @InterceptorBinding");
                    }
                }
            }
        }

    }

}
