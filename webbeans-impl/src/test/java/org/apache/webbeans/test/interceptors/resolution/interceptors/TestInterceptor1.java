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

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.AroundTimeout;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import org.apache.webbeans.test.interceptors.resolution.beans.UtilitySampleBean;
import org.apache.webbeans.util.ExceptionUtil;


@Interceptor
@TestIntercepted1
public class TestInterceptor1 extends TestInterceptorParent
{
    public static int invocationCount = 0;
    public static int exceptionCount = 0;
    public static int postConstructCount = 0;

    @Inject
    public TestInterceptor1(UtilitySampleBean ctUtility)
    {
        super();
        ctUtility.setI(42);
    }



    @AroundInvoke
    public Object caller(InvocationContext context) throws Exception
    {
        try
        {
            invocationCount++;
            return context.proceed();
            
        } catch(Exception e)
        {
            exceptionCount++;
            throw e;
        }
    }

    /**
     * another name -> no invocation -> 2 interceptors
     */
    @PostConstruct
    public void anotherPostConstruct(InvocationContext context)
    {
        postConstructCount++;
        try
        {
            context.proceed();
        }
        catch (Exception e)
        {
            ExceptionUtil.throwAsRuntimeException(e);
        }

    }

    /**
     * 2 private methods -> 2 interceptors
     */
    @PreDestroy
    private void preDestroy(InvocationContext context)
    {
        preDestroyCount++;
        try
        {
            context.proceed();
        }
        catch (Exception e)
        {
            ExceptionUtil.throwAsRuntimeException(e);
        }

    }

    /**
     * overridden method -> only 1 interceptor
     * @param context
     */
    @Override
    @AroundTimeout
    public Object aroundTimeout(InvocationContext context) throws Exception
    {
        aroundTimeoutCount++;
        return context.proceed();
    }


}
