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
package org.apache.webbeans.container;

import java.lang.reflect.Modifier;

import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.InjectionTarget;

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.portable.AbstractDecoratorInjectionTarget;

public class DecoratorInjectionTargetFactory<T> extends InjectionTargetFactoryImpl<T>
{

    public DecoratorInjectionTargetFactory(AnnotatedType<T> annotatedType, WebBeansContext webBeansContext)
    {
        super(annotatedType, webBeansContext);
    }

    @Override
    public InjectionTarget<T> createInjectionTarget(Bean<T> bean)
    {
        if (Modifier.isAbstract(getAnnotatedType().getJavaClass().getModifiers()))
        {
            return new AbstractDecoratorInjectionTarget<>(
                getAnnotatedType(),
                createInjectionPoints(bean),
                getWebBeansContext(),
                getPostConstructMethods(),
                getPreDestroyMethods());
        }
        else
        {
            return super.createInjectionTarget(bean);
        }
    }
}
