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
import jakarta.interceptor.AroundTimeout;
import jakarta.interceptor.InvocationContext;

import org.apache.webbeans.test.interceptors.resolution.beans.UtilitySampleBean;
import org.apache.webbeans.util.ExceptionUtil;


public class TestInterceptorParent
{
    public static int postConstructCount = 0;
    public static int preDestroyCount = 0;
    public static int aroundTimeoutCount = 0;

    @Inject
    private UtilitySampleBean fieldUtility;

    public UtilitySampleBean getFieldUtility()
    {
        return fieldUtility;
    }

    @PostConstruct
    public void postConstruct(InvocationContext context)
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

    @AroundTimeout
    public Object aroundTimeout(InvocationContext context) throws Exception
    {
        aroundTimeoutCount++;
        return context.proceed();
    }

}
