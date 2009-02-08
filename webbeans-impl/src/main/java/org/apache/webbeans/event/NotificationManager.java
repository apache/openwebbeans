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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.event.IfExists;
import javax.event.Observer;
import javax.event.ObserverException;
import javax.event.Observes;
import javax.inject.TypeLiteral;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.Transaction;

import org.apache.webbeans.component.ObservesMethodsOwner;
import org.apache.webbeans.config.WebBeansFinder;
import org.apache.webbeans.logger.WebBeansLogger;
import org.apache.webbeans.spi.ServiceLoader;
import org.apache.webbeans.spi.TransactionService;
import org.apache.webbeans.util.AnnotationUtil;
import org.apache.webbeans.util.Asserts;

@SuppressWarnings("unchecked")
public final class NotificationManager implements Synchronization
{
    private static final WebBeansLogger logger = WebBeansLogger.getLogger(NotificationManager.class);

    private Map<Class<?>, Set<ObserverImpl<?>>> observers = new ConcurrentHashMap<Class<?>, Set<ObserverImpl<?>>>();

    private Set<TransactionalNotifier> transactionSet = new CopyOnWriteArraySet<TransactionalNotifier>();
    
    private TransactionService transactionService = ServiceLoader.getService(TransactionService.class);

    public NotificationManager()
    {

    }

    public static NotificationManager getInstance()
    {
        NotificationManager instance = (NotificationManager) WebBeansFinder.getSingletonInstance(WebBeansFinder.SINGLETON_NOTIFICATION_MANAGER);
        return instance;
    }

    public <T> void addObserver(Observer<T> observer, Class<T> eventType, Annotation... annotations)
    {
        addObserver(observer, false, TransactionalObserverType.NONE, eventType, annotations);
    }

    public <T> void addObserver(Observer<T> observer, boolean ifExist, TransactionalObserverType type, Class<T> eventType, Annotation... annotations)
    {
        EventUtil.checkEventType(eventType);
        EventUtil.checkEventBindings(annotations);

        ObserverImpl<T> observerImpl = new ObserverImpl<T>(observer, ifExist, type, eventType, annotations);

        Set<ObserverImpl<?>> set = observers.get(eventType);
        if (set == null)
        {
            set = new HashSet<ObserverImpl<?>>();
            observers.put(eventType, set);
        }

        set.add(observerImpl);
    }

    public <T> void addObserver(Observer<T> observer, TypeLiteral<T> eventType, Annotation... annotations)
    {
        EventUtil.checkEventType(eventType.getRawType());
        EventUtil.checkEventBindings(annotations);

        ObserverImpl<T> observerImpl = new ObserverImpl<T>(observer, eventType.getRawType(), annotations);

        Set<ObserverImpl<?>> set = observers.get(eventType);
        if (set == null)
        {
            set = new HashSet<ObserverImpl<?>>();
            observers.put(eventType.getRawType(), set);
        }

        set.add(observerImpl);
    }

    public <T> void removeObserver(Observer<T> observer, Class<T> eventType, Annotation... annotations)
    {
        EventUtil.checkEventType(eventType);
        EventUtil.checkEventBindings(annotations);

        if (observers.containsKey(eventType))
        {
            Set<ObserverImpl<?>> set = observers.get(eventType);
            Iterator<ObserverImpl<?>> it = set.iterator();
            while (it.hasNext())
            {
                ObserverImpl<?> s = it.next();
                Observer<T> ob = (Observer<T>) s.getObserver();

                if (ob.equals(observer))
                {
                    set.remove(s);
                }
            }
        }
    }

    public <T> void removeObserver(Observer<T> observer, TypeLiteral<T> eventType, Annotation... annotations)
    {
        EventUtil.checkEventType(eventType.getRawType());
        EventUtil.checkEventBindings(annotations);

        if (observers.containsKey(eventType))
        {
            Set<ObserverImpl<?>> set = observers.get(eventType.getRawType());
            Iterator<ObserverImpl<?>> it = set.iterator();
            while (it.hasNext())
            {
                ObserverImpl<?> s = it.next();
                Observer<T> ob = (Observer<T>) s.getObserver();

                if (ob.equals(observer))
                {
                    set.remove(s);
                }
            }
        }
    }

    public <T> Set<Observer<T>> resolveObservers(T event, Annotation... bindings)
    {

        Set<ObserverImpl<?>> resolvedSet = new HashSet<ObserverImpl<?>>();
        Set<Observer<T>> unres = new HashSet<Observer<T>>();

        Class<T> eventType = (Class<T>) event.getClass();

        EventUtil.checkEventType(eventType);
        EventUtil.checkEventBindings(bindings);

        Set<Class<?>> keySet = this.observers.keySet();
        Iterator<Class<?>> itKeySet = keySet.iterator();

        while (itKeySet.hasNext())
        {
            Class<?> type = itKeySet.next();
            if (type.isAssignableFrom(eventType))
            {
                resolvedSet.addAll(this.observers.get(type));
            }
        }

        Iterator<ObserverImpl<?>> it = resolvedSet.iterator();
        while (it.hasNext())
        {
            ObserverImpl<T> impl = (ObserverImpl<T>) it.next();

            if (impl.isObserverOfBindings(bindings))
            {
                unres.add(impl.getObserver());
            }
        }

        return unres;
    }

    public void fireEvent(Object event, Annotation... bindings)
    {
        Set<Observer<Object>> observers = resolveObservers(event, bindings);
        Iterator<Observer<Object>> it = observers.iterator();

        TransactionalNotifier transNotifier = null;
        while (it.hasNext())
        {
            Observer<Object> observer = it.next();
            try
            {
                if (observer instanceof BeanObserverImpl)
                {
                    BeanObserverImpl<Object> beanObserver = (BeanObserverImpl<Object>) observer;
                    TransactionalObserverType type = beanObserver.getType();
                    if (!type.equals(TransactionalObserverType.NONE))
                    {
                        Transaction transaction = transactionService.getTransaction();
                        if (transaction != null)
                        {
                            transaction.registerSynchronization(this);
                            if (transNotifier == null)
                            {
                                transNotifier = new TransactionalNotifier(event);
                                this.transactionSet.add(transNotifier);
                            }

                            // Register for transaction
                            if (type.equals(TransactionalObserverType.AFTER_TRANSACTION_COMPLETION))
                            {
                                transNotifier.addAfterCompletionObserver(observer);
                            }
                            else if (type.equals(TransactionalObserverType.AFTER_TRANSACTION_SUCCESS))
                            {
                                transNotifier.addAfterCompletionSuccessObserver(observer);
                            }
                            else if (type.equals(TransactionalObserverType.AFTER_TRANSACTION_FAILURE))
                            {
                                transNotifier.addAfterCompletionFailureObserver(observer);
                            }
                            else if (type.equals(TransactionalObserverType.BEFORE_TRANSACTION_COMPLETION))
                            {
                                transNotifier.addBeforeCompletionObserver(observer);
                            }
                        }
                        else
                        {
                            observer.notify(event);
                        }
                    }
                    else
                    {
                        observer.notify(event);
                    }
                }
                else
                {
                    observer.notify(event);
                }

            }
            catch (Exception e)
            {
                if (!RuntimeException.class.isAssignableFrom(e.getClass()))
                {
                    throw new ObserverException("Exception is thrown while handling event object with type : " + event.getClass().getName(), e);
                }
            }
        }
    }

    public <T> void addObservableComponentMethods(ObservesMethodsOwner<?> component)
    {
        Asserts.assertNotNull(component, "component parameter can not be null");
        Set<Method> observableMethods = component.getObservableMethods();
        Iterator<Method> itMethods = observableMethods.iterator();

        while (itMethods.hasNext())
        {
            Method observableMethod = itMethods.next();
            Annotation[] bindingTypes = AnnotationUtil.getMethodFirstParameterBindingTypesWithGivenAnnotation(observableMethod, Observes.class);

            boolean ifExist = false;

            if (AnnotationUtil.isMethodParameterAnnotationExist(observableMethod, IfExists.class))
            {
                ifExist = true;
            }

            TransactionalObserverType type = EventUtil.getObserverMethodTransactionType(observableMethod);

            BeanObserverImpl<T> observer = new BeanObserverImpl(component, observableMethod, ifExist, type);

            Class<T> clazz = (Class<T>) AnnotationUtil.getMethodFirstParameterTypeClazzWithAnnotation(observableMethod, Observes.class);

            addObserver(observer, ifExist, type, clazz, bindingTypes);
        }

    }

    public void afterCompletion(int status)
    {
        try
        {
            Iterator<TransactionalNotifier> it = this.transactionSet.iterator();

            while (it.hasNext())
            {
                TransactionalNotifier notifier = it.next();

                notifier.notifyAfterCompletion();

                if (status == Status.STATUS_COMMITTED)
                {
                    notifier.notifyAfterCompletionSuccess();

                }
                else if (status == Status.STATUS_ROLLEDBACK)
                {
                    notifier.notifyAfterCompletionFailure();
                }

            }

        }
        catch (Exception e)
        {
            logger.error("Exception is occured in the transational observer ", e);
        }
        finally
        {
            this.transactionSet.clear();
        }
    }

    public void beforeCompletion()
    {
        // Call @BeforeTransactionCompletion
        try
        {
            Iterator<TransactionalNotifier> it = this.transactionSet.iterator();
            while (it.hasNext())
            {
                TransactionalNotifier notifier = it.next();
                notifier.notifyBeforeCompletion();
            }

        }
        catch (Exception e)
        {
            logger.error("Exception is occured in the transational observer ", e);
        }
    }
}