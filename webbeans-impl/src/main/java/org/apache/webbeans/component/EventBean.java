/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.apache.webbeans.component;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Event;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.util.TypeLiteral;

import org.apache.webbeans.event.EventImpl;
import org.apache.webbeans.exception.WebBeansException;

/**
 * Implicit observable bean definition.
 * 
 * @version $Rev$ $Date$
 */
public class EventBean<T> extends AbstractBean<Event<T>>
{
    public static ThreadLocal<InjectionPoint> local = new ThreadLocal<InjectionPoint>();

    /**
     * Creates a new instance of event bean.
     * 
     * @param returnType Event class
     * @param eventType event type
     * @param definedType webbeans type
     */
    @SuppressWarnings("serial")
    public EventBean()
    {
        super(WebBeansType.OBSERVABLE,new TypeLiteral<Event<T>>(){}.getRawType());
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected Event<T> createInstance(CreationalContext<Event<T>> creationalContext)
    {
        Event<T> instance = null;

        InjectionPoint injectionPoint = local.get();
        Type eventType = null;
        
        if(injectionPoint != null)
        {
            Type[] eventActualTypeArgs = new Type[0];
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
                instance = new EventImpl<T>(qualifiers, eventType);
            }
            catch (Exception e)
            {
                throw new WebBeansException("Exception in creating Event implicit component for event type : " + eventType);
            }           
            finally
            {
                local.set(null);
            }
            
        }
                        
        return instance;
    }
}
