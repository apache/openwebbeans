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

import javax.context.CreationalContext;
import javax.inject.New;

import org.apache.webbeans.exception.WebBeansException;

/**
 * Component definition with {@link New} binding annotation.
 * <p>
 * It is defined as concrete java class component.
 * </p>
 * 
 * @author <a href="mailto:gurkanerdogdu@yahoo.com">Gurkan Erdogdu</a>
 * @since 1.0
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
