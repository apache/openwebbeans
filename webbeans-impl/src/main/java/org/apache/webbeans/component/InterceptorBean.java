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
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;

import javax.enterprise.inject.spi.BeanAttributes;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.container.InjectionTargetFactoryImpl;
import org.apache.webbeans.container.InterceptorInjectionTargetFactory;
import org.apache.webbeans.util.CDI11s;
import org.apache.webbeans.util.ExceptionUtil;

/**
 * <p>Abstract {@link javax.enterprise.inject.spi.Interceptor} Bean implementation.
 *
 * <p>Any Interceptor is also an InjectionTarget as they can contain
 * &#064;Inject InjectionPoints.</p>
 */
public abstract class InterceptorBean<T> extends InjectionTargetBean<T> implements Interceptor<T>
{

    /**
     * The Methods to be called per interception type.
     * The method signature must be
     * <pre>Object <METHOD>(InvocationContext) throws Exception</pre>
     */
    private Map<InterceptionType, Method[]> interceptionMethods;

    /**
     * This is for performance reasons
     */
    protected Method aroundInvokeMethod = null;
    protected Method aroundConstructMethod = null;

    protected InterceptorBean(WebBeansContext webBeansContext,
                                  AnnotatedType<T> annotatedType,
                                  BeanAttributes<T> beanAttributes,
                                  Class<T> beanClass,
                                  Map<InterceptionType, Method[]> interceptionMethods,
                                  Method aroundConstruct,
                                  InjectionTargetFactoryImpl<T> factory)
    {
        super(webBeansContext,
                WebBeansType.INTERCEPTOR,
                annotatedType,
                beanAttributes,
                beanClass,
                factory);
        this.interceptionMethods = Collections.unmodifiableMap(interceptionMethods);

        for (Method[] methods: interceptionMethods.values())
        {
            for (Method method: methods)
            {
                if (!method.isAccessible())
                {
                    method.setAccessible(true);
                }
            }
        }

        aroundConstructMethod = aroundConstruct;
        if (aroundConstructMethod != null && !aroundConstructMethod.isAccessible())
        {
            aroundConstructMethod.setAccessible(true);
        }

        Method[] aroundInvokeMethods = interceptionMethods.get(InterceptionType.AROUND_INVOKE);
        if (aroundInvokeMethods != null && aroundInvokeMethods.length == 1)
        {
            aroundInvokeMethod = aroundInvokeMethods[0];
        }
    }

    public InterceptorBean(WebBeansContext webBeansContext, 
                           AnnotatedType<T> annotatedType,
                           BeanAttributesImpl<T> beanAttributes,
                           Class<T> beanClass,
                           Map<InterceptionType, Method[]> interceptionMethods,
                           Method aroundConstructMethod)
    {
        this(webBeansContext, annotatedType, beanAttributes, beanClass, interceptionMethods, aroundConstructMethod,
                new InterceptorInjectionTargetFactory<T>(annotatedType, webBeansContext));
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
        return  interceptionMethods.get(interceptionType);
    }


    @Override
    public boolean intercepts(InterceptionType interceptionType)
    {
        return interceptionMethods.containsKey(interceptionType) || (interceptionType.equals(CDI11s.AROUND_CONSTRUCT) && aroundConstructMethod != null);
    }

    @Override
    public Object intercept(InterceptionType interceptionType, T instance, InvocationContext invocationContext)
    {
        try
        {
            if (interceptionType.equals(CDI11s.AROUND_CONSTRUCT) && aroundConstructMethod != null)
            {
                return aroundConstructMethod.invoke(instance, invocationContext);
            }

            if (InterceptionType.AROUND_INVOKE == interceptionType && aroundInvokeMethod != null)
            {
                return aroundInvokeMethod.invoke(instance, invocationContext);
            }

            Method[] interceptorMethods = getInterceptorMethods(interceptionType);
            if (interceptorMethods == null || interceptorMethods.length == 0)
            {
                // this very interceptor doesn't support this interception type.
                // this might happen for lifecycle callback methods
                // let's continue with the next interceptor
                return invocationContext.proceed();
            }
            else if (interceptorMethods.length == 1)
            {
                // directly invoke the interceptor method with the given InvocationContext
                if (interceptorMethods[0].getParameterTypes().length == 1)
                {
                    return interceptorMethods[0].invoke(instance, invocationContext);
                } // else it can be a @PostContruct void pc(); which shouldn't be called from here
                else
                {
                    return invocationContext.proceed();
                }
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
        catch (InvocationTargetException ite)
        {
            throw ExceptionUtil.throwAsRuntimeException(ite.getCause());
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
            currentInterceptorIdx = 0; // we start with method 0
        }

        public int getCurrentInterceptorIdx()
        {
            // in proceed we +1 to handle index properly whatever the stack is but we need to -1 here to not skip
            // index 0 when we have multiple interceptors
            return currentInterceptorIdx - 1;
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

        // @Override
        public Constructor getConstructor()
        {
            return null;
        }
    }

}
