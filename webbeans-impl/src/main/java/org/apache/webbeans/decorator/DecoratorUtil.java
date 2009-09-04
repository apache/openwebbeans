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
package org.apache.webbeans.decorator;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.decorator.Decorates;
import javax.enterprise.inject.spi.Decorator;

import org.apache.webbeans.component.ManagedBean;
import org.apache.webbeans.container.BeanManagerImpl;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.exception.WebBeansException;
import org.apache.webbeans.logger.WebBeansLogger;
import org.apache.webbeans.util.AnnotationUtil;
import org.apache.webbeans.util.Asserts;
import org.apache.webbeans.util.ClassUtil;

/**
 * Decorator related utility class.
 * 
 * @version $Rev$ $Date$
 *
 */
public final class DecoratorUtil
{
    //Logger instance
    private static final WebBeansLogger logger = WebBeansLogger.getLogger(DecoratorUtil.class);

    //Non-instantiate
    private DecoratorUtil()
    {
        //Empty
    }

    /**
     * Check decorator conditions.
     * @param decoratorClazz decorator class
     */
    public static void checkDecoratorConditions(Class<?> decoratorClazz)
    {       
        Set<Type> decoratorSet = new HashSet<Type>();
        ClassUtil.setInterfaceTypeHierarchy(decoratorSet, decoratorClazz);
        
        //No-Decorates found, check from super class
        if(!checkInternalDecoratorConditions(decoratorClazz, decoratorSet))
        {
           boolean found = checkInternalDecoratorConditionsRecursivley(decoratorClazz,decoratorSet);
           
           if(!found)
           {
               throw new WebBeansConfigurationException("Decorator delegate attribute for decorator class : " + decoratorClazz.getName() + " can not be found!");
           }
        }
    }
    
    private static boolean checkInternalDecoratorConditionsRecursivley(Class<?> decoratorClazz,Set<Type> decoratorSet)
    {
        Class<?> superClazz = decoratorClazz.getSuperclass();
        if(!superClazz.equals(Object.class))
        {
            boolean found = checkInternalDecoratorConditions(superClazz, decoratorSet);
            if(!found)
            {
                return checkInternalDecoratorConditionsRecursivley(superClazz, decoratorSet);
            }
            else
            {
                return true;
            }
        }
        
        return false;
    }
    
    private static boolean checkInternalDecoratorConditions(Class<?> decoratorClazz,Set<Type> decoratorSet)
    {
        Field[] fields = decoratorClazz.getDeclaredFields();
        boolean found = false;
        for (Field field : fields)
        {
            if (AnnotationUtil.isAnnotationExist(field.getDeclaredAnnotations(), Decorates.class))
            {
                if (found)
                {
                    throw new WebBeansConfigurationException("Decorator class : " + decoratorClazz.getName() + " can only contain one delegate attribute but find more than one!.");
                }
                else
                {
                    Class<?> fieldType = field.getType();
                    if (!ClassUtil.isInterface(fieldType.getModifiers()))
                    {
                        throw new WebBeansConfigurationException("Decorator class : " + decoratorClazz.getName() + " delegate attribute type must be interface");
                    }

                    for (Type decType : decoratorSet)
                    {
                        if (!fieldType.isAssignableFrom((Class<?>)decType))
                        {
                            throw new WebBeansConfigurationException("Decorator class : " + decoratorClazz.getName() + " delegate attribute must implement all of the decorator decorated types.");
                        }
                    }

                    found = true;
                }
            }
        }
        
        return found;
        
    }

    public static void checkManagedBeanDecoratorConditions(ManagedBean<?> component)
    {
        Asserts.assertNotNull("component", "component parameter can not be null");

        Set<Annotation> annSet = component.getQualifiers();
        Annotation[] anns = new Annotation[annSet.size()];
        anns = annSet.toArray(anns);

        List<Decorator<?>> decoratorList = BeanManagerImpl.getManager().resolveDecorators(component.getTypes(), anns);
        if (!decoratorList.isEmpty())
        {
            Class<?> clazz = component.getReturnType();
            if (ClassUtil.isFinal(clazz.getModifiers()))
            {
                throw new WebBeansConfigurationException("Managed Bean : " + component.getReturnType().getName() + " can not be declared final, because it has one or more decorators");
            }

            Method[] methods = clazz.getDeclaredMethods();
            for (Method method : methods)
            {
                int modifiers = method.getModifiers();
                if (!ClassUtil.isStatic(modifiers) && !ClassUtil.isPrivate(modifiers) && ClassUtil.isFinal(modifiers))
                {
                    // Check decorator implements this
                    Iterator<Decorator<?>> itDecorator = decoratorList.iterator();
                    while (itDecorator.hasNext())
                    {
                        WebBeansDecorator<?> decorator = (WebBeansDecorator<?>) itDecorator.next();
                        Class<?> decClazz = decorator.getClazz();

                        try
                        {
                            if (decClazz.getMethod(method.getName(), method.getParameterTypes()) != null)
                            {
                                throw new WebBeansConfigurationException("Managed Bean : " + component.getReturnType().getName() + " can not define non-private, non-static, final method : " + method.getName() + ", because one of its decorators implements this method");

                            }

                        }
                        catch (SecurityException e)
                        {
                            logger.error("Security exception, can not access decorator class : " + decClazz.getName() + " method : " + method.getName(), e);
                            throw new WebBeansException(e);

                        }
                        catch (NoSuchMethodException e)
                        {
                            continue;
                        }

                    }
                }
            }
        }
    }

}