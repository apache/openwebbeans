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
package org.apache.webbeans.ejb.common.component;

import java.util.List;
import java.util.Set;

import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.AnnotatedMethod;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.BeanAttributes;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.enterprise.inject.spi.InjectionTarget;

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.ejb.common.util.EjbValidator;
import org.apache.webbeans.portable.AbstractEjbInjectionTarget;
import org.apache.webbeans.util.Asserts;

/**
 * EjbBeanCreatorImpl.
 * 
 * @version $Rev$ $Date$
 *
 * @param <T> ejb class type
 */
public abstract class EjbBeanBuilder<T, E extends BaseEjbBean<T>>
{
    protected final WebBeansContext webBeansContext;
    protected final AnnotatedType<T> annotatedType;
    protected final BeanAttributes<T> beanAttributes;

    public EjbBeanBuilder(WebBeansContext webBeansContext, AnnotatedType<T> annotatedType, BeanAttributes<T> beanAttributes)
    {
        Asserts.assertNotNull(webBeansContext, Asserts.PARAM_NAME_WEBBEANSCONTEXT);
        Asserts.assertNotNull(annotatedType, "annotated type");
        Asserts.assertNotNull(beanAttributes, "beanAttributes");
        this.webBeansContext = webBeansContext;
        this.annotatedType = annotatedType;
        this.beanAttributes = beanAttributes;
    }

    protected InjectionTarget<T> buildInjectionTarget(AnnotatedType<T> annotatedType,
                                                      Set<InjectionPoint> points,
                                                      WebBeansContext webBeansContext,
                                                      List<AnnotatedMethod<?>> postConstructMethods,
                                                      List<AnnotatedMethod<?>> preDestroyMethods)

    {
        return new AbstractEjbInjectionTarget<T>(annotatedType, points, webBeansContext)
        {
            @Override
            public T produce(CreationalContext<T> creationalContext)
            {
                return getInstance(creationalContext);
            }
        };
    }
    
    protected final E createBean(Class<T> beanClass)
    {
        return createBean(beanClass, webBeansContext.getWebBeansUtil().isBeanEnabled(annotatedType, beanAttributes.getStereotypes()));
    }
    
    protected abstract E createBean(Class<T> beanClass, boolean beanEnabled);

    public E getBean()
    {
        E bean = createBean(annotatedType.getJavaClass());
        EjbValidator.validateDecoratorOrInterceptor(bean.getReturnType());
        EjbValidator.validateEjbScopeType(bean);
        EjbValidator.validateGenericBeanType(bean.getReturnType(), bean.getScope());
        return bean;
    }

    protected abstract T getInstance(CreationalContext<T> creationalContext);
}
