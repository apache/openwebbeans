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
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.ContextNotActiveException;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.Context;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Observes;
import javax.enterprise.event.Reception;
import javax.enterprise.event.TransactionPhase;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.ObserverMethod;

import org.apache.webbeans.annotation.AnnotationManager;
import org.apache.webbeans.annotation.DefaultLiteral;
import org.apache.webbeans.component.AbstractOwbBean;
import org.apache.webbeans.component.AbstractInjectionTargetBean;
import org.apache.webbeans.component.InjectionPointBean;
import org.apache.webbeans.component.InjectionTargetBean;
import org.apache.webbeans.config.OWBLogConst;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.container.BeanManagerImpl;
import org.apache.webbeans.exception.WebBeansException;
import org.apache.webbeans.inject.impl.InjectionPointFactory;
import org.apache.webbeans.logger.WebBeansLogger;
import org.apache.webbeans.portable.AnnotatedElementFactory;
import org.apache.webbeans.util.AnnotationUtil;
import org.apache.webbeans.util.ClassUtil;
import org.apache.webbeans.util.SecurityUtil;
import org.apache.webbeans.util.WebBeansUtil;

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
 * and event qualifier is <code>Current</code>. Whenever event is fired, its {@link ObserverMethod#notify()}
 * method is called.
 * </p>
 * 
 * @version $Rev$ $Date$
 *
 * @param <T> event type
 */
public class ObserverMethodImpl<T> implements ObserverMethod<T>
{
    /**Logger instance*/
    private final WebBeansLogger logger = WebBeansLogger.getLogger(ObserverMethodImpl.class);

    /**Observer owner bean that defines observer method*/
    private final InjectionTargetBean<?> bean;

    /**Event observer method*/
    private final Method observerMethod;

    /**Using existing bean instance or not*/
    private final boolean ifExist;
    
    /** the observed qualifiers */
    private final Set<Annotation> observedQualifiers;

    /** the type of the observed event */
    private final Type observedEventType;
    
    /** the transaction phase */
    private final TransactionPhase phase;
    
    /**Annotated method*/
    private AnnotatedMethod<T> annotatedMethod = null;
    
    private static class ObserverParams
    {
        private Bean<Object> bean;
        
        private Object instance;
        
        private CreationalContext<Object> creational;
        
        private boolean isBean = false;
    }
    
    /**
     * Creates a new bean observer instance.
     * 
     * @param bean owner
     * @param observerMethod method
     * @param ifExist if exist parameter
     */
    public ObserverMethodImpl(InjectionTargetBean<?> bean, Method observerMethod, boolean ifExist)
    {
        this.bean = bean;
        this.observerMethod = observerMethod;
        this.ifExist = ifExist;

        Annotation[] qualifiers =
            getWebBeansContext().getAnnotationManager().getMethodFirstParameterQualifierWithGivenAnnotation(
                observerMethod, Observes.class);
        getWebBeansContext().getAnnotationManager().checkQualifierConditions(qualifiers);
        this.observedQualifiers = new HashSet<Annotation>(qualifiers.length);
        
        for (Annotation qualifier : qualifiers)
        {
            observedQualifiers.add(qualifier);
        }
        
        this.observedEventType = AnnotationUtil.getTypeOfParameterWithGivenAnnotation(observerMethod, Observes.class);
        
        this.phase = EventUtil.getObserverMethodTransactionType(observerMethod);
    }

    /**
     * used if the qualifiers and event type are already known, e.g. from the XML.
     * @param bean
     * @param observerMethod
     * @param ifExist
     * @param qualifiers
     * @param observedEventType
     */
    public ObserverMethodImpl(InjectionTargetBean<?> bean, Method observerMethod, boolean ifExist,
                                 Annotation[] qualifiers, Type observedEventType)
    {
        this.bean = bean;
        this.observerMethod = observerMethod;
        this.ifExist = ifExist;
        this.observedQualifiers = new HashSet<Annotation>(qualifiers.length);
        for (Annotation qualifier : qualifiers)
        {
            observedQualifiers.add(qualifier);
        }
        this.observedEventType = observedEventType;
        this.phase = EventUtil.getObserverMethodTransactionType(observerMethod);

    }
    
    /**
     * Sets annotated method.
     * @param annotatedMethod annotated method
     */
    public void setAnnotatedMethod(AnnotatedMethod<T> annotatedMethod)
    {
        this.annotatedMethod = annotatedMethod;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public void notify(T event)
    {
        logger.trace("Notifying with event payload : [{0}]", event);
        
        AbstractOwbBean<Object> baseComponent = (AbstractOwbBean<Object>) bean;
        AbstractOwbBean<Object> specializedComponent = null;
        Object object = null;
        
        CreationalContext<Object> creationalContext = null;
        List<ObserverParams> methodArgsMap = null;
        if(annotatedMethod == null)
        {
            methodArgsMap = getMethodArguments(event);
        }
        else
        {
            methodArgsMap = getAnnotatedMethodArguments(event);
        }
        
        ObserverParams[] obargs = null;
        try
        {
            boolean isPrivateMethod = !this.observerMethod.isAccessible();
            if (isPrivateMethod)
            {
                SecurityUtil.doPrivilegedSetAccessible(observerMethod, true);
            }

            obargs = new ObserverParams[methodArgsMap.size()];
            obargs = methodArgsMap.toArray(obargs);
            Object[] args = new Object[obargs.length];
            int i = 0;
            for(ObserverParams param : obargs)
            {
                args[i++] = param.instance;
            }
            
            //Static or not
            if (Modifier.isStatic(this.observerMethod.getModifiers()))
            {
                //Invoke Method
                this.observerMethod.invoke(object, args);
            }
            else
            {
                BeanManagerImpl manager = WebBeansContext.getInstance().getBeanManagerImpl();
                specializedComponent = (AbstractOwbBean<Object>)WebBeansUtil.getMostSpecializedBean(manager, baseComponent);        
                Context context = null;
                try
                {
                    context = manager.getContext(specializedComponent.getScope());
                }
                catch (ContextNotActiveException cnae)
                {
                    // this may happen if we try to e.g. send an event to a @ConversationScoped bean from a ServletListener
                    logger.info(OWBLogConst.INFO_0010, bean);
                    return;
                }
                

                // on Reception.IF_EXISTS: ignore this bean if a the contextual instance doesn't already exist
                object = context.get(specializedComponent);

                if (ifExist && object == null)
                {
                    return;
                }

                creationalContext = manager.createCreationalContext(specializedComponent);

                if (isPrivateMethod)
                {
                    // since private methods cannot be intercepted, we can just call them directly
                    // so we get the contextual instance directly from the context because we do not
                    // proxy private methods (thus the invocation on the contextual reference would fail)
                    if (object == null)
                    {
                        object = context.get(specializedComponent, creationalContext);
                    }
                }
                else
                {
                    // on Reception.ALWAYS we must get a contextual reference if we didn't find the contextual instance
                    // we need to pick the contextual reference because of section 7.2:
                    //  "Invocations of producer, disposer and observer methods by the container are
                    //  business method invocations and are in- tercepted by method interceptors and decorators."
                    object = manager.getReference(specializedComponent, specializedComponent.getBeanClass(), creationalContext);
                }

                if (object != null)
                {
                    //Invoke Method
                    this.observerMethod.invoke(object, args);
                }
            }                        
        }
        catch (Exception e)
        {
                throw new WebBeansException(e);
        }
        finally
        {
            //Destory bean instance
            if (baseComponent.getScope().equals(Dependent.class) && object != null)
            {
                baseComponent.destroy(object,creationalContext);
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

    /**
     * Returns list of observer method parameters.
     * 
     * @param event event instance
     * @return list of observer method parameters
     */
    @SuppressWarnings("unchecked")
    protected List<ObserverParams> getMethodArguments(Object event)
    {
        WebBeansContext webBeansContext = WebBeansContext.getInstance();
        AnnotatedElementFactory annotatedElementFactory = webBeansContext.getAnnotatedElementFactory();
        AnnotationManager annotationManager = webBeansContext.getAnnotationManager();

        //Define annotated parameter
        AnnotatedType<T> annotatedType = (AnnotatedType<T>) annotatedElementFactory.newAnnotatedType(this.bean.getReturnType());
        AnnotatedMethod<T> annotatedMethod = (AnnotatedMethod<T>)annotatedElementFactory.newAnnotatedMethod(this.observerMethod, annotatedType);
        
        Type[] types = this.observerMethod.getGenericParameterTypes();
        Annotation[][] annots = this.observerMethod.getParameterAnnotations();
        List<ObserverParams> list = new ArrayList<ObserverParams>();

        BeanManagerImpl manager = webBeansContext.getBeanManagerImpl();
        ObserverParams param = null;
        if (types.length > 0)
        {
            int i = 0;
            for (Type type : types)
            {
                Annotation[] annot = annots[i];

                boolean observesAnnotation = false;

                if (annot.length == 0)
                {
                    annot = new Annotation[1];
                    annot[0] = new DefaultLiteral();
                }
                else
                {
                    for (Annotation observersAnnot : annot)
                    {
                        if (observersAnnot.annotationType().equals(Observes.class))
                        {
                            param = new ObserverParams();
                            param.instance = event;
                            list.add(param); 
                            observesAnnotation = true;
                            break;
                        }
                    }
                }

                if (!observesAnnotation)
                {
                    //Get parameter annotations
                    Annotation[] bindingTypes = annotationManager.getQualifierAnnotations(annot);
                    
                    //Annotated parameter
                    AnnotatedParameter<T> annotatedParameter = annotatedMethod.getParameters().get(i);
                    
                    //Creating injection point
                    InjectionPoint point = InjectionPointFactory.getPartialInjectionPoint(this.bean, type, this.observerMethod, annotatedParameter, bindingTypes);
                    
                    //Injected Bean
                    Bean<Object> injectedBean = (Bean<Object>)getWebBeansContext().getBeanManagerImpl().getInjectionResolver().getInjectionPointBean(point);
                    
                    //Set for @Inject InjectionPoint
                    if(WebBeansUtil.isDependent(injectedBean))
                    {
                        if(!InjectionPoint.class.isAssignableFrom(ClassUtil.getClass(point.getType())))
                        {
                            InjectionPointBean.local.set(point);   
                        }
                    }                           
                    
                    CreationalContext<Object> creational = manager.createCreationalContext(injectedBean);
                    Object instance = manager.getInstance(injectedBean, creational); 
                    
                    param = new ObserverParams();
                    param.isBean = true;
                    param.creational = creational;
                    param.instance = instance;
                    param.bean = injectedBean;
                    list.add(param);
                }
                
                i++;
            }
        }

        return list;
    }
    
    /**
     * Gets observer method parameters.
     * @param event event payload
     * @return observer method parameters
     */
    protected List<ObserverParams> getAnnotatedMethodArguments(Object event)
    {
        final WebBeansContext webBeansContext = WebBeansContext.getInstance();
        final AnnotationManager annotationManager = webBeansContext.getAnnotationManager();
        final BeanManagerImpl manager = webBeansContext.getBeanManagerImpl();
        List<ObserverParams> list = new ArrayList<ObserverParams>();
        List<AnnotatedParameter<T>> parameters = this.annotatedMethod.getParameters();
        ObserverParams param = null;
        for(AnnotatedParameter<T> parameter : parameters)
        {
            if(parameter.isAnnotationPresent(Observes.class))
            {
                param = new ObserverParams();
                param.instance = event;
                list.add(param);                 
            }
            else
            {
                //Get parameter annotations
                Annotation[] bindingTypes =
                    annotationManager.getQualifierAnnotations(AnnotationUtil.
                        getAnnotationsFromSet(parameter.getAnnotations()));

                InjectionPoint point = InjectionPointFactory.getPartialInjectionPoint(this.bean, parameter.getBaseType(), 
                        parameter.getDeclaringCallable().getJavaMember(), parameter, bindingTypes);

                //Get observer parameter instance
                @SuppressWarnings("unchecked")
                Bean<Object> injectedBean = (Bean<Object>)getWebBeansContext().getBeanManagerImpl().getInjectionResolver().getInjectionPointBean(point);

                //Set for @Inject InjectionPoint
                if(WebBeansUtil.isDependent(injectedBean))
                {
                    if(!InjectionPoint.class.isAssignableFrom(ClassUtil.getClass(point.getType())))
                    {
                        InjectionPointBean.local.set(point);   
                    }
                }                    
                
                CreationalContext<Object> creational = manager.createCreationalContext(injectedBean);
                Object instance = manager.getInstance(injectedBean, creational); 
                                    
                param = new ObserverParams();
                param.isBean = true;
                param.creational = creational;
                param.instance = instance;
                param.bean = injectedBean;
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
    @SuppressWarnings("unchecked")
    public Class<?> getBeanClass()
    {
        AbstractInjectionTargetBean<T> abs = (AbstractInjectionTargetBean<T>)this.bean;
        return abs.getBeanClass();
    }

    /** 
     * {@inheritDoc}
     */
    public Set<Annotation> getObservedQualifiers() 
    {
        return observedQualifiers;
    }
    
    /** 
     * {@inheritDoc}
     */
    public Type getObservedType() 
    {
        return observedEventType;
    }

    /** 
     * {@inheritDoc}
     */
    public Reception getReception() 
    {
        return ifExist ? Reception.IF_EXISTS : Reception.ALWAYS;
    }

    public TransactionPhase getTransactionPhase()
    {
        return phase;
    }
    
    public Method getObserverMethod()
    {
        return this.observerMethod;
    }

    protected WebBeansContext getWebBeansContext()
    {
        return bean.getWebBeansContext();
    }
}
