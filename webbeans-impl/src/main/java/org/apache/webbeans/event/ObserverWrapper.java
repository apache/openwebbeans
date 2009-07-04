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
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.event.Observer;


/**
 * Wrapper around the {@link Observer} instance.
 * 
 * @param <T> generic event type
 */
public class ObserverWrapper<T>
{
    /** Event binding types apeearing on the parameter */
    private Set<Annotation> eventBindingTypes = new HashSet<Annotation>();

    /** Event object type */
    private Type eventType;

    /**Wrapped observer instance*/
    private Observer<T> observer;

    /**Using <code>@IfExist</code>*/
    private boolean ifExist;

    /**Transactional observer type*/
    private TransactionalObserverType transObserverType;

    /**
     * Constructs new observer instance.
     * 
     * @param component web beans component defines the observer method
     * @param observerMethod observer method
     * @param eventType event type
     */
    public ObserverWrapper(Observer<T> observer, Class<T> eventType, Annotation... annotations)
    {
        this(observer, false, TransactionalObserverType.NONE, eventType, annotations);
    }

    public ObserverWrapper(Observer<T> observer, boolean ifExist, TransactionalObserverType type, Type eventType, Annotation... annotations)
    {
        for (Annotation annotation : annotations)
        {
            this.eventBindingTypes.add(annotation);
        }

        this.observer = observer;
        this.eventType = eventType;
        this.ifExist = ifExist;
        this.transObserverType = type;
    }

    public boolean isObserverOfBindings(Annotation... annotations)
    {
        boolean ok = true;
                
        if(this.eventBindingTypes.size() >= annotations.length)
        {
            for (Annotation annot : annotations)
            {
                if (!this.eventBindingTypes.contains(annot))
                {
                    ok = false;
                    break;
                }
            }            
        }
        else
        {
            Set<Annotation> eventAnnots = new HashSet<Annotation>();
            
            for(Annotation eventAnnot : annotations)
            {
                eventAnnots.add(eventAnnot);
            }
            
            for (Annotation annot : this.eventBindingTypes)
            {
                if (!eventAnnots.contains(annot))
                {
                    ok = false;
                    break;
                }
            }            
            
        }
        
        return ok;

    }

    
    /**
     * Gets event binding types.
     */
    public Set<Annotation> getEventBindingTypes()
    {
        return this.eventBindingTypes;
    }

    /**
     * Gets event type.
     */
    public Type getEventType()
    {
        return this.eventType;
    }

    /**
     * @return the observer
     */
    public Observer<T> getObserver()
    {
        return observer;
    }

    /**
     * @return the ifExist
     */
    public boolean isIfExist()
    {
        return ifExist;
    }

    /**
     * @return the transObserverType
     */
    public TransactionalObserverType getTransObserverType()
    {
        return transObserverType;
    }

}