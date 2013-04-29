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

import javax.enterprise.context.spi.CreationalContext;
import javax.inject.Provider;

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.proxy.NormalScopeProxyFactory;
import org.apache.webbeans.util.WebBeansUtil;

public class ProviderBasedProxyProducer<T> extends AbstractProducer<T>
{

    private WebBeansContext webBeansContext;
    private Class<T> returnType;
    private Provider<T> provider;
    private T proxyInstance;

    public ProviderBasedProxyProducer(WebBeansContext webBeansContext, Class<T> returnType, Provider<T> provider)
    {
        this.webBeansContext = webBeansContext;
        this.returnType = returnType;
        this.provider = provider;
    }

    @Override
    public T produce(CreationalContext<T> creationalContext)
    {
        if (proxyInstance == null)
        {
            NormalScopeProxyFactory proxyFactory = webBeansContext.getNormalScopeProxyFactory();
            ClassLoader loader = returnType.getClassLoader();
            if (loader == null) {
                loader = WebBeansUtil.getCurrentClassLoader();
            }
            Class<T> proxyClass = proxyFactory.createProxyClass(loader, returnType);
            proxyInstance = proxyFactory.createProxyInstance(proxyClass, provider);
        }
        return proxyInstance;
    }
}
