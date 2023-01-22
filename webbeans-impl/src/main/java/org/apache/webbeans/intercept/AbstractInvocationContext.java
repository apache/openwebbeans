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
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import jakarta.inject.Provider;
import jakarta.interceptor.InvocationContext;

import org.apache.webbeans.util.ExceptionUtil;

public abstract class AbstractInvocationContext<T> implements InvocationContext
{

    protected Provider<T> target;
    private AccessibleObject member;
    protected Object[] parameters;
    private Map<String, Object> contextData;
    private Object timer;

    public AbstractInvocationContext(Provider<T> target, AccessibleObject member, Object[] parameters)
    {
        this.target = target;
        this.member = member;
        this.parameters = parameters;
        if (!member.isAccessible())
        {
            member.setAccessible(true);
        }
    }

    public AbstractInvocationContext(Provider<T> target, Method method, Object[] parameters, Object timer)
    {
        this(target, method, parameters);
        this.timer = timer;
    }
    
    @Override
    public T getTarget()
    {
        return target.get();
    }

    @Override
    public Method getMethod()
    {
        if (Method.class.isInstance(member))
        {
            return Method.class.cast(member);
        }
        return null;
    }

    @Override
    public Object[] getParameters()
    {
        return parameters;
    }

    @Override
    public void setParameters(Object[] parameters)
    {
        this.parameters = parameters;
    }

    @Override
    public Map<String, Object> getContextData()
    {
        if (contextData == null)
        {
            contextData = new HashMap<>();
        }
        return contextData;
    }

    @Override
    public Object getTimer()
    {
        return timer;
    }

    @Override
    public Object proceed() throws Exception
    {
        return directProceed();
    }

    public Object directProceed() throws Exception
    {
        try
        {
            return getMethod().invoke(target.get(), parameters);
        }
        catch (InvocationTargetException ite)
        {
            // unpack the reflection Exception
            throw ExceptionUtil.throwAsRuntimeException(ite.getCause());
        }
    }

    // @Override
    public Constructor getConstructor()
    {
        if (Constructor.class.isInstance(member))
        {
            return Constructor.class.cast(member);
        }
        return null;
    }
}
