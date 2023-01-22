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

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import org.apache.webbeans.test.component.intercept.webbeans.bindings.Action;

/**
 * This is an interceptor for an InterceptorBinding annotation (Action)
 * which has another InterceptorBinding (Transactional) as meta-annotation.
 */
@Action
@Interceptor
public class ActionInterceptor
{
    public static Action.Type type = null;

    @AroundInvoke
    public Object call(InvocationContext context) throws Exception
    {
        type = Action.Type.STANDARD;
        WInterceptorComponent.sWithMeta = 10;
        WMetaInterceptorComponent.sWithMeta = 10;
        return context.proceed();
    }
}
