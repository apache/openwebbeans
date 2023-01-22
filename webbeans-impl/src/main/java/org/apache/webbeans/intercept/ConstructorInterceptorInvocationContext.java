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
package org.apache.webbeans.intercept;

import org.apache.webbeans.util.ExceptionUtil;

import jakarta.enterprise.inject.spi.InterceptionType;
import jakarta.enterprise.inject.spi.Interceptor;
import jakarta.inject.Provider;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;

public class ConstructorInterceptorInvocationContext<T> extends InterceptorInvocationContext<T>
{
    protected Object newInstance;

    public ConstructorInterceptorInvocationContext(Provider<T> provider,
                                                   List<Interceptor<?>> aroundConstructInterceptors,
                                                   Map<Interceptor<?>, ?> interceptorInstances,
                                                   Constructor<T> cons, Object[] parameters)
    {
        super(provider, InterceptionType.AROUND_CONSTRUCT, aroundConstructInterceptors, interceptorInstances, cons, parameters);
    }

    public Object getNewInstance()
    {
        return newInstance;
    }

    @Override
    public Object directProceed() throws Exception
    {
        if (newInstance != null) // already called
        {
            return newInstance;
        }
        try
        {
            newInstance = getConstructor().newInstance(parameters);
            return null;
        }
        catch (InvocationTargetException ite)
        {
            // unpack the reflection Exception
            throw ExceptionUtil.throwAsRuntimeException(ite.getCause());
        }
    }
}
