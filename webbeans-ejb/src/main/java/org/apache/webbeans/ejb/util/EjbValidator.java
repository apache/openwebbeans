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
package org.apache.webbeans.ejb.util;

import javax.decorator.Decorator;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.spi.SessionBeanType;
import javax.interceptor.Interceptor;

import org.apache.webbeans.ejb.EjbConstants;
import org.apache.webbeans.ejb.component.EjbBean;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.util.AnnotationUtil;
import org.apache.webbeans.util.Asserts;

/**
 * Validates session beans.
 * 
 * @version $Rev$ $Date$
 */
public final class EjbValidator
{
    // No-instaniate
    private EjbValidator()
    {
        // Empty
    }

    /**
     * Validates session bean's scope types.
     * 
     * @param ejbBean ejb bean
     */
    public static void validateEjbScopeType(EjbBean<?> ejbBean)
    {
        Asserts.assertNotNull(ejbBean, "Session Bean can not be null");

        if (ejbBean.getScopeType() == null)
        {
            throw new NullPointerException("Session Bean scope type can not be null");
        }

        if (ejbBean.getEjbType() == null)
        {
            throw new NullPointerException("Session Bean type can not be null. It must be one of @Stateless, @Stateful, @Singleton");
        }

        if (ejbBean.getEjbType().equals(SessionBeanType.STATELESS))
        {
            if (!ejbBean.getScopeType().equals(Dependent.class))
            {
                throw new WebBeansConfigurationException("Stateless Session Bean class : " + ejbBean.getReturnType() + " " + "can not define scope other than @Dependent");
            }
        }
        else if (ejbBean.getEjbType().equals(SessionBeanType.SINGLETON))
        {
            if (!(ejbBean.getScopeType().equals(Dependent.class) || ejbBean.getScopeType().equals(ApplicationScoped.class)))
            {
                throw new WebBeansConfigurationException("Singleton Session Bean class : " + ejbBean.getReturnType() + " " + "can not define scope other than @Dependent or @ApplicationScoped");
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
        Asserts.assertNotNull(ejbClass, "ejbClass parameter can not be null");

        if (AnnotationUtil.isAnnotationExistOnClass(ejbClass, Decorator.class))
        {
            throw new WebBeansConfigurationException(EjbConstants.EJB_WEBBEANS_ERROR_CLASS_PREFIX + ejbClass.getName() + " can not annotated with @Decorator");

        }

        if (AnnotationUtil.isAnnotationExistOnClass(ejbClass, Interceptor.class))
        {
            throw new WebBeansConfigurationException(EjbConstants.EJB_WEBBEANS_ERROR_CLASS_PREFIX + ejbClass.getName() + " can not annotated with @Interceptor");
        }
    }

}