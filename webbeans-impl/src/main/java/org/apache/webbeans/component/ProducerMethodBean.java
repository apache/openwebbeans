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

import java.lang.reflect.Method;

import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.BeanAttributes;

import org.apache.webbeans.component.creation.MethodProducerFactory;


/**
 * Concrete implementation of the {@link AbstractOwbBean}.
 * <p>
 * It is defined as producer method component.
 * </p>
 * 
 * @version $Rev$ $Date$
 */
public class ProducerMethodBean<T> extends AbstractProducerBean<T>
{

    /** Creator method of the parent component */
    protected Method creatorMethod;


    /**
     * Creates a new instance.
     * 
     * @param ownerComponent parent bean
     * @param returnType producer method return type
     */
    public <P> ProducerMethodBean(InjectionTargetBean<P> ownerComponent,
                                  BeanAttributes<T> beanAttributes,
                                  Class<T> returnType,
                                  MethodProducerFactory<P> producerFactory)
    {
        super(ownerComponent, WebBeansType.PRODUCERMETHOD, beanAttributes, returnType, producerFactory);
    }

    /**
     * Gets the creator method.
     * 
     * @return producer method
     */
    public Method getCreatorMethod()
    {
        return creatorMethod;
    }


    /**
     * Sets the method.
     * 
     * @param creatorMethod producer method
     */
    public void setCreatorMethod(Method creatorMethod)
    {
        this.creatorMethod = creatorMethod;
    }


    @Override
    public String getId()
    {
        if (passivatingId == null)
        {
            String id = super.getId();
            
            passivatingId = id + "#" + creatorMethod.toGenericString();
        }
        return passivatingId;
    }
    
    @Override
    public T create(CreationalContext<T> creationalContext)
    {
        T instance = super.create(creationalContext);
        // Check null instance
        checkNullInstance(instance, creatorMethod.getName());

        // Check scope type
        checkScopeType(creatorMethod.getName(), instance);
        return instance;
    }

    @Override
    public boolean isPassivationCapable()
    {
        return isPassivationCapable(creatorMethod.getReturnType(), creatorMethod.getModifiers());
    }

    /**
     * For producer beans we add the info about the owner component
     */
    @Override
    protected void addToStringInfo(StringBuilder builder)
    {
        builder.append(", Producer Method: ").append(creatorMethod);
    }

}
