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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.webbeans.Observer;

import org.apache.webbeans.util.AnnotationUtil;

/**
 * Implementation of the {@link Observer} interface contract.
 * 
 * @author <a href="mailto:gurkanerdogdu@yahoo.com">Gurkan Erdogdu</a>
 * @since 1.0
 * @param <T> generic event type
 */
public class ObserverImpl<T>
{
    /** Event binding types apeearing on the parameter */
    private Set<Annotation> eventBindingTypes = new HashSet<Annotation>();

    /** Event object type */
    private Class<T> eventType;

    private Observer<T> observer;

    private boolean ifExist;

    private TransactionalObserverType transObserverType;

    /**
     * Constructs new observer instance.
     * 
     * @param component web beans component defines the observer method
     * @param observerMethod observer method
     * @param eventType event type
     */
    public ObserverImpl(Observer<T> observer, Class<T> eventType, Annotation... annotations)
    {
        this(observer, false, TransactionalObserverType.NONE, eventType, annotations);
    }

    public ObserverImpl(Observer<T> observer, boolean ifExist, TransactionalObserverType type, Class<T> eventType, Annotation... annotations)
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
        for (Annotation annot : annotations)
        {
            if (!this.eventBindingTypes.contains(annot))
            {
                ok = false;
                break;
            }
            else
            {
                if (!AnnotationUtil.isAnnotationMemberExist(annot.annotationType(), annot, getAnnotation(annot.annotationType())))
                {
                    ok = false;
                    break;
                }
            }

        }

        return ok;

    }

    private Annotation getAnnotation(Class<? extends Annotation> type)
    {
        Iterator<Annotation> it = this.eventBindingTypes.iterator();
        while (it.hasNext())
        {
            Annotation annot = it.next();

            if (annot.annotationType().equals(type))
            {
                return annot;
            }
        }

        return null;
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
    public Class<T> getEventType()
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