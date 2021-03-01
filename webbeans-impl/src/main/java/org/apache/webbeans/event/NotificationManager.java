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

import java.io.Closeable;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.event.NotificationOptions;
import javax.enterprise.event.ObserverException;
import javax.enterprise.event.TransactionPhase;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.AnnotatedCallable;
import javax.enterprise.inject.spi.AnnotatedConstructor;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.EventContext;
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
import javax.enterprise.inject.spi.ProcessSyntheticBean;
import javax.enterprise.inject.spi.ProcessSyntheticObserverMethod;

import org.apache.webbeans.component.AbstractOwbBean;
import org.apache.webbeans.config.OWBLogConst;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.exception.WebBeansDeploymentException;
import org.apache.webbeans.exception.WebBeansException;
import org.apache.webbeans.logger.WebBeansLoggerFacade;
import org.apache.webbeans.portable.events.ProcessAnnotatedTypeImpl;
import org.apache.webbeans.portable.events.generics.GProcessObserverMethod;
import org.apache.webbeans.portable.events.generics.GenericBeanEvent;
import org.apache.webbeans.portable.events.generics.GenericProducerObserverEvent;
import org.apache.webbeans.portable.events.generics.TwoParametersGenericBeanEvent;
import org.apache.webbeans.spi.ContextsService;
import org.apache.webbeans.spi.TransactionService;
import org.apache.webbeans.util.AnnotationUtil;
import org.apache.webbeans.util.Asserts;
import org.apache.webbeans.util.ClassUtil;
import org.apache.webbeans.util.GenericsUtil;
import org.apache.webbeans.util.WebBeansUtil;

import static java.util.Collections.emptyList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.stream.Collectors.toMap;

public class NotificationManager
{
    private final Map<Type, Set<ObserverMethod<?>>> observers = new ConcurrentHashMap<>();
    private final WebBeansContext webBeansContext;

    private final NotificationOptions defaultNotificationOptions;

    /**
     * Contains information whether certain Initialized and Destroyed events have observer methods.
     */
    private final ConcurrentMap<Annotation, Boolean> hasContextLifecycleEventObservers
        = new ConcurrentHashMap<>();

    /**
     * List of ObserverMethods cached by their raw types.
     */
    private final ConcurrentHashMap<Class<?>, Set<ObserverMethod<?>>> observersByRawType
        = new ConcurrentHashMap<>();

    // this is actually faster than a lambda Comparator.comparingInt(ObserverMethod::getPriority)
    private Comparator<? super ObserverMethod<? super Object>> observerMethodComparator
        = new Comparator<ObserverMethod<? super Object>>()
            {
                @Override
                public int compare(ObserverMethod<? super Object> o1, ObserverMethod<? super Object> o2)
                {
                    return Integer.compare(o1.getPriority(), o2.getPriority());
                }
            };

    // idea is to be able to skip O(n) events in favor of an algorithm closer to O(1) impl
    // statistically, it is not rare to not use all these events so we enable to skip most of them
    private Map<Type, Set<ObserverMethod<?>>> processAnnotatedTypeObservers;
    private Map<Type, Set<ObserverMethod<?>>> processBeanAttributesObservers;
    private Map<Type, Set<ObserverMethod<?>>> processInjectionTargetObservers;
    private Map<Type, Set<ObserverMethod<?>>> processManagedBeanObservers;
    private Map<Type, Set<ObserverMethod<?>>> processBeanObservers;
    private Map<Type, Set<ObserverMethod<?>>> processInjectionPointObservers;
    private Map<Type, Set<ObserverMethod<?>>> processObserverMethodObservers;
    private Map<Type, Set<ObserverMethod<?>>> processProducerObservers;
    private Map<Type, Set<ObserverMethod<?>>> processProducerFieldObservers;
    private Map<Type, Set<ObserverMethod<?>>> processProducerMethodObservers;
    private Map<Type, Set<ObserverMethod<?>>> processSyntheticBeanObservers;
    private Map<Type, Set<ObserverMethod<?>>> processSyntheticObserverMethodObservers;

    public NotificationManager(WebBeansContext webBeansContext)
    {
        this.webBeansContext = webBeansContext;
        this.defaultNotificationOptions = NotificationOptions.ofExecutor(getDefaultExecutor());
    }

    public void afterStart()
    {
        processAnnotatedTypeObservers = null;
        processBeanAttributesObservers = null;
        processInjectionTargetObservers = null;
        processManagedBeanObservers = null;
        processBeanObservers = null;
        processInjectionPointObservers = null;
        processObserverMethodObservers = null;
        processProducerObservers = null;
        processProducerFieldObservers = null;
        processProducerMethodObservers = null;
        processSyntheticBeanObservers = null;
        processSyntheticObserverMethodObservers = null;
    }

    private Executor getDefaultExecutor()
    {
        // here it would be nice to support to use a produced bean like @Named("openwebbeansCdiExecutor")
        // instead of a direct spi
        //
        // logic is: if an Executor is registered as a spi use it, otherwise use JVM default one
        Executor service = webBeansContext.getService(Executor.class);
        return service != null ? service : new CloseableExecutor();
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
        List<ObserverMethod<?>> observerMethods = new ArrayList<>();
        for (Set<ObserverMethod<?>> methods: observers.values())
        {
            for (ObserverMethod<?> method: methods)
            {
                observerMethods.add(method);
            }
        }
        return observerMethods;
    }

    public <T> void addObserver(ObserverMethod<T> observer)
    {
        webBeansContext.getAnnotationManager().checkQualifierConditions(observer.getObservedQualifiers());

        Set<ObserverMethod<?>> set = observers.computeIfAbsent(observer.getObservedType(), k -> new HashSet<>());

        set.add(observer);
    }

    public boolean hasProcessAnnotatedTypeObservers()
    {
        cacheIfNeeded(new ProcessAnnotatedTypeImpl<>(null, null));
        return !processAnnotatedTypeObservers.isEmpty();
    }

    public <T> Collection<ObserverMethod<? super T>> resolveObservers(T event, EventMetadataImpl metadata, boolean isLifecycleEvent)
    {
        if (isLifecycleEvent) // goal here is to skip any resolution if not needed
        {
            Collection observerMethods = cacheIfNeeded(event);
            if (observerMethods != null) // emptyList()
            {
                return observerMethods;
            }
        }
        Type eventType = metadata.validatedType();
        Collection<ObserverMethod<? super T>> observersMethods = filterByQualifiers(
                filterByType(event, eventType, isLifecycleEvent), metadata.getQualifiers());

        if (isLifecycleEvent && event instanceof ProcessAnnotatedType)
        {
            observersMethods = filterByWithAnnotations(observersMethods, ((ProcessAnnotatedType) event).getAnnotatedType());
        }
        else if (!isLifecycleEvent && observersMethods.isEmpty())
        {
            //this check for the TCK is only needed if no observer was found
            EventUtil.checkEventBindings(webBeansContext, metadata.getQualifiers());
            EventUtil.checkQualifierImplementations(metadata.getQualifiers());
        }

        return observersMethods;
    }

    private <T> Collection<ObserverMethod<?>> cacheIfNeeded(final T event)
    {
        if (event instanceof ProcessAnnotatedType)
        {
            if (processAnnotatedTypeObservers == null)
            {
                processAnnotatedTypeObservers = findObservers(ProcessAnnotatedType.class);
            }
            if (processAnnotatedTypeObservers.isEmpty())
            {
                return emptyList();
            }
        }
        else if (event instanceof ProcessManagedBean)
        {
            if (processManagedBeanObservers == null)
            {
                processManagedBeanObservers = findObservers(ProcessManagedBean.class);
                if (processBeanObservers == null)
                {
                    processBeanObservers = findObservers(ProcessBean.class);
                }
                processBeanObservers.forEach((k, v) -> processManagedBeanObservers
                        .computeIfAbsent(k, it -> new HashSet<>())
                        .addAll(v));
            }
            if (processManagedBeanObservers.isEmpty())
            {
                return emptyList();
            }
        }
        else if (event instanceof ProcessProducerField)
        {
            if (processProducerFieldObservers == null)
            {
                processProducerFieldObservers = findObservers(ProcessProducerField.class);
                if (processBeanObservers == null)
                {
                    processBeanObservers = findObservers(ProcessBean.class);
                }
                processBeanObservers.forEach((k, v) -> processProducerFieldObservers
                        .computeIfAbsent(k, it -> new HashSet<>())
                        .addAll(v));
            }
            if (processProducerFieldObservers.isEmpty())
            {
                return emptyList();
            }
        }
        else if (event instanceof ProcessProducerMethod)
        {
            if (processProducerMethodObservers == null)
            {
                processProducerMethodObservers = findObservers(ProcessProducerMethod.class);
                if (processBeanObservers == null)
                {
                    processBeanObservers = findObservers(ProcessBean.class);
                }
                processBeanObservers.forEach((k, v) -> processProducerMethodObservers
                        .computeIfAbsent(k, it -> new HashSet<>())
                        .addAll(v));
            }
            if (processProducerMethodObservers.isEmpty())
            {
                return emptyList();
            }
        }
        else if (event instanceof ProcessSyntheticBean)
        {
            if (processSyntheticBeanObservers == null)
            {
                processSyntheticBeanObservers = findObservers(ProcessSyntheticBean.class);
                if (processBeanObservers == null)
                {
                    processBeanObservers = findObservers(ProcessBean.class);
                }
                processBeanObservers.forEach((k, v) -> processSyntheticBeanObservers
                        .computeIfAbsent(k, it -> new HashSet<>())
                        .addAll(v));
            }
            if (processSyntheticBeanObservers.isEmpty())
            {
                return emptyList();
            }
        }
        else if (event instanceof ProcessSyntheticObserverMethod)
        {
            if (processSyntheticObserverMethodObservers == null)
            {
                processSyntheticObserverMethodObservers = findObservers(ProcessSyntheticObserverMethod.class);
                if (processObserverMethodObservers == null)
                {
                    processObserverMethodObservers = findObservers(ProcessObserverMethod.class);
                }
                processObserverMethodObservers.forEach((k, v) -> processSyntheticObserverMethodObservers
                        .computeIfAbsent(k, it -> new HashSet<>())
                        .addAll(v));
            }
            if (processSyntheticObserverMethodObservers.isEmpty())
            {
                return emptyList();
            }
        }
        else if (event instanceof ProcessBean)
        {
            if (processBeanObservers == null)
            {
                processBeanObservers = findObservers(ProcessBean.class);
            }
            if (processBeanObservers.isEmpty())
            {
                return emptyList();
            }
        }
        else if (event instanceof ProcessBeanAttributes)
        {
            if (processBeanAttributesObservers == null)
            {
                processBeanAttributesObservers = findObservers(ProcessBeanAttributes.class);
            }
            if (processBeanAttributesObservers.isEmpty())
            {
                return emptyList();
            }
        }
        else if (event instanceof ProcessInjectionTarget)
        {
            if (processInjectionTargetObservers == null)
            {
                processInjectionTargetObservers = findObservers(ProcessInjectionTarget.class);
            }
            if (processInjectionTargetObservers.isEmpty())
            {
                return emptyList();
            }
        }
        else if (event instanceof ProcessInjectionPoint)
        {
            if (processInjectionPointObservers == null)
            {
                processInjectionPointObservers = findObservers(ProcessInjectionPoint.class);
            }
            if (processInjectionPointObservers.isEmpty())
            {
                return emptyList();
            }
        }
        else if (event instanceof ProcessObserverMethod)
        {
            if (processObserverMethodObservers == null)
            {
                processObserverMethodObservers = findObservers(ProcessObserverMethod.class);
            }
            if (processObserverMethodObservers.isEmpty())
            {
                return emptyList();
            }
        }
        else if (event instanceof ProcessProducer)
        {
            if (processProducerObservers == null)
            {
                processProducerObservers = findObservers(ProcessProducer.class);
            }
            if (processProducerObservers.isEmpty())
            {
                return emptyList();
            }
        }
        // note: don't forget to update filterByExtensionEventType method too
        return null;
    }

    private <T> Collection<ObserverMethod<? super T>> filterByWithAnnotations(Collection<ObserverMethod<? super T>> observersMethods, AnnotatedType annotatedType)
    {
        List<ObserverMethod<? super T>> observerMethodsWithAnnotations = new ArrayList<>();

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

                for (Annotation meta : annotation.annotationType().getAnnotations())
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

        Set<ObserverMethod<? super T>> matching = new HashSet<>();

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
                        && GenericsUtil.isAssignableFrom(true, false, observedType, ParameterizedType.class.cast(eventType).getRawType(), new HashMap<>()))
                    || GenericsUtil.isAssignableFrom(true, false, observedType, eventType, new HashMap<>()))
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
        Set<ObserverMethod<? super T>> matching = new HashSet<>();
        final Map<Type, Set<ObserverMethod<?>>> sourceMap;
        if (event instanceof ProcessAnnotatedType) // check resolveObservers
        {
            sourceMap = processAnnotatedTypeObservers;
        }
        else if (event instanceof ProcessSyntheticObserverMethod)
        {
            sourceMap = processSyntheticObserverMethodObservers;
        }
        else if (event instanceof ProcessObserverMethod)
        {
            sourceMap = processObserverMethodObservers;
        }
        else if (event instanceof ProcessProducerField)
        {
            sourceMap = processProducerFieldObservers;
        }
        else if (event instanceof ProcessProducerMethod)
        {
            sourceMap = processProducerMethodObservers;
        }
        else if (event instanceof ProcessSyntheticBean)
        {
            sourceMap = processSyntheticBeanObservers;
        }
        else if (event instanceof ProcessProducer)
        {
            sourceMap = processProducerObservers;
        }
        else if (event instanceof ProcessManagedBean)
        {
            sourceMap = processManagedBeanObservers;
        }
        else if (event instanceof ProcessBean)
        {
            sourceMap = processBeanObservers;
        }
        else if (event instanceof ProcessBeanAttributes)
        {
            sourceMap = processBeanAttributesObservers;
        }
        else if (event instanceof ProcessInjectionTarget)
        {
            sourceMap = processInjectionTargetObservers;
        }
        else if (event instanceof ProcessInjectionPoint)
        {
            sourceMap = processInjectionPointObservers;
        }
        else
        {
            sourceMap = observers;
        }

        Set<Type> keySet = sourceMap.keySet();
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
        else if (observerTypeActualArg instanceof ParameterizedType)
        {
            return GenericsUtil.isAssignableFrom(false, true, observerTypeActualArg, beanClass, new HashMap<>());
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
                            || GenericsUtil.isAssignableFrom(true, false, actualArgs[1], secondParam, new HashMap<>())))
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
    private <T> Collection<ObserverMethod<? super T>> filterByQualifiers(Collection<ObserverMethod<? super T>> observers, Set<Annotation> eventQualifiers)
    {
        List<ObserverMethod<? super T>> matching = new ArrayList<>(observers.size());

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

    public NotificationOptions getDefaultNotificationOptions()
    {
        return defaultNotificationOptions;
    }

    /**
     * Fire the given event
     * @param notificationOptions if {@code null} then this is a synchronous event. Otherwise fireAsync
     */
    public <T> CompletionStage<T> fireEvent(Object event, EventMetadataImpl metadata, boolean isLifecycleEvent,
                                            NotificationOptions notificationOptions)
    {
        boolean async = notificationOptions != null;
        if (!isLifecycleEvent && webBeansContext.getWebBeansUtil().isContainerEventType(event))
        {
            throw new IllegalArgumentException("Firing container events is forbidden");
        }
        return doFireEvent(
                event, metadata, isLifecycleEvent, notificationOptions, async,
                new ArrayList<>(resolveObservers(event, metadata, isLifecycleEvent)));

    }

    public <T> CompletionStage<T> doFireEvent(Object event, EventMetadataImpl metadata, boolean isLifecycleEvent,
                                              NotificationOptions notificationOptions, boolean async,
                                              List<ObserverMethod<? super Object>> observerMethods)
    {
        prepareObserverListForFire(isLifecycleEvent, async, observerMethods);
        if (observerMethods.isEmpty())
        {
            if (async)
            {
                return completedFuture((T) event);
            }
            return null;
        }
        EventContextImpl<Object> context = new EventContextImpl<>(event, metadata);
        if (async)
        {
            return doFireAsync(context, isLifecycleEvent, notificationOptions, observerMethods);
        }
        doFireSync(context, isLifecycleEvent, observerMethods);
        return null;

    }

    public <T> CompletionStage<T> doFireAsync(EventContext<?> context,
                                              boolean isLifecycleEvent, NotificationOptions notificationOptions,
                                              List<ObserverMethod<? super Object>> observerMethods)
    {
        List<CompletableFuture<Void>> completableFutures = new ArrayList<>();
        for (ObserverMethod<? super Object> observer : observerMethods)
        {
            try
            {
                TransactionPhase phase = observer.getTransactionPhase();

                if (phase == null || phase == TransactionPhase.IN_PROGRESS)
                {
                    completableFutures.add(invokeObserverMethodAsync(context, observer, notificationOptions));
                }
                else
                {
                    throw new WebBeansConfigurationException("Async Observer Methods can only use TransactionPhase.IN_PROGRESS!");
                }
            }
            catch (WebBeansException e)
            {
                return onWebBeansException(context.getEvent(), isLifecycleEvent, e);
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
        return complete(completableFutures, (T) context.getEvent());
    }

    public void doFireSync(EventContext<?> context, boolean isLifecycleEvent,
                           List<ObserverMethod<? super Object>> observerMethods)
    {
        if (observerMethods.isEmpty())
        {
            return;
        }
        // synchronous case
        for (ObserverMethod<? super Object> observer : observerMethods)
        {
            try
            {
                TransactionPhase phase = observer.getTransactionPhase();

                if (phase == null || phase == TransactionPhase.IN_PROGRESS)
                {
                    invokeObserverMethod(context, observer);
                }
                else
                {
                    TransactionService transactionService = webBeansContext.getTransactionService();
                    if(transactionService != null)
                    {
                        transactionService.registerTransactionSynchronization(phase, observer, context.getEvent());
                    }
                    else
                    {
                        invokeObserverMethod(context, observer);
                    }
                }
            }
            catch (WebBeansException e)
            {
                onWebBeansException(context.getEvent(), isLifecycleEvent, e);
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

    public void prepareObserverListForFire(boolean isLifecycleEvent, boolean async,
                                           List<ObserverMethod<? super Object>> observerMethods)
    {
        // async doesn't apply to Extension lifecycle events
        if (!isLifecycleEvent)
        {
            // filter for all async or all synchronous observermethods
            // oldschool and not Streams, because of performance and avoiding tons of temporary objects
            observerMethods.removeIf(observerMethod -> async != observerMethod.isAsync());
        }
        else
        {
            observerMethods.removeIf(observer -> !Extension.class.isAssignableFrom(observer.getBeanClass()));
        }

        // new in CDI-2.0: sort observers
        if (observerMethods.size() > 1)
        {
            observerMethods.sort(observerMethodComparator);
        }
    }

    private <T> CompletionStage<T> onWebBeansException(final Object event, final boolean isLifecycleEvent,
                                                       final WebBeansException e)
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
        RuntimeException rte = (RuntimeException) exc;
        throw rte;
    }

    private <T> CompletableFuture<T> complete(List<CompletableFuture<Void>> completableFutures, T event)
    {
        if (completableFutures == null)
        {
            return null;
        }
        if (completableFutures.isEmpty())
        {
            return CompletableFuture.completedFuture(event);
        }
        CDICompletionFuture<T> future = new CDICompletionFuture<>(event, completableFutures.size());
        completableFutures.forEach(f -> f.handle((t, e) ->
        {
            future.addResult(e);
            return null;
        }));
        return future;
    }

    private CompletableFuture invokeObserverMethodAsync(EventContext<?> context,
                                           ObserverMethod<? super Object> observer,
                                           NotificationOptions notificationOptions)
    {
        CompletableFuture<?> future = new CompletableFuture<>();
        CompletableFuture.runAsync(() -> {
            try
            {
                runAsync(context, observer);
                future.complete(null);
            }
            catch (WebBeansException wbe)
            {
                future.completeExceptionally(wbe.getCause());
            }
        }, notificationOptions.getExecutor() == null ? defaultNotificationOptions.getExecutor() : notificationOptions.getExecutor());
        return future;
    }

    private void runAsync(EventContext<?> context, ObserverMethod<? super Object> observer)
    {
        //X TODO set up threads, requestcontext etc
        final ContextsService contextsService = webBeansContext.getContextsService();
        contextsService.startContext(RequestScoped.class, null);
        try
        {
            invokeObserverMethod(context, observer);
        }
        finally
        {
            contextsService.endContext(RequestScoped.class, null);
        }
    }

    private void invokeObserverMethod(EventContext context, ObserverMethod<?> observer)
    {
        observer.notify(context);
    }

    /**
     * Gets observer method from given annotated method.
     * @param <T> bean type info
     * @param annotatedMethod annotated method for observer
     * @param ownerBean bean instance
     * @return ObserverMethod
     */
    public <T> ObserverMethod<?> getObservableMethodForAnnotatedMethod(AnnotatedMethod<?> annotatedMethod, AnnotatedParameter<?> annotatedParameter, AbstractOwbBean<T> ownerBean)
    {
        Asserts.assertNotNull(annotatedParameter, "annotatedParameter");

        ObserverMethodImpl<T> observer = null;
        // Observer creation from annotated method
        if (isContainerEvent(annotatedParameter))
        {
            observer = new ContainerEventObserverMethodImpl(ownerBean, annotatedMethod, annotatedParameter);
            addObserver(observer);
        }
        else
        {
            observer = new ObserverMethodImpl(ownerBean, annotatedMethod, annotatedParameter);

            GProcessObserverMethod event = new GProcessObserverMethod(webBeansContext, annotatedMethod, observer);

            //Fires ProcessObserverMethod
            webBeansContext.getBeanManagerImpl().fireEvent(event, true, AnnotationUtil.EMPTY_ANNOTATION_ARRAY);

            webBeansContext.getWebBeansUtil().inspectDefinitionErrorStack("There are errors that are added by ProcessObserverMethod event observers for " +
                "observer methods. Look at logs for further details");

            if (!event.isVetoed())
            {
                //Adds this observer
                addObserver(event.getObserverMethod());
            }
            else
            {
                observer = null;
            }
            event.setStarted();
        }

        return observer;
    }

    public boolean isContainerEvent(AnnotatedParameter<?> annotatedParameter)
    {
        AnnotatedCallable<?> method = annotatedParameter.getDeclaringCallable();
        if (!AnnotatedMethod.class.isInstance(method) || method.getParameters().isEmpty())
        {
            return false;
        }
        Class<?> paramType = AnnotatedMethod.class.cast(method).getJavaMember().getParameterTypes()[0];
        return webBeansContext.getWebBeansUtil().isContainerEventType(paramType);
    }

    // for lifecycle parameterized events for now
    private Map<Type, Set<ObserverMethod<?>>> findObservers(final Class<?> type)
    {
        return observers.entrySet().stream()
                .filter(it -> {
                    final Class<?> keyType = ClassUtil.getClass(it.getKey());
                    return type.isAssignableFrom(keyType);
                })
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    // this behaves as a future aggregator, we don't strictly need to represent it but found it more expressive
    private static final class CDICompletionFuture<T> extends CompletableFuture<T>
    {
        private final T event;
        private final AtomicInteger counter;
        private AtomicReference<CompletionException> error = new AtomicReference<>();

        private CDICompletionFuture(T event, int total)
        {
            this.event = event;
            this.counter = new AtomicInteger(total);
        }

        CDICompletionFuture<T> addResult(Throwable t)
        {
            if (t != null)
            {
                if (error.get() == null)
                {
                    error.compareAndSet(null, new CompletionException(t));
                }
                error.get().addSuppressed(t);
            }
            if (counter.decrementAndGet() == 0)
            {
                if (error.get() != null)
                {
                    completeExceptionally(error.get());
                }
                else
                {
                    complete(event);
                }
            }
            return this;
        }
    }

    private static final class CloseableExecutor implements Executor, Closeable
    {
        private final Collection<Runnable> tracker = new CopyOnWriteArrayList<>();
        private volatile boolean reject;

        @Override
        public void close() throws IOException
        {
            reject = true;
            tracker.forEach(r -> {
                try
                {
                    r.run();
                }
                catch (RuntimeException re)
                {
                    WebBeansLoggerFacade.getLogger(NotificationManager.class).warning(re.getMessage());
                }
            });
        }

        @Override
        public void execute(Runnable command)
        {
            if (reject)
            {
                throw new RejectedExecutionException("CDI executor is shutdown");
            }

            tracker.add(command);
            ForkJoinPool.commonPool().execute(() ->
            {
                try
                {
                    command.run();
                }
                finally
                {
                    tracker.remove(command);
                }
            });
        }
    }
}
