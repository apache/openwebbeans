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
package org.apache.webbeans.test.component.intercept.webbeans;

import jakarta.annotation.PostConstruct;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import org.apache.webbeans.test.component.intercept.webbeans.bindings.Secure;
import org.apache.webbeans.util.ExceptionUtil;

@Secure
@Interceptor
public class SecureInterceptor
{
    public static boolean CALL = false;

    @PostConstruct
    public void atCreationTime(InvocationContext ic)
    {
        try
        {
            ic.proceed();
        }
        catch (Exception e)
        {
            throw ExceptionUtil.throwAsRuntimeException(e);
        }
    }

    @AroundInvoke
    public Object manageTransaction(InvocationContext ctx) throws Exception
    {
        try
        {
            CALL = true;
            return ctx.proceed();

        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return null;
    }

}
