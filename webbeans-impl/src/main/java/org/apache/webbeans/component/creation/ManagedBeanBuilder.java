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
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.spi.AnnotatedConstructor;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.ObserverMethod;
import javax.enterprise.inject.spi.ProcessInjectionTarget;
import javax.enterprise.inject.spi.Producer;
import javax.inject.Inject;

import org.apache.webbeans.component.ManagedBean;
import org.apache.webbeans.component.ProducerFieldBean;
import org.apache.webbeans.component.ProducerMethodBean;
import org.apache.webbeans.config.OWBLogConst;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.container.BeanManagerImpl;
import org.apache.webbeans.decorator.DecoratorUtil;
import org.apache.webbeans.decorator.WebBeansDecoratorConfig;
import org.apache.webbeans.event.ObserverMethodImpl;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.exception.inject.DeploymentException;
import org.apache.webbeans.inject.impl.InjectionPointFactory;
import org.apache.webbeans.logger.WebBeansLoggerFacade;
import org.apache.webbeans.portable.events.ProcessBeanImpl;
import org.apache.webbeans.portable.events.ProcessProducerImpl;
import org.apache.webbeans.portable.events.generics.GProcessManagedBean;
import org.apache.webbeans.util.Asserts;
import org.apache.webbeans.util.WebBeansUtil;

/**
 * Bean builder for <i>Managed Beans</i>. A <i>ManagedBean</i> is a class
 * which gets scanned and picked up as {@link javax.enterprise.inject.spi.Bean}.
 * 
 * @version $Rev$ $Date$
 *
 * @param <T> bean type info
 */
public class ManagedBeanBuilder<T> extends AbstractInjectionTargetBeanBuilder<T>
{
    private final WebBeansContext webBeansContext;
    
    /**
     * Creates a new creator.
     */
    public ManagedBeanBuilder(WebBeansContext webBeansContext, AnnotatedType<T> annotatedType)
    {
        this(new ManagedBean<T>(webBeansContext, annotatedType.getJavaClass(), annotatedType), null);
    }

    protected ManagedBeanBuilder(ManagedBean<T> managedBean, Class<? extends Annotation> scopeType)
    {
        super(managedBean, scopeType);
        webBeansContext = managedBean.getWebBeansContext();
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
        addConstructorInjectionPointMetaData();
    }

    /**
     * {@inheritDoc}
     */
    public ManagedBean<T> getBean()
    {
        ManagedBean<T> bean = (ManagedBean<T>)super.getBean();
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

        defineSerializable();

        //Check for Enabled via Alternative
        defineEnabled();

        checkCreateConditions();
        defineName();
        defineQualifiers();

        defineConstructor();
        defineInjectedFields();
        defineInjectedMethods();

        Set<ObserverMethod<?>> observerMethods = new HashSet<ObserverMethod<?>>();
        if(isEnabled())
        {
            observerMethods = defineObserverMethods();
        }
        ManagedBean<T> managedBean = getBean();
        Set<ProducerMethodBean<?>> producerMethods = defineProducerMethods();
        Set<ProducerFieldBean<?>> producerFields = defineProducerFields();

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

        return managedBean;
    }

    /**
     * @deprecated replaced via the various {@link InterceptorBeanBuilder}s
     */
    public void defineInterceptor(ProcessInjectionTarget<T> injectionTargetEvent)
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
        }

    }
    
    protected AnnotatedConstructor<T> getBeanConstructor()
    {
        Asserts.assertNotNull(getAnnotated(),"Type is null");
        AnnotatedConstructor<T> result = null;
        
        Set<AnnotatedConstructor<T>> annConsts = getAnnotated().getConstructors();
        if(annConsts != null)
        {
            boolean found = false;
            boolean noParamConsIsDefined = false;
            for(AnnotatedConstructor<T> annConst : annConsts)
            {
                if(annConst.isAnnotationPresent(Inject.class))
                {
                    if (found)
                    {
                        throw new WebBeansConfigurationException("There are more than one constructor with @Inject annotation in annotation type : "
                                                                 + getAnnotated());
                    }
                    
                    found = true;
                    result = annConst;
                }
                else
                {
                    if(!found && !noParamConsIsDefined)
                    {
                        List<AnnotatedParameter<T>> parameters = annConst.getParameters();
                        if(parameters != null && parameters.isEmpty())
                        {
                            result = annConst;
                            noParamConsIsDefined = true;
                        }                        
                    }
                }
            }
        }
        
        if (result == null)
        {
            throw new WebBeansConfigurationException("No constructor is found for the annotated type : " + getAnnotated());
        }
        
        List<AnnotatedParameter<T>> parameters = result.getParameters();
        for(AnnotatedParameter<T> parameter : parameters)
        {
            if (parameter.isAnnotationPresent(Disposes.class))
            {
                throw new WebBeansConfigurationException("Constructor parameter annotations can not contain @Disposes annotation in annotated constructor : "
                                                         + result);
            }
            
            if(parameter.isAnnotationPresent(Observes.class))
            {
                throw new WebBeansConfigurationException("Constructor parameter annotations can not contain @Observes annotation in annotated constructor : " + result);
            }
            
        }

        return result;
    }
    
    protected void addConstructorInjectionPointMetaData()
    {
        InjectionPointFactory injectionPointFactory = webBeansContext.getInjectionPointFactory();
        AnnotatedConstructor<T> beanConstructor = getBeanConstructor();
        List<InjectionPoint> injectionPoints = injectionPointFactory.getConstructorInjectionPointData(getBean(), beanConstructor);
        for (InjectionPoint injectionPoint : injectionPoints)
        {
            addImplicitComponentForInjectionPoint(injectionPoint);
            getBean().addInjectionPoint(injectionPoint);
        }
        getBean().setConstructor(beanConstructor.getJavaMember());
    }

    public void addConstructorInjectionPointMetaData(Constructor<T> constructor)
    {
        List<InjectionPoint> injectionPoints = webBeansContext.getInjectionPointFactory().getConstructorInjectionPointData(getBean(), constructor);
        for (InjectionPoint injectionPoint : injectionPoints)
        {
            addImplicitComponentForInjectionPoint(injectionPoint);
            getBean().addInjectionPoint(injectionPoint);
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
}
