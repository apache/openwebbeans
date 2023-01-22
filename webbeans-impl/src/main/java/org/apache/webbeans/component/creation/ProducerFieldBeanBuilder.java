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

import jakarta.enterprise.inject.spi.AnnotatedField;
import jakarta.enterprise.inject.spi.BeanAttributes;

import org.apache.webbeans.component.InjectionTargetBean;
import org.apache.webbeans.component.ProducerFieldBean;

public class ProducerFieldBeanBuilder<T, P extends ProducerFieldBean<T>> extends AbstractProducerBeanBuilder<T, AnnotatedField<?>, P>
{

    public ProducerFieldBeanBuilder(InjectionTargetBean<?> owner, AnnotatedField<?> annotated, BeanAttributes<T> beanAttributes)
    {
        super(owner, annotated, beanAttributes);
    }

    @Override
    protected <X> P createBean(InjectionTargetBean<X> owner, Class<T> beanClass)
    {
        return (P) new ProducerFieldBean<T>(owner, beanAttributes, beanClass, new FieldProducerFactory(annotatedMember, owner, owner.getWebBeansContext()));
    }
    
    public P getBean()
    {
        return createBean((Class<T>) annotatedMember.getJavaMember().getType());
    }
}
