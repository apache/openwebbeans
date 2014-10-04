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
package org.apache.webbeans.component;

import java.io.Serializable;
import java.lang.reflect.Modifier;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.PassivationCapable;
import javax.enterprise.inject.spi.Producer;

import org.apache.webbeans.component.spi.BeanAttributes;
import org.apache.webbeans.component.spi.ProducerFactory;
import org.apache.webbeans.util.WebBeansUtil;


/**
 * Abstract class for producer components.
 * 
 * @version $Rev$ $Date$
 * @param <T> bean type info
 */
public class AbstractProducerBean<T> extends AbstractOwbBean<T> implements PassivationCapable
{
    private final InjectionTargetBean<?> ownerBean;
    private final Class<T> returnType;
    private Producer<T> producer;

    /**
     * Create a new instance.
     * 
     * @param returnType bean type info
     * @param ownerComponent bean which contains this producer method or field
     */
    public AbstractProducerBean(InjectionTargetBean<?> ownerComponent,
            WebBeansType webBeansType,
            BeanAttributes<T> beanAttributes,
            Class<T> returnType,
            ProducerFactory<?> producerFactory)
    {
        super(ownerComponent.getWebBeansContext(), webBeansType, beanAttributes, ownerComponent.getBeanClass(), !returnType.isPrimitive());
        this.ownerBean = ownerComponent;
        this.returnType = returnType;
        producer = producerFactory.createProducer(this);
    }

    public InjectionTargetBean<?> getOwnerBean()
    {
        return ownerBean;
    }

    @Override
    public Producer<T> getProducer()
    {
        return producer;
    }

    @Override
    public Class<T> getReturnType()
    { 
        return returnType;
    }

    /**
     * {@inheritDoc}
     */
    public void dispose(T instance, CreationalContext<T> creationalContext)
    {
        // Do nothing
    }
    
    /**
     * Check null control.
     * 
     * @param instance bean instance
     */
    protected void checkNullInstance(Object instance, String producerName)
    {
        String errorMessage = "WebBeans producer : %s" +
                              " return type in the component implementation class : %s" +
                              " scope type must be @Dependent to create null instance";
        WebBeansUtil.checkNullInstance(instance, getScope(), errorMessage, producerName,
                getBeanClass().getName());
    }

    /**
     * Check passivation check.
     */
    protected void checkScopeType(String producerName)
    {
        String errorMessage = "WebBeans producer : %s" +
                              " return type in the component implementation class : %s" +
                              " with passivating scope @%s" +
                              " must be Serializable";
        getWebBeansContext().getWebBeansUtil().checkSerializableScopeType(getScope(),
                Serializable.class.isAssignableFrom(getReturnType()), errorMessage, producerName, getBeanClass().getName(),
                getScope().getName());

    }

    protected boolean isPassivationCapable(Class<?> returnType, Integer modifiers)
    {
        if(Modifier.isFinal(modifiers) && !(Serializable.class.isAssignableFrom(returnType)))
        {
            return false;
        }
        
        return true;
    }
}
