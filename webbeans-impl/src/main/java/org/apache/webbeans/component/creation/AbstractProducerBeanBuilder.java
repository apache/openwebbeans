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

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.spi.AnnotatedMember;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.BeanAttributes;
import jakarta.enterprise.inject.spi.DefinitionException;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import org.apache.webbeans.component.AbstractProducerBean;
import org.apache.webbeans.component.InjectionTargetBean;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.util.Asserts;
import org.apache.webbeans.util.ClassUtil;
import org.apache.webbeans.util.GenericsUtil;

public abstract class AbstractProducerBeanBuilder<T, A extends AnnotatedMember<?>, P extends AbstractProducerBean<T>>
{

    protected final InjectionTargetBean<?> parent;
    protected final A annotatedMember;
    protected final BeanAttributes<T> beanAttributes;

    public AbstractProducerBeanBuilder(InjectionTargetBean<?> parent, A annotated, BeanAttributes<T> beanAttributes)
    {
        Asserts.assertNotNull(parent, Asserts.PARAM_NAME_WEBBEANSCONTEXT);
        Asserts.assertNotNull(annotated, "annotated");
        Asserts.assertNotNull(beanAttributes, "beanAttributes");
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
        Type type = annotatedMember.getBaseType();
        if (type instanceof GenericArrayType)
        {
            throw new WebBeansConfigurationException("Produced Type must not be a GenericArrayType");
        }
        else if (ClassUtil.isParametrizedType(type))
        {
            if (GenericsUtil.containsWildcardType(type))
            {
                throw new WebBeansConfigurationException("Produced type must not be a WildcardType");
            }
            else if (!Dependent.class.equals(beanAttributes.getScope()))
            {

                ParameterizedType parameterizedType = GenericsUtil.getParameterizedType(type);
                if (GenericsUtil.containTypeVariable(parameterizedType.getActualTypeArguments()))
                {
                    throw new WebBeansConfigurationException("Produced ParametrizedType must be @Dependent-Scope");
                }
            }
        }
    }

    protected abstract <X> P createBean(InjectionTargetBean<X> parent, Class<T> beanClass);

    protected P createBean(Class<T> beanClass)
    {
        return createBean(parent, beanClass);
    }
}
