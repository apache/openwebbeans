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
package org.apache.webbeans.portable.events.generics;

import org.apache.webbeans.portable.InjectionTargetImpl;
import org.apache.webbeans.portable.events.ProcessInjectionTargetImpl;

import jakarta.enterprise.inject.spi.AnnotatedType;

@SuppressWarnings("unchecked")
public class GProcessInjectionTarget extends ProcessInjectionTargetImpl implements GenericBeanEvent
{
    public GProcessInjectionTarget(InjectionTargetImpl<?> injectionTarget,AnnotatedType<?> annotatedType)
    {
        super(injectionTarget, annotatedType);
    }

    @Override
    public Class<?> getBeanClassFor(Class<?> eventClass)
    {
        return getAnnotatedType().getJavaClass();
    }
}
