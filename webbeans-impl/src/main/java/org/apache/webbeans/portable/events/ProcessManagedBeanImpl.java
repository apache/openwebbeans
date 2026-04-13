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
package org.apache.webbeans.portable.events;

import jakarta.enterprise.inject.spi.AnnotatedMethod;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.DeploymentException;
import jakarta.enterprise.inject.spi.ProcessManagedBean;

import jakarta.enterprise.invoke.Invoker;
import jakarta.enterprise.invoke.InvokerBuilder;
import org.apache.webbeans.component.AbstractOwbBean;
import org.apache.webbeans.component.InjectionTargetBean;
import org.apache.webbeans.invoke.InvokerBuilderImpl;

/**
 * Implementation of {@link ProcessManagedBean}.
 * 
 * @version $Rev$ $Date$
 *
 * @param <X> bean class info
 */
public class ProcessManagedBeanImpl<X> extends ProcessBeanImpl<X> implements ProcessManagedBean<X>
{
    /**Annotated managed bean class*/
    private final AnnotatedType<X> annotatedBeanClass;

    public ProcessManagedBeanImpl(InjectionTargetBean<X> bean, AnnotatedType<X> annotatedType)
    {
        super(bean, annotatedType);
        annotatedBeanClass = annotatedType;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public AnnotatedType<X> getAnnotatedBeanClass()
    {
        checkState();
        return annotatedBeanClass;
    }

    @Override
    public InvokerBuilder<Invoker<X, ?>> createInvoker(AnnotatedMethod<? super X> method)
    {
        checkState();
        if (!(getBean() instanceof AbstractOwbBean))
        {
            throw new DeploymentException("Cannot create invoker for bean: " + getBean());
        }
        InvokerBuilderImpl.validateCreateInvoker(getBean(), annotatedBeanClass, method);
        return new InvokerBuilderImpl<>((AbstractOwbBean<?>) getBean(), annotatedBeanClass, method);
    }
}
