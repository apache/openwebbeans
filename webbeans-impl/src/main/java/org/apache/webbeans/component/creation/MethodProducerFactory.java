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
package org.apache.webbeans.component.creation;

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.portable.ProducerMethodProducer;
import org.apache.webbeans.util.Asserts;

import jakarta.enterprise.inject.spi.AnnotatedMember;
import jakarta.enterprise.inject.spi.AnnotatedMethod;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.enterprise.inject.spi.Producer;
import java.util.HashSet;
import java.util.Set;

public class MethodProducerFactory<P> extends BaseProducerFactory<P>
{
    private AnnotatedMethod<? super P> producerMethod;

    public MethodProducerFactory(AnnotatedMethod<? super P> producerMethod, Bean<P> parent, WebBeansContext webBeansContext)
    {
        super(parent, webBeansContext);
        Asserts.assertNotNull(producerMethod, "producer method");
        this.producerMethod = producerMethod;
        defineDisposalMethod();
    }

    @Override
    protected AnnotatedMember<? super P> producerType()
    {
        return producerMethod;
    }

    public Class<?> getReturnType()
    {
        return producerMethod.getJavaMember().getReturnType();
    }

    @Override
    public <T> Producer<T> createProducer(Bean<T> bean)
    {
        Set<InjectionPoint> disposalIPs = getInjectionPoints(bean);
        Producer<T> producer = new ProducerMethodProducer<>(parent, producerMethod, disposalMethod, createInjectionPoints(bean), disposalIPs, webBeansContext);
        return webBeansContext.getWebBeansUtil().fireProcessProducerEvent(producer, producerMethod);
    }

    protected Set<InjectionPoint> createInjectionPoints(Bean<?> bean)
    {
        return new HashSet<>(webBeansContext.getInjectionPointFactory().buildInjectionPoints(bean, producerMethod));
    }
}
