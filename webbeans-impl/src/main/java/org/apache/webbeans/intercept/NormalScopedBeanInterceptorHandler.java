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

import jakarta.enterprise.context.spi.Context;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.UnproxyableResolutionException;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.PassivationCapable;
import jakarta.inject.Provider;
import java.io.NotSerializableException;
import java.io.ObjectStreamException;
import java.io.Serializable;

import org.apache.webbeans.config.WebBeansContext;

/**
 * <p>A Provider which handles all NormalScoped proxying.
 * It's two main responsibilities are to provide the one active
 * Contextual Instance, the second is to provide serialisation.</p>
 *
 * <p>The generated proxy will writeReplace() with this class and any
 * NormalScopedBean provider must readResolve() and regenerate the
 * proxy class from the {@link org.apache.webbeans.proxy.NormalScopeProxyFactory}
 * again.
 * </p>
 * <p>Any subclass should either declare all their fields <code>transient</code>
 * or handle the serialisation properly!</p>
 */
public class NormalScopedBeanInterceptorHandler implements Provider, Serializable
{
    private transient BeanManager beanManager;
    protected transient Bean<?> bean;

    /**
     * The passivation if in case this is a {@link PassivationCapable} bean.
     * we just keep this field for serializing it away
     */
    private String beanPassivationId;

    public NormalScopedBeanInterceptorHandler(BeanManager beanManager, Bean<?> bean)
    {
        this.beanManager = beanManager;
        this.bean = bean;
        if (bean instanceof PassivationCapable)
        {
            beanPassivationId = ((PassivationCapable) bean).getId();
        }
    }

    @Override
    public Object get()
    {
        return getContextualInstance();
    }

    public Bean<?> getBean()
    {
        return bean;
    }
    
    protected BeanManager getBeanManager()
    {
        return beanManager;
    }

    protected Object getContextualInstance()
    {
        Object webbeansInstance;

        //Context of the bean
        Context context = beanManager.getContext(bean.getScope());

        //Already saved in context?
        webbeansInstance = context.get(bean);
        if (webbeansInstance != null)
        {
            // voila, we are finished if we found an existing contextual instance
            return webbeansInstance;
        }

        // finally, we create a new contextual instance
        CreationalContext cc = beanManager.createCreationalContext(bean);
        webbeansInstance = context.get(bean, cc);

        if (webbeansInstance == null)
        {
            throw new UnproxyableResolutionException("Cannot find a contextual instance of bean " + bean.toString());
        }
        return webbeansInstance;
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
    */
    protected Object readResolve() throws ObjectStreamException
    {
        WebBeansContext webBeansContext = WebBeansContext.getInstance();
        beanManager = webBeansContext.getBeanManagerImpl();
        bean = beanManager.getPassivationCapableBean(beanPassivationId);
        if (bean == null)
        {
            throw new NotSerializableException("Failure during de-serialisation: Cannot load Bean with passivationId " + beanPassivationId);
        }

        return webBeansContext.getNormalScopeProxyFactory().createNormalScopeProxy(bean);
    }
}
