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

import org.apache.webbeans.config.WebBeansContext;

import javax.enterprise.inject.spi.BeanAttributes;
import javax.enterprise.inject.spi.Producer;
import javax.enterprise.inject.spi.ProducerFactory;
import java.io.NotSerializableException;
import java.io.ObjectStreamException;
import java.io.Serializable;

public class ProducerAwareInjectionTargetBean<T> extends AbstractOwbBean<T> implements Serializable
{
    private final Producer<T> producer;

    public ProducerAwareInjectionTargetBean(final WebBeansContext webBeansContext, final WebBeansType webBeansType,
                                            final BeanAttributes<T> beanAttributes, final Class<?> beanClass,
                                            final boolean nullable,
                                            final ProducerFactory<?> factory)
    {
        super(webBeansContext, webBeansType, beanAttributes, beanClass, nullable);
        this.producer = factory.createProducer(this);

        final String id = super.getId();
        if (id != null)
        {
            passivatingId = "ProvidedProducer" + id + ",factory=" + factory.hashCode();
        }
    }

    @Override
    public Producer<T> getProducer()
    {
        return producer;
    }

    @Override
    public String getId()
    {
        return passivatingId;
    }

    private Object writeReplace() throws ObjectStreamException
    {
        String passivationId = getId();
        if (passivationId == null)
        {
            throw new NotSerializableException("Bean is about to be serialized and does not have any any PassivationCapable id: " + toString());
        }
        return new PassivationBeanWrapper(passivationId);
    }


    public static class PassivationBeanWrapper implements Serializable
    {
        private static final long serialVersionUID = 1L;

        private final String passivationId;

        public PassivationBeanWrapper(final String passivationId)
        {
            this.passivationId = passivationId;
        }

        private Object readResolve() throws ObjectStreamException
        {
            return WebBeansContext.getInstance().getBeanManagerImpl().getPassivationCapableBean(passivationId);
        }
    }
}
