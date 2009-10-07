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
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.event.Reception;
import javax.enterprise.event.Observer;
import javax.enterprise.event.ObserverException;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.TypeLiteral;
import javax.enterprise.inject.Default;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.Transaction;

import org.apache.webbeans.component.InjectionTargetBean;
import org.apache.webbeans.container.BeanManagerImpl;
import org.apache.webbeans.container.activity.ActivityManager;
import org.apache.webbeans.exception.WebBeansException;
import org.apache.webbeans.logger.WebBeansLogger;
import org.apache.webbeans.spi.ServiceLoader;
import org.apache.webbeans.spi.TransactionService;
import org.apache.webbeans.util.AnnotationUtil;
import org.apache.webbeans.util.Asserts;
import org.apache.webbeans.util.ClassUtil;
import org.apache.webbeans.util.ArrayUtil;
import static org.apache.webbeans.util.ArrayUtil.asSet;

@SuppressWarnings("unchecked")
public final class NotificationManager
{
    private static final WebBeansLogger logger = WebBeansLogger.getLogger(NotificationManager.class);

    private final Map<Type, Set<ObserverWrapper<?>>> observers = new ConcurrentHashMap<Type, Set<ObserverWrapper<?>>>();

    private final TransactionService transactionService = ServiceLoader.getService(TransactionService.class);

    public NotificationManager()
    {

    }

    public static NotificationManager getInstance()
    {
        BeanManagerImpl manager =  ActivityManager.getInstance().getCurrentActivity();
        
        return manager.getNotificationManager();
    }

    public <T> void addObserver(Observer<T> observer, Type eventType, Annotation... annotations)
    {
        EventUtil.checkEventBindings(annotations);

        Set<ObserverWrapper<?>> set = observers.get(eventType);
        if (set == null)
        {
            set = new HashSet<ObserverWrapper<?>>();
            observers.put(eventType, set);
        }

        set.add(new ObserverWrapper<T>(observer, annotations));
    }

    public <T> void addObserver(Observer<T> observer, TypeLiteral<T> typeLiteral, Annotation... annotations)
    {
        EventUtil.checkEventType(typeLiteral.getRawType());

        addObserver(observer, typeLiteral.getRawType(), annotations);
    }

    public <T> void removeObserver(Observer<T> observer, Class<T> eventType, Annotation... annotations)
    {
        EventUtil.checkEventType(eventType);
        EventUtil.checkEventBindings(annotations);

        if (observers.containsKey(eventType))
        {
            Set<ObserverWrapper<?>> set = observers.get(eventType);
            for (ObserverWrapper<?> s : set)
            {
                Observer<T> ob = (Observer<T>) s.getObserver();

                Set<Annotation> evenBindings = s.getQualifiers();
                Annotation[] anns = new Annotation[evenBindings.size()];
                anns = evenBindings.toArray(anns);

                if (ob.equals(observer) && Arrays.equals(anns, annotations))
                {
                    set.remove(s);
                }
            }
        }
    }

    public <T> void removeObserver(Observer<T> observer, TypeLiteral<T> typeLiteral, Annotation... annotations)
    {
        removeObserver(observer, typeLiteral.getRawType(), annotations);
    }

    public <T> Set<Observer<T>> resolveObservers(T event, Annotation... eventQualifiers)
    {
        EventUtil.checkEventBindings(eventQualifiers);

        Class<T> eventType = (Class<T>) event.getClass();
        //EventUtil.checkEventType(eventType);

        Set<ObserverWrapper<T>> wrappers = filterByType(eventType);

        wrappers = filterByQualifiers(wrappers, eventQualifiers);

        return unwrap(wrappers);
    }

    private <T> Set<ObserverWrapper<T>> filterByType(Class<T> eventType)
    {
        Set<ObserverWrapper<T>> matching = new HashSet<ObserverWrapper<T>>();

        Set<Type> types = new HashSet<Type>();
        ClassUtil.setTypeHierarchy(types, eventType);

        Set<Type> keySet = this.observers.keySet();

        for (Type type : keySet)
        {
            for (Type check : types)
            {
                if (ClassUtil.isAssignable(check, type))
                {
                    Set<ObserverWrapper<?>> wrappers = this.observers.get(type);

                    for (ObserverWrapper<?> wrapper : wrappers)
                    {
                        matching.add((ObserverWrapper<T>) wrapper);
                    }
                    break;
                }
            }
        }
        return matching;
    }

    private <T> Set<ObserverWrapper<T>> filterByQualifiers(Set<ObserverWrapper<T>> wrappers, Annotation... annotations)
    {
        Set<Annotation> eventQualifiers = toQualiferSet(annotations);

        Set<ObserverWrapper<T>> matching = new HashSet<ObserverWrapper<T>>();

        search: for (ObserverWrapper<T> wrapper : wrappers)
        {
            Set<Annotation> qualifiers = wrapper.getQualifiers();

            if (qualifiers.size() > eventQualifiers.size())
            {
                continue;
            }

            for (Annotation qualifier : qualifiers)
            {
                if (!eventQualifiers.contains(qualifier))
                {
                    continue search;
                }
            }

            matching.add(wrapper);
        }
        
        return matching;
    }

    private <T> Set<Observer<T>> unwrap(Set<ObserverWrapper<T>> wrappers)
    {
        Set<Observer<T>> observers = new HashSet<Observer<T>>();

        for (ObserverWrapper<T> wrapper : wrappers)
        {
            observers.add(wrapper.getObserver());
        }

        return observers;
    }
    
    public void fireEvent(Object event, Annotation... qualifiers)
    {
        Transaction transaction = transactionService.getTransaction();

        Set<Observer<Object>> observers = resolveObservers(event, qualifiers);

        for (Observer<Object> observer: observers)
        {
            try
            {
                if (transaction != null && isTransactional(observer))
                {
                    BeanObserverImpl<Object> beanObserver = (BeanObserverImpl<Object>) observer;

                    TransactionalObserverType type = beanObserver.getType();

                    if (type.equals(TransactionalObserverType.AFTER_TRANSACTION_COMPLETION))
                    {
                        transaction.registerSynchronization(new AfterCompletion(observer, event));
                    }
                    else if (type.equals(TransactionalObserverType.AFTER_TRANSACTION_SUCCESS))
                    {
                        transaction.registerSynchronization(new AfterCompletionSuccess(observer, event));
                    }
                    else if (type.equals(TransactionalObserverType.AFTER_TRANSACTION_FAILURE))
                    {
                        transaction.registerSynchronization(new AfterCompletionFailure(observer, event));
                    }
                    else if (type.equals(TransactionalObserverType.BEFORE_TRANSACTION_COMPLETION))
                    {
                        transaction.registerSynchronization(new BeforeCompletion(observer, event));
                    }
                    else {
                        throw new IllegalStateException("TransactionalObserverType not supported: " + type);
                    }
                }
                else
                {
                    observer.notify(event);
                }
            }
            catch (WebBeansException e)
            {
                if (!RuntimeException.class.isAssignableFrom(e.getCause().getClass()))
                {
                    throw new ObserverException("Exception is thrown while handling event object with type : " + event.getClass().getName(), e);
                }
                else
                {
                    RuntimeException rte = (RuntimeException)e.getCause();
                    throw rte;
                }
            }
            catch(RuntimeException e)
            {
                throw e;
            }
            
            catch(Exception e)
            {
                throw new WebBeansException(e);
            }
        }
    }

    private boolean isTransactional(Observer<?> observer)
    {
        if (!(observer instanceof BeanObserverImpl))
        {
            return false;
        }

        BeanObserverImpl<?> beanObserver = (BeanObserverImpl<?>) observer;

        if (beanObserver.getType().equals(TransactionalObserverType.NONE))
        {
            return false;
        }

        return true;
    }

    public <T> void addObservableComponentMethods(InjectionTargetBean<?> component)
    {
        Asserts.assertNotNull(component, "component parameter can not be null");
        Set<Method> observableMethods = component.getObservableMethods();

        for (Method observableMethod : observableMethods)
        {
            Observes observes = AnnotationUtil.getMethodFirstParameterAnnotation(observableMethod, Observes.class);

            Annotation[] qualifiers = AnnotationUtil.getMethodFirstParameterQualifierWithGivenAnnotation(observableMethod, Observes.class);

            boolean ifExist = false;

            if (observes.notifyObserver().equals(Reception.IF_EXISTS))
            {
                ifExist = true;
            }

            TransactionalObserverType type = EventUtil.getObserverMethodTransactionType(observableMethod);

            BeanObserverImpl<T> observer = new BeanObserverImpl(component, observableMethod, ifExist, type);

            Class<T> clazz = (Class<T>) AnnotationUtil.getMethodFirstParameterTypeClazzWithAnnotation(observableMethod, Observes.class);

            addObserver(observer, clazz, qualifiers);
        }

    }

    private static Set<Annotation> toQualiferSet(Annotation... qualifiers)
    {
        Set<Annotation> set = ArrayUtil.asSet(qualifiers);

        return pruneDefault(set);
    }
    
    private static Set<Annotation> pruneDefault(Set<Annotation> set) {
        Iterator<Annotation> iterator = set.iterator();
        while (iterator.hasNext()) {
            if (iterator.next() instanceof Default)
            {
                iterator.remove();
            }
        }
        return set;
    }

    /**
     * Wrapper around the {@link javax.enterprise.event.Observer} instance.
     *
     * @param <T> generic event type
     */
    private static class ObserverWrapper<T>
    {
        /** Event qualifiers appearing on the parameter */
        private final Set<Annotation> qualifiers;

        /**Wrapped observer instance*/
        private final Observer<T> observer;

        public ObserverWrapper(Observer<T> observer, Annotation... qualifiers)
        {
            this.qualifiers = toQualiferSet(qualifiers);
            this.qualifiers.remove(Default.class);
            this.observer = observer;
        }

        public Set<Annotation> getQualifiers()
        {
            return this.qualifiers;
        }

        public Observer<T> getObserver()
        {
            return observer;
        }
    }

    private static class AbstractSynchronization<T> implements Synchronization {

        private final Observer<T> observer;
        private final T event;

        public AbstractSynchronization(Observer<T> observer, T event)
        {
            this.observer = observer;
            this.event = event;
        }

        public void beforeCompletion()
        {
        }

        public void afterCompletion(int i)
        {
        }

        public void notifyObserver() {
            try
            {
                observer.notify(event);
            }
            catch (Exception e)
            {
                logger.error("Exception is occured in the transactional observer ", e);
            }
        }
    }

    private static class BeforeCompletion extends AbstractSynchronization {
        private BeforeCompletion(Observer observer, Object event)
        {
            super(observer, event);
        }

        @Override
        public void beforeCompletion()
        {
            notifyObserver();
        }
    }

    private static class AfterCompletion extends AbstractSynchronization {
        private AfterCompletion(Observer observer, Object event)
        {
            super(observer, event);
        }

        @Override
        public void afterCompletion(int i)
        {
            notifyObserver();
        }
    }

    private static class AfterCompletionSuccess extends AbstractSynchronization {
        private AfterCompletionSuccess(Observer observer, Object event)
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

    private static class AfterCompletionFailure extends AbstractSynchronization {
        private AfterCompletionFailure(Observer observer, Object event)
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