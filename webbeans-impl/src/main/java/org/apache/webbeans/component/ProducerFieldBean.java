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
import java.lang.reflect.Field;

import javax.enterprise.context.spi.CreationalContext;

import org.apache.webbeans.component.spi.ProducerFactory;
import org.apache.webbeans.util.ClassUtil;
import org.apache.webbeans.util.WebBeansUtil;

/**
 * Defines the producer field component implementation.
 * 
 * @param <T> Type of the field decleration
 */
public class ProducerFieldBean<T> extends AbstractProducerBean<T>
{

    /** Producer field that defines the component */
    private Field producerField = null;

    /**
     * Defines the new producer field component.
     * 
     * @param returnType type of the field decleration
     */
    public ProducerFieldBean(InjectionTargetBean<?> ownerComponent, BeanAttributesImpl<T> beanAttributes, Class<T> returnType, ProducerFactory<T> producerFactory)
    {
        super(ownerComponent.getBeanClass(), ownerComponent.getWebBeansContext(), WebBeansType.PRODUCERFIELD, beanAttributes, returnType, producerFactory);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public T create(CreationalContext<T> creationalContext)
    {
        T instance = null;
        
        instance = super.create(creationalContext);
        checkNullInstance(instance);
        checkScopeType();

        return instance;

    }

    /**
     * Gets creator field.
     * 
     * @return creator field
     */
    public Field getCreatorField()
    {
        return producerField;
    }

    /**
     * Set producer field.
     * 
     * @param field producer field
     */
    public void setProducerField(Field field)
    {
        producerField = field;
    }

    /**
     * Check null instance.
     * 
     * @param instance bean instance
     */
    protected void checkNullInstance(Object instance)
    {
        String errorMessage = "WebBeans producer field : %s" +
                              " return type in the component implementation class : %s" +
                              " scope must be @Dependent to create null instance";
        WebBeansUtil.checkNullInstance(instance, getScope(), errorMessage, producerField.getName(),
                getBeanClass().getName());
    }

    /**
     * Check scope type passivation controls.
     */
    protected void checkScopeType()
    {
        String errorMessage = "WebBeans producer method : %s" + 
                              " return type in the component implementation class : %s" +
                              " with passivating scope @%s" +
                              " must be Serializable";
        getWebBeansContext().getWebBeansUtil().checkSerializableScopeType(getScope(),
                ClassUtil.isClassAssignable(Serializable.class, getReturnType()), errorMessage, producerField.getName(), 
                getBeanClass().getName(), getScope().getName());
    }
    
    @Override
    public boolean isPassivationCapable()
    {
        return isPassivationCapable(producerField.getType(), producerField.getModifiers());
    }
    
    @Override
    public String getId()
    {
        if (passivatingId == null)
        {
            String id = super.getId();
            
            passivatingId = id + "#" + producerField.toGenericString();
        }
        return passivatingId;
    }
}
