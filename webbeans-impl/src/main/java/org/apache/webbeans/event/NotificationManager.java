/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.webbeans.event;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.event.ObserverException;
import javax.enterprise.event.Observes;
import javax.enterprise.event.Reception;
import javax.enterprise.event.TransactionPhase;
import javax.enterprise.inject.spi.ObserverMethod;
import javax.enterprise.util.TypeLiteral;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.Transaction;

import org.apache.webbeans.annotation.AnyLiteral;
import org.apache.webbeans.component.InjectionTargetBean;
import org.apache.webbeans.config.OWBLogConst;
import org.apache.webbeans.container.BeanManagerImpl;
import org.apache.webbeans.exception.WebBeansException;
import org.apache.webbeans.logger.WebBeansLogger;
import org.apache.webbeans.spi.ServiceLoader;
import org.apache.webbeans.spi.TransactionService;
import org.apache.webbeans.util.AnnotationUtil;
import org.apache.webbeans.util.ArrayUtil;
import org.apache.webbeans.util.Asserts;
import org.apache.webbeans.util.ClassUtil;
import org.apache.webbeans.util.WebBeansUtil;

@SuppressWarnings("unchecked")
public final class NotificationManager
{
    private static final WebBeansLogger logger = WebBeansLogger.getLogger(NotificationManager.class);

    private final Map<Type, Set<ObserverMethod<?>>> observers = new ConcurrentHashMap<Type, Set<ObserverMethod<?>>>();

    private final TransactionService transactionService = ServiceLoader.getService(TransactionService.class);

    public NotificationManager()
    {

    }

    public static NotificationManager getInstance()
    {
        BeanManagerImpl manager = BeanManagerImpl.getManager();

        return manager.getNotificationManager();
    }

    public <T> void addObserver(ObserverMethod<T> observer, Type eventType)
    {
        EventUtil.checkEventBindings(observer.getObservedQualifiers());

        Set<ObserverMethod<?>> set = observers.get(eventType);
        if (set == null)
        {
            set = new HashSet<ObserverMethod<?>>();
            observers.put(eventType, set);
        }

        set.add(observer);
    }

    public <T> void addObserver(ObserverMethod<T> observer, TypeLiteral<T> typeLiteral)
    {
        EventUtil.checkEventType(typeLiteral.getRawType());

        addObserver(observer, typeLiteral.getType());
    }

    public <T> void removeObserver(ObserverMethod<T> observer, Class<T> eventType, Annotation... annotations)
    {
        EventUtil.checkEventType(eventType);
        EventUtil.checkEventBindings(annotations);

        if (observers.containsKey(eventType))
        {
            Set<ObserverMethod<?>> set = this.observers.get(eventType);
            for (ObserverMethod<?> ob : set)
            {
                Set<Annotation> evenBindings = ob.getObservedQualifiers();
                Annotation[] anns = new Annotation[evenBindings.size()];
                anns = evenBindings.toArray(anns);

                if (ob.equals(observer) && Arrays.equals(anns, annotations))
                {
                    set.remove(ob);
                }
            }
        }
    }

    public <T> void removeObserver(ObserverMethod<T> observer, TypeLiteral<T> typeLiteral, Annotation... annotations)
    {
        removeObserver(observer, typeLiteral.getRawType(), annotations);
    }

    public <T> Set<ObserverMethod<? super T>> resolveObservers(T event, Annotation... eventQualifiers)
    {
        EventUtil.checkEventBindings(eventQualifiers);

        Set<Annotation> qualifiers = ArrayUtil.asSet(eventQualifiers);

        Class<T> eventType = (Class<T>) event.getClass();
        // EventUtil.checkEventType(eventType);

        Set<ObserverMethod<? super T>> observers = filterByType(eventType);

        observers = filterByQualifiers(observers, qualifiers);

        return observers;
    }

    private <T> Set<ObserverMethod<? super T>> filterByType(Class<T> eventType)
    {
        if(WebBeansUtil.isExtensionEventType(eventType))
        {
            return filterByExtensionEventType(eventType);
        }
        
        Set<ObserverMethod<? super T>> matching = new HashSet<ObserverMethod<? super T>>();

        Set<Type> types = new HashSet<Type>();
        types.add(eventType);
        
        Type superClazz = eventType.getGenericSuperclass();
        if(superClazz != null)
        {
            types.add(superClazz);    
        }
        
        Type[] genericInts = eventType.getGenericInterfaces();
        
        if(genericInts != null && genericInts.length > 0)
        {
            for(Type genericInt : genericInts)
            {
                types.add(genericInt);
            }            
        }

        Set<Type> keySet = this.observers.keySet();

        for (Type type : keySet)
        {
            for (Type check : types)
            {
                if (ClassUtil.checkEventTypeAssignability(check, type))
                {
                    Set<ObserverMethod<?>> wrappers = this.observers.get(type);

                    for (ObserverMethod<?> wrapper : wrappers)
                    {
                        matching.add((ObserverMethod<T>) wrapper);
                    }
                    break;
                }
            }
        }
        return matching;
    }
    
    private <T> Set<ObserverMethod<? super T>> filterByExtensionEventType(Class<T> eventType)
    {
        Set<ObserverMethod<? super T>> matching = new HashSet<ObserverMethod<? super T>>();
        
        Set<Type> keySet = this.observers.keySet();

        for (Type type : keySet)
        {
            Class<?> clazz = null;
            
            if(ClassUtil.isTypeVariable(type))
            {
                TypeVariable<?> tvBeanTypeArg = (TypeVariable<?>)type;
                Type tvBound = tvBeanTypeArg.getBounds()[0];
                
                clazz = ClassUtil.getClass(tvBound);
            }
            
            if(clazz == null)
            {
                clazz = ClassUtil.getClazz(type);    
            }
            
            if(clazz.isAssignableFrom(eventType))
            {
                Set<ObserverMethod<?>> wrappers = this.observers.get(type);

                for (ObserverMethod<?> wrapper : wrappers)
                {
                    matching.add((ObserverMethod<T>) wrapper);
                }
                break;                
            }
        }
        
        return matching;
        
    }

    /**
     * filter out all {@code ObserverMethod}s which do not fit the given
     * qualifiers.
     */
    private <T> Set<ObserverMethod<? super T>> filterByQualifiers(Set<ObserverMethod<? super T>> observers, Set<Annotation> eventQualifiers)
    {
        eventQualifiers.add(new AnyLiteral());
                
        Set<ObserverMethod<? super T>> matching = new HashSet<ObserverMethod<? super T>>();

        search: for (ObserverMethod<? super T> ob : observers)
        {
            Set<Annotation> qualifiers = ob.getObservedQualifiers();

            if (qualifiers.size() > eventQualifiers.size())
            {
                continue;
            }
            

            for (Annotation qualifier : qualifiers)
            {
                boolean found = false;
                for(Annotation inList : eventQualifiers)
                {
                    if(AnnotationUtil.hasAnnotationMember(qualifier.annotationType(), inList, qualifier))
                    {
                        found = true;
                        break;
                    }
                }
                
                if(!found)
                {
                    continue search;
                }
            }

            matching.add(ob);
        }

        return matching;
    }

    public void fireEvent(Object event, Annotation... qualifiers)
    {
        Transaction transaction = transactionService.getTransaction();

        Set<ObserverMethod<? super Object>> observers = resolveObservers(event, qualifiers);

        for (ObserverMethod<? super Object> observer : observers)
        {
            try
            {
                TransactionPhase phase = observer.getTransactionPhase();
                if (transaction != null && phase != null)
                {
                    if (phase.equals(TransactionPhase.AFTER_COMPLETION))
                    {
                        transaction.registerSynchronization(new AfterCompletion(observer, event));
                    }
                    else if (phase.equals(TransactionPhase.AFTER_SUCCESS))
                    {
                        transaction.registerSynchronization(new AfterCompletionSuccess(observer, event));
                    }
                    else if (phase.equals(TransactionPhase.AFTER_FAILURE))
                    {
                        transaction.registerSynchronization(new AfterCompletionFailure(observer, event));
                    }
                    else if (phase.equals(TransactionPhase.BEFORE_COMPLETION))
                    {
                        transaction.registerSynchronization(new BeforeCompletion(observer, event));
                    }
                    else
                    {
                        throw new IllegalStateException(logger.getTokenString(OWBLogConst.EXCEPT_0007) + phase);
                    }
                }
                else
                {
                    observer.notify(event);
                }
            }
            catch (WebBeansException e)
            {
                Throwable exc = e.getCause();
                if(exc instanceof InvocationTargetException)
                {
                    InvocationTargetException invt = (InvocationTargetException)exc;
                    exc = invt.getCause();
                }
                
                if (!RuntimeException.class.isAssignableFrom(exc.getClass()))
                {
                    throw new ObserverException(logger.getTokenString(OWBLogConst.EXCEPT_0008) + event.getClass().getName(), e);
                }
                else
                {
                    RuntimeException rte = (RuntimeException) exc;
                    throw rte;
                }
            }
            catch (RuntimeException e)
            {
                throw e;
            }

            catch (Exception e)
            {
                throw new WebBeansException(e);
            }
        }
    }

    public <T> Set<ObserverMethod<?>> addObservableComponentMethods(InjectionTargetBean<?> component)
    {
        Asserts.assertNotNull(component, "component parameter can not be null");
        Set<Method> observableMethods = component.getObservableMethods();
        Set<ObserverMethod<?>> observerMethods = new HashSet<ObserverMethod<?>>();

        for (Method observableMethod : observableMethods)
        {
            Observes observes = AnnotationUtil.getMethodFirstParameterAnnotation(observableMethod, Observes.class);

            boolean ifExist = false;

            if (observes.notifyObserver().equals(Reception.IF_EXISTS))
            {
                ifExist = true;
            }

            ObserverMethodImpl<T> observer = new ObserverMethodImpl(component, observableMethod, ifExist);

            Type type = AnnotationUtil.getMethodFirstParameterWithAnnotation(observableMethod, Observes.class);

            addObserver(observer, type);
            
            observerMethods.add(observer);
        }

        return observerMethods;
    }


    private static class AbstractSynchronization<T> implements Synchronization
    {

        private final ObserverMethod<T> observer;
        private final T event;

        public AbstractSynchronization(ObserverMethod<T> observer, T event)
        {
            this.observer = observer;
            this.event = event;
        }

        public void beforeCompletion()
        {
            // Do nothing
        }

        public void afterCompletion(int i)
        {
            //Do nothing
        }

        public void notifyObserver()
        {
            try
            {
                observer.notify(event);
            }
            catch (Exception e)
            {
                logger.error(OWBLogConst.ERROR_0003, e);
            }
        }
    }

    private static class BeforeCompletion extends AbstractSynchronization
    {
        private BeforeCompletion(ObserverMethod observer, Object event)
        {
            super(observer, event);
        }

        @Override
        public void beforeCompletion()
        {
            notifyObserver();
        }
    }

    private static class AfterCompletion extends AbstractSynchronization
    {
        private AfterCompletion(ObserverMethod observer, Object event)
        {
            super(observer, event);
        }

        @Override
        public void afterCompletion(int i)
        {
            notifyObserver();
        }
    }

    private static class AfterCompletionSuccess extends AbstractSynchronization
    {
        private AfterCompletionSuccess(ObserverMethod observer, Object event)
        {
            super(observer, event);
        }

        @Override
        public void afterCompletion(int i)
        {
            if (i == Status.STATUS_COMMITTED)
            {
                notifyObserver();
            }
        }
    }

    private static class AfterCompletionFailure extends AbstractSynchronization
    {
        private AfterCompletionFailure(ObserverMethod observer, Object event)
        {
            super(observer, event);
        }

        @Override
        public void afterCompletion(int i)
        {
            if (i != Status.STATUS_COMMITTED)
            {
                notifyObserver();
            }
        }
    }
}
