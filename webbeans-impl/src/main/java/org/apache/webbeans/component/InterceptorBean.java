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
package org.apache.webbeans.component;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.InterceptionType;
import javax.enterprise.inject.spi.Interceptor;
import javax.interceptor.InvocationContext;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.exception.WebBeansException;
import org.apache.webbeans.util.Asserts;
import org.apache.webbeans.util.ExceptionUtil;

/**
 * <p>Abstract {@link javax.enterprise.inject.spi.Interceptor} Bean implementation.
 *
 * <p>Any Interceptor is also an InjectionTarget as they can contain
 * &#064;Inject InjectionPoints.</p>
 */
public abstract class InterceptorBean<T> extends AbstractInjectionTargetBean<T> implements Interceptor<T>
{
    /**
     *
     * @param returnType the return Type of the Bean
     * @param annotatedType AnnotatedType will be returned by some methods in the SPI
     * @param webBeansContext
     * @param intercepts the InterceptionTypes this Bean handles on the intercepted target
     */
    public InterceptorBean(Class<T> returnType, AnnotatedType<T> annotatedType,
                           WebBeansContext webBeansContext,
                           Set<InterceptionType> intercepts)
    {
        super(WebBeansType.INTERCEPTOR, returnType, annotatedType, webBeansContext);

        Asserts.assertNotNull(intercepts, "Interceptor does not handle any InterceptionTypes!");
        this.intercepts = intercepts;
    }



    private Set<InterceptionType> intercepts;

    /**
     * The Method to be called for InterceptionType.AROUND_INVOKE.
     * The method signature must be
     * <pre>Object <METHOD>(InvocationContext) throws Exception</pre>
     */
    private Method aroundInvokeMethod;

    /**
     * The Method to be called for InterceptionType.AROUND_TIMEOUT.
     * The method signature must be
     * <pre>Object <METHOD>(InvocationContext) throws Exception</pre>
     */
    private Method aroundTimeoutMethod;

    /**
     * All &#064;PostConstruct interceptor method
     * The method signature must be
     * <pre>void <METHOD>(InvocationContext) throws Exception</pre>
     */
    private Method postConstructMethod;

    /**
     * All &#064;PreDestroy interceptor method
     * The method signature must be
     * <pre>void <METHOD>(InvocationContext) throws Exception</pre>
     */
    private Method preDestroyMethod;

    /**
     * All &#064;PrePassivate interceptor method
     * The method signature must be
     * <pre>void <METHOD>(InvocationContext) throws Exception</pre>
     */
    private Method prePassivateMethod;

    /**
     * All &#064;PostActivate interceptor method
     * The method signature must be
     * <pre>void <METHOD>(InvocationContext) throws Exception</pre>
     */
    private Method postActivateMethod;

    public void setAroundInvokeMethod(Method aroundInvokeMethod)
    {
        this.aroundInvokeMethod = aroundInvokeMethod;
    }

    public void setAroundTimeoutMethod(Method aroundTimeoutMethod)
    {
        this.aroundTimeoutMethod = aroundTimeoutMethod;
    }

    public void setPostActivateMethod(Method postActivateMethod)
    {
        this.postActivateMethod = postActivateMethod;
    }

    public void setPostConstructMethod(Method postConstructMethod)
    {
        this.postConstructMethod = postConstructMethod;
    }

    public void setPreDestroyMethod(Method preDestroyMethod)
    {
        this.preDestroyMethod = preDestroyMethod;
    }

    public void setPrePassivateMethod(Method prePassivateMethod)
    {
        this.prePassivateMethod = prePassivateMethod;
    }

    /**
     * Interceptors are by default &#064;Dependent scoped.
     */
    @Override
    public Class<? extends Annotation> getScope()
    {
        return Dependent.class;
    }

    /**
     * @param interceptionType
     * @return the underlying interceptor method for the given InterceptionType or <code>null</code>
     */
    protected Method getInterceptorMethod(InterceptionType interceptionType)
    {
        if (InterceptionType.AROUND_INVOKE.equals(interceptionType))
        {
            return aroundInvokeMethod;
        }
        if (InterceptionType.POST_CONSTRUCT.equals(interceptionType))
        {
            return postConstructMethod;
        }
        if (InterceptionType.PRE_DESTROY.equals(interceptionType))
        {
            return preDestroyMethod;
        }
        if (InterceptionType.AROUND_TIMEOUT.equals(interceptionType))
        {
            return aroundTimeoutMethod;
        }
        if (InterceptionType.POST_ACTIVATE.equals(interceptionType))
        {
            return postActivateMethod;
        }
        if (InterceptionType.PRE_PASSIVATE.equals(interceptionType))
        {
            return prePassivateMethod;
        }

        throw new WebBeansException("InterceptionType not yet supported: " + interceptionType);
    }


    @Override
    public boolean intercepts(InterceptionType interceptionType)
    {
        return intercepts.contains(interceptionType);
    }

    @Override
    public Object intercept(InterceptionType interceptionType, T instance, InvocationContext invocationContext)
    {
        try
        {
            return getInterceptorMethod(interceptionType).invoke(instance, invocationContext);
        }
        catch (InvocationTargetException ite)
        {
            throw ExceptionUtil.throwAsRuntimeException(ite);
        }
        catch (IllegalAccessException iae)
        {
            throw ExceptionUtil.throwAsRuntimeException(iae);
        }
    }

}
