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
import java.util.List;
import java.util.Set;

import javax.enterprise.inject.spi.AnnotatedConstructor;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.ProcessInjectionTarget;

import org.apache.webbeans.component.ManagedBean;
import org.apache.webbeans.component.WebBeansType;
import org.apache.webbeans.config.OWBLogConst;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.decorator.DecoratorUtil;
import org.apache.webbeans.decorator.WebBeansDecoratorConfig;
import org.apache.webbeans.exception.inject.DeploymentException;
import org.apache.webbeans.inject.impl.InjectionPointFactory;
import org.apache.webbeans.logger.WebBeansLoggerFacade;
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


    public ManagedBean<T> defineManagedBean(AnnotatedType<T> annotatedType)
    {
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

        return getBean();
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
            component = defineManagedBean(annotatedType);

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
                delegate = defineManagedBean(processInjectionTargetEvent.getAnnotatedType());
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

        ManagedBean<T> bean = defineManagedBean(processInjectionTargetEvent.getAnnotatedType());
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
