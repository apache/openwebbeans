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

import java.lang.reflect.Type;

import jakarta.enterprise.inject.spi.AnnotatedMethod;
import jakarta.enterprise.inject.spi.AnnotatedParameter;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanAttributes;
import jakarta.enterprise.inject.spi.DefinitionException;

import org.apache.webbeans.component.InjectionTargetBean;
import org.apache.webbeans.component.ProducerMethodBean;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.util.ClassUtil;

public class ProducerMethodBeanBuilder<T> extends AbstractProducerBeanBuilder<T, AnnotatedMethod<?>, ProducerMethodBean<T>>
{

    public ProducerMethodBeanBuilder(InjectionTargetBean<T> parent, AnnotatedMethod<?> annotatedMethod, BeanAttributes<T> beanAttributes)
    {
        super(parent, annotatedMethod, beanAttributes);
    }

    @Override
    protected <P> ProducerMethodBean<T> createBean(InjectionTargetBean<P> parent, Class<T> beanClass)
    {
        AnnotatedMethod<P> annotatedMethod = (AnnotatedMethod<P>) annotatedMember;
        ProducerMethodBean<T> producerMethodBean
            = new ProducerMethodBean<>(parent, beanAttributes, beanClass, new MethodProducerFactory<>(annotatedMethod, parent, parent.getWebBeansContext()));
        return producerMethodBean;
    }
    
    public ProducerMethodBean<T> getBean()
    {
        return createBean((Class<T>) annotatedMember.getJavaMember().getReturnType());
    }

    @Override
    public void validate() throws DefinitionException
    {
        super.validate();

        for (AnnotatedParameter<?> parameter : annotatedMember.getParameters())
        {
            Type type = parameter.getBaseType();
            if (Bean.class.equals(ClassUtil.getClass(type)) &&
                !annotatedMember.getBaseType().equals(ClassUtil.getActualTypeArguments(type)[0]))
            {
                throw new WebBeansConfigurationException("Type parameter of the injected bean must be the same as the return type. Producer method: " + annotatedMember);
            }
        }
    }
}
