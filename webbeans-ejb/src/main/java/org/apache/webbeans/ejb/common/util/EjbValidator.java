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
package org.apache.webbeans.ejb.common.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import jakarta.decorator.Decorator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.spi.AnnotatedMethod;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.ObserverMethod;
import jakarta.enterprise.inject.spi.SessionBeanType;
import jakarta.interceptor.Interceptor;

import org.apache.webbeans.ejb.common.component.BaseEjbBean;
import org.apache.webbeans.event.ObserverMethodImpl;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.portable.AnnotatedElementFactory;
import org.apache.webbeans.util.AnnotationUtil;
import org.apache.webbeans.util.Asserts;

/**
 * Validates session beans.
 * 
 * @version $Rev: 915746 $ $Date: 2010-02-24 12:43:43 +0200 (Wed, 24 Feb 2010) $
 */
public final class EjbValidator
{
    // No-instaniate
    private EjbValidator()
    {
        // Empty
    }

    /**
     * Validates session bean's scope.
     * 
     * @param ejbBean ejb bean
     */
    public static void validateEjbScopeType(BaseEjbBean<?> ejbBean)
    {
        Asserts.assertNotNull(ejbBean, "Session Bean");

        if (ejbBean.getScope() == null)
        {
            throw new NullPointerException("Session Bean scope can not be null");
        }

        if (ejbBean.getEjbType() == null)
        {
            throw new NullPointerException("Session Bean type can not be null. It must be one of @Stateless, @Stateful, @Singleton");
        }

        if (ejbBean.getEjbType() == SessionBeanType.STATELESS)
        {
            if (!ejbBean.getScope().equals(Dependent.class))
            {
                throw new WebBeansConfigurationException("Stateless Session Bean class : " + ejbBean.getReturnType() + " " + "can not define scope other than @Dependent");
            }
        }
        else if (ejbBean.getEjbType() == SessionBeanType.SINGLETON)
        {
            if (!ejbBean.getScope().equals(Dependent.class) && !ejbBean.getScope().equals(ApplicationScoped.class))
            {
                throw new WebBeansConfigurationException("Singleton Session Bean class : " + ejbBean.getReturnType() + " "
                                                         + "can not define scope other than @Dependent or @ApplicationScoped");
            }
        }
    }

    /**
     * Validates session bean decorator/interceptor conditions.
     * 
     * @param ejbClass ejb bean class
     */
    public static void validateDecoratorOrInterceptor(Class<?> ejbClass)
    {
        Asserts.assertNotNull(ejbClass, "ejbClass");

        if (AnnotationUtil.hasClassAnnotation(ejbClass, Decorator.class))
        {
            throw new WebBeansConfigurationException(EjbConstants.EJB_WEBBEANS_ERROR_CLASS_PREFIX + ejbClass.getName() + " can not annotated with @Decorator");

        }

        if (AnnotationUtil.hasClassAnnotation(ejbClass, Interceptor.class))
        {
            throw new WebBeansConfigurationException(EjbConstants.EJB_WEBBEANS_ERROR_CLASS_PREFIX + ejbClass.getName() + " can not annotated with @Interceptor");
        }
    }
    
    /**
     * Check generic type conditions.
     * @param ejbClass ebj class
     * @param scopeType scope type
     */
    public static void validateGenericBeanType(Class<?> ejbClass, Class<? extends Annotation> scopeType)
    {
        Asserts.assertNotNull(ejbClass, "ejbClass");
        Asserts.assertNotNull(ejbClass, "scopeType");
        
        if (ejbClass.getTypeParameters().length > 0)
        {
            if(!scopeType.equals(Dependent.class))
            {
                throw new WebBeansConfigurationException("Ejb generic bean class : " + ejbClass.getName() + "scope must be @Dependent");
            }
        }
    }
    
    public static <T> void validateObserverMethods(BaseEjbBean<?> bean, Set<ObserverMethod<T>> observers)
    {
        for(ObserverMethod<T> observer : observers)
        {
            ObserverMethodImpl<T> obs = (ObserverMethodImpl<T>)observer;
            AnnotatedMethod<T> method = obs.getObserverMethod();
            List<?> locals =  bean.getBusinessLocalInterfaces();
            if(locals != null)
            {
                Iterator<?> it = locals.iterator();
                boolean found = false;
                while(it.hasNext())
                {
                    Class<?> clazz = (Class<?>)it.next();
                    Method classMethod = bean.getWebBeansContext().getSecurityService().
                            doPrivilegedGetDeclaredMethod(clazz, method.getJavaMember().getName(), method.getJavaMember().getParameterTypes());
                    if(classMethod == null)
                    {
                        continue;
                    }
                    else
                    {
                        //Should only be a single method that matches the names & params
                        AnnotatedElementFactory annotatedElementFactory = bean.getWebBeansContext().getAnnotatedElementFactory();
                        AnnotatedType<T> declaringType = (AnnotatedType<T>) annotatedElementFactory.newAnnotatedType(classMethod.getDeclaringClass());
                        obs.setObserverMethod(annotatedElementFactory.newAnnotatedMethod(classMethod, declaringType));
                        found = true;
                        break;
                    }
                }
                
                if(!found)
                {
                    if(!method.isStatic())
                    {
                        throw new WebBeansConfigurationException("Observer method : " + method.getJavaMember().getName() + " in session bean class : " + 
                                bean.getBeanClass() + " must be business method");                                            
                    }
                }
            }
        }
    }

}
