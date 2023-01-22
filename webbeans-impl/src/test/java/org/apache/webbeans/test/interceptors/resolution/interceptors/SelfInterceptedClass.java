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
package org.apache.webbeans.test.interceptors.resolution.interceptors;

import jakarta.enterprise.context.RequestScoped;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;

/**
 * Sample bean which has an AroundInvoke interceptor on itself.
 */
@RequestScoped
public class SelfInterceptedClass
{
    public static int interceptionCount = 0;

    private int meaningOfLife = 0;

    public void someBusinessMethod()
    {
        meaningOfLife = 42;
    }

    public int getMeaningOfLife()
    {
        return meaningOfLife;
    }

    @AroundInvoke
    protected Object interceptMe(InvocationContext ic) throws Exception
    {
        interceptionCount++;
        return ic.proceed();
    }
}

