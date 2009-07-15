/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
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

public final class EjbValidator
{
    private EjbValidator()
    {
        
    }
    
    
    public static void validateEjbScopeType(EjbBean<?> ejbComponent)
    {
        Asserts.assertNotNull(ejbComponent, "scopeType parameter can not be null");

        if(ejbComponent.getScopeType() == null)
        {
            throw new NullPointerException("EjbComponent scope type can not be null");
        }
        
        if(ejbComponent.getEjbType() == null)
        {
            throw new NullPointerException("EjbComponent ejb type can not be null");
        }
        
        if(ejbComponent.getEjbType().equals(SessionBeanType.STATELESS))
        {
            if(!ejbComponent.getScopeType().equals(Dependent.class))
            {
                throw new WebBeansConfigurationException("Stateless ejb class : " + ejbComponent.getReturnType() + " " +
                		"can not define scope other than @Dependent");
            }
        }
        else if(ejbComponent.getEjbType().equals(SessionBeanType.SINGLETON))
        {
            if(!(ejbComponent.getScopeType().equals(Dependent.class) || ejbComponent.getScopeType().equals(ApplicationScoped.class) ))
            {
                throw new WebBeansConfigurationException("Singleton ejb class : " + ejbComponent.getReturnType() + " " +
                        "can not define scope other than @Dependent or @ApplicationScoped");
            }            
        }                
    }
    
    public static void validateDecoratorOrInterceptor(Class<?> ejbClass)
    {
        Asserts.assertNotNull(ejbClass,"ejbClass parameter can not be null");
        
        if(AnnotationUtil.isAnnotationExistOnClass(ejbClass, Decorator.class))
        {
            throw new WebBeansConfigurationException(EjbConstants.EJB_WEBBEANS_ERROR_CLASS_PREFIX + ejbClass.getName() + " can not annotated with @Decorator");
            
        }
        
        if(AnnotationUtil.isAnnotationExistOnClass(ejbClass, Interceptor.class))
        {
            throw new WebBeansConfigurationException(EjbConstants.EJB_WEBBEANS_ERROR_CLASS_PREFIX + ejbClass.getName() + " can not annotated with @Interceptor");            
        }
    }

}
