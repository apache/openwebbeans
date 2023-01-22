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

import jakarta.enterprise.event.Event;
import jakarta.enterprise.util.TypeLiteral;

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.event.EventImpl;
import org.apache.webbeans.portable.EventProducer;
import org.apache.webbeans.util.CollectionUtil;

/**
 * Implicit observable bean definition.
 * 
 * @version $Rev$ $Date$
 */
public class EventBean<T> extends BuiltInOwbBean<Event<T>>
{

    /**
     * Creates a new instance of event bean.
     * @param webBeansContext
     */
    @SuppressWarnings("serial")
    public EventBean(WebBeansContext webBeansContext)
    {
        super(webBeansContext,
              WebBeansType.OBSERVABLE,
              new BeanAttributesImpl<>(CollectionUtil.<Type>unmodifiableSet(new TypeLiteral<Event<T>>()
              {
              }.getRawType(), Object.class)),
              new TypeLiteral<Event<T>>(){}.getRawType(),
              new SimpleProducerFactory<>(new EventProducer<T>(webBeansContext)));
    }
    
    /* (non-Javadoc)
     * @see org.apache.webbeans.component.AbstractOwbBean#isPassivationCapable()
     */
    @Override
    public boolean isPassivationCapable()
    {
        return true;
    }

    @Override
    public Class<?> proxyableType()
    {
        return EventImpl.class;
    }
}
