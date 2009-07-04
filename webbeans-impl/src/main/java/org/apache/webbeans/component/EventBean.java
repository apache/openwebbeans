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
import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;

import org.apache.webbeans.exception.WebBeansException;

/**
 * Implicit observable bean definition.
 * 
 * @version $Rev$ $Date$
 */
public class EventBean<T> extends AbstractBean<T>
{
    private Type eventType = null;

    /**
     * Creates a new instance of event bean.
     * 
     * @param returnType Event class
     * @param eventType event type
     * @param definedType webbeans type
     */
    public EventBean(Class<T> returnType, Type eventType, WebBeansType definedType)
    {
        super(definedType,returnType);
        this.eventType = eventType;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected T createInstance(CreationalContext<T> creationalContext)
    {
        Set<Annotation> setBindingTypes = getBindings();
        Annotation[] bindingTypes = new Annotation[setBindingTypes.size()];

        bindingTypes = setBindingTypes.toArray(bindingTypes);

        T instance = null;

        try
        {
            Constructor<T> constructor = null;
            constructor = returnType.getConstructor(new Class<?>[] { Annotation[].class, Type.class });

            instance = constructor.newInstance(new Object[] { bindingTypes, eventType });
        }
        catch (Exception e)
        {
            throw new WebBeansException("Exception in creating Event implicit component for event type : " + eventType);
        }

        return instance;

    }

    /**
     * Returns the event type.
     * 
     * @return the eventType
     */
    public Type getEventType()
    {
        return eventType;
    }
}