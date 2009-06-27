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
import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;

import org.apache.webbeans.exception.WebBeansException;

/**
 * Implicit observable bean definition.
 * 
 * @version $Rev$ $Date$
 */
public class ObservableComponentImpl<T, K> extends ComponentImpl<T>
{
    private WebBeansType definedType;

    private Class<K> eventType = null;

    public ObservableComponentImpl(Class<T> returnType, Class<K> eventType, WebBeansType definedType)
    {
        super(returnType);
        this.definedType = definedType;
        this.eventType = eventType;
    }

    @Override
    protected T createInstance(CreationalContext<T> creationalContext)
    {
        Set<Annotation> setBindingTypes = getBindings();
        Annotation[] bindingTypes = new Annotation[setBindingTypes.size()];

        bindingTypes = setBindingTypes.toArray(bindingTypes);

        T instance = null;

        try
        {
            instance = getConstructor().newInstance(new Object[] { bindingTypes, eventType });

        }
        catch (Exception e)
        {
            throw new WebBeansException("Exception in creating Observable implicit component for event type : " + eventType.getName());
        }

        return instance;

    }

    @Override
    protected void destroyInstance(T instance)
    {
        super.destroyInstance(instance);
    }

    /**
     * @return the definedType
     */
    public WebBeansType getDefinedType()
    {
        return definedType;
    }

    /**
     * @return the eventType
     */
    public Class<K> getEventType()
    {
        return eventType;
    }
}