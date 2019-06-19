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
package org.apache.webbeans.portable;

import java.util.Map;

import javax.enterprise.inject.spi.Interceptor;
import javax.inject.Provider;

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.context.creational.CreationalContextImpl;
import org.apache.webbeans.proxy.NormalScopeProxyFactory;
import org.apache.webbeans.util.WebBeansUtil;

public class ProviderBasedProducer<T> extends AbstractProducer<T>
{

    private WebBeansContext webBeansContext;
    private Class<T> returnType;
    private Provider<T> provider;
    private T proxyInstance;
    private boolean proxy;

    public ProviderBasedProducer(WebBeansContext webBeansContext, Class<T> returnType, Provider<T> provider, boolean proxy)
    {
        this.webBeansContext = webBeansContext;
        this.returnType = returnType;
        this.provider = provider;
        this.proxy = proxy;
    }

    @Override
    protected T produce(Map<Interceptor<?>, ?> interceptors, CreationalContextImpl<T> creationalContext)
    {
        if (proxyInstance == null)
        {
            if (proxy)
            {
                NormalScopeProxyFactory proxyFactory = webBeansContext.getNormalScopeProxyFactory();
                ClassLoader loader = returnType.getClassLoader();
                if (loader == null || !proxyFactory.hasMarkerInterface(loader))
                {
                    loader = WebBeansUtil.getCurrentClassLoader();
                }
                Class<T> proxyClass = proxyFactory.createProxyClass(loader, returnType);
                proxyInstance = proxyFactory.createProxyInstance(proxyClass, provider);
            }
            else
            {
                proxyInstance = provider.get();
            }
        }
        return proxyInstance;
    }
}
