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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.enterprise.event.Event;
import javax.enterprise.event.NotificationOptions;
import javax.enterprise.inject.spi.EventMetadata;
import javax.enterprise.inject.spi.ObserverMethod;
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

    // cache for metadata != this.metadata
    private volatile transient ConcurrentMap<ObserverCacheKey, List<ObserverMethod<? super Object>>> observers;
    private volatile transient ConcurrentMap<ObserverCacheKey, List<ObserverMethod<? super Object>>> asyncObservers;
    // cache for metadata == this.metadata (fast path)
    private volatile transient List<ObserverMethod<? super Object>> defaultMetadataObservers;
    private volatile transient List<ObserverMethod<? super Object>> defaultMetadataAsyncObservers;

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
        // earger validation to bypass it at runtime
        this.webBeansContext.getWebBeansUtil().validEventType(metadata.getType(), metadata.getType());
        if (webBeansContext.getWebBeansUtil().isContainerEventType(this.metadata.validatedType()))
        {
            throw new IllegalArgumentException("Firing container events is forbidden");
        }
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
        if (metadata.validatedType() == eventType)
        {
            // already validated so don't recall validEventType()
            doFireSyncEvent(event, metadata);
        }
        else
        {
            webBeansContext.getWebBeansUtil().validEventType(eventType.getClass(), metadata.getType());
            doFireSyncEvent(event, metadata.select(eventType));
        }
    }

    @Override
    public <U extends T> CompletionStage<U> fireAsync(U event)
    {
        return fireAsync(event, webBeansContext.getNotificationManager().getDefaultNotificationOptions());
    }

    @Override
    public <U extends T> CompletionStage<U> fireAsync(U event, NotificationOptions notificationOptions)
    {
        Type eventType = event.getClass();
        if (eventType != metadata.validatedType())
        {
            webBeansContext.getWebBeansUtil().validEventType(eventType.getClass(), metadata.getType());
            return webBeansContext.getNotificationManager()
                    .fireEvent(event, metadata.select(eventType), false, notificationOptions);
        }
        return doFireAsyncEvent(event, metadata, notificationOptions);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Event<T> select(Annotation... bindings)
    {
        return new EventImpl<>(metadata.select(bindings), webBeansContext);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public <U extends T> Event<U> select(Class<U> subtype, Annotation... bindings)
    {
        return new EventImpl<>(metadata.select(subtype, bindings), webBeansContext);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public <U extends T> Event<U> select(TypeLiteral<U> subtype, Annotation... bindings)
    {
        return new EventImpl<>(metadata.select(subtype, bindings), webBeansContext);
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

    private void doFireSyncEvent(T event, EventMetadataImpl metadata)
    {
        final NotificationManager notificationManager = webBeansContext.getNotificationManager();
        List<ObserverMethod<? super Object>> observerMethods;
        if (metadata == this.metadata) // no validation of isContainerEventType, already done
        {
            if (defaultMetadataObservers == null)
            {
                final List<ObserverMethod<? super Object>> tmp = new ArrayList<>( // faster than LinkedList
                        webBeansContext.getBeanManagerImpl().resolveObserverMethods(event, metadata));
                notificationManager.prepareObserverListForFire(false, false, tmp);
                this.defaultMetadataObservers = tmp;
            }
            observerMethods = defaultMetadataObservers;
        }
        else
        {
            if (webBeansContext.getWebBeansUtil().isContainerEventType(event))
            {
                throw new IllegalArgumentException("Firing container events is forbidden");
            }

            if (observers == null)
            {
                synchronized (this)
                {
                    if (observers == null)
                    {
                        observers = new ConcurrentHashMap<>();
                    }
                }
            }
            final ObserverCacheKey key = new ObserverCacheKey(
                    event.getClass(), metadata.validatedType(), metadata.getQualifiers());
            observerMethods = observers.get(key);
            if (observerMethods == null)
            {
                observerMethods = new ArrayList<>( // faster than LinkedList
                        webBeansContext.getBeanManagerImpl().resolveObserverMethods(event, metadata));
                notificationManager.prepareObserverListForFire(false, false, observerMethods);
                this.observers.putIfAbsent(key, observerMethods);
            }
        }
        notificationManager.doFireSync(new EventContextImpl<>(event, metadata), false, observerMethods);
    }

    private <U extends T> CompletionStage<U> doFireAsyncEvent(T event, EventMetadataImpl metadata, NotificationOptions options)
    {
        final NotificationManager notificationManager = webBeansContext.getNotificationManager();
        List<ObserverMethod<? super Object>> observerMethods;
        if (metadata == this.metadata) // no validation of isContainerEventType, already done
        {
            if (defaultMetadataAsyncObservers == null)
            {
                final List<ObserverMethod<? super Object>> tmp = new ArrayList<>( // faster than LinkedList
                        webBeansContext.getBeanManagerImpl().resolveObserverMethods(event, metadata));
                notificationManager.prepareObserverListForFire(false, true, tmp);
                this.defaultMetadataAsyncObservers = tmp;
            }
            observerMethods = defaultMetadataAsyncObservers;
        }
        else
        {
            if (webBeansContext.getWebBeansUtil().isContainerEventType(event))
            {
                throw new IllegalArgumentException("Firing container events is forbidden");
            }

            if (asyncObservers == null)
            {
                synchronized (this)
                {
                    if (asyncObservers == null)
                    {
                        asyncObservers = new ConcurrentHashMap<>();
                    }
                }
            }
            final ObserverCacheKey key = new ObserverCacheKey(
                    event.getClass(), metadata.validatedType(), metadata.getQualifiers());
            observerMethods = asyncObservers.get(key);
            if (observerMethods == null)
            {
                observerMethods = new ArrayList<>( // faster than LinkedList
                        webBeansContext.getBeanManagerImpl().resolveObserverMethods(event, metadata));
                notificationManager.prepareObserverListForFire(false, true, observerMethods);
                this.asyncObservers.putIfAbsent(key, observerMethods);
            }
        }
        return notificationManager.doFireAsync(
                new EventContextImpl<>(event, metadata), false, options, observerMethods);
    }

    private static class ObserverCacheKey
    {
        private final Class<?> clazz;
        private final Type type;
        private final Collection<Annotation> qualifiers;
        private final int hash;

        private ObserverCacheKey(Class<?> clazz, Type type, Collection<Annotation> qualifiers)
        {
            this.clazz = clazz;
            this.type = type;
            this.qualifiers = qualifiers;
            this.hash = Objects.hash(clazz, type, qualifiers);
        }

        @Override
        public boolean equals(final Object o)
        {
            if (this == o)
            {
                return true;
            }
            if (o == null || getClass() != o.getClass())
            {
                return false;
            }
            ObserverCacheKey that = ObserverCacheKey.class.cast(o);
            return Objects.equals(clazz, that.clazz) &&
                    Objects.equals(type, that.type) &&
                    Objects.equals(qualifiers, that.qualifiers);
        }

        @Override
        public int hashCode()
        {
            return hash;
        }
    }
}
