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
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.enterprise.inject.spi.AnnotatedConstructor;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.ObserverMethod;
import javax.enterprise.inject.spi.ProcessInjectionTarget;
import javax.enterprise.inject.spi.Producer;

import org.apache.webbeans.component.ManagedBean;
import org.apache.webbeans.component.ProducerFieldBean;
import org.apache.webbeans.component.ProducerMethodBean;
import org.apache.webbeans.config.OWBLogConst;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.container.BeanManagerImpl;
import org.apache.webbeans.decorator.DecoratorUtil;
import org.apache.webbeans.decorator.WebBeansDecoratorConfig;
import org.apache.webbeans.event.ObserverMethodImpl;
import org.apache.webbeans.exception.inject.DeploymentException;
import org.apache.webbeans.logger.WebBeansLoggerFacade;
import org.apache.webbeans.portable.events.ProcessBeanImpl;
import org.apache.webbeans.portable.events.ProcessProducerImpl;
import org.apache.webbeans.portable.events.generics.GProcessManagedBean;
import org.apache.webbeans.util.WebBeansAnnotatedTypeUtil;
import org.apache.webbeans.util.WebBeansUtil;

/**
 * Implementation of the {@link ManagedBeanCreator}.
 * 
 * @version $Rev$ $Date$
 *
 * @param <T> bean type info
 */
public class ManagedBeanCreatorImpl<T> extends AbstractInjecionTargetBeanCreator<T> implements ManagedBeanCreator<T>
{
    private final WebBeansContext webBeansContext;

    /**
     * Creates a new creator.
     */
    public ManagedBeanCreatorImpl(AnnotatedType<T> annotatedType, WebBeansContext webBeansContext)
    {
        this(new ManagedBean<T>(annotatedType.getJavaClass(), annotatedType, webBeansContext));
    }
    
    private ManagedBeanCreatorImpl(ManagedBean<T> managedBean)
    {
        super(managedBean);
        webBeansContext = managedBean.getWebBeansContext();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void checkCreateConditions()
    {
        webBeansContext.getWebBeansUtil().checkManagedBeanCondition(getAnnotatedType());
        WebBeansUtil.checkGenericType(getBean());
        //Check Unproxiable
        webBeansContext.getWebBeansUtil().checkUnproxiableApiType(getBean(), getBean().getScope());
    }


    /**
     * {@inheritDoc}
     */
    public void defineConstructor()
    {
        AnnotatedConstructor<T> annotated = WebBeansAnnotatedTypeUtil.getBeanConstructor(getAnnotatedType());
        Constructor<T> constructor = annotated.getJavaMember();
        webBeansContext.getAnnotatedTypeUtil().addConstructorInjectionPointMetaData(getBean(), annotated);
        getBean().setConstructor(constructor);
    }

    /**
     * {@inheritDoc}
     */
    public ManagedBean<T> getBean()
    {
        return (ManagedBean<T>)super.getBean();
    }

    public static <T> void lazyInitializeManagedBean(ManagedBean<T> bean)
    {
        ManagedBeanCreatorImpl<T> managedBeanCreator = new ManagedBeanCreatorImpl<T>(bean);

        managedBeanCreator.lazyInitializeManagedBean(bean.getBeanClass(), bean);
    }

    public void lazyInitializeManagedBean(Class<?> clazz, ManagedBean<?> managedBean)
    {
        defineConstructor();
        defineProducerMethods();
        defineProducerFields();
        defineInjectedFields();
        defineInjectedMethods();
        defineObserverMethods();
        webBeansContext.getDefinitionUtil().defineDecoratorStack(managedBean);
        webBeansContext.getDefinitionUtil().defineBeanInterceptorStack(managedBean);

        defineDisposalMethods(); //Define disposal method after adding producers
    }

    public ManagedBean<T> defineManagedBean(ProcessInjectionTarget<T> processInjectionTargetEvent, boolean allowLazyInit)
    {
        BeanManagerImpl manager = webBeansContext.getBeanManagerImpl();

        //Annotated type
        AnnotatedType<T> annotatedType = processInjectionTargetEvent.getAnnotatedType();
        ManagedBean<T> managedBean = getBean();

        Class<T> clazz = annotatedType.getJavaClass();

        defineApiType();

        //Define meta-data
        defineStereoTypes();
        //Scope type
        defineScopeType(WebBeansLoggerFacade.getTokenString(OWBLogConst.TEXT_MB_IMPL) + clazz.getName() +
                WebBeansLoggerFacade.getTokenString(OWBLogConst.TEXT_SAME_SCOPE), allowLazyInit);

        defineSerializable();

        //Check for Enabled via Alternative
        webBeansContext.getWebBeansUtil().setInjectionTargetBeanEnableFlag(managedBean);

        checkCreateConditions();
        defineName(WebBeansUtil.getManagedBeanDefaultName(clazz.getSimpleName()));
        defineQualifier();

        if (managedBean.isFullInit())
        {
            defineConstructor();
            Set<ProducerMethodBean<?>> producerMethods = defineProducerMethods();
            Set<ProducerFieldBean<?>> producerFields = defineProducerFields();
            defineInjectedFields();
            defineInjectedMethods();

            Set<ObserverMethod<?>> observerMethods = new HashSet<ObserverMethod<?>>();
            if(managedBean.isEnabled())
            {
                observerMethods = defineObserverMethods();
            }

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
                defineDisposalMethods();//Define disposal method after adding producers
                for (ProducerFieldBean<?> producerField : producerFields)
                {
                    // add them one after the other to enable serialization handling et al
                    beanManager.addBean(producerField);
                }
            }
        }
        else
        {
            // we still need to fire a ProcessManagedBean event, even for lazily initiated beans
            // (which most probably are no beans at all...)

            BeanManagerImpl beanManager = webBeansContext.getBeanManagerImpl();

            //Fires ProcessManagedBean
            ProcessBeanImpl<T> processBeanEvent = new GProcessManagedBean(managedBean, annotatedType);
            beanManager.fireEvent(processBeanEvent);
            webBeansContext.getWebBeansUtil().inspectErrorStack("There are errors that are added by ProcessManagedBean event observers for " +
                    "managed beans. Look at logs for further details");
            if(!webBeansContext.getWebBeansUtil().isAnnotatedTypeDecoratorOrInterceptor(annotatedType))
            {
                beanManager.addBean(managedBean);
            }
        }

        return managedBean;
    }

    public void defineInterceptor(ProcessInjectionTarget<T> injectionTargetEvent)
    {
        Class<?> clazz = injectionTargetEvent.getAnnotatedType().getJavaClass();
        AnnotatedType annotatedType = injectionTargetEvent.getAnnotatedType();

        if (webBeansContext.getInterceptorsManager().isInterceptorClassEnabled(clazz))
        {
            ManagedBean<T> component;

            webBeansContext.getInterceptorUtil().checkInterceptorConditions(annotatedType);
            component = defineManagedBean(injectionTargetEvent, false);

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
        }

    }

    /**
     * Define decorator bean.
     * @param processInjectionTargetEvent
     */
    public void defineDecorator(ProcessInjectionTarget<T> processInjectionTargetEvent)
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
                delegate = defineManagedBean(processInjectionTargetEvent, false);
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
        }
    }

    private ManagedBean<T> defineAbstractDecorator(ProcessInjectionTarget<T> processInjectionTargetEvent)
    {

        ManagedBean<T> bean = defineManagedBean(processInjectionTargetEvent, false);
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
}
