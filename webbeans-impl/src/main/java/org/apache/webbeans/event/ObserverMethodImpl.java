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
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.context.ContextNotActiveException;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.Context;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Observes;
import javax.enterprise.event.Reception;
import javax.enterprise.event.TransactionPhase;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.EventMetadata;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.WithAnnotations;

import org.apache.webbeans.component.AbstractOwbBean;
import org.apache.webbeans.component.WebBeansType;
import org.apache.webbeans.config.OWBLogConst;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.container.BeanManagerImpl;
import org.apache.webbeans.context.creational.CreationalContextImpl;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.exception.WebBeansException;
import org.apache.webbeans.inject.impl.InjectionPointFactory;
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
    private final AbstractOwbBean<?> bean;

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
    
    private static class ObserverParams
    {
        private Bean<Object> bean;
        
        private Object instance;
        
        private CreationalContext<Object> creational;
        
        private boolean isBean = false;
    }

    /**
     * used if the qualifiers and event type are already known, e.g. from the XML.
     * @param bean
     * @param annotatedObserverMethod
     */
    public ObserverMethodImpl(AbstractOwbBean<?> bean, AnnotatedMethod<T> annotatedObserverMethod, AnnotatedParameter<T> annotatedObservesParameter)
    {
        this.bean = bean;
        this.annotatedObservesParameter = annotatedObservesParameter;
        this.annotatedObserverMethod = annotatedObserverMethod;
        observedEventType = annotatedObservesParameter.getBaseType();
        Observes observes = annotatedObservesParameter.getAnnotation(Observes.class);
        ifExist = observes.notifyObserver() == Reception.IF_EXISTS;
        phase = observes.during();
        observedQualifiers = new HashSet<Annotation>();
        for (Annotation annotation: annotatedObservesParameter.getAnnotations())
        {
            if (bean.getWebBeansContext().getAnnotationManager().isQualifierAnnotation(annotation.annotationType()))
            {
                observedQualifiers.add(annotation);
            }
        }
        
        final OpenWebBeansEjbPlugin ejbPlugin = getWebBeansContext().getPluginLoader().getEjbPlugin();
        if (ejbPlugin != null && ejbPlugin.isNewSessionBean(bean.getBeanClass()))
        {
            view = ejbPlugin.resolveViewMethod(bean , annotatedObserverMethod.getJavaMember());
        }
        else
        {
            view = annotatedObserverMethod.getJavaMember();
        }

        injectionPoints = new LinkedHashSet<InjectionPoint>();
        for (AnnotatedParameter<?> parameter: annotatedObserverMethod.getParameters())
        {
            if (!parameter.isAnnotationPresent(Observes.class))
            {
                Collection<Annotation> qualifierAnnots = getWebBeansContext().getAnnotationManager().getQualifierAnnotations(parameter.getAnnotations());

                injectionPoints.add(InjectionPointFactory.getPartialInjectionPoint(bean, parameter, qualifierAnnots));
            }
        }

        checkObserverCondition(annotatedObservesParameter);
    }

    protected void checkObserverCondition(AnnotatedParameter<T> annotatedObservesParameter)
    {
        if (annotatedObservesParameter.getAnnotation(WithAnnotations.class) != null)
        {
            throw new WebBeansConfigurationException("@WithAnnotations must only be used for ProcessAnnotatedType events");
        }
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
     */
    @Override
    public void notify(T event)
    {
        notify(event, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public void notify(T event, EventMetadata metadata)
    {
        AbstractOwbBean<Object> component = (AbstractOwbBean<Object>) bean;
        if (!bean.isEnabled())
        {
            return;
        }

        Object object = null;
        
        List<ObserverParams> methodArgsMap = getMethodArguments(event, metadata);
        
        BeanManagerImpl manager = bean.getWebBeansContext().getBeanManagerImpl();
        CreationalContextImpl<Object> creationalContext = manager.createCreationalContext(component);
        if (metadata != null)
        {
            creationalContext.putInjectionPoint(metadata.getInjectionPoint());
            creationalContext.putEventMetadata(metadata);
        }
        
        ObserverParams[] obargs = null;
        try
        {
            obargs = new ObserverParams[methodArgsMap.size()];
            obargs = methodArgsMap.toArray(obargs);
            Object[] args = new Object[obargs.length];
            int i = 0;
            for(ObserverParams param : obargs)
            {
                args[i++] = param.instance;
            }

            //Static or not
            if (Modifier.isStatic(view.getModifiers()))
            {
                if (!view.isAccessible())
                {
                    view.setAccessible(true);
                }
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
                    // this may happen if we try to e.g. send an event to a @ConversationScoped bean from a ServletListener
                    logger.log(Level.INFO, OWBLogConst.INFO_0010, bean);
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
                    if (!view.isAccessible())
                    {
                        bean.getWebBeansContext().getSecurityService().doPrivilegedSetAccessible(view, true);
                    }

                    if (Modifier.isPrivate(view.getModifiers()))
                    {
                        // since private methods cannot be intercepted, we have to unwrap anny possible proxy
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
            if(methodArgsMap != null)
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

    protected void invoke(final Object object, final Object[] args) throws IllegalAccessException, InvocationTargetException
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
        List<ObserverParams> list = new ArrayList<ObserverParams>();
        if (annotatedObservesParameter.getPosition() == 0)
        {
            ObserverParams param = new ObserverParams();
            param.instance = event;
            list.add(param);
        }
        final WebBeansContext webBeansContext = bean.getWebBeansContext();
        final BeanManagerImpl manager = webBeansContext.getBeanManagerImpl();

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
        return bean.getBeanClass();
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
        return bean.getWebBeansContext();
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
