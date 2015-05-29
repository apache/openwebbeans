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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.enterprise.event.ObserverException;
import javax.enterprise.event.TransactionPhase;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.AfterTypeDiscovery;
import javax.enterprise.inject.spi.AnnotatedCallable;
import javax.enterprise.inject.spi.AnnotatedConstructor;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.BeforeShutdown;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ObserverMethod;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.ProcessBean;
import javax.enterprise.inject.spi.ProcessBeanAttributes;
import javax.enterprise.inject.spi.ProcessInjectionPoint;
import javax.enterprise.inject.spi.ProcessInjectionTarget;
import javax.enterprise.inject.spi.ProcessManagedBean;
import javax.enterprise.inject.spi.ProcessObserverMethod;
import javax.enterprise.inject.spi.ProcessProducer;
import javax.enterprise.inject.spi.ProcessProducerField;
import javax.enterprise.inject.spi.ProcessProducerMethod;
import javax.enterprise.inject.spi.ProcessSyntheticAnnotatedType;
import javax.enterprise.util.TypeLiteral;

import org.apache.webbeans.component.AbstractOwbBean;
import org.apache.webbeans.config.OWBLogConst;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.exception.WebBeansDeploymentException;
import org.apache.webbeans.exception.WebBeansException;
import org.apache.webbeans.logger.WebBeansLoggerFacade;
import org.apache.webbeans.portable.events.ProcessSessionBeanImpl;
import org.apache.webbeans.portable.events.generics.GenericBeanEvent;
import org.apache.webbeans.portable.events.generics.GenericProducerObserverEvent;
import org.apache.webbeans.portable.events.generics.TwoParametersGenericBeanEvent;
import org.apache.webbeans.spi.TransactionService;
import org.apache.webbeans.util.AnnotationUtil;
import org.apache.webbeans.util.Asserts;
import org.apache.webbeans.util.ClassUtil;
import org.apache.webbeans.util.GenericsUtil;
import org.apache.webbeans.util.WebBeansUtil;

public final class NotificationManager
{
    private final Map<Type, Set<ObserverMethod<?>>> observers = new ConcurrentHashMap<Type, Set<ObserverMethod<?>>>();
    private final WebBeansContext webBeansContext;

    /**
     * Contains information whether certain Initialized and Destroyed events have observer methods.
     */
    private final ConcurrentMap<Annotation, Boolean> hasContextLifecycleEventObservers
        = new ConcurrentHashMap<Annotation, Boolean>();

    /**
     * List of ObserverMethods cached by their raw types.
     */
    private final ConcurrentHashMap<Class<?>, Set<ObserverMethod<?>>> observersByRawType
        = new ConcurrentHashMap<Class<?>, Set<ObserverMethod<?>>>();



    public static final Set<Class> CONTAINER_EVENT_CLASSES = new HashSet<Class>(
        Arrays.asList(new Class[]{
            AfterBeanDiscovery.class,
            AfterDeploymentValidation.class,
            AfterTypeDiscovery.class,
            BeforeBeanDiscovery.class,
            BeforeShutdown.class,
            ProcessAnnotatedType.class,
            ProcessBean.class,
            ProcessBeanAttributes.class,
            ProcessInjectionPoint.class,
            ProcessInjectionTarget.class,
            ProcessManagedBean.class,
            ProcessObserverMethod.class,
            ProcessProducer.class,
            ProcessProducerField.class,
            ProcessProducerMethod.class,
            ProcessSessionBeanImpl.class,
            ProcessSyntheticAnnotatedType.class,
        }));

    public NotificationManager(WebBeansContext webBeansContext)
    {
        this.webBeansContext = webBeansContext;
    }

    /**
     * This methods needs to get called after the container got started.
     * This is to avoid that events which already got fired during bootstrap in Extensions
     * will get cached and events from beans thus get ignored.
     */
    public void clearCaches()
    {
        observersByRawType.clear();
        hasContextLifecycleEventObservers.clear();
    }

    /**
     *
     * @param lifecycleEvent e.g. {@link org.apache.webbeans.annotation.DestroyedLiteral#INSTANCE_REQUEST_SCOPED}
     * @return whether the given Initialized or Destroyed event has observer methods.
     */
    public boolean hasContextLifecycleObserver(Annotation lifecycleEvent)
    {
        Boolean hasObserver = hasContextLifecycleEventObservers.get(lifecycleEvent);
        if (hasObserver == null)
        {
            hasObserver = Boolean.FALSE;
            for (ObserverMethod<?> observerMethod : getObserverMethods())
            {
                if (observerMethod.getObservedQualifiers().contains(lifecycleEvent))
                {
                    hasObserver = Boolean.TRUE;
                    break;
                }
            }
            hasContextLifecycleEventObservers.putIfAbsent(lifecycleEvent, hasObserver);
        }

        return hasObserver;
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


    public <T> Set<ObserverMethod<? super T>> resolveObservers(T event, EventMetadataImpl metadata, boolean isLifecycleEvent)
    {
        Type eventType = metadata.validatedType();
        Set<ObserverMethod<? super T>> observersMethods = filterByType(event, eventType, isLifecycleEvent);

        observersMethods = filterByQualifiers(observersMethods, metadata.getQualifiers());

        if (isLifecycleEvent && event instanceof ProcessAnnotatedType)
        {
            observersMethods = filterByWithAnnotations(observersMethods, ((ProcessAnnotatedType) event).getAnnotatedType());
        }

        if (!isLifecycleEvent && observersMethods.isEmpty())
        {
            //this check for the TCK is only needed if no observer was found
            EventUtil.checkEventBindings(webBeansContext, metadata.getQualifiers());
            EventUtil.checkQualifierImplementations(metadata.getQualifiers());
        }

        return observersMethods;
    }

    private <T> Set<ObserverMethod<? super T>> filterByWithAnnotations(Set<ObserverMethod<? super T>> observersMethods, AnnotatedType annotatedType)
    {
        Set<ObserverMethod<? super T>> observerMethodsWithAnnotations = new HashSet<ObserverMethod<? super T>>();

        for (ObserverMethod<? super T> observerMethod : observersMethods)
        {
            Class[] withAnnotations = ((ContainerEventObserverMethodImpl) observerMethod).getWithAnnotations();
            if (withAnnotations != null && withAnnotations.length > 0)
            {
                if (annotatedTypeHasAnnotations(annotatedType, withAnnotations))
                {
                    observerMethodsWithAnnotations.add(observerMethod);
                }

                continue;
            }

            // no WithAnnotations
            observerMethodsWithAnnotations.add(observerMethod);

        }

        return observerMethodsWithAnnotations;
    }

    private boolean annotatedTypeHasAnnotations(AnnotatedType annotatedType, Class<? extends Annotation>[] withAnnotations)
    {
        if (hasAnnotation(annotatedType.getAnnotations(), withAnnotations))
        {
            return true;
        }

        Set<AnnotatedField> fields = annotatedType.getFields();
        for (AnnotatedField annotatedField : fields)
        {
            if (hasAnnotation(annotatedField.getAnnotations(), withAnnotations))
            {
                return true;
            }
        }

        Set<AnnotatedMethod> annotatedMethods = annotatedType.getMethods();
        for (AnnotatedMethod annotatedMethod : annotatedMethods)
        {
            if (hasAnnotation(annotatedMethod.getAnnotations(), withAnnotations))
            {
                return true;
            }
            for (AnnotatedParameter annotatedParameter : (List<AnnotatedParameter>) annotatedMethod.getParameters())
            {
                if (hasAnnotation(annotatedParameter.getAnnotations(), withAnnotations))
                {
                    return true;
                }
            }
        }

        Set<AnnotatedConstructor<?>> annotatedConstructors = annotatedType.getConstructors();
        for (AnnotatedConstructor<?> annotatedConstructor : annotatedConstructors)
        {
            if (hasAnnotation(annotatedConstructor.getAnnotations(), withAnnotations))
            {
                return true;
            }
            for (AnnotatedParameter annotatedParameter : annotatedConstructor.getParameters())
            {
                if (hasAnnotation(annotatedParameter.getAnnotations(), withAnnotations))
                {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean hasAnnotation(Set<Annotation> annotations, Class<? extends Annotation>[] withAnnotations)
    {
        for (Class<? extends Annotation> withAnnotation : withAnnotations)
        {
            for (Annotation annotation : annotations)
            {
                if (withAnnotation.isAssignableFrom(annotation.annotationType()))
                {
                    return true;
                }

                for (final Annotation meta : annotation.annotationType().getAnnotations())
                {
                    if (withAnnotation.isAssignableFrom(meta.annotationType()))
                    {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private <T> Set<ObserverMethod<? super T>> filterByType(T event, Type declaredEventType, boolean isLifecycleEvent)
    {
        if (isLifecycleEvent)
        {
            return filterByExtensionEventType(event, declaredEventType);
        }
        Class<?> eventClass = event.getClass();

        // whether the fired event is a raw java class or a generic type
        boolean isRawEvent = declaredEventType instanceof Class;
        if (isRawEvent)
        {
            Set rawTypeObservers = observersByRawType.get(eventClass);
            if (rawTypeObservers != null)
            {
                return rawTypeObservers;
            }
        }

        Set<ObserverMethod<? super T>> matching = new HashSet<ObserverMethod<? super T>>();

        Set<Type> eventTypes = GenericsUtil.getTypeClosure(declaredEventType, eventClass);
        if (GenericsUtil.containTypeVariable(eventTypes))
        {
            throw new IllegalArgumentException("event type may not contain unbound type variable: " + eventTypes);
        }

        for (Map.Entry<Type, Set<ObserverMethod<?>>> observerEntry : observers.entrySet())
        {
            Type observedType = observerEntry.getKey();
            for (Type eventType : eventTypes)
            {
                if ((ParameterizedType.class.isInstance(eventType) && Class.class.isInstance(observedType)
                        && GenericsUtil.isAssignableFrom(true, false, observedType, ParameterizedType.class.cast(eventType).getRawType()))
                    || GenericsUtil.isAssignableFrom(true, false, observedType, eventType))
                {
                    Set<ObserverMethod<?>> observerMethods = observerEntry.getValue();

                    for (ObserverMethod<?> observerMethod : observerMethods)
                    {
                        matching.add((ObserverMethod<T>) observerMethod);
                    }
                    break;
                }
            }
        }

        if (isRawEvent)
        {
            // cache the result
            observersByRawType.putIfAbsent(eventClass, (Set) matching);
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
            Class<?> beanClass;
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
                                Type secondParam = null;
                                if (TwoParametersGenericBeanEvent.class.isInstance(event))
                                {
                                    secondParam = TwoParametersGenericBeanEvent.class.cast(event).getInjectionType();
                                }
                                addToMatchingWithParametrizedForBeans(type, matching, beanClass, secondParam);
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
                                addToMatchingWithParametrizedForBeans(type, matching, beanClass, null);
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
    private boolean checkEventTypeParameterForExtensions(Type beanClass, Type observerTypeActualArg)
    {
        if(ClassUtil.isTypeVariable(observerTypeActualArg))
        {
            TypeVariable<?> tv = (TypeVariable<?>)observerTypeActualArg;
            Type tvBound = tv.getBounds()[0];
            
            if(tvBound instanceof Class)
            {
                Class<?> clazzTvBound = (Class<?>)tvBound;
                
                if(Class.class.isInstance(beanClass) && clazzTvBound.isAssignableFrom(Class.class.cast(beanClass)))
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
            if(Class.class.isInstance(beanClass) && observerClass.isAssignableFrom(Class.class.cast(beanClass)))
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
    
    private <T> void addToMatchingWithParametrizedForBeans(Type type, Set<ObserverMethod<? super T>> matching,
                                                           Class<?> beanClass, Type secondParam)
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
            if(checkEventTypeParameterForExtensions(beanClass, actualArgs[0])
                    && (secondParam == null || actualArgs.length == 1
                            || checkEventTypeParameterForExtensions(secondParam, actualArgs[1])
                            || GenericsUtil.isAssignableFrom(false, false, secondParam, actualArgs[1])))
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

    public void fireEvent(Object event, EventMetadataImpl metadata, boolean isLifecycleEvent)
    {
        if (!isLifecycleEvent && webBeansContext.getWebBeansUtil().isContainerEventType(event))
        {
            throw new IllegalArgumentException("Firing container events is forbidden");
        }

        Set<ObserverMethod<? super Object>> observerMethods = resolveObservers(event, metadata, isLifecycleEvent);

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

                if (isLifecycleEvent)
                {
                    if (event instanceof AfterDeploymentValidation)
                    {
                        throw new WebBeansDeploymentException("Error while sending SystemEvent to a CDI Extension! " + event.toString(), e);
                    }
                    else
                    {
                        throw new WebBeansConfigurationException("Error while sending SystemEvent to a CDI Extension! " + event.toString(), e);
                    }
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
    public <T> ObserverMethod<?> getObservableMethodForAnnotatedMethod(AnnotatedMethod<?> annotatedMethod, AnnotatedParameter<?> annotatedParameter, AbstractOwbBean<T> bean)
    {
        Asserts.assertNotNull(annotatedParameter, "annotatedParameter");
        
        //Observer creation from annotated method
        ObserverMethodImpl<T> observer = isContainerEvent(annotatedParameter)?
                new ContainerEventObserverMethodImpl(bean, annotatedMethod, annotatedParameter) :
                new ObserverMethodImpl(bean, annotatedMethod, annotatedParameter);
        
        //Adds this observer
        addObserver(observer, annotatedParameter.getBaseType());

        return observer;
    }

    public boolean isContainerEvent(final AnnotatedParameter<?> annotatedParameter)
    {
        final AnnotatedCallable<?> method = annotatedParameter.getDeclaringCallable();
        if (!AnnotatedMethod.class.isInstance(method) || method.getParameters().size() == 0)
        {
            return false;
        }
        final Class<?> paramType = AnnotatedMethod.class.cast(method).getJavaMember().getParameterTypes()[0];
        return CONTAINER_EVENT_CLASSES.contains(paramType);
    }

}
