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

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import javax.enterprise.inject.spi.InterceptionType;
import javax.enterprise.inject.spi.Interceptor;

import org.apache.webbeans.proxy.InterceptorHandler;
import org.apache.webbeans.util.ExceptionUtil;

public class DefaultInterceptorHandler<T> implements InterceptorHandler
{
    /**
     * The native contextual instance target instance.
     * This is the unproxies and undecorated instance.
     * It e.g. get's used for direct event delivery to private Observer methods.
     */
    private T target;

    /**
     * The instance the Interceptors get applied on.
     * If there is no Decorator involved, then this is the same like {@link #target}.
     * For decorated beans this will point to the outermost Decorator instance.
     */
    private T delegate;

    private Map<Method, List<Interceptor<?>>> interceptors;
    private Map<Interceptor<?>, ?> instances;

    public DefaultInterceptorHandler(T target,
                                     T delegate,
                                     Map<Method, List<Interceptor<?>>> interceptors,
                                     Map<Interceptor<?>, ?> instances)
    {
        this.target = target;
        this.delegate = delegate;
        this.instances = instances;
        this.interceptors = interceptors;
    }

    public T getTarget()
    {
        return target;
    }

    public T getDelegate()
    {
        return delegate;
    }

    public Map<Interceptor<?>, ?> getInstances()
    {
        return instances;
    }

    public Map<Method, List<Interceptor<?>>> getInterceptors()
    {
        return interceptors;
    }

    public Object invoke(Method method, Object[] parameters)
    {
        try
        {
            List<Interceptor<?>> methodInterceptors = interceptors.get(method);

            InterceptorInvocationContext<T> ctx
                = new InterceptorInvocationContext<T>(delegate, InterceptionType.AROUND_INVOKE, methodInterceptors, instances, method, parameters);

            return ctx.proceed();
        }
        catch (Exception e)
        {
            return ExceptionUtil.throwAsRuntimeException(e);
        }
    }
}
