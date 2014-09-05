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

import javax.enterprise.inject.spi.AnnotatedMember;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeanAttributes;
import javax.enterprise.inject.spi.DefinitionException;

import org.apache.webbeans.component.AbstractProducerBean;
import org.apache.webbeans.component.InjectionTargetBean;
import org.apache.webbeans.util.Asserts;

public abstract class AbstractProducerBeanBuilder<T, A extends AnnotatedMember<?>, P extends AbstractProducerBean<T>>
{

    protected final InjectionTargetBean<?> parent;
    protected final A annotatedMember;
    protected final BeanAttributes<T> beanAttributes;

    public AbstractProducerBeanBuilder(InjectionTargetBean<?> parent, A annotated, BeanAttributes<T> beanAttributes)
    {
        Asserts.assertNotNull(parent, "webBeansContext may not be null");
        Asserts.assertNotNull(annotated, "annotated may not be null");
        Asserts.assertNotNull(beanAttributes, "beanAttributes may not be null");
        this.parent = parent;
        annotatedMember = annotated;
        this.beanAttributes = beanAttributes;
    }

    protected AnnotatedType<?> getSuperType()
    {
        Class<?> superclass = annotatedMember.getDeclaringType().getJavaClass().getSuperclass();
        if (superclass == null)
        {
            return null;
        }
        return parent.getWebBeansContext().getAnnotatedElementFactory().getAnnotatedType(superclass);
    }

    /**
     * Check if the producer rules are met.
     */
    public void validate() throws DefinitionException
    {

    }

    protected abstract <X> P createBean(InjectionTargetBean<X> parent, Class<T> beanClass);

    protected P createBean(Class<T> beanClass)
    {
        return createBean(parent, beanClass);
    }
}
