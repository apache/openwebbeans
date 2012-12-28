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
package org.apache.webbeans.container;

import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.Set;

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.util.ClassUtil;

public final class ResolutionUtil
{

    private final WebBeansContext webBeansContext;

    public ResolutionUtil(WebBeansContext webBeansContext)
    {
        this.webBeansContext = webBeansContext;
    }

    public static boolean checkBeanTypeAssignableToGivenType(Set<Type> beanTypes, Type givenType, boolean newBean)
    {
        Iterator<Type> itBeanApiTypes = beanTypes.iterator();
        while (itBeanApiTypes.hasNext())
        {
            Type beanApiType = itBeanApiTypes.next();

            if(ClassUtil.isAssignable(beanApiType, givenType))
            {
                return true;
            }
            else
            {
                //Check for @New
                if(newBean && ClassUtil.isParametrizedType(givenType))
                {
                    Class<?> requiredType = ClassUtil.getClass(givenType);
                    if(ClassUtil.isClassAssignable(requiredType, ClassUtil.getClass(beanApiType)))
                    {
                        return true;
                    }
                }
            }
        }

        return false;
    }
}
