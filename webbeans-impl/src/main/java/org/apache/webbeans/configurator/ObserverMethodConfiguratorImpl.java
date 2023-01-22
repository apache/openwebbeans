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
package org.apache.webbeans.configurator;

import jakarta.annotation.Priority;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.enterprise.event.Reception;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.enterprise.inject.spi.AnnotatedMethod;
import jakarta.enterprise.inject.spi.AnnotatedParameter;
import jakarta.enterprise.inject.spi.EventContext;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ObserverMethod;
import jakarta.enterprise.inject.spi.configurator.ObserverMethodConfigurator;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.container.BeanManagerImpl;
import org.apache.webbeans.event.EventContextImpl;
import org.apache.webbeans.exception.WebBeansDeploymentException;
import org.apache.webbeans.exception.WebBeansException;


public class ObserverMethodConfiguratorImpl<T> implements ObserverMethodConfigurator<T>
{
    private final WebBeansContext webBeansContext;

    private ObserverMethod<T> originalObserverMethod;

    /**
     * The Extension which added this very ObserverMethod
     */
    private final Extension extension;

    private Class<?> beanClass;
    private Set<Annotation> qualifiers = new HashSet<>();
    private Type observedType;
    private Reception reception = Reception.ALWAYS;
    private TransactionPhase transactionPhase = TransactionPhase.IN_PROGRESS;
    private int priority = ObserverMethod.DEFAULT_PRIORITY;
    private boolean async;

    private EventConsumer notifyWith;


    /**
     * Used for ProcessObserverMethod and ProcessSyntheticObserverMethod.
     *
     * In this case the Configurator just tweaks the Qualifiers, etc, but does _not_
     * change the Notification.
     * EXCEPT a {@link #notifyWith(EventConsumer)} is set!
     */
    public ObserverMethodConfiguratorImpl(WebBeansContext webBeansContext, Extension extension, ObserverMethod<T> observerMethod)
    {
        this(webBeansContext, extension);
        read(observerMethod);

        this.originalObserverMethod = observerMethod;
    }

    public ObserverMethodConfiguratorImpl(WebBeansContext webBeansContext, Extension extension)
    {
        this.webBeansContext = webBeansContext;
        this.extension = extension;
    }

    @Override
    public ObserverMethodConfigurator<T> read(Method method)
    {
        this.qualifiers = getQualifiers(Arrays.asList(method.getAnnotations()));
        this.beanClass = method.getDeclaringClass();

        for (Parameter parameter : method.getParameters())
        {
            Observes observes = parameter.getAnnotation(Observes.class);
            ObservesAsync observesAsync = parameter.getAnnotation(ObservesAsync.class);
            if (observes != null || observesAsync != null)
            {
                observedType = parameter.getParameterizedType();

                if (observes != null)
                {
                    this.reception = observes.notifyObserver();
                    this.transactionPhase = observes.during();
                }
                else
                    {
                    this.reception = observesAsync.notifyObserver();
                    this.transactionPhase = TransactionPhase.IN_PROGRESS;
                    this.async = true;
                }

                Priority prio = parameter.getAnnotation(Priority.class);
                if (prio != null)
                {
                    this.priority = prio.value();
                }
                break;
            }
        }

        //X TODO CDI-2.0
        return this;
    }

    @Override
    public ObserverMethodConfigurator<T> read(AnnotatedMethod annotatedMethod)
    {
        this.qualifiers = getQualifiers(annotatedMethod.getAnnotations());
        this.beanClass = annotatedMethod.getDeclaringType().getJavaClass();

        List<AnnotatedParameter<?>> parameters = annotatedMethod.getParameters();
        for (AnnotatedParameter parameter : parameters)
        {
            Observes observes = parameter.getAnnotation(Observes.class);
            ObservesAsync observesAsync = parameter.getAnnotation(ObservesAsync.class);
            if (observes != null || observesAsync != null)
            {
                observedType = parameter.getBaseType();

                if (observes != null)
                {
                    this.reception = observes.notifyObserver();
                    this.transactionPhase = observes.during();
                }
                else
                    {
                    this.reception = observesAsync.notifyObserver();
                    this.transactionPhase = TransactionPhase.IN_PROGRESS;
                    this.async = true;
                }

                Priority prio = parameter.getAnnotation(Priority.class);
                if (prio != null)
                {
                    this.priority = prio.value();
                }
                break;
            }
        }

        //X TODO CDI-2.0
        return this;
    }

    private Set<Annotation> getQualifiers(Collection<Annotation> annotations)
    {
        BeanManagerImpl bm = webBeansContext.getBeanManagerImpl();
        return annotations.stream()
            .filter(a -> bm.isQualifier(a.annotationType()))
            .collect(Collectors.toSet());
    }

    @Override
    public ObserverMethodConfigurator<T> read(ObserverMethod observerMethod)
    {
        this.beanClass = observerMethod.getBeanClass();
        this.qualifiers.addAll(observerMethod.getObservedQualifiers());
        this.observedType = observerMethod.getObservedType();
        this.reception = observerMethod.getReception();
        this.transactionPhase = observerMethod.getTransactionPhase();
        this.priority = observerMethod.getPriority();
        this.async = observerMethod.isAsync();

        return this;
    }

    @Override
    public ObserverMethodConfigurator<T> beanClass(Class<?> beanClass)
    {
        this.beanClass = beanClass;
        return this;
    }

    @Override
    public ObserverMethodConfigurator<T> observedType(Type type)
    {
        this.observedType = type;
        return this;
    }

    @Override
    public ObserverMethodConfigurator<T> addQualifier(Annotation qualifier)
    {
        this.qualifiers.add(qualifier);
        return this;
    }

    @Override
    public ObserverMethodConfigurator<T> addQualifiers(Annotation... qualifiers)
    {
        for (Annotation qualifier : qualifiers)
        {
            this.qualifiers.add(qualifier);
        }
        return this;
    }

    @Override
    public ObserverMethodConfigurator<T> addQualifiers(Set qualifiers)
    {
        this.qualifiers.addAll(qualifiers);
        return this;
    }

    @Override
    public ObserverMethodConfigurator<T> qualifiers(Annotation... qualifiers)
    {
        this.qualifiers.clear();
        addQualifiers(qualifiers);
        return this;
    }

    @Override
    public ObserverMethodConfigurator<T> qualifiers(Set qualifiers)
    {
        this.qualifiers.clear();
        addQualifiers(qualifiers);
        return this;
    }

    @Override
    public ObserverMethodConfigurator<T> reception(Reception reception)
    {
        this.reception = reception;
        return this;
    }

    @Override
    public ObserverMethodConfigurator<T> transactionPhase(TransactionPhase transactionPhase)
    {
        this.transactionPhase = transactionPhase;
        return this;
    }

    @Override
    public ObserverMethodConfigurator<T> priority(int priority)
    {
        this.priority = priority;
        return this;
    }

    @Override
    public ObserverMethodConfigurator<T> notifyWith(EventConsumer callback)
    {
        this.notifyWith = callback;
        return this;
    }

    @Override
    public ObserverMethodConfigurator<T> async(boolean async)
    {
        this.async = async;
        return this;
    }

    public <T> ObserverMethod<T> getObserverMethod()
    {
        if (observedType ==  null)
        {
            String extensionName = extension != null ? "(" + extension.toString() + ") ! " : "! ";
            WebBeansDeploymentException e = new WebBeansDeploymentException("ObserverMethod observedType is missing "
                + extensionName
                + toString());
            webBeansContext.getBeanManagerImpl().getErrorStack().pushError(e);
            return null;
        }
        if (originalObserverMethod == null && notifyWith == null)
        {
            String extensionName = extension != null ? "(" + extension.toString() + ") ! " : "! ";
            WebBeansDeploymentException e = new WebBeansDeploymentException("ObserverMethod notifyWith is missing "
                + extensionName
                + toString());
            webBeansContext.getBeanManagerImpl().getErrorStack().pushError(e);
            return null;
        }

        return new ConfiguredObserverMethod();
    }

    public Extension getExtension()
    {
        return extension;
    }

    public class ConfiguredObserverMethod<T> implements ObserverMethod<T>
    {
        @Override
        public Class<?> getBeanClass()
        {
            return beanClass != null ? beanClass : extension.getClass();
        }

        @Override
        public Type getObservedType()
        {
            return observedType;
        }

        @Override
        public Set<Annotation> getObservedQualifiers()
        {
            return qualifiers;
        }

        @Override
        public Reception getReception()
        {
            return reception;
        }

        @Override
        public TransactionPhase getTransactionPhase()
        {
            return transactionPhase;
        }

        @Override
        public void notify(T event)
        {
            notify(new EventContextImpl<>(event, null));
        }

        @Override
        public void notify(EventContext<T> eventContext)
        {
            try
            {
                if (notifyWith == null && originalObserverMethod != null)
                {
                    // In case of ProcessObserverMethod with no switched-out Consumer
                    // we just deletage through to the original ObserverMethos
                    originalObserverMethod.notify((EventContext) eventContext);
                }
                else
                {
                    notifyWith.accept(eventContext);
                }
            }
            catch (Exception e)
            {
                throw new WebBeansException(e);
            }
        }

        @Override
        public boolean isAsync()
        {
            return async;
        }

        @Override
        public int getPriority()
        {
            return priority;
        }
    }


    @Override
    public String toString()
    {
        return "ObserverMethodConfiguratorImpl{" +
            "beanClass=" + beanClass +
            ", qualifiers=" + qualifiers +
            ", observedType=" + observedType +
            ", reception=" + reception +
            ", transactionPhase=" + transactionPhase +
            ", priority=" + priority +
            ", async=" + async +
            ", notifyWith=" + notifyWith +
            '}';
    }
}
