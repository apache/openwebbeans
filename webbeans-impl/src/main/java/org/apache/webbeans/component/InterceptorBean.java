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
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.InterceptionType;
import javax.enterprise.inject.spi.Interceptor;
import javax.interceptor.InvocationContext;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.exception.WebBeansException;
import org.apache.webbeans.util.ExceptionUtil;

/**
 * <p>Abstract {@link javax.enterprise.inject.spi.Interceptor} Bean implementation.
 *
 * <p>Any Interceptor is also an InjectionTarget as they can contain
 * &#064;Inject InjectionPoints.</p>
 */
public abstract class InterceptorBean<T> extends InjectionTargetBean<T> implements Interceptor<T>, EnterpriseBeanMarker
{
   /**
     * Constructor of the web bean component
     */
    private Constructor<T> constructor;

    /**
     * The Methods to be called per interception type.
     * The method signature must be
     * <pre>Object <METHOD>(InvocationContext) throws Exception</pre>
     */
    private Map<InterceptionType, Method[]> interceptionMethods;

    /**
     * This is for performance reasons
     */
    private Method aroundInvokeMethod = null;

    public InterceptorBean(WebBeansContext webBeansContext, 
                           AnnotatedType<T> annotatedType,
                           Set<Type> types,
                           Class<T> beanClass,
                           Map<InterceptionType, Method[]> interceptionMethods)
    {
        super(webBeansContext,
              WebBeansType.INTERCEPTOR,
              annotatedType,
              types,
              Collections.<Annotation>emptySet(),
              Dependent.class,
              beanClass,
              Collections.<Class<? extends Annotation>>emptySet());
        this.interceptionMethods = Collections.unmodifiableMap(interceptionMethods);

        // extract the aroundInvokeMethod if any
        Method[] aroundInvokeMethods = interceptionMethods.get(InterceptionType.AROUND_INVOKE);
        if (aroundInvokeMethods != null && aroundInvokeMethods.length == 1)
        {
            aroundInvokeMethod = aroundInvokeMethods[0];
            if (!aroundInvokeMethod.isAccessible())
            {
                aroundInvokeMethod.setAccessible(true);
            }
        }
    }

    @Override
    protected T createComponentInstance(CreationalContext<T> creationalContext)
    {
        return getInjectionTarget().produce(creationalContext);
    }


    /**
     * Get constructor.
     *
     * @return constructor
     */
    public Constructor<T> getConstructor()
    {
        return constructor;
    }

    /**
     * Set constructor.
     *
     * @param constructor constructor instance
     */
    public void setConstructor(Constructor<T> constructor)
    {
        this.constructor = constructor;
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
    public Method[] getInterceptorMethods(InterceptionType interceptionType)
    {
        Method[] methods = interceptionMethods.get(interceptionType);
        if (methods == null || methods.length == 0)
        {
            throw new WebBeansException("InterceptionType not yet supported: " + interceptionType);
        }
        return methods;
    }


    @Override
    public boolean intercepts(InterceptionType interceptionType)
    {
        return interceptionMethods.containsKey(interceptionType);
    }

    @Override
    public Object intercept(InterceptionType interceptionType, T instance, InvocationContext invocationContext)
    {
        try
        {
            if (InterceptionType.AROUND_INVOKE == interceptionType && aroundInvokeMethod != null)
            {
                return aroundInvokeMethod.invoke(instance, invocationContext);
            }

            Method[] interceptorMethods = getInterceptorMethods(interceptionType);
            if (interceptorMethods.length == 1)
            {
                // directly invoke the interceptor method with the given InvocationContext
                return interceptorMethods[0].invoke(instance, invocationContext);
            }
            else
            {
                // otherwise we need to wrap the InvocationContext into an own temporary InvocationContext
                // which handles multiple interceptor methods at a time
                if (invocationContext instanceof MultiMethodInvocationContext)
                {
                    // this happens while we recurse through the interceptors which have multiple interceptor-methods
                    MultiMethodInvocationContext mmInvocationContext = (MultiMethodInvocationContext) invocationContext;
                    int methodIndex = mmInvocationContext.getCurrentInterceptorIdx();
                    if (methodIndex < (interceptorMethods.length -1))
                    {
                        return interceptorMethods[methodIndex].invoke(instance, invocationContext);
                    }
                    else
                    {
                        return interceptorMethods[methodIndex].invoke(instance, mmInvocationContext.getWrapped());
                    }
                }
                else
                {
                    // We need to create the wrapper InvocationContext on the first time.
                    // This will internally walk through all the methods
                    MultiMethodInvocationContext mmInvocationContext
                            = new MultiMethodInvocationContext(invocationContext, interceptionType, instance, this);
                    return mmInvocationContext.proceed();
                }
            }
        }
        catch (Exception e)
        {
            throw ExceptionUtil.throwAsRuntimeException(e);
        }
    }

    /**
     * An InvocationContext wraper for handling multiple interceptor methods.
     * We will first make sure the own interceptor methods get handled and only
     * then continue with the wrapped InvocationHandler.
     */
    public static class MultiMethodInvocationContext implements InvocationContext
    {
        private final InvocationContext wrapped;
        private final Interceptor interceptor;
        private final InterceptionType interceptionType;
        private final Object instance;
        private int currentInterceptorIdx;

        public MultiMethodInvocationContext(InvocationContext wrapped,
                                            InterceptionType interceptionType,
                                            Object instance,
                                            Interceptor interceptor)
        {
            this.wrapped = wrapped;
            this.interceptor = interceptor;
            this.interceptionType = interceptionType;
            this.instance = instance;
            this.currentInterceptorIdx = 0; // we start with method 0
        }

        public int getCurrentInterceptorIdx()
        {
            return currentInterceptorIdx;
        }

        public InvocationContext getWrapped()
        {
            return wrapped;
        }

        @Override
        public Object proceed() throws Exception
        {
            currentInterceptorIdx++;
            return interceptor.intercept(interceptionType, instance, this);
        }


        @Override
        public Map<String, Object> getContextData()
        {
            return wrapped.getContextData();
        }

        @Override
        public Method getMethod()
        {
            return wrapped.getMethod();
        }

        @Override
        public Object getTarget()
        {
            return wrapped.getTarget();
        }

        @Override
        public Object getTimer()
        {
            return wrapped.getTimer();
        }

        @Override
        public Object[] getParameters()
        {
            return wrapped.getParameters();
        }

        @Override
        public void setParameters(Object[] parameters)
        {
            wrapped.setParameters(parameters);
        }
    }

}
