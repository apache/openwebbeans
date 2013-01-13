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
package org.apache.webbeans.component.creation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.enterprise.inject.spi.AnnotatedConstructor;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.ObserverMethod;
import javax.enterprise.inject.spi.ProcessInjectionTarget;
import javax.enterprise.inject.spi.Producer;

import org.apache.webbeans.component.ManagedBean;
import org.apache.webbeans.component.ProducerFieldBean;
import org.apache.webbeans.component.ProducerMethodBean;
import org.apache.webbeans.component.WebBeansType;
import org.apache.webbeans.config.OWBLogConst;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.container.BeanManagerImpl;
import org.apache.webbeans.decorator.DecoratorUtil;
import org.apache.webbeans.decorator.WebBeansDecoratorConfig;
import org.apache.webbeans.event.ObserverMethodImpl;
import org.apache.webbeans.exception.inject.DeploymentException;
import org.apache.webbeans.inject.impl.InjectionPointFactory;
import org.apache.webbeans.logger.WebBeansLoggerFacade;
import org.apache.webbeans.portable.creation.InjectionTargetProducer;
import org.apache.webbeans.portable.events.ProcessBeanImpl;
import org.apache.webbeans.portable.events.ProcessProducerImpl;
import org.apache.webbeans.portable.events.generics.GProcessManagedBean;
import org.apache.webbeans.util.WebBeansUtil;

/**
 * Bean builder for <i>Managed Beans</i>. A <i>ManagedBean</i> is a class
 * which gets scanned and picked up as {@link javax.enterprise.inject.spi.Bean}.
 * 
 * @version $Rev$ $Date$
 *
 * @param <T> bean type info
 */
public class ManagedBeanBuilder<T, M extends ManagedBean<T>> extends AbstractInjectionTargetBeanBuilder<T, M>
{
    private AnnotatedConstructor<T> constructor;
    
    /**
     * Creates a new creator.
     */
    public ManagedBeanBuilder(WebBeansContext webBeansContext, AnnotatedType<T> annotatedType)
    {
        super(webBeansContext, annotatedType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void checkCreateConditions()
    {
        webBeansContext.getWebBeansUtil().checkManagedBeanCondition(getAnnotated());
        WebBeansUtil.checkGenericType(getBeanType(), getScope());
        //Check Unproxiable
        checkUnproxiableApiType();
    }


    /**
     * {@inheritDoc}
     */
    public void defineConstructor()
    {
        constructor = getBeanConstructor();
    }

    /**
     * {@inheritDoc}
     */
    public M getBean()
    {
        M bean = super.getBean();
        addConstructorInjectionPointMetaData(bean);
        return bean;
    }


    public ManagedBean<T> defineManagedBean(ProcessInjectionTarget<T> processInjectionTargetEvent)
    {
        //Annotated type
        AnnotatedType<T> annotatedType = processInjectionTargetEvent.getAnnotatedType();

        Class<T> clazz = annotatedType.getJavaClass();

        defineApiType();

        //Define meta-data
        defineStereoTypes();
        //Scope type
        defineScopeType(WebBeansLoggerFacade.getTokenString(OWBLogConst.TEXT_MB_IMPL) + clazz.getName() +
                WebBeansLoggerFacade.getTokenString(OWBLogConst.TEXT_SAME_SCOPE));

        //Check for Enabled via Alternative
        defineEnabled();

        checkCreateConditions();
        defineName();
        defineQualifiers();

        defineConstructor();
        defineInjectedFields();
        defineInjectedMethods();
        defineDisposalMethods();

        Set<ObserverMethod<?>> observerMethods = new HashSet<ObserverMethod<?>>();
        ManagedBean<T> managedBean = getBean();
        ((InjectionTargetProducer)processInjectionTargetEvent.getInjectionTarget()).setBean(managedBean);
        if(isEnabled())
        {
            observerMethods = defineObserverMethods(managedBean);
        }
        Set<ProducerMethodBean<?>> producerMethods = defineProducerMethods(managedBean);
        Set<ProducerFieldBean<?>> producerFields = defineProducerFields(managedBean);

        //Put final InjectionTarget instance
        managedBean.setProducer(processInjectionTargetEvent.getInjectionTarget());

        Map<ProducerMethodBean<?>,AnnotatedMethod<?>> annotatedMethods =
                new HashMap<ProducerMethodBean<?>, AnnotatedMethod<?>>();

        for(ProducerMethodBean<?> producerMethod : producerMethods)
        {
            AnnotatedMethod<?> method = webBeansContext.getAnnotatedElementFactory().newAnnotatedMethod(producerMethod.getCreatorMethod(), annotatedType);
            ProcessProducerImpl<?, ?> producerEvent = webBeansContext.getWebBeansUtil().fireProcessProducerEventForMethod(producerMethod,
                                                                                                    method);
            webBeansContext.getWebBeansUtil().inspectErrorStack("There are errors that are added by ProcessProducer event observers for "
                                           + "ProducerMethods. Look at logs for further details");

            annotatedMethods.put(producerMethod, method);
            producerMethod.setProducer((Producer) producerEvent.getProducer());
        }

        Map<ProducerFieldBean<?>,AnnotatedField<?>> annotatedFields =
                new HashMap<ProducerFieldBean<?>, AnnotatedField<?>>();

        for(ProducerFieldBean<?> producerField : producerFields)
        {
            AnnotatedField<?> field = webBeansContext.getAnnotatedElementFactory().newAnnotatedField(producerField.getCreatorField(), annotatedType);
            ProcessProducerImpl<?, ?> producerEvent = webBeansContext.getWebBeansUtil().fireProcessProducerEventForField(producerField, field);
            webBeansContext.getWebBeansUtil().inspectErrorStack("There are errors that are added by ProcessProducer event observers for"
                                           + " ProducerFields. Look at logs for further details");

            annotatedFields.put(producerField, field);
            producerField.setProducer((Producer) producerEvent.getProducer());
        }

        Map<ObserverMethod<?>,AnnotatedMethod<?>> observerMethodsMap =
                new HashMap<ObserverMethod<?>, AnnotatedMethod<?>>();

        for(ObserverMethod<?> observerMethod : observerMethods)
        {
            ObserverMethodImpl<?> impl = (ObserverMethodImpl<?>)observerMethod;
            AnnotatedMethod<?> method = webBeansContext.getAnnotatedElementFactory().newAnnotatedMethod(impl.getObserverMethod(), annotatedType);

            observerMethodsMap.put(observerMethod, method);
        }

        BeanManagerImpl beanManager = webBeansContext.getBeanManagerImpl();

        //Fires ProcessManagedBean
        ProcessBeanImpl<T> processBeanEvent = new GProcessManagedBean(managedBean, annotatedType);
        beanManager.fireEvent(processBeanEvent);
        webBeansContext.getWebBeansUtil().inspectErrorStack("There are errors that are added by ProcessManagedBean event observers for " +
                "managed beans. Look at logs for further details");

        //Fires ProcessProducerMethod
        webBeansContext.getWebBeansUtil().fireProcessProducerMethodBeanEvent(annotatedMethods, annotatedType);
        webBeansContext.getWebBeansUtil().inspectErrorStack("There are errors that are added by ProcessProducerMethod event observers for " +
                "producer method beans. Look at logs for further details");

        //Fires ProcessProducerField
        webBeansContext.getWebBeansUtil().fireProcessProducerFieldBeanEvent(annotatedFields);
        webBeansContext.getWebBeansUtil().inspectErrorStack("There are errors that are added by ProcessProducerField event observers for " +
                "producer field beans. Look at logs for further details");

        //Fire ObservableMethods
        webBeansContext.getWebBeansUtil().fireProcessObservableMethodBeanEvent(observerMethodsMap);
        webBeansContext.getWebBeansUtil().inspectErrorStack("There are errors that are added by ProcessObserverMethod event observers for " +
                "observer methods. Look at logs for further details");

        if(!webBeansContext.getWebBeansUtil().isAnnotatedTypeDecoratorOrInterceptor(annotatedType))
        {
            beanManager.addBean(managedBean);
            for (ProducerMethodBean<?> producerMethod : producerMethods)
            {
                // add them one after the other to enable serialization handling et al
                beanManager.addBean(producerMethod);
            }
            validateDisposalMethods(managedBean);//Define disposal method after adding producers
            for (ProducerFieldBean<?> producerField : producerFields)
            {
                // add them one after the other to enable serialization handling et al
                beanManager.addBean(producerField);
            }
        }

        return managedBean;
    }

    /**
     * @deprecated replaced via the various {@link InterceptorBeanBuilder}s
     */
    public ManagedBean<T> defineInterceptor(ProcessInjectionTarget<T> injectionTargetEvent)
    {
        Class<?> clazz = injectionTargetEvent.getAnnotatedType().getJavaClass();
        AnnotatedType annotatedType = injectionTargetEvent.getAnnotatedType();

        if (webBeansContext.getInterceptorsManager().isInterceptorClassEnabled(clazz))
        {
            ManagedBean<T> component;

            webBeansContext.getInterceptorUtil().checkInterceptorConditions(annotatedType);
            component = defineManagedBean(injectionTargetEvent);

            if (component != null)
            {
                Annotation[] anns = annotatedType.getAnnotations().toArray(new Annotation[annotatedType.getAnnotations().size()]);
                webBeansContext.getWebBeansInterceptorConfig().configureInterceptorClass(component,
                        webBeansContext.getAnnotationManager().getInterceptorBindingMetaAnnotations(anns));
            }
            else
            {
                // TODO could probably be a bit more descriptive
                throw new DeploymentException("Cannot create Interceptor for class" + injectionTargetEvent.getAnnotatedType());
            }
            return component;
        }
        else
        {
            return null;
        }
    }

    protected void addConstructorInjectionPointMetaData(ManagedBean<T> bean)
    {
        if (constructor == null)
        {
            return;
        }
        InjectionPointFactory injectionPointFactory = webBeansContext.getInjectionPointFactory();
        List<InjectionPoint> injectionPoints = injectionPointFactory.getConstructorInjectionPointData(bean, constructor);
        for (InjectionPoint injectionPoint : injectionPoints)
        {
            addImplicitComponentForInjectionPoint(injectionPoint);
            bean.addInjectionPoint(injectionPoint);
        }
        bean.setConstructor(constructor.getJavaMember());
    }

    /**
     * Define decorator bean.
     * @param processInjectionTargetEvent
     */
    public ManagedBean<T> defineDecorator(ProcessInjectionTarget<T> processInjectionTargetEvent)
    {
        Class<T> clazz = processInjectionTargetEvent.getAnnotatedType().getJavaClass();
        if (webBeansContext.getDecoratorsManager().isDecoratorEnabled(clazz))
        {
            ManagedBean<T> delegate = null;

            DecoratorUtil.checkDecoratorConditions(clazz);

            if(Modifier.isAbstract(clazz.getModifiers()))
            {
                delegate = defineAbstractDecorator(processInjectionTargetEvent);
            }
            else
            {
                delegate = defineManagedBean(processInjectionTargetEvent);
            }

            if (delegate != null)
            {
                WebBeansDecoratorConfig.configureDecoratorClass(delegate);
            }
            else
            {
                // TODO could probably be a bit more descriptive
                throw new DeploymentException("Cannot create Decorator for class" + processInjectionTargetEvent.getAnnotatedType());
            }
            return delegate;
        }
        else
        {
            return null;
        }
    }

    private ManagedBean<T> defineAbstractDecorator(ProcessInjectionTarget<T> processInjectionTargetEvent)
    {

        ManagedBean<T> bean = defineManagedBean(processInjectionTargetEvent);
        if (bean == null)
        {
            // TODO could probably be a bit more descriptive
            throw new DeploymentException("Cannot create ManagedBean for class" + processInjectionTargetEvent.getAnnotatedType());
        }

        //X TODO move proxy instance creation into JavassistProxyFactory!
        Class clazz = webBeansContext.getProxyFactory().createAbstractDecoratorProxyClass(bean);

        bean.setConstructor(webBeansContext.getWebBeansUtil().defineConstructor(clazz));
        bean.setIsAbstractDecorator(true);
        return bean;
    }

    @Override
    protected M createBean(Set<Type> types,
                           Set<Annotation> qualifiers,
                           Class<? extends Annotation> scope,
                           String name,
                           boolean nullable,
                           Class<T> beanClass,
                           Set<Class<? extends Annotation>> stereotypes,
                           boolean alternative,
                           boolean enabled)
    {
        M managedBean = (M)new ManagedBean<T>(webBeansContext, WebBeansType.MANAGED, getAnnotated(), types, qualifiers, scope, name, beanClass, stereotypes, alternative);
        managedBean.setEnabled(enabled);
        return managedBean;
    }
}
