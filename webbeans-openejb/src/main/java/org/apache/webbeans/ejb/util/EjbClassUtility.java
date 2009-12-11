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

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import org.apache.webbeans.util.Asserts;
import org.apache.webbeans.util.ClassUtil;

public final class EjbClassUtility
{
    private EjbClassUtility()
    {
        
    }
    
    
    public static Class<?> getLocalInterfaceClass(Type localInterfaceGenericType)
    {
        Asserts.assertNotNull(localInterfaceGenericType);
        
        Class<?> localInterface = null;
        if(ClassUtil.isParametrizedType(localInterfaceGenericType))
        {
            ParameterizedType parametrizedType = (ParameterizedType)localInterfaceGenericType;
            
            if(ClassUtil.checkParametrizedType(parametrizedType))
            {
                Type rawType = parametrizedType.getRawType();
                localInterface = (Class<?>)rawType;
            }
            
        }
        else if(localInterfaceGenericType instanceof Class)
        {
            localInterface = (Class<?>)localInterfaceGenericType;
        }
        
        return localInterface;
    }

}
