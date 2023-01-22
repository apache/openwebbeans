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

import java.util.Collections;
import java.util.List;

import jakarta.enterprise.inject.spi.AnnotatedMethod;
import jakarta.enterprise.inject.spi.AnnotatedType;

import org.apache.webbeans.config.WebBeansContext;

public class InterceptorInjectionTargetFactory<T> extends InjectionTargetFactoryImpl<T>
{

    public InterceptorInjectionTargetFactory(AnnotatedType<T> annotatedType, WebBeansContext webBeansContext)
    {
        super(annotatedType, webBeansContext);
    }

    @Override
    protected List<AnnotatedMethod<?>> getPostConstructMethods()
    {
        return Collections.<AnnotatedMethod<?>>emptyList();
    }

    @Override
    protected List<AnnotatedMethod<?>> getPreDestroyMethods()
    {
        return Collections.<AnnotatedMethod<?>>emptyList();
    }
}
