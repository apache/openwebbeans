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

import java.lang.reflect.Type;

import javax.enterprise.inject.spi.Producer;

import org.apache.webbeans.util.Asserts;
import org.apache.webbeans.util.CollectionUtil;

import javax.enterprise.inject.spi.ProducerFactory;
import org.apache.webbeans.config.WebBeansContext;

public abstract class BuiltInOwbBean<T> extends AbstractOwbBean<T>
{

    private Producer<T> producer;

    protected BuiltInOwbBean(WebBeansContext webBeansContext, WebBeansType webBeansType, Class<T> returnType, ProducerFactory<T> producerFactory)
    {
        this(webBeansContext, webBeansType, new BeanAttributesImpl<T>(CollectionUtil.<Type>unmodifiableSet(returnType, Object.class)), returnType, false, producerFactory);
    }
    
    protected BuiltInOwbBean(
            WebBeansContext webBeansContext,
            WebBeansType webBeansType,
            BeanAttributesImpl<T> beanAttributes,
            Class<T> returnType,
            boolean nullable,
            ProducerFactory<T> producerFactory)
    {
        super(webBeansContext, webBeansType, beanAttributes, returnType, nullable);
        Asserts.assertNotNull(producerFactory, "ProducerFactory");
        producer = producerFactory.createProducer(this);
    }
    
    @Override
    public Producer<T> getProducer()
    {
        return producer;
    }

    /**
     * @return some built in beans can be decorated, this class is the one used to generate the proxy
     */
    public abstract Class<?> proxyableType();
}
