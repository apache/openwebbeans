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
package org.apache.webbeans.test.interceptors.common;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import org.apache.webbeans.test.interceptors.annotation.DependentInterceptorBindingType;

@Interceptor @DependentInterceptorBindingType
public class DependentInterceptor
{
    public static boolean DEP_OK = false;
    
    public static int refCount = 0;

    public static Class<?> exceptionTarget = null;
    
    public DependentInterceptor()
    {
        refCount++;
    }

    @AroundInvoke
    public Object aroundInvoke(InvocationContext ctx) throws Exception
    {
        DEP_OK = true;
        try {  
            return ctx.proceed();
        }
        catch (Exception e) { 
            exceptionTarget = ctx.getTarget().getClass();
            throw e;
        }
    }
}
