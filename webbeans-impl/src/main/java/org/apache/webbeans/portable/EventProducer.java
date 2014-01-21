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
import java.util.Map;

import javax.enterprise.event.Event;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.Interceptor;

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.context.creational.CreationalContextImpl;
import org.apache.webbeans.event.EventImpl;
import org.apache.webbeans.exception.WebBeansException;

public class EventProducer<T> extends AbstractProducer<Event<T>>
{

    private WebBeansContext webBeansContext;
    
    public EventProducer(WebBeansContext webBeansContext)
    {
        this.webBeansContext = webBeansContext;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected Event<T> produce(Map<Interceptor<?>, ?> interceptors, CreationalContextImpl<Event<T>> creationalContext)
    {
        Event<T> instance = null;
        InjectionPoint injectionPoint = null;
        //TODO What should we do here if creationalContext is not instanceof CreationalContextImpl?
        if (creationalContext instanceof CreationalContextImpl)
        {
            injectionPoint = ((CreationalContextImpl<Event<T>>)creationalContext).getInjectionPoint();
        }

        Type eventType;
        
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
                instance = new EventImpl<T>(qualifiers, eventType, injectionPoint, webBeansContext);
            }
            catch (Exception e)
            {
                throw new WebBeansException("Exception in creating Event implicit component for event type : "
                                            + eventType);
            }           
            finally
            {
                if (creationalContext instanceof CreationalContextImpl)
                {
                    ((CreationalContextImpl<Event<T>>)creationalContext).removeInjectionPoint();
                }
            }
            
        }
                        
        return instance;
    }
}
