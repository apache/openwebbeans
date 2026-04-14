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
package org.apache.webbeans.invoke;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.AnnotatedMethod;
import jakarta.enterprise.inject.spi.AnnotatedParameter;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.enterprise.invoke.Invoker;

import org.apache.webbeans.component.AbstractOwbBean;
import org.apache.webbeans.container.BeanManagerImpl;
import org.apache.webbeans.inject.impl.InjectionPointFactory;

/**
 * Invoker that delegates to {@link Method#invoke}, optionally performing CDI lookups.
 */
public final class InvokerImpl<X> implements Invoker<X, Object>
{
    private final AbstractOwbBean<?> bean;
    private final AnnotatedType<X> annotatedBeanClass;
    private final AnnotatedMethod<? super X> annotatedMethod;
    private final Method javaMethod;
    private final boolean instanceLookup;
    private final boolean[] argumentLookup;

    InvokerImpl(AbstractOwbBean<?> bean, AnnotatedType<X> annotatedBeanClass,
                AnnotatedMethod<? super X> annotatedMethod, Method javaMethod,
                boolean instanceLookup, boolean[] argumentLookup)
    {
        this.bean = bean;
        this.annotatedBeanClass = annotatedBeanClass;
        this.annotatedMethod = annotatedMethod;
        this.javaMethod = javaMethod;
        this.instanceLookup = instanceLookup;
        this.argumentLookup = argumentLookup;
        javaMethod.setAccessible(true);
    }

    @Override
    public Object invoke(X instance, Object[] arguments) throws Exception
    {
        boolean isStatic = Modifier.isStatic(javaMethod.getModifiers());
        if (!isStatic && !instanceLookup && instance == null)
        {
            throw new NullPointerException("Instance must not be null");
        }
        int paramCount = javaMethod.getParameterCount();
        if (arguments != null && arguments.length > paramCount)
        {
            throw new IllegalArgumentException("Too many arguments for " + javaMethod);
        }
        if (paramCount > 0 && (arguments == null || arguments.length < paramCount))
        {
            throw new IllegalArgumentException("Too few arguments for " + javaMethod);
        }

        BeanManagerImpl bm = bean.getWebBeansContext().getBeanManagerImpl();
        Bean<?> targetBean = bean;

        CreationalContext<?> targetCc = bm.createCreationalContext(targetBean);
        try
        {
            Object receiver = null;
            if (!isStatic)
            {
                if (instanceLookup)
                {
                    receiver = bm.getReference(targetBean, targetBean.getBeanClass(), targetCc);
                }
                else
                {
                    receiver = instance;
                }
            }

            Object[] args;
            if (paramCount == 0)
            {
                args = new Object[0];
            }
            else
            {
                args = new Object[paramCount];
                InjectionPointFactory ipf = new InjectionPointFactory(bean.getWebBeansContext());
                List<? extends AnnotatedParameter<? super X>> params = annotatedMethod.getParameters();
                for (int i = 0; i < paramCount; i++)
                {
                    AnnotatedParameter<? super X> ap = params.get(i);
                    if (argumentLookup[i])
                    {
                        InjectionPoint ip = ipf.buildInjectionPoint(targetBean, ap, false);
                        args[i] = bm.getInjectableReference(ip, targetCc);
                    }
                    else
                    {
                        args[i] = arguments[i];
                    }
                }
            }

            return javaMethod.invoke(receiver, args);
        }
        finally
        {
            targetCc.release();
        }
    }
}
