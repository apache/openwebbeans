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
package org.apache.webbeans.intercept.ejb;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.interceptor.AroundInvoke;
import javax.interceptor.AroundTimeout;
import javax.interceptor.Interceptors;

import org.apache.webbeans.intercept.InterceptorData;
import org.apache.webbeans.util.AnnotationUtil;
import org.apache.webbeans.util.Asserts;
import org.apache.webbeans.util.ClassUtil;
import org.apache.webbeans.util.WebBeansUtil;

/**
 * Configures the EJB related interceptors.
 * 
 * @author <a href="mailto:gurkanerdogdu@yahoo.com">Gurkan Erdogdu</a>
 * @since 1.0
 */
public final class EJBInterceptorConfig
{
    /*
     * Private constructor
     */
    private EJBInterceptorConfig()
    {

    }

    /**
     * Configures the given class for applicable interceptors.
     * 
     * @param clazz configuration interceptors for this
     */
    public static void configure(Class<?> clazz, List<InterceptorData> stack)
    {
        Asserts.nullCheckForClass(clazz);

        if (AnnotationUtil.hasClassAnnotation(clazz, Interceptors.class))
        {
            Interceptors incs = clazz.getAnnotation(Interceptors.class);
            Class<?>[] intClasses = incs.value();

            for (Class<?> intClass : intClasses)
            {
                configureInterceptorAnnots(intClass, stack, false, null);
            }

        }

        configureBeanAnnots(clazz, stack);        
        checkInheritedButOverridenMethod(clazz, stack);        
    }
    
    /**
     * Return true if given candidate is listed in interceptors list. 
     * @param mainClass bean class
     * @param candidateClass interceptor candidate class
     * @return true if given candidate is listed in interceptors list
     */
    private static boolean checkGivenClassIsInInterceptorList(Class<?> mainClass, Class<?> candidateClass)
    {
        if (AnnotationUtil.hasClassAnnotation(mainClass, Interceptors.class))
        {
            Interceptors incs = mainClass.getAnnotation(Interceptors.class);
            Class<?>[] intClasses = incs.value();
            
            for (Class<?> intClass : intClasses)
            {
                if(intClass.equals(candidateClass))
                {
                    return true;
                }
                else
                {
                    if(checkInInterceptorHierarchy(intClass, candidateClass))
                    {
                        return true;
                    }
                }
            }
        }
        
        return false;
    }
    
    /**
     * Return true if candidate class is a super class of given interceptor class.
     * @param interceptorClass interceptor class
     * @param candidateClass candaite class
     * @return true if candidate class is a super class of given interceptor class
     */
    private static boolean checkInInterceptorHierarchy(Class<?> interceptorClass, Class<?> candidateClass)
    {
        Class<?> superClassInterceptor = interceptorClass.getSuperclass();
        if(superClassInterceptor != null && !superClassInterceptor.equals(Object.class))
        {
            if(superClassInterceptor.equals(candidateClass))
            {
                return true;
            }
            
            else
            {
                return checkInInterceptorHierarchy(superClassInterceptor, candidateClass);
            }
        }
        
        return false;
    }
    
    /**
     * Remove bean inherited but overriden lifecycle interceptor method from
     * its stack list.
     * @param clazz bean class
     * @param stack bean interceptor stack
     */
    private static void checkInheritedButOverridenMethod(Class<?> clazz,List<InterceptorData> stack)
    {
        Iterator<InterceptorData> it = stack.iterator();
        while(it.hasNext())
        {
            InterceptorData interceptorData = it.next();
            
            if(interceptorData.isLifecycleInterceptor())
            {
                if(removeInheritedButOverridenInterceptor(clazz, interceptorData))
                {
                    it.remove();
                }                
            }
        }
    }
    
    /**
     * @see EJBInterceptorConfig#checkInheritedButOverridenMethod(Class, List)  
     */
    private static boolean removeInheritedButOverridenInterceptor(Class<?> clazz, InterceptorData interceptorData)
    {    
        Method interceptor = interceptorData.getInterceptorMethod();
        Class<?> declaringClass = interceptor.getDeclaringClass();
        
        //Not look for Interceptor classes
        if(checkGivenClassIsInInterceptorList(clazz, declaringClass))
        {
            return false;
        }
        
        if(!declaringClass.equals(clazz))
        {
            Method found = ClassUtil.getDeclaredMethod(clazz,interceptor.getName(), interceptor.getParameterTypes());                
            if(found != null)
            {
                return true;
            }
            else
            {
                Class<?> superClass = clazz.getSuperclass();
                if(superClass != null && !superClass.equals(Object.class))
                {
                    return removeInheritedButOverridenInterceptor(superClass, interceptorData);   
                }            
            }            
        }
                
        return false;
    }
    
    /**
     * Configure {@link Interceptors} on bean class.
     * @param clazz bean class
     * @param stack interceptor stack of bean
     * @param isMethod if interceptor definition is on the bean
     * @param m if isMethod true, then it is intercepted method
     */
    private static void configureInterceptorAnnots(Class<?> clazz, List<InterceptorData> stack, boolean isMethod, Method m)
    {
        // 1- Look interceptor class super class
        // 2- Look interceptor class
        Class<?> superClass = clazz.getSuperclass();
        if (superClass != null && !superClass.equals(Object.class))
        {
            configureInterceptorAnnots(superClass, stack, false, null);
        }

        WebBeansUtil.configureInterceptorMethods(null, clazz, AroundInvoke.class, true, isMethod, stack, m, false);
        WebBeansUtil.configureInterceptorMethods(null, clazz, AroundTimeout.class, true, isMethod, stack, m, false);
        WebBeansUtil.configureInterceptorMethods(null, clazz, PostConstruct.class, true, isMethod, stack, m, false);
        WebBeansUtil.configureInterceptorMethods(null, clazz, PreDestroy.class, true, isMethod, stack, m, false);

    }
    
    /**
     * Configure bean class defined interceptors.
     * @param clazz bean class
     * @param stack interceptor stack
     */
    private static void configureBeanAnnots(Class<?> clazz, List<InterceptorData> stack)
    {
        // 1- Look method intercepor class annotations
        // 2- Look super class around invoke
        // 3- Look bean around invoke

        // 1-
        Method[] methods = clazz.getDeclaredMethods();

        for (Method method : methods)
        {
            Interceptors incs = method.getAnnotation(Interceptors.class);
            if (incs != null)
            {
                Class<?>[] intClasses = incs.value();

                for (Class<?> intClass : intClasses)
                {
                    configureInterceptorAnnots(intClass, stack, true, method);
                }

            }
        }

        // 2- Super clazz
        List<Class<?>> listSuperClazz = ClassUtil.getSuperClasses(clazz, new ArrayList<Class<?>>());
        configureBeanSuperClassAnnots(listSuperClazz, stack);

        // 3- Bean itself
        WebBeansUtil.configureInterceptorMethods(null, clazz, AroundInvoke.class, false, false, stack, null, false);
        WebBeansUtil.configureInterceptorMethods(null, clazz, AroundTimeout.class, false, false, stack, null, false);
        WebBeansUtil.configureInterceptorMethods(null, clazz, PostConstruct.class, false, false, stack, null, false);
        WebBeansUtil.configureInterceptorMethods(null, clazz, PreDestroy.class, false, false, stack, null, false);

    }
    
    /**
     * Configures super classes interceptors.
     * @param list super classes
     * @param stack interceptor stack
     */
    private static void configureBeanSuperClassAnnots(List<Class<?>> list, List<InterceptorData> stack)
    {
        int i = list.size();

        for (int j = i - 1; j >= 0; j--)
        {
            Class<?> clazz = list.get(j);
            if (!clazz.equals(Object.class))
            {
                WebBeansUtil.configureInterceptorMethods(null, clazz, AroundInvoke.class, false, false, stack, null, false);
                WebBeansUtil.configureInterceptorMethods(null, clazz, AroundTimeout.class, false, false, stack, null, false);
                WebBeansUtil.configureInterceptorMethods(null, clazz, PostConstruct.class, false, false, stack, null, false);
                WebBeansUtil.configureInterceptorMethods(null, clazz, PreDestroy.class, false, false, stack, null, false);
            }
        }
    }

}
