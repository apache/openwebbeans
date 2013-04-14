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
import java.lang.reflect.Method;

import javax.enterprise.context.spi.CreationalContext;

import org.apache.webbeans.component.creation.MethodProducerFactory;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.util.ClassUtil;
import org.apache.webbeans.util.WebBeansUtil;

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

    /** Disposal method */
    protected Method disposalMethod;

    /**
     * Creates a new instance.
     * 
     * @param ownerComponent parent bean
     * @param returnType producer method return type
     */
    public <P> ProducerMethodBean(InjectionTargetBean<P> ownerComponent,
                                  BeanAttributesImpl<T> beanAttributes,
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

    /**
     * Sets the disposal method.
     * 
     * @param disposalMethod disposal method of this producer method component
     */
    public void setDisposalMethod(Method disposalMethod)
    {
        if (this.disposalMethod != null)
        {
            throw new WebBeansConfigurationException("There are multiple disposal method for producer method " +
                    "component with name : " + getName() + " with implementation class " +
                    getParent().getReturnType().getName() + " with disposal method name : " +
                    disposalMethod.getName());
        }
        this.disposalMethod = disposalMethod;
    }

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
        checkNullInstance(instance);

        // Check scope type
        checkScopeType();
        return instance;
    }
    
    /**
     * Check null control.
     * 
     * @param instance bean instance
     */
    protected void checkNullInstance(Object instance)
    {
        String errorMessage = "WebBeans producer method : %s" +
                              " return type in the component implementation class : %s" +
                              " scope type must be @Dependent to create null instance";
        WebBeansUtil.checkNullInstance(instance, getScope(), errorMessage, creatorMethod.getName(),
                ownerComponent.getReturnType().getName());
    }

    /**
     * Check passivation check.
     */
    protected void checkScopeType()
    {
        String errorMessage = "WebBeans producer method : %s" +
                              " return type in the component implementation class : %s" +
                              " with passivating scope @%s" +
                              " must be Serializable";
        getWebBeansContext().getWebBeansUtil().checkSerializableScopeType(getScope(),
                ClassUtil.isClassAssignable(Serializable.class, getReturnType()), errorMessage, creatorMethod.getName(), ownerComponent.getReturnType().getName(),
                getScope().getName());

    }

    @Override
    public boolean isPassivationCapable()
    {
        return isPassivationCapable(creatorMethod.getReturnType(), creatorMethod.getModifiers());
    }
}
