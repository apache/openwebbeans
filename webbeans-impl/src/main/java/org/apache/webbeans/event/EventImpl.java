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
package org.apache.webbeans.event;

import java.io.IOException;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Set;
import java.util.concurrent.CompletionStage;

import javax.enterprise.event.Event;
import javax.enterprise.event.NotificationOptions;
import javax.enterprise.inject.spi.EventMetadata;
import javax.enterprise.util.TypeLiteral;

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.util.Asserts;

/**
 * Event implementation.
 * 
 * @param <T> event type
 * @see Event
 */
public class EventImpl<T> implements Event<T>, Serializable
{
    private static final long serialVersionUID = 393021493190378023L;

    private EventMetadataImpl metadata;

    private transient WebBeansContext webBeansContext;

    /**
     * Creates a new event.
     * 
     * @param webBeansContext
     */
    public EventImpl(EventMetadata metadata, WebBeansContext webBeansContext)
    {
        Asserts.assertNotNull(metadata, "event metadata");
        this.metadata = wrapMetadata(metadata);
        this.webBeansContext = webBeansContext;
    }

    private EventMetadataImpl wrapMetadata(EventMetadata metadata)
    {
        if (metadata instanceof EventMetadataImpl)
        {
            return (EventMetadataImpl)metadata;
        }
        else
        {
            Set<Annotation> qualifiers = metadata.getQualifiers();
            return new EventMetadataImpl(null, metadata.getType(), metadata.getInjectionPoint(), qualifiers.toArray(new Annotation[qualifiers.size()]), webBeansContext);
        }
    }

    /**
     * Fires event with given event object.
     */
    @Override
    public void fire(T event)
    {
        Type eventType = event.getClass();
        webBeansContext.getWebBeansUtil().validEventType(eventType.getClass(), metadata.getType());
        webBeansContext.getBeanManagerImpl().fireEvent(event, metadata.select(eventType), false);
    }

    @Override
    public <U extends T> CompletionStage<U> fireAsync(U event)
    {
        Type eventType = event.getClass();
        webBeansContext.getWebBeansUtil().validEventType(eventType.getClass(), metadata.getType());
        return webBeansContext.getNotificationManager().fireEvent(event, metadata.select(eventType), false,
                    webBeansContext.getNotificationManager().getDefaultNotificationOptions());
    }

    //X TODO OWB-1182 CDI 2.0
    @Override
    public <U extends T> CompletionStage<U> fireAsync(U event, NotificationOptions notificationOptions)
    {
        Type eventType = event.getClass();
        webBeansContext.getWebBeansUtil().validEventType(eventType.getClass(), metadata.getType());
        return webBeansContext.getNotificationManager().fireEvent(event, metadata.select(eventType), false,
                    notificationOptions);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Event<T> select(Annotation... bindings)
    {
        return new EventImpl<T>(metadata.select(bindings), webBeansContext);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public <U extends T> Event<U> select(Class<U> subtype, Annotation... bindings)
    {
        return new EventImpl<U>(metadata.select(subtype, bindings), webBeansContext);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public <U extends T> Event<U> select(TypeLiteral<U> subtype, Annotation... bindings)
    {
        return new EventImpl<U>(metadata.select(subtype, bindings), webBeansContext);
    }
    
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException
    {
        in.defaultReadObject();
        webBeansContext = WebBeansContext.currentInstance();
    }

    public EventMetadataImpl getMetadata()
    {
        return metadata;
    }
}
