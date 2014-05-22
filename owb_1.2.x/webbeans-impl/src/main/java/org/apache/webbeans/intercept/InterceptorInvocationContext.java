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

import java.lang.reflect.AccessibleObject;
import java.util.List;
import java.util.Map;

import javax.enterprise.inject.spi.InterceptionType;
import javax.enterprise.inject.spi.Interceptor;

/**
 * InvocationContext for business method interceptors
 */
public class InterceptorInvocationContext<T> extends AbstractInvocationContext<T>
{

    protected InterceptionType type;
    protected List<Interceptor<?>> interceptors;
    protected Map<Interceptor<?>, ?> instances;
    protected int index = 0;
    
    public InterceptorInvocationContext(T target, InterceptionType type,
                                        List<Interceptor<?>> interceptors, Map<Interceptor<?>, ?> instances,
                                        AccessibleObject method, Object[] parameters)
    {
        super(target, method, parameters);
        this.type = type;
        this.interceptors = interceptors;
        this.instances = instances;
    }

    @Override
    public Object proceed() throws Exception
    {
        if (index < interceptors.size())
        {
            Interceptor interceptor = interceptors.get(index++);
            if (!interceptor.intercepts(type))
            {
                // continue with next interceptor
                // this e.g. happens for lifecycle interceptors
                return proceed();
            }

            return interceptor.intercept(type, instances.get(interceptor), this);
        }
        else
        {
            return super.proceed();
        }
    }
}
