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
import java.lang.reflect.Type;
import java.util.Set;

import javax.enterprise.inject.Specializes;
import javax.enterprise.inject.spi.AnnotatedField;

import org.apache.webbeans.component.InjectionTargetBean;
import org.apache.webbeans.component.ProducerFieldBean;
import org.apache.webbeans.util.WebBeansUtil;

public class ProducerFieldBeanBuilder<T, P extends ProducerFieldBean<T>> extends AbstractProducerBeanBuilder<T, AnnotatedField<?>, P>
{

    public ProducerFieldBeanBuilder(InjectionTargetBean<?> owner, AnnotatedField<?> annotated)
    {
        super(owner, annotated);
    }
    
    /**
     * {@inheritDoc}
     */
    public void defineName()
    {
        if (getAnnotated().isAnnotationPresent(Specializes.class))
        {
            AnnotatedField<?> superAnnotated = getSuperAnnotated();
            defineName(superAnnotated, WebBeansUtil.getProducerDefaultName(superAnnotated.getJavaMember().getName()));
        }
        if (getName() == null)
        {
            defineName(getAnnotated(), WebBeansUtil.getProducerDefaultName(getAnnotated().getJavaMember().getName()));
        }
    }

    @Override
    protected Class<T> getBeanType()
    {
        return (Class<T>) getAnnotated().getJavaMember().getType();
    }

    protected AnnotatedField<?> getSuperAnnotated()
    {
        AnnotatedField<?> thisField = getAnnotated();
        for (AnnotatedField<?> superField: getSuperType().getFields())
        {
            if (thisField.getJavaMember().getName().equals(superField.getJavaMember().getName())
                && thisField.getBaseType().equals(superField.getBaseType()))
            {
                return superField;
            }
        }
        return null;
    }

    @Override
    protected P createBean(InjectionTargetBean<?> owner,
                           Set<Type> types,
                           Set<Annotation> qualifiers,
                           Class<? extends Annotation> scope,
                           String name,
                           boolean nullable,
                           Class<T> beanClass,
                           Set<Class<? extends Annotation>> stereotypes,
                           boolean alternative)
    {
        return (P) new ProducerFieldBean<T>(owner, types, qualifiers, scope, name, nullable, beanClass, stereotypes, alternative);
    }
}
