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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InjectionTarget;

import org.apache.webbeans.component.BeanAttributesImpl;
import org.apache.webbeans.component.InjectionTargetBean;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.portable.InjectionTargetImpl;
import org.apache.webbeans.util.Asserts;

/**
 * Abstract implementation of {@link AbstractBeanBuilder}.
 * 
 * @version $Rev$ $Date$
 *
 * @param <T> bean class type
 */
public abstract class AbstractInjectionTargetBeanBuilder<T, I extends InjectionTargetBean<T>>
{    
    
    protected final WebBeansContext webBeansContext;
    protected final AnnotatedType<T> annotatedType;
    protected final BeanAttributesImpl<T> beanAttributes;
    private boolean enabled = true;

    /**
     * Creates a new instance.
     * 
     */
    public AbstractInjectionTargetBeanBuilder(WebBeansContext webBeansContext, AnnotatedType<T> annotatedType, BeanAttributesImpl<T> beanAttributes)
    {
        Asserts.assertNotNull(webBeansContext, "webBeansContext may not be null");
        Asserts.assertNotNull(annotatedType, "annotated type may not be null");
        Asserts.assertNotNull(beanAttributes, "beanAttributes may not be null");
        this.webBeansContext = webBeansContext;
        this.annotatedType = annotatedType;
        this.beanAttributes = beanAttributes;
    }

    protected AnnotatedType<? super T> getSuperAnnotated()
    {
        Class<? super T> superclass = annotatedType.getJavaClass().getSuperclass();
        if (superclass == null)
        {
            return null;
        }
        return webBeansContext.getAnnotatedElementFactory().getAnnotatedType(superclass);
    }

    protected InjectionTarget<T> buildInjectionTarget(AnnotatedType<T> annotatedType,
                                                      Set<InjectionPoint> points,
                                                      WebBeansContext webBeansContext,
                                                      List<AnnotatedMethod<?>> postConstructMethods,
                                                      List<AnnotatedMethod<?>> preDestroyMethods)
    {
        InjectionTargetImpl<T> injectionTarget = new InjectionTargetImpl<T>(annotatedType, points, webBeansContext, postConstructMethods, preDestroyMethods);

        return injectionTarget;
    }

    protected abstract I createBean(Class<T> beanClass, boolean enabled);

    protected final I createBean(Class<T> beanClass)
    {
        I bean =  createBean(beanClass, enabled);

        //X TODO hack to set the InjectionTarget
        InjectionTarget<T> injectionTarget
                = buildInjectionTarget(bean.getAnnotatedType(), bean.getInjectionPoints(), webBeansContext, getPostConstructMethods(), getPreDestroyMethods());
        bean.setProducer(injectionTarget);

        return bean;
    }

    protected List<AnnotatedMethod<?>> getPostConstructMethods()
    {
        List<AnnotatedMethod<?>> postConstructMethods = new ArrayList<AnnotatedMethod<?>>();
        collectPostConstructMethods(annotatedType.getJavaClass(), postConstructMethods);
        return postConstructMethods;
    }

    private void collectPostConstructMethods(Class<?> type, List<AnnotatedMethod<?>> postConstructMethods)
    {
        if (type == null)
        {
            return;
        }
        collectPostConstructMethods(type.getSuperclass(), postConstructMethods);
        for (AnnotatedMethod<?> annotatedMethod: annotatedType.getMethods())
        {
            if (annotatedMethod.getJavaMember().getDeclaringClass() == type
                && annotatedMethod.isAnnotationPresent(PostConstruct.class)
                && annotatedMethod.getParameters().isEmpty())
            {
                postConstructMethods.add(annotatedMethod);
            }
        }
    }

    protected List<AnnotatedMethod<?>> getPreDestroyMethods()
    {
        List<AnnotatedMethod<?>> preDestroyMethods = new ArrayList<AnnotatedMethod<?>>();
        collectPreDestroyMethods(annotatedType.getJavaClass(), preDestroyMethods);
        return preDestroyMethods;
    }

    private void collectPreDestroyMethods(Class<?> type, List<AnnotatedMethod<?>> preDestroyMethods)
    {
        if (type == null)
        {
            return;
        }
        collectPreDestroyMethods(type.getSuperclass(), preDestroyMethods);
        for (AnnotatedMethod<?> annotatedMethod: annotatedType.getMethods())
        {
            if (annotatedMethod.getJavaMember().getDeclaringClass() == type
                && annotatedMethod.isAnnotationPresent(PreDestroy.class)
                && annotatedMethod.getParameters().isEmpty())
            {
                preDestroyMethods.add(annotatedMethod);
            }
        }
    }

    public boolean isEnabled()
    {
        return enabled;
    }

    public void defineEnabled()
    {
        enabled = webBeansContext.getWebBeansUtil().isBeanEnabled(annotatedType, annotatedType.getJavaClass(), beanAttributes.getStereotypes());
    }
    
    public I getBean()
    {
        I bean = createBean(annotatedType.getJavaClass());
        for (InjectionPoint injectionPoint: webBeansContext.getInjectionPointFactory().buildInjectionPoints(bean, annotatedType))
        {
            bean.addInjectionPoint(injectionPoint);
        }
        return bean;
    }
}
