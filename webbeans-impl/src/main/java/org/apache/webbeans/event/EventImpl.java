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
package org.apache.webbeans.event;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import javax.event.Event;
import javax.event.Fires;
import javax.event.Observer;

import org.apache.webbeans.container.ManagerImpl;

public class EventImpl<T> implements Event<T>
{
    private Annotation[] injectedBindings;

    private Class<T> eventType;

    private ManagerImpl manager = null;

    public EventImpl(Annotation[] injectedBindings, Class<T> eventType)
    {
        this.injectedBindings = injectedBindings;
        this.eventType = eventType;
        this.manager = ManagerImpl.getManager();
    }

    public void fire(T event, Annotation... bindings)
    {
        EventUtil.checkEventBindings(getEventBindings(bindings));
        this.manager.fireEvent(event, getEventBindings(bindings));

    }

    public void observe(Observer<T> observer, Annotation... bindings)
    {
        EventUtil.checkEventBindings(getEventBindings(bindings));
        this.manager.addObserver(observer, eventType, bindings);
    }

    private Annotation[] getEventBindings(Annotation... annotations)
    {
        List<Annotation> eventBindings = new ArrayList<Annotation>();
        Annotation[] anns = null;

        for (Annotation binding : injectedBindings)
        {
            if (!binding.annotationType().equals(Fires.class))
            {
                eventBindings.add(binding);
            }
        }

        for (Annotation binding : annotations)
        {
            eventBindings.add(binding);
        }

        anns = new Annotation[eventBindings.size()];
        anns = eventBindings.toArray(anns);

        return anns;

    }
}
