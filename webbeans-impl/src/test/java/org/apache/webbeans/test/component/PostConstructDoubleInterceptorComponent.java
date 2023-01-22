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
package org.apache.webbeans.test.component;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.interceptor.Interceptors;

import org.apache.webbeans.test.annotation.binding.Check;
import org.apache.webbeans.test.component.intercept.Interceptor1;
import org.apache.webbeans.test.component.intercept.Interceptor2;

@RequestScoped
@Interceptors(value = { Interceptor1.class, Interceptor2.class })
public class PostConstructDoubleInterceptorComponent
{
    private @Inject @Check(type = "CHECK") IPayment payment;

    private IPayment p = null;

    static String setininterceptor2 = null;

    @PostConstruct
    public void init()
    {
        this.p = payment;
    }

    public IPayment getP()
    {
        return p;
    }

    public static String getValue()
    {
        return setininterceptor2;
    }
    
    public static void setValue(String s)
    {
        setininterceptor2 = s;
    }
    
}
