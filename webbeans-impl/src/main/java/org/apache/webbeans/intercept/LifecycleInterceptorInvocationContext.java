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

import jakarta.enterprise.inject.spi.AnnotatedMethod;
import jakarta.enterprise.inject.spi.InterceptionType;
import jakarta.enterprise.inject.spi.Interceptor;
import jakarta.interceptor.InvocationContext;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * InvocationContext for lifecycle methods like &#064;PostConstruct, etc.
 */
public class LifecycleInterceptorInvocationContext<T> implements InvocationContext
{
    private T target;
    private InterceptionType type;
    private List<Interceptor<?>> interceptors;
    private Map<Interceptor<?>, ?> instances;
    private Map<String, Object> contextData = new HashMap<>();
    private int interceptorIndex;
    private List<AnnotatedMethod<?>> lifecycleMethods;

    public LifecycleInterceptorInvocationContext(T target, InterceptionType type, List<Interceptor<?>> interceptors, Map<Interceptor<?>, ?> instances,
                                                 List<AnnotatedMethod<?>> lifecycleMethods)
    {
        this.target = target;
        this.type = type;
        this.interceptors = interceptors;
        this.instances = instances;
        this.lifecycleMethods = lifecycleMethods;
    }

    @Override
    public T getTarget()
    {
        return target;
    }

    @Override
    public Map<String, Object> getContextData()
    {
        return contextData;
    }

    public void setContextData(Map<String, Object> contextData)
    {
        this.contextData = contextData;
    }

    @Override
    public Object proceed() throws Exception
    {
        if (interceptors != null && interceptorIndex < interceptors.size())
        {
            Interceptor interceptor = interceptors.get(interceptorIndex++);

            if (interceptor.intercepts(type))
            {
                return interceptor.intercept(type, instances.get(interceptor), this);
            }
            else
            {
                return proceed();
            }
        }
        else
        {
            if (lifecycleMethods != null)
            {
                // only if there is a lifecycle method, otherwise re immediately return
                for (AnnotatedMethod<?> lifecycleMethod : lifecycleMethods)
                {   Method m = lifecycleMethod.getJavaMember();
                    if (!m.isAccessible())
                    {
                        m.setAccessible(true);
                    }
                    try
                    {
                        m.invoke(getTarget());
                    }
                    catch (InvocationTargetException ite)
                    {
                        throw ExceptionUtil.throwAsRuntimeException(ite.getCause());
                    }
                }
            }
            // else, see interceptors spec
            // "For lifecycle callback interceptor methods, if there is no callback method
            // defined on the target class, the invocation of proceed in the last
            // interceptor method in the chain is a no-op, and null is returned."

            return null;
        }
    }


    @Override
    public Method getMethod()
    {
        return null;
    }

    @Override
    public Object[] getParameters()
    {
        return new Object[0];
    }

    @Override
    public void setParameters(Object[] parameters)
    {
    }

    @Override
    public Object getTimer()
    {
        return null;
    }

    // @Override
    public Constructor getConstructor()
    {
        return null;
    }
}
