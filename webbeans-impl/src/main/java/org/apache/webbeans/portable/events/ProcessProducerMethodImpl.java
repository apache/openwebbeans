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
import jakarta.enterprise.inject.spi.AnnotatedParameter;
import jakarta.enterprise.inject.spi.ProcessProducerMethod;

import org.apache.webbeans.component.ProducerMethodBean;

/**
 * Implementation of {@link ProcessProducerMethod}.
 * 
 * @version $Rev$ $Date$
 *
 * @param <X> producer method return type
 * @param <T> producer method bean class
 */
public class ProcessProducerMethodImpl<X,T> extends ProcessBeanImpl<T> implements ProcessProducerMethod<X, T>
{
    /**Disposed parameter*/
    private final AnnotatedParameter<X> annotatedDisposedParameter;
    
    /**Producer method*/
    private final AnnotatedMethod<X> annotatedProducerMethod;

    public ProcessProducerMethodImpl(ProducerMethodBean<T> bean, AnnotatedMethod<X> method, AnnotatedParameter<X> disposeParameter)
    {
        super(bean, method);
        annotatedProducerMethod = method;
        annotatedDisposedParameter = disposeParameter;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public AnnotatedParameter<X> getAnnotatedDisposedParameter()
    {
        checkState();
        return annotatedDisposedParameter;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AnnotatedMethod<X> getAnnotatedProducerMethod()
    {
        checkState();
        return annotatedProducerMethod;
    }
    
}
