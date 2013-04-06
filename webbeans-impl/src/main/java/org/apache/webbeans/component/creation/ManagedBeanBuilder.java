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

import javax.enterprise.inject.spi.AnnotatedType;

import org.apache.webbeans.component.BeanAttributesImpl;
import org.apache.webbeans.component.ManagedBean;
import org.apache.webbeans.component.WebBeansType;
import org.apache.webbeans.config.WebBeansContext;
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
public class ManagedBeanBuilder<T, M extends ManagedBean<T>>
{
    protected final WebBeansContext webBeansContext;
    protected final AnnotatedType<T> annotatedType;
    protected final BeanAttributesImpl<T> beanAttributes;

    /**
     * Creates a new creator.
     */
    public ManagedBeanBuilder(WebBeansContext webBeansContext, AnnotatedType<T> annotatedType, BeanAttributesImpl<T> beanAttributes)
    {
        Asserts.assertNotNull(webBeansContext, "webBeansContext may not be null");
        Asserts.assertNotNull(annotatedType, "annotated type may not be null");
        Asserts.assertNotNull(beanAttributes, "beanAttributes may not be null");
        this.webBeansContext = webBeansContext;
        this.annotatedType = annotatedType;
        this.beanAttributes = beanAttributes;
    }

    /**
     * {@inheritDoc}
     */
    public M getBean()
    {
        M bean = (M)new ManagedBean<T>(webBeansContext, WebBeansType.MANAGED, annotatedType, beanAttributes, annotatedType.getJavaClass());
        bean.setEnabled(webBeansContext.getWebBeansUtil().isBeanEnabled(annotatedType, annotatedType.getJavaClass(), beanAttributes.getStereotypes()));
        webBeansContext.getWebBeansUtil().checkManagedBeanCondition(annotatedType);
        WebBeansUtil.checkGenericType(annotatedType.getJavaClass(), beanAttributes.getScope());
        webBeansContext.getDeploymentValidationService().validateProxyable(bean);
        return bean;
    }
}
