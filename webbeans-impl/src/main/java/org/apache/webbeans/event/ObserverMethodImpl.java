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
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Priority;
import javax.enterprise.context.ContextNotActiveException;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.Context;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Observes;
import javax.enterprise.event.ObservesAsync;
import javax.enterprise.event.Reception;
import javax.enterprise.event.TransactionPhase;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.EventContext;
import javax.enterprise.inject.spi.EventMetadata;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.ObserverMethod;
import javax.enterprise.inject.spi.WithAnnotations;

import org.apache.webbeans.component.AbstractOwbBean;
import org.apache.webbeans.component.WebBeansType;
import org.apache.webbeans.config.OWBLogConst;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.container.BeanManagerImpl;
import org.apache.webbeans.context.creational.CreationalContextImpl;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.exception.WebBeansException;
import org.apache.webbeans.logger.WebBeansLoggerFacade;
import org.apache.webbeans.proxy.OwbNormalScopeProxy;
import org.apache.webbeans.spi.plugins.OpenWebBeansEjbPlugin;

/**
 * Defines observers that are declared in observer methods.
 * <p>
 * Example:
 * <pre>
 *  public class X {
 *      
 *      public void afterLoggedIn(@Observes @Current LoggedInEvent event)
 *      {
 *          .....
 *      }
 *  }
 * </pre>
 * Above class X instance observes for the event with type <code>LoggedInEvent</code>
 * and event qualifier is <code>Current</code>. Whenever event is fired, its {@link javax.enterprise.inject.spi.ObserverMethod#notify()}
 * method is called.
 * </p>
 * 
 * @version $Rev$ $Date$
 *
 * @param <T> event type
 */
public class ObserverMethodImpl<T> implements OwbObserverMethod<T>
{
    /**Logger instance*/
    private static final Logger logger = WebBeansLoggerFacade.getLogger(ObserverMethodImpl.class);

    /**Observer owner bean that defines observer method*/
    private final AbstractOwbBean<?> ownerBean;

    /**Using existing bean instance or not*/
    private final boolean ifExist;
    
    /** the observed qualifiers */
    private final Set<Annotation> observedQualifiers;

    /** the type of the observed event */
    private final Type observedEventType;

    /** the transaction phase */
    private final TransactionPhase phase;
    
    /** the injection points */
    private final Set<InjectionPoint> injectionPoints;

    private final Method view;

    /**Annotated method*/
    private AnnotatedMethod<T> annotatedObserverMethod;
    
    /**\@Observes parameter*/
    private AnnotatedParameter<T> annotatedObservesParameter;

    private int priority = ObserverMethod.DEFAULT_PRIORITY;

    private boolean isAsync;
    
    private static class ObserverParams
    {
        private Bean<Object> bean;
        
        private Object instance;
        
        private CreationalContext<Object> creational;
        
        private boolean isBean;
    }

    /**
     * used if the qualifiers and event type are already known, e.g. from the XML.
     */
    public ObserverMethodImpl(AbstractOwbBean<?> ownerBean, AnnotatedMethod<T> annotatedObserverMethod, AnnotatedParameter<T> annotatedObservesParameter)
    {
        this(ownerBean, annotatedObserverMethod, annotatedObservesParameter, true);
    }

    protected ObserverMethodImpl(AbstractOwbBean<?> ownerBean, AnnotatedMethod<T> annotatedObserverMethod, AnnotatedParameter<T> annotatedObservesParameter, boolean fireEvent)
    {
        this.ownerBean = ownerBean;
        this.annotatedObservesParameter = annotatedObservesParameter;
        this.annotatedObserverMethod = annotatedObserverMethod;
        observedEventType = annotatedObservesParameter.getBaseType();
        Observes observes = annotatedObservesParameter.getAnnotation(Observes.class);

        Class<? extends Annotation> observerAnnotation;

        if (observes != null)
        {
            ifExist = observes.notifyObserver() == Reception.IF_EXISTS;
            phase = observes.during();
            observerAnnotation = Observes.class;
        }
        else
        {
            ObservesAsync observesAsync = annotatedObservesParameter.getAnnotation(ObservesAsync.class);
            ifExist = observesAsync.notifyObserver() == Reception.IF_EXISTS;
            phase = TransactionPhase.IN_PROGRESS;
            observerAnnotation = ObservesAsync.class;

            isAsync = true;
        }

        observedQualifiers = new HashSet<>();
        for (Annotation annotation: annotatedObservesParameter.getAnnotations())
        {
            if (ownerBean.getWebBeansContext().getAnnotationManager().isQualifierAnnotation(annotation.annotationType()))
            {
                observedQualifiers.add(annotation);
            }
        }

        // detect the Priority
        Priority priorityAnn = annotatedObservesParameter.getAnnotation(Priority.class);
        if (priorityAnn != null)
        {
            priority = priorityAnn.value();
        }
        
        OpenWebBeansEjbPlugin ejbPlugin = getWebBeansContext().getPluginLoader().getEjbPlugin();
        if (ejbPlugin != null && ejbPlugin.isNewSessionBean(ownerBean.getBeanClass()))
        {
            view = ejbPlugin.resolveViewMethod(ownerBean , annotatedObserverMethod.getJavaMember());
        }
        else
        {
            view = annotatedObserverMethod.getJavaMember();
        }

        injectionPoints = new LinkedHashSet<>();
        for (AnnotatedParameter<?> parameter: annotatedObserverMethod.getParameters())
        {
            if (!parameter.isAnnotationPresent(observerAnnotation))
            {
                injectionPoints.add(getWebBeansContext().getInjectionPointFactory().buildInjectionPoint(ownerBean, parameter, fireEvent));
            }
        }

        checkObserverCondition(annotatedObservesParameter);

        if (!view.isAccessible())
        {
            ownerBean.getWebBeansContext().getSecurityService().doPrivilegedSetAccessible(view, true);
        }
    }

    protected void checkObserverCondition(AnnotatedParameter<T> annotatedObservesParameter)
    {
        if (annotatedObservesParameter.getAnnotation(WithAnnotations.class) != null)
        {
            throw new WebBeansConfigurationException("@WithAnnotations must only be used for ProcessAnnotatedType events");
        }
    }

    @Override
    public AbstractOwbBean<?> getOwnerBean()
    {
        return ownerBean;
    }

    @Override
    public boolean isAsync()
    {
        return isAsync;
    }

    @Override
    public int getPriority()
    {
        return priority;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<InjectionPoint> getInjectionPoints()
    {
        return injectionPoints;
    }

    /**
     * {@inheritDoc}
     *
     * @deprecated actually. Use the method with the EventContext instead
     */
    @Override
    public void notify(T event)
    {
        notify(new EventContextImpl<>(event, null));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public void notify(EventContext<T> eventContext)
    {
        T event = eventContext.getEvent();
        EventMetadata metadata = eventContext.getMetadata();

        AbstractOwbBean<Object> component = (AbstractOwbBean<Object>) ownerBean;
        if (!ownerBean.isEnabled())
        {
            return;
        }

        Object object = null;
        
        List<ObserverParams> methodArgsMap = getMethodArguments(event, metadata);
        
        BeanManagerImpl manager = ownerBean.getWebBeansContext().getBeanManagerImpl();
        CreationalContextImpl<Object> creationalContext = manager.createCreationalContext(component);
        if (metadata != null)
        {
            creationalContext.putInjectionPoint(metadata.getInjectionPoint());
            creationalContext.putEventMetadata(metadata);
        }
        
        ObserverParams[] obargs = null;
        try
        {
            Object[] args;
            if (methodArgsMap == null)
            {
                args = new Object[]{event};
            }
            else
            {
                args = new Object[methodArgsMap.size()];
                obargs = new ObserverParams[args.length];
                obargs = methodArgsMap.toArray(obargs);
                int i = 0;
                for (ObserverParams param : obargs)
                {
                    args[i++] = param.instance;
                }
            }

            //Static or not
            if (Modifier.isStatic(view.getModifiers()))
            {
                //Invoke Method
                view.invoke(null, args);
            }
            else
            {
                Context context;
                try
                {
                    context = manager.getContext(component.getScope());
                }
                catch (ContextNotActiveException cnae)
                {
                    if (ifExist)
                    {
                        return;
                    }
                    // this may happen if we try to e.g. send an event to a @ConversationScoped bean from a ServletListener
                    logger.log(Level.INFO, OWBLogConst.INFO_0010, ownerBean);
                    return;
                }
                

                // on Reception.IF_EXISTS: ignore this bean if a the contextual instance doesn't already exist
                object = context.get(component);

                if (ifExist && object == null)
                {
                    return;
                }

                if (object == null)
                {
                    object = context.get(component, creationalContext);
                }

                if (object == null)
                {
                    // this might happen for EJB components.
                    Type t = component.getBeanClass();

                    // If the bean is an EJB, its beanClass may not be one of
                    // its types. Instead pick a local interface
                    if (component.getWebBeansType() == WebBeansType.ENTERPRISE)
                    {
                        t = (Type) component.getTypes().toArray()[0];
                    }

                    object = manager.getReference(component, t, creationalContext);

                }

                if (object != null)
                {
                    if (Modifier.isPrivate(view.getModifiers()))
                    {
                        // since private methods cannot be intercepted, we have to unwrap any possible proxy
                        if (object instanceof OwbNormalScopeProxy)
                        {
                            object = getWebBeansContext().getInterceptorDecoratorProxyFactory().unwrapInstance(object);
                        }
                    }

                    //Invoke Method
                    invoke(object, args);
                }
            }                        
        }
        catch (InvocationTargetException ite)
        {
            throw new WebBeansException(ite.getCause());
        }
        catch (Exception e)
        {
            throw new WebBeansException(e);
        }
        finally
        {
            creationalContext.removeEventMetadata();
            creationalContext.removeInjectionPoint();
            //Destory bean instance
            if (component.getScope().equals(Dependent.class) && object != null)
            {
                component.destroy(object, creationalContext);
            }
            
            //Destroy observer method dependent instances
            if(methodArgsMap != null && obargs != null)
            {
                for(ObserverParams param : obargs)
                {
                    if(param.isBean && param.bean.getScope().equals(Dependent.class))
                    {
                        param.bean.destroy(param.instance, param.creational);
                    }
                }
            }
        }

    }

    protected void invoke(Object object, Object[] args) throws IllegalAccessException, InvocationTargetException
    {
        view.invoke(object, args);
    }

    /**
     * Gets observer method parameters.
     * @param event event payload
     * @return observer method parameters
     */
    protected List<ObserverParams> getMethodArguments(Object event, EventMetadata metadata)
    {
        if (injectionPoints.isEmpty() && annotatedObservesParameter.getPosition() == 0)
        {
            return null; // special handling
        }

        List<ObserverParams> list = new ArrayList<>();
        if (annotatedObservesParameter.getPosition() == 0)
        {
            ObserverParams param = new ObserverParams();
            param.instance = event;
            list.add(param);
        }
        WebBeansContext webBeansContext = ownerBean.getWebBeansContext();
        BeanManagerImpl manager = webBeansContext.getBeanManagerImpl();

        for (InjectionPoint injectionPoint: injectionPoints)
        {
            Bean<Object> injectedBean = (Bean<Object>)manager.getInjectionResolver().getInjectionPointBean(injectionPoint);
            
            CreationalContextImpl<Object> creational = manager.createCreationalContext(injectedBean);
            creational.putInjectionPoint(metadata.getInjectionPoint());
            creational.putInjectionPoint(injectionPoint);
            creational.putEventMetadata(metadata);
            Object instance;
            try
            {
                instance = manager.getReference(injectedBean, null, creational);
            }
            finally
            {
                creational.removeEventMetadata();
                creational.removeInjectionPoint();
                creational.removeInjectionPoint();
            }
                                
            ObserverParams param = new ObserverParams();
            param.isBean = true;
            param.creational = creational;
            param.instance = instance;
            param.bean = injectedBean;
            list.add(param);

            if (list.size() == annotatedObservesParameter.getPosition())
            {
                param = new ObserverParams();
                param.instance = event;
                list.add(param);                 
            }
        }
                
        return list;
    }

    /**
     * Returns observer owner bean.
     * 
     * @return the bean
     */
    @Override
    @SuppressWarnings("unchecked")
    public Class<?> getBeanClass()
    {
        return ownerBean.getBeanClass();
    }

    /** 
     * {@inheritDoc}
     */
    @Override
    public Set<Annotation> getObservedQualifiers()
    {
        return observedQualifiers;
    }
    
    /** 
     * {@inheritDoc}
     */
    @Override
    public Type getObservedType()
    {
        return observedEventType;
    }

    /** 
     * {@inheritDoc}
     */
    @Override
    public Reception getReception()
    {
        return ifExist ? Reception.IF_EXISTS : Reception.ALWAYS;
    }

    @Override
    public TransactionPhase getTransactionPhase()
    {
        return phase;
    }
    
    public AnnotatedMethod<T> getObserverMethod()
    {
        return annotatedObserverMethod;
    }

    protected WebBeansContext getWebBeansContext()
    {
        return ownerBean.getWebBeansContext();
    }
    
    /**
     * Provides a way to set the observer method. This may need to be done for
     * EJBs so that the method used will be from an interface and not the
     * EJB class that likely can not be invoked on the EJB proxy
     * 
     * @param m method to be invoked as the observer
     */
    public void setObserverMethod(AnnotatedMethod<T> m)
    {
        annotatedObserverMethod = m;
    }
}
