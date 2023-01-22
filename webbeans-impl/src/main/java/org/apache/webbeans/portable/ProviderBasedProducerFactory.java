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

import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.Producer;
import jakarta.inject.Provider;

import jakarta.enterprise.inject.spi.ProducerFactory;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.util.Asserts;

public class ProviderBasedProducerFactory<P> implements ProducerFactory<P>
{

    private Class<?> providerType;
    private boolean proxy;

    protected Provider<?> provider;
    protected WebBeansContext webBeansContext;

    public ProviderBasedProducerFactory(boolean proxy, Provider<?> provider, Class<?> providerType, WebBeansContext context)
    {
        Asserts.assertNotNull(provider);
        Asserts.assertNotNull(providerType);
        Asserts.assertNotNull(context);
        this.provider = provider;
        this.providerType = providerType;
        webBeansContext = context;
        this.proxy = proxy;
    }

    @Override
    public <T> Producer<T> createProducer(Bean<T> bean)
    {
        return new ProviderBasedProducer<>(webBeansContext, (Class<T>) providerType, (Provider<T>) provider, proxy);
    }
}
