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

import javax.enterprise.inject.spi.AnnotatedField;

import org.apache.webbeans.component.InjectionTargetBean;
import org.apache.webbeans.component.ProducerFieldBean;
import org.apache.webbeans.util.ClassUtil;

public class ProducerFieldBeanBuilder<T> extends AbstractProducerBeanBuilder<T>
{

    public ProducerFieldBeanBuilder(InjectionTargetBean<T> parent, AnnotatedField<? super T> annotatedField)
    {
        super(new ProducerFieldBean<T>(parent, (Class<T>)ClassUtil.getClass(annotatedField.getBaseType())), annotatedField);
    }

    protected ProducerFieldBeanBuilder(ProducerFieldBean<T> bean, AnnotatedField<? super T> annotatedField)
    {
        super(bean, annotatedField);
    }

    /**
     * {@inheritDoc}
     */
    public ProducerFieldBean<T> getBean()
    {
        return (ProducerFieldBean<T>) super.getBean();
    }

    @Override
    protected Class<?> getBeanType()
    {
        return ((AnnotatedField<T>)getAnnotated()).getJavaMember().getType();
    }
}
