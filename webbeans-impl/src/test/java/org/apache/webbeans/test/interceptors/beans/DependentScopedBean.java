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
package org.apache.webbeans.test.interceptors.beans;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Named;

import org.apache.webbeans.test.interceptors.annotation.DependentInterceptorBindingType;

@Named("org.apache.webbeans.test.interceptors.beans.DependentScopedBean")
public class DependentScopedBean
{
    public static boolean SAY_HELLO = false;
    
    public static boolean POST_CONSTRUCT = false;
    
    public static boolean PRE_DESTROY = false;
    
    public DependentScopedBean()
    {
        
    }
    
    @DependentInterceptorBindingType
    public void sayHello()
    {
        SAY_HELLO = true;
    }

    @DependentInterceptorBindingType
    public void throwException()
    {
        throw new RuntimeException("goodbye");
    }
 
    @PostConstruct
    public void postConstruct()
    {
        POST_CONSTRUCT  = true;
    }
    
    @PreDestroy
    public void preDestroy()
    {
        PRE_DESTROY = true;
    }
}
