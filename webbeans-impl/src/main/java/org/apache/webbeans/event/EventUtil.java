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
package org.apache.webbeans.event;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Set;

import jakarta.enterprise.event.Event;
import jakarta.enterprise.inject.spi.InjectionPoint;

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.util.ClassUtil;

public final class EventUtil
{
    private EventUtil()
    {
        // avoid construction for utility class
    }

    //expensive check needed by the TCK (EventBindingTypesTest#testFireEventWithNonRuntimeBindingTypeFails) - see OWB-798
    public static void checkQualifierImplementations(Set<Annotation> qualifiers)
    {
        for (Annotation qualifier : qualifiers)
        {
            //This is added, because TCK Event tests for this.
            Retention retention = qualifier.annotationType().getAnnotation(Retention.class);
            RetentionPolicy policy = retention.value();
            if(policy != RetentionPolicy.RUNTIME)
            {
                throw new IllegalArgumentException("Event qualifier RetentionPolicy must be RUNTIME for qualifier : " + qualifier.annotationType().getName());
            }
        }
    }

    public static void checkEventBindings(WebBeansContext webBeansContext, Set<Annotation> annotations)
    {
        webBeansContext.getAnnotationManager().checkQualifierConditions(annotations);
    }

    public static boolean checkObservableInjectionPointConditions(InjectionPoint injectionPoint)
    {
        Type type = injectionPoint.getType();
        
        Class<?> candidateClazz = null;
        if(type instanceof Class)
        {
            candidateClazz = (Class<?>)type;
        }
        else if(type instanceof ParameterizedType)
        {
            ParameterizedType pt = (ParameterizedType)type;
            candidateClazz = (Class<?>)pt.getRawType();
        }
        else
        {
            throw new IllegalArgumentException("Can't determine the type for " + type);
        }
        
        if(!candidateClazz.equals(Event.class))
        {
            return false;
        }                

        if (!ClassUtil.isParametrizedType(injectionPoint.getType()))
        {
            throw new WebBeansConfigurationException("@Observable field injection " + injectionPoint
                                                     + " must be ParametrizedType with actual type argument");
        }
        else
        {                        
            if(ClassUtil.isParametrizedType(injectionPoint.getType()))
            {
                ParameterizedType pt = (ParameterizedType)injectionPoint.getType();
                
                Class<?> rawType = (Class<?>) pt.getRawType();
                
                Type[] typeArgs = pt.getActualTypeArguments();
                
                if(!(rawType.equals(Event.class)))
                {
                    return false;
                }                
                else
                {                                        
                    if(typeArgs.length != 1)
                    {
                        throw new IllegalArgumentException("@Observable field injection " + injectionPoint.toString()
                                                           + " must not have more than one actual type argument");
                    }
                }                                
            }
            else
            {
                throw new IllegalArgumentException("@Observable field injection " + injectionPoint.toString()
                                                   + " must be defined as ParameterizedType with one actual type argument");
            }        
        }
        
        return true;

    }

}
