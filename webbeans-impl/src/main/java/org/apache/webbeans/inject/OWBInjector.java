/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.webbeans.inject;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Set;

import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InjectionTarget;

import org.apache.webbeans.container.BeanManagerImpl;
import org.apache.webbeans.util.ClassUtil;

/**
 * Injects dependencies of the given Java EE component
 * instance.
 * 
 * @version $Rev$ $Date$
 *
 */
public final class OWBInjector
{
    private OWBInjector()
    {
        
    }
    
    @SuppressWarnings("unchecked")
    public static void inject(Object javaEeComponentInstance) throws Exception
    {
        BeanManager beanManager = BeanManagerImpl.getManager();
        try
        {
            Class<?> injectableComponentClass = javaEeComponentInstance.getClass();
            AnnotatedType<Object> annotated = (AnnotatedType<Object>) beanManager.createAnnotatedType(injectableComponentClass);
            InjectionTarget<Object> injectionTarget = beanManager.createInjectionTarget(annotated);
            Set<InjectionPoint> injectionPoints = injectionTarget.getInjectionPoints();
            if(injectionPoints != null && injectionPoints.size() > 0)
            {
                for(InjectionPoint injectionPoint : injectionPoints)
                {
                    Object object = beanManager.getInjectableReference(injectionPoint, beanManager.createCreationalContext(injectionPoint.getBean()));
                    if(injectionPoint.getMember() instanceof Method)
                    {
                        Method method = (Method)injectionPoint.getMember();
                        ClassUtil.callInstanceMethod(method, javaEeComponentInstance, new Object[]{object});                        
                    }
                    else if(injectionPoint.getMember() instanceof Field)
                    {
                        Field field = (Field)injectionPoint.getMember();
                        ClassUtil.setField(javaEeComponentInstance, field, object);
                    }
                }
            }
            
            
        }catch(Exception e)
        {
            throw e;
        }
    }
}
