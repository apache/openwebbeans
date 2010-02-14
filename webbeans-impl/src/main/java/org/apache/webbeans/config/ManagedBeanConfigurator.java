/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable
 * law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 * for the specific language governing permissions and limitations under the License.
 */
package org.apache.webbeans.config;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.util.Set;

import javax.decorator.Decorator;
import javax.interceptor.Interceptor;

import org.apache.webbeans.component.ManagedBean;
import org.apache.webbeans.component.ProducerFieldBean;
import org.apache.webbeans.component.ProducerMethodBean;
import org.apache.webbeans.component.WebBeansType;
import org.apache.webbeans.container.BeanManagerImpl;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.intercept.InterceptorUtil;
import org.apache.webbeans.util.AnnotationUtil;
import org.apache.webbeans.util.ClassUtil;
import org.apache.webbeans.util.WebBeansUtil;

/**
 * Configures Simple WebBeans Component.
 * <p>
 * Contains useful static methods for creating Simple WebBeans Components.
 * </p>
 * 
 * @version $Rev$ $Date$
 */
public final class ManagedBeanConfigurator
{
    /**
     * Private constructor.
     */
    private ManagedBeanConfigurator()
    {

    }

    /**
     * Checks the implementation class for checking conditions.
     * 
     * @param clazz implementation class
     * @throws WebBeansConfigurationException if any configuration exception occurs
     */
    public static void checkManagedBeanCondition(Class<?> clazz) throws WebBeansConfigurationException
    {
        int modifier = clazz.getModifiers();

        if (AnnotationUtil.hasClassAnnotation(clazz, Decorator.class) && AnnotationUtil.hasClassAnnotation(clazz, Interceptor.class))
        {
            throw new WebBeansConfigurationException("ManagedBean implementation class : " + clazz.getName() + " may not annotated with both @Interceptor and @Decorator annotation");
        }

        if (!AnnotationUtil.hasClassAnnotation(clazz, Decorator.class) && !AnnotationUtil.hasClassAnnotation(clazz, Interceptor.class))
        {
            InterceptorUtil.checkSimpleWebBeansInterceptorConditions(clazz);
        }

        if (ClassUtil.isInterface(modifier))
        {
            throw new WebBeansConfigurationException("ManagedBean implementation class : " + clazz.getName() + " may not defined as interface");
        }
    }

    /**
     * Returns true if this class can be candidate for simple web bean, false otherwise.
     * 
     * @param clazz implementation class
     * @return true if this class can be candidate for simple web bean
     * @throws WebBeansConfigurationException if any configuration exception occurs
     */
    public static boolean isManagedBean(Class<?> clazz) throws WebBeansConfigurationException
    {
        try
        {
            WebBeansUtil.isManagedBeanClass(clazz);

        }
        catch (WebBeansConfigurationException e)
        {
            return false;
        }

        return true;
    }

    /**
     * Returns the newly created Simple WebBean Component.
     * 
     * @param clazz Simple WebBean Component implementation class
     * @return the newly created Simple WebBean Component
     * @throws WebBeansConfigurationException if any configuration exception occurs
     * @deprecated
     */
    public static <T> ManagedBean<T> define(Class<T> clazz, WebBeansType type) throws WebBeansConfigurationException
    {
        BeanManagerImpl manager = BeanManagerImpl.getManager();

        checkManagedBeanCondition(clazz);

        ManagedBean<T> component = new ManagedBean<T>(clazz, type);
        
        WebBeansUtil.setInjectionTargetBeanEnableFlag(component);   
        
        DefinitionUtil.defineSerializable(component);
        DefinitionUtil.defineStereoTypes(component, clazz.getDeclaredAnnotations());
                
        Annotation[] clazzAnns = clazz.getDeclaredAnnotations();

        DefinitionUtil.defineApiTypes(component, clazz);
        DefinitionUtil.defineScopeType(component, clazzAnns, "Simple WebBean Component implementation class : " + clazz.getName() + " stereotypes must declare same @Scope annotations");
        
        WebBeansUtil.checkGenericType(component);
        DefinitionUtil.defineQualifiers(component, clazzAnns);
        DefinitionUtil.defineName(component, clazzAnns, WebBeansUtil.getManagedBeanDefaultName(clazz.getSimpleName()));

        Constructor<T> constructor = WebBeansUtil.defineConstructor(clazz);
        component.setConstructor(constructor);
        DefinitionUtil.addConstructorInjectionPointMetaData(component, constructor);

        //Dropped from the speicification
        //WebBeansUtil.checkSteroTypeRequirements(component, clazz.getDeclaredAnnotations(), "Simple WebBean Component implementation class : " + clazz.getName());

        Set<ProducerMethodBean<?>> producerComponents = DefinitionUtil.defineProducerMethods(component);
        manager.getBeans().addAll(producerComponents);

        Set<ProducerFieldBean<?>> producerFields = DefinitionUtil.defineProduerFields(component);
        manager.getBeans().addAll(producerFields);

        DefinitionUtil.defineDisposalMethods(component);
        DefinitionUtil.defineInjectedFields(component);
        DefinitionUtil.defineInjectedMethods(component);
        DefinitionUtil.defineObserverMethods(component, clazz);

        return component;
    }    
}