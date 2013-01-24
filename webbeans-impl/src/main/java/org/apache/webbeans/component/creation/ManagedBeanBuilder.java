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

import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.inject.spi.AnnotatedConstructor;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;

import org.apache.webbeans.component.BeanAttributesImpl;
import org.apache.webbeans.component.ManagedBean;
import org.apache.webbeans.component.WebBeansType;
import org.apache.webbeans.config.WebBeansContext;
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
    public ManagedBeanBuilder(WebBeansContext webBeansContext, AnnotatedType<T> annotatedType, BeanAttributesImpl<T> beanAttributes)
    {
        super(webBeansContext, annotatedType, beanAttributes);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void checkCreateConditions()
    {
        webBeansContext.getWebBeansUtil().checkManagedBeanCondition(getAnnotated());
        WebBeansUtil.checkGenericType(getBeanType(), getBeanAttributes().getScope());
        //Check Unproxiable
        checkUnproxiableApiType();
    }


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
        //Check for Enabled via Alternative
        defineEnabled();

        checkCreateConditions();

        defineConstructor();

        return getBean();
    }

    @Override
    protected List<AnnotatedMethod<?>> getPostConstructMethods()
    {
        return webBeansContext.getInterceptorUtil().getLifecycleMethods(getAnnotated(), PostConstruct.class, true);
    }

    @Override
    protected List<AnnotatedMethod<?>> getPreDestroyMethods()
    {
        return webBeansContext.getInterceptorUtil().getLifecycleMethods(getAnnotated(), PreDestroy.class, false);
    }

    protected void addConstructorInjectionPointMetaData(ManagedBean<T> bean)
    {
        if (constructor == null)
        {
            return;
        }
        bean.setConstructor(constructor.getJavaMember());
    }


    @Override
    protected M createBean(Class<T> beanClass, boolean enabled)
    {
        M managedBean = (M)new ManagedBean<T>(webBeansContext, WebBeansType.MANAGED, getAnnotated(), getBeanAttributes(), beanClass);
        managedBean.setEnabled(enabled);
        return managedBean;
    }
}
