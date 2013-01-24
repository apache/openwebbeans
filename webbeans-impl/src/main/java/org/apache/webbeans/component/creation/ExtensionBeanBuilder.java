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
import java.util.List;
import java.util.Set;

import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InjectionTarget;

import org.apache.webbeans.component.ExtensionBean;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.portable.ExtensionProducer;

public class ExtensionBeanBuilder<T> extends AbstractInjectionTargetBeanBuilder<T, ExtensionBean<T>>
{

    public ExtensionBeanBuilder(WebBeansContext webBeansContext, Class<T> type)
    {
        super(webBeansContext,
              webBeansContext.getAnnotatedElementFactory().newAnnotatedType(type),
              BeanAttributesBuilder.forContext(webBeansContext).newBeanAttibutes(webBeansContext.getAnnotatedElementFactory().getAnnotatedType(type)).build());
    }

    protected InjectionTarget<T> buildInjectionTarget(Set<Type> types,
                                                      Set<Annotation> qualifiers,
                                                      AnnotatedType<T> annotatedType,
                                                      Set<InjectionPoint> points,
                                                      WebBeansContext webBeansContext,
                                                      List<AnnotatedMethod<?>> postConstructMethods,
                                                      List<AnnotatedMethod<?>> preDestroyMethods)
    {
        return new ExtensionProducer<T>(annotatedType, points, webBeansContext);
    }

    @Override
    protected ExtensionBean<T> createBean(Class<T> beanClass, boolean enabled)
    {
        return new ExtensionBean<T>(webBeansContext, beanClass);
    }
}
