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

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.spi.AnnotatedType;

import org.apache.webbeans.component.NewManagedBean;
import org.apache.webbeans.component.WebBeansType;
import org.apache.webbeans.config.WebBeansContext;

public class NewManagedBeanBuilder<T> extends ManagedBeanBuilder<T>
{

    public NewManagedBeanBuilder(AnnotatedType<T> annotatedType, WebBeansContext webBeansContext)
    {
        super(new NewManagedBean<T>(webBeansContext, annotatedType.getJavaClass(), WebBeansType.MANAGED, annotatedType), Dependent.class);
    }

    /**
     * {@inheritDoc}
     */
    public NewManagedBean<T> getBean()
    {
        return (NewManagedBean<T>)super.getBean();
    }
}
