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
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.inject.spi.AnnotatedMethod;
import jakarta.enterprise.inject.spi.AnnotatedType;

import org.apache.webbeans.annotation.AnnotationManager;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.exception.WebBeansDeploymentException;
import org.apache.webbeans.util.Asserts;


public final class InterceptorUtil
{

    private final WebBeansContext webBeansContext;

    public InterceptorUtil(WebBeansContext webBeansContext)
    {
        this.webBeansContext = webBeansContext;

    }

    /**
     * @return the Type hierarchy in the order superclass first. Object.class is <b>not</b> included!
     */
    public List<Class> getReverseClassHierarchy(Class clazz)
    {
        List<Class> hierarchy = new ArrayList<>();
        while (clazz != null && clazz != Object.class)
        {
            hierarchy.add(0, clazz);
            clazz = clazz.getSuperclass();
        }

        return hierarchy;
    }


    public List<AnnotatedMethod<?>> getLifecycleMethods(AnnotatedType<?> annotatedType, Class<? extends Annotation> annotation)
    {
        List<AnnotatedMethod<?>> lifecycleMethods = new ArrayList<>();

        List<Class> classes = getReverseClassHierarchy(annotatedType.getJavaClass());
        for (Class clazz : classes)
        {
            for (AnnotatedMethod<?> annotatedMethod : webBeansContext.getAnnotatedElementFactory().getFilteredAnnotatedMethods(annotatedType))
            {
                if (annotatedMethod.getJavaMember().getDeclaringClass() != clazz)
                {
                    continue;
                }

                if (annotatedMethod.isAnnotationPresent(annotation))
                {
                    lifecycleMethods.add(annotatedMethod);
                }
            }
        }

        return lifecycleMethods;
    }

    public void checkSimpleWebBeansInterceptorConditions(Class<?> clazz)
    {
        Asserts.nullCheckForClass(clazz);
        Annotation[] anns = clazz.getDeclaredAnnotations();

        boolean hasClassInterceptors = false;
        AnnotationManager annotationManager = webBeansContext.getAnnotationManager();
        if (annotationManager.getInterceptorBindingMetaAnnotations(anns).length > 0)
        {
            hasClassInterceptors = true;
        }
        else
        {
            Annotation[] stereoTypes = annotationManager.getStereotypeMetaAnnotations(clazz.getDeclaredAnnotations());
            for (Annotation stero : stereoTypes)
            {
                if (annotationManager.hasInterceptorBindingMetaAnnotation(stero.annotationType().getDeclaredAnnotations()))
                {
                    hasClassInterceptors = true;
                    break;
                }
            }
        }

        //Simple webbeans
        if(Modifier.isFinal(clazz.getModifiers()) && hasClassInterceptors)
        {
            throw new WebBeansDeploymentException("Final Simple class with name : " + clazz.getName() + " can not define any InterceptorBindings");
        }

        Method[] methods = webBeansContext.getSecurityService().doPrivilegedGetDeclaredMethods(clazz);

        for (Method method : methods)
        {
            int modifiers = method.getModifiers();
            if (!method.isSynthetic() && !method.isBridge() && !Modifier.isStatic(modifiers) && !Modifier.isPrivate(modifiers) && Modifier.isFinal(modifiers))
            {
                if (hasClassInterceptors)
                {
                    throw new WebBeansDeploymentException("Simple web bean class : " + clazz.getName()
                                                             + " can not define non-static, non-private final methods. "
                                                             + "Because it is annotated with at least one @InterceptorBinding");
                }
                else
                {
                    if (annotationManager.hasInterceptorBindingMetaAnnotation(
                        method.getDeclaredAnnotations()))
                    {
                        throw new WebBeansDeploymentException("Method : " + method.getName() + "in simple web bean class : "
                                                                 + clazz.getName()
                                                                 + " can not be defined as non-static, non-private and final. "
                                                                 + "Because it is annotated with at least one @InterceptorBinding");
                    }
                }
            }
        }

    }



}
