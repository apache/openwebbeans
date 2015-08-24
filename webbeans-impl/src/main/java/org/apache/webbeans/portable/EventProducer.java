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

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.enterprise.event.Event;
import javax.enterprise.inject.spi.Decorator;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.Interceptor;

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.context.creational.CreationalContextImpl;
import org.apache.webbeans.event.EventImpl;
import org.apache.webbeans.event.EventMetadataImpl;
import org.apache.webbeans.exception.WebBeansException;
import org.apache.webbeans.util.GenericsUtil;

public class EventProducer<T> extends AbstractProducer<Event<T>>
{

    private WebBeansContext webBeansContext;
    
    public EventProducer(WebBeansContext webBeansContext)
    {
        this.webBeansContext = webBeansContext;
    }

    @Override
    protected List<Decorator<?>> filterDecorators(final Event<T> instance, final List<Decorator<?>> decorators)
    {
        if (!EventImpl.class.isInstance(instance)) // is this test useless?
        {
            return decorators;
        }

        final Type type = EventImpl.class.cast(instance).getMetadata().getType();
        final ArrayList<Decorator<?>> list = new ArrayList<Decorator<?>>(decorators.size());
        for (final Decorator<?> original : decorators)
        {
            final Type event = original.getDelegateType();
            if (ParameterizedType.class.isInstance(event))
            {
                final ParameterizedType arg = ParameterizedType.class.cast(event);
                final Type[] actualTypeArguments = arg.getActualTypeArguments();
                if (actualTypeArguments.length > 0 && GenericsUtil.isAssignableFrom(true, false, actualTypeArguments[0], type))
                {
                    list.add(original);
                }
            }
        }
        return list;
    }
    /**
     * {@inheritDoc}
     */
    @Override
    protected Event<T> produce(Map<Interceptor<?>, ?> interceptors, CreationalContextImpl<Event<T>> creationalContext)
    {
        Event<T> instance = null;
        Type eventType;

        InjectionPoint injectionPoint = creationalContext.getInjectionPoint();
        if(injectionPoint != null)
        {
            Type[] eventActualTypeArgs;
            Type type = injectionPoint.getType();                        
            ParameterizedType pt = (ParameterizedType) type;
            eventActualTypeArgs = pt.getActualTypeArguments();

            //First argument and sole argument is actual Event type
            //Example : Event<MyEvent>
            eventType = eventActualTypeArgs[0];
            
            //Event qualifiers
            Annotation[] qualifiers = new Annotation[injectionPoint.getQualifiers().size()];
            qualifiers = injectionPoint.getQualifiers().toArray(qualifiers);
            
            try
            {
                instance = new EventImpl<T>(new EventMetadataImpl(null, eventType, injectionPoint, qualifiers, webBeansContext), webBeansContext);
            }
            catch (Exception e)
            {
                throw new WebBeansException("Exception in creating Event implicit component for event type : "
                                            + eventType);
            }           
            finally
            {
                creationalContext.removeInjectionPoint();
            }
        }
                        
        return instance;
    }
}
