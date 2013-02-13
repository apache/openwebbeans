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

import java.io.ObjectStreamException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InterceptionType;
import javax.enterprise.inject.spi.Interceptor;

import org.apache.webbeans.config.WebBeansContext;
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

    /**
     * The passivation if in case this is a
     * {@link javax.enterprise.inject.spi.PassivationCapable} bean.
     * we just keep this field for serializing it away
     */
    private String beanPassivationId;


    private Map<Method, List<Interceptor<?>>> interceptors;
    private Map<Interceptor<?>, ?> instances;

    /**
     * InterceptorHandler wich gets used in our InjectionTargets which
     * support interceptors and decorators
     * @param target the decorated and intercepted instance. Needed for delivering Events to private methods, etc.
     * @param delegate the outermost Decorator or the intercepted instance
     * @param interceptors Map with all active interceptors for each method.
     * @param instances the Interceptor instances
     * @param beanPassivationId passivationId if a Bean is {@link javax.enterprise.inject.spi.PassivationCapable}
     */
    public DefaultInterceptorHandler(T target,
                                     T delegate,
                                     Map<Method, List<Interceptor<?>>> interceptors,
                                     Map<Interceptor<?>, ?> instances,
                                     String beanPassivationId)
    {
        this.target = target;
        this.delegate = delegate;
        this.instances = instances;
        this.interceptors = interceptors;
        this.beanPassivationId = beanPassivationId;
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

    /**
     * The following code gets generated into the proxy:
     *
     * <pre>
     * Object writeReplace() throws ObjectStreamException
     * {
     *     return provider;
     * }
     * </pre>
     *
     * The trick is to replace the generated proxy class with this handler
     * and on deserialisation we use readResolve to create/resolve
     * the proxy class again.
     */
    @SuppressWarnings("unused")
    Object readResolve() throws ObjectStreamException
    {
        WebBeansContext webBeansContext = WebBeansContext.getInstance();
        BeanManager beanManager = webBeansContext.getBeanManagerImpl();
        Bean<?> bean = beanManager.getPassivationCapableBean(beanPassivationId);

        return webBeansContext.getInterceptorDecoratorProxyFactory().getCachedProxyClass(bean);
    }

}
