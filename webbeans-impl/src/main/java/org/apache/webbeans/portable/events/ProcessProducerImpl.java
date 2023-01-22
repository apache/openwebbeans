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
package org.apache.webbeans.portable.events;

import jakarta.enterprise.inject.spi.AnnotatedMember;
import jakarta.enterprise.inject.spi.ProcessProducer;
import jakarta.enterprise.inject.spi.Producer;
import jakarta.enterprise.inject.spi.configurator.ProducerConfigurator;

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.configurator.ProducerConfiguratorImpl;

/**
 * Implementation of {@link ProcessProducer}.
 * 
 * @version $Rev$ $Date$
 *
 * @param <X> bean class
 * @param <T> producer return type class
 */
public class ProcessProducerImpl<X,T> extends EventBase implements ProcessProducer<X, T>
{
    /**Annotated method or annotated field according to producer method or field*/
    private final AnnotatedMember<X> annotateMember;
    
    /**Used by container to produce instance for producer method or field*/
    private Producer<T> producer;

    private ProducerConfiguratorImpl<T> producerConfigurator;

    private boolean customProducerSet;
    
    public ProcessProducerImpl(Producer<T> producer,AnnotatedMember<X> annotateMember)
    {
        this.annotateMember = annotateMember;
        this.producer = producer;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void addDefinitionError(Throwable t)
    {
        checkState();
        WebBeansContext.getInstance().getBeanManagerImpl().getErrorStack().pushError(t);
    }

    @Override
    public ProducerConfigurator<T> configureProducer()
    {
        checkState();
        if (customProducerSet)
        {
            throw new IllegalStateException("Only one of setProducer() and configureProducer() must be used!");
        }

        this.producerConfigurator = new ProducerConfiguratorImpl<>();
        this.producer = null;
        return producerConfigurator;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AnnotatedMember<X> getAnnotatedMember()
    {
        checkState();
        return annotateMember;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Producer<T> getProducer()
    {
        checkState();
        if (producerConfigurator != null)
        {
            return producerConfigurator.getProducer();
        }
        return producer;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setProducer(Producer<T> producer)
    {
        checkState();
        if (this.producerConfigurator != null)
        {
            throw new IllegalStateException("Only one of setProducer() and configureProducer() must be used!");
        }
        this.producer = producer;
        customProducerSet = true;
    }
}
