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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.enterprise.event.Reception;
import javax.enterprise.event.Observer;
import javax.enterprise.event.ObserverException;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.TypeLiteral;
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

@SuppressWarnings("unchecked")
public final class NotificationManager implements Synchronization
{
    private static final WebBeansLogger logger = WebBeansLogger.getLogger(NotificationManager.class);

    private final Map<Type, Set<ObserverWrapper<?>>> observers = new ConcurrentHashMap<Type, Set<ObserverWrapper<?>>>();

    private final Set<TransactionalNotifier> transactionSet = new CopyOnWriteArraySet<TransactionalNotifier>();
    
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
        addObserver(observer, false, TransactionalObserverType.NONE, eventType, annotations);
    }

    public <T> void addObserver(Observer<T> observer, boolean ifExist, TransactionalObserverType type, Type eventType, Annotation... annotations)
    {
        EventUtil.checkEventBindings(annotations);

        ObserverWrapper<T> observerImpl = new ObserverWrapper<T>(observer, ifExist, type, eventType, annotations);

        Set<ObserverWrapper<?>> set = observers.get(eventType);
        if (set == null)
        {
            set = new HashSet<ObserverWrapper<?>>();
            observers.put(eventType, set);
        }

        set.add(observerImpl);
    }

    public <T> void addObserver(Observer<T> observer, TypeLiteral<T> eventType, Annotation... annotations)
    {
        EventUtil.checkEventType(eventType.getRawType());
        EventUtil.checkEventBindings(annotations);

        ObserverWrapper<T> observerImpl = new ObserverWrapper<T>(observer, eventType.getRawType(), annotations);

        Set<ObserverWrapper<?>> set = observers.get(eventType.getRawType());
        if (set == null)
        {
            set = new HashSet<ObserverWrapper<?>>();
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
            Set<ObserverWrapper<?>> set = observers.get(eventType);
            for (ObserverWrapper<?> s : set)
            {
                Observer<T> ob = (Observer<T>) s.getObserver();

                Set<Annotation> evenBindings = s.getEventQualifiers();
                Annotation[] anns = new Annotation[evenBindings.size()];
                anns = evenBindings.toArray(anns);

                if (ob.equals(observer) && Arrays.equals(anns, annotations))
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

        if (observers.containsKey(eventType.getRawType()))
        {
            Set<ObserverWrapper<?>> set = observers.get(eventType.getRawType());
            for (ObserverWrapper<?> s : set)
            {
                Observer<T> ob = (Observer<T>) ((ObserverWrapper<?>) s).getObserver();

                Set<Annotation> evenBindings = s.getEventQualifiers();
                Annotation[] anns = new Annotation[evenBindings.size()];
                anns = evenBindings.toArray(anns);

                if (ob.equals(observer) && Arrays.equals(anns, annotations))
                {
                    set.remove(s);
                }
            }
        }
    }

    public <T> Set<Observer<T>> resolveObservers(T event, Annotation... bindings)
    {

        Set<ObserverWrapper<?>> resolvedSet = new HashSet<ObserverWrapper<?>>();
        Set<Observer<T>> unres = new HashSet<Observer<T>>();

        Class<T> eventType = (Class<T>) event.getClass();
        
        
        Set<Type> types = new HashSet<Type>();
        ClassUtil.setTypeHierarchy(types, eventType);

        //EventUtil.checkEventType(eventType);
        EventUtil.checkEventBindings(bindings);

        Set<Type> keySet = this.observers.keySet();

        for (Type type : keySet)
        {
            for (Type check : types)
            {
                if (ClassUtil.isAssignable(check, type))
                {
                    resolvedSet.addAll(this.observers.get(type));
                    break;
                }
            }
        }

        for (ObserverWrapper<?> impl : resolvedSet)
        {
            if (impl.isObserverOfQualifiers(bindings))
            {
                unres.add(((ObserverWrapper<T>) impl).getObserver());
            }
        }

        return unres;
    }

    public void fireEvent(Object event, Annotation... bindings)
    {
        Set<Observer<Object>> observers = resolveObservers(event, bindings);

        TransactionalNotifier transNotifier = null;
        for (Observer<Object> observer: observers)
        {
            try
            {
                if(observer instanceof BeanObserverImpl)
                {
                    BeanObserverImpl<Object> beanObserver = (BeanObserverImpl<Object>) observer;
                    TransactionalObserverType type = beanObserver.getType();
                    if (!(type.equals(TransactionalObserverType.NONE)))
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

    public <T> void addObservableComponentMethods(InjectionTargetBean<?> component)
    {
        Asserts.assertNotNull(component, "component parameter can not be null");
        Set<Method> observableMethods = component.getObservableMethods();

        for (Method observableMethod : observableMethods)
        {
            Observes observes = AnnotationUtil.getMethodFirstParameterAnnotation(observableMethod, Observes.class);

            Annotation[] bindingTypes = AnnotationUtil.getMethodFirstParameterQualifierWithGivenAnnotation(observableMethod, Observes.class);

            boolean ifExist = false;

            if (observes.notifyObserver().equals(Reception.IF_EXISTS))
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

            for (TransactionalNotifier notifier : this.transactionSet)
            {
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
            for (TransactionalNotifier notifier : this.transactionSet)
            {
                notifier.notifyBeforeCompletion();
            }

        }
        catch (Exception e)
        {
            logger.error("Exception is occured in the transational observer ", e);
        }
    }
}