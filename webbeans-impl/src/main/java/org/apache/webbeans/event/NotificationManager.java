/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.webbeans.event;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.event.ObserverException;
import javax.enterprise.event.TransactionPhase;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.EventMetadata;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ObserverMethod;
import javax.enterprise.inject.spi.ProcessProducer;
import javax.enterprise.util.TypeLiteral;

import org.apache.webbeans.component.AbstractOwbBean;
import org.apache.webbeans.config.OWBLogConst;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.exception.WebBeansException;
import org.apache.webbeans.logger.WebBeansLoggerFacade;
import org.apache.webbeans.portable.events.generics.GenericBeanEvent;
import org.apache.webbeans.portable.events.generics.GenericProducerObserverEvent;
import org.apache.webbeans.spi.TransactionService;
import org.apache.webbeans.util.AnnotationUtil;
import org.apache.webbeans.util.Asserts;
import org.apache.webbeans.util.ClassUtil;
import org.apache.webbeans.util.GenericsUtil;
import org.apache.webbeans.util.WebBeansUtil;

@SuppressWarnings("unchecked")
public final class NotificationManager
{
    private final Map<Type, Set<ObserverMethod<?>>> observers = new ConcurrentHashMap<Type, Set<ObserverMethod<?>>>();
    private final WebBeansContext webBeansContext;

    public NotificationManager(WebBeansContext webBeansContext)
    {
        this.webBeansContext = webBeansContext;
    }
    
    public List<ObserverMethod<?>> getObserverMethods()
    {
        List<ObserverMethod<?>> observerMethods = new ArrayList<ObserverMethod<?>>();
        for (Set<ObserverMethod<?>> methods: observers.values())
        {
            for (ObserverMethod<?> method: methods)
            {
                observerMethods.add(method);
            }
        }
        return observerMethods;
    }

    public <T> void addObserver(ObserverMethod<T> observer, Type eventType)
    {
        webBeansContext.getAnnotationManager().checkQualifierConditions(observer.getObservedQualifiers());

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
        addObserver(observer, typeLiteral.getType());
    }


    public <T> Set<ObserverMethod<? super T>> resolveObservers(T event, EventMetadata metadata)
    {
        EventUtil.checkEventBindings(webBeansContext, metadata.getQualifiers());

        Type eventType = metadata.getType();
        Set<ObserverMethod<? super T>> observersMethods = filterByType(event, eventType);

        observersMethods = filterByQualifiers(observersMethods, metadata.getQualifiers());

        //this check for the TCK is only needed if no observer was found
        if (observersMethods.isEmpty())
        {
            EventUtil.checkQualifierImplementations(metadata.getQualifiers());
        }

        return observersMethods;
    }

    private <T> Set<ObserverMethod<? super T>> filterByType(T event, Type declaredEventType)
    {
        if(WebBeansUtil.isExtensionEventType(declaredEventType))
        {
            return filterByExtensionEventType(event, declaredEventType);
        }
        Class<?> eventClass = event.getClass();
        
        Set<ObserverMethod<? super T>> matching = new HashSet<ObserverMethod<? super T>>();

        Set<Type> eventTypes = GenericsUtil.getTypeClosure(declaredEventType, eventClass);
        if (GenericsUtil.containTypeVariable(eventTypes))
        {
            throw new IllegalArgumentException("event type may not contain unbound type variable: " + eventTypes);
        }

        Set<Type> observedTypes = observers.keySet();

        for (Type observedType : observedTypes)
        {
            for (Type eventType : eventTypes)
            {
                if (GenericsUtil.isAssignableFrom(false, observedType, eventType))
                {
                    Set<ObserverMethod<?>> observerMethods = observers.get(observedType);

                    for (ObserverMethod<?> observerMethod : observerMethods)
                    {
                        matching.add((ObserverMethod<T>) observerMethod);
                    }
                    break;
                }
            }
        }
        return matching;
    }
    
    private <T> Set<ObserverMethod<? super T>> filterByExtensionEventType(T event, Type eventType)
    {
        Class<?> eventClass = ClassUtil.getClazz(eventType);
        Set<ObserverMethod<? super T>> matching = new HashSet<ObserverMethod<? super T>>();        
        Set<Type> keySet = observers.keySet();
        for (Type type : keySet)
        {
            Class<?> beanClass = null;
            Class<?> observerClass = ClassUtil.getClazz(type);
            
            if(observerClass != null)
            {
                if(observerClass.isAssignableFrom(eventClass))
                {
                    //ProcessBean,ProcessAnnotateType, ProcessInjectionTarget
                    if(WebBeansUtil.isExtensionBeanEventType(eventType))
                    {
                        if(WebBeansUtil.isDefaultExtensionBeanEventType(observerClass))
                        {                
                            GenericBeanEvent genericBeanEvent = (GenericBeanEvent)event;
                            beanClass = genericBeanEvent.getBeanClassFor(observerClass);
                            
                            if(ClassUtil.isParametrizedType(type))
                            {
                                addToMathingWithParametrizedForBeans(type,beanClass,matching);
                            }
                            else
                            {
                                addToMatching(type, matching);
                            }
                        }
                    }
                    //ProcessProducer, ProcessProducerMethod, ProcessProducerField,ProcessObserverMEthod
                    else if(WebBeansUtil.isExtensionProducerOrObserverEventType(eventType))
                    {
                        GenericProducerObserverEvent genericBeanEvent = (GenericProducerObserverEvent)event;
                        beanClass = genericBeanEvent.getBeanClass();
                        Class<?> producerOrObserverReturnClass = genericBeanEvent.getProducerOrObserverType();

                        if(WebBeansUtil.isDefaultExtensionProducerOrObserverEventType(observerClass))
                        {   
                            boolean processProducerEvent = false;
                            if(observerClass.equals(ProcessProducer.class))
                            {
                                processProducerEvent = true;
                            }
                            
                            if(ClassUtil.isParametrizedType(type))
                            {
                                addToMatchingWithParametrizedForProducers(processProducerEvent,type, beanClass, producerOrObserverReturnClass, matching);
                            }
                            else
                            {
                                addToMatching(type, matching);
                            }
                        }
                        else if(observerClass.isAssignableFrom(eventClass))
                        {
                            if(ClassUtil.isParametrizedType(type))
                            {
                                addToMathingWithParametrizedForBeans(type,beanClass,matching);
                            }
                            else
                            {
                                addToMatching(type, matching);
                            }                            
                        }
                    }
                    //BeforeBeanDiscovery,AfterBeanDiscovery,AfterDeploymentValidation
                    //BeforeShutDown Events
                    else
                    {
                        if(observerClass.isAssignableFrom(eventClass))
                        {                
                            addToMatching(type, matching);
                        }
                    }                
                }                            
            }            
        }            
        
        return matching;        
    }
    
    /**
     * Returns true if fired event class is assignable with 
     * given observer type argument.
     * @param beanClass fired event class.
     * @param observerTypeActualArg actual type argument, 
     * such as in case ProcessProducerField&lt;Book&gt; is Book.class
     * @return true if fired event class is assignable with 
     * given observer type argument.
     */
    private boolean checkEventTypeParameterForExtensions(Class<?> beanClass, Type observerTypeActualArg)
    {
        if(ClassUtil.isTypeVariable(observerTypeActualArg))
        {
            TypeVariable<?> tv = (TypeVariable<?>)observerTypeActualArg;
            Type tvBound = tv.getBounds()[0];
            
            if(tvBound instanceof Class)
            {
                Class<?> clazzTvBound = (Class<?>)tvBound;
                
                if(clazzTvBound.isAssignableFrom(beanClass))
                {
                    return true;
                }                    
            }            

        }
        else if(ClassUtil.isWildCardType(observerTypeActualArg))
        {
            return ClassUtil.checkRequiredTypeIsWildCard(beanClass, observerTypeActualArg);
        }
        else if(observerTypeActualArg instanceof Class)
        {
            Class<?> observerClass = (Class<?>)observerTypeActualArg;
            if(observerClass.isAssignableFrom(beanClass))
            {
                return true;
            }
        }
        
        return false;
    }
    
    private <T> void addToMatching(Type type, Set<ObserverMethod<? super T>> matching)
    {
        Set<ObserverMethod<?>> wrappers = observers.get(type);

        for (ObserverMethod<?> wrapper : wrappers)
        {
            matching.add((ObserverMethod<T>) wrapper);
        }        
    }
    
    private <T> void addToMathingWithParametrizedForBeans(Type type, Class<?> beanClass, Set<ObserverMethod<? super T>> matching )
    {
        ParameterizedType pt = (ParameterizedType)type;
        Type[] actualArgs = pt.getActualTypeArguments();
        
        if(actualArgs.length == 0)
        {
            Class<?> rawType = (Class<?>)pt.getRawType();
            if(rawType.isAssignableFrom(beanClass))
            {
                addToMatching(type, matching);
            }
        }
        else
        {
            if(checkEventTypeParameterForExtensions(beanClass, actualArgs[0]))
            {
                addToMatching(type, matching);   
            }
        }
        
    }
    
    /**
     * Add to matching.
     * @param <T> generic observer method parameter type 
     * fired event because of observer method or not
     * @param type one of observer method parameter base type
     * @param beanClass observer method owner bean class
     * @param producerOrObserverReturnClass observer even normal class
     * @param matching set of observer method that match the given type
     */
    private <T> void addToMatchingWithParametrizedForProducers(boolean processProducer, Type type, Class<?> beanClass,
                                                              Class<?> producerOrObserverReturnClass, Set<ObserverMethod<? super T>> matching )
    {
        ParameterizedType pt = (ParameterizedType)type;
        Type[] actualArgs = pt.getActualTypeArguments();
        
        if(actualArgs.length == 0)
        {
            Class<?> rawType = (Class<?>)pt.getRawType();
            if(rawType.isAssignableFrom(beanClass))
            {
                addToMatching(type, matching);
            }
        }
        else
        {   
            //Bean class argument
            //For observer related event, observer owner bean class.
            Type beanClassArg = actualArgs[1];
            
            //Event payload
            Type returnClassArg = actualArgs[0];
            
            //For ProcessProducer<BeanClass, Event Class>
            if(processProducer)
            {
                beanClassArg = actualArgs[0];
                returnClassArg = actualArgs[1];
            }
                        
            if(checkEventTypeParameterForExtensions(beanClass, beanClassArg) && 
                    checkEventTypeParameterForExtensions(producerOrObserverReturnClass, returnClassArg))
            {
                addToMatching(type, matching);   
            }
        }
        
    }    

    /**
     * filter out all {@code ObserverMethod}s which do not fit the given
     * qualifiers.
     */
    private <T> Set<ObserverMethod<? super T>> filterByQualifiers(Set<ObserverMethod<? super T>> observers, Set<Annotation> eventQualifiers)
    {
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
                    if(AnnotationUtil.isCdiAnnotationEqual(inList, qualifier))
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

    public void fireEvent(Object event, EventMetadata metadata, boolean isLifecycleEvent)
    {
        Set<ObserverMethod<? super Object>> observerMethods = resolveObservers(event, metadata);

        for (ObserverMethod<? super Object> observer : observerMethods)
        {
            try
            {
                if (isLifecycleEvent && !Extension.class.isAssignableFrom(observer.getBeanClass()))
                {
                    // we must not fire Extension Lifecycle events to beans which are no Extensions
                    continue;
                }

                TransactionPhase phase = observer.getTransactionPhase();
                
                if(phase != null && !phase.equals(TransactionPhase.IN_PROGRESS))
                {
                    TransactionService transactionService = webBeansContext.getService(TransactionService.class);
                    if(transactionService != null)
                    {
                        transactionService.registerTransactionSynchronization(phase, observer, event);
                    }
                    else
                    {
                        if (observer instanceof OwbObserverMethod)
                        {
                            ((OwbObserverMethod<? super Object>)observer).notify(event, metadata);
                        }
                        else
                        {
                            observer.notify(event);
                        }
                    }                    
                }
                else
                {
                    if (observer instanceof OwbObserverMethod)
                    {
                        ((OwbObserverMethod<? super Object>)observer).notify(event, metadata);
                    }
                    else
                    {
                        observer.notify(event);
                    }
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
                    throw new ObserverException(WebBeansLoggerFacade.getTokenString(OWBLogConst.EXCEPT_0008) + event.getClass().getName(), e);
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

    /**
     * Gets observer method from given annotated method.
     * @param <T> bean type info
     * @param annotatedMethod annotated method for observer
     * @param bean bean instance 
     * @return ObserverMethod
     */
    public <T> ObserverMethod<?> getObservableMethodForAnnotatedMethod(AnnotatedParameter<?> annotatedParameter, AbstractOwbBean<T> bean)
    {
        Asserts.assertNotNull(annotatedParameter, "annotatedParameter can not be null");
        
        //Observer creation from annotated method
        ObserverMethodImpl<T> observer = new ObserverMethodImpl(bean, annotatedParameter);
        
        //Adds this observer
        addObserver(observer, annotatedParameter.getBaseType());

        return observer;
    }

}
