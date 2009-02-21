/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.webbeans.component;

import java.lang.reflect.Field;

import javax.context.Context;
import javax.context.CreationalContext;
import javax.context.Dependent;
import javax.inject.CreationException;
import javax.inject.manager.Bean;

import org.apache.webbeans.context.creational.CreationalContextImpl;
import org.apache.webbeans.util.WebBeansUtil;

/**
 * Defines the producer field component implementation.
 * 
 * @param <T> Type of the field decleration
 */
public class ProducerFieldComponent<T> extends AbstractComponent<T>
{
    /**Producer field that defines the component*/
    private Field producerField;
    
    /**Owner of the producer field component*/
    private AbstractComponent<?> ownerComponent;
    
    
    /**
     * Defines the new producer field component.
     * 
     * @param returnType type of the field decleration 
     */
    public ProducerFieldComponent(AbstractComponent<?> ownerComponent, Class<T> returnType)
    {
        super(WebBeansType.PRODUCERFIELD,returnType);
        this.ownerComponent = ownerComponent;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected T createInstance(CreationalContext<T> creationalContext)
    {
        T instance = null;
        Object parentInstance = getParentInstance();

        try
        {
            if(!producerField.isAccessible())
            {
                producerField.setAccessible(true);
            }

            instance =  (T)producerField.get(parentInstance);
        }
        catch(Exception e)
        {
            throw new CreationException(e);
        }
        finally
        {
            if (this.ownerComponent.getScopeType().equals(Dependent.class))
            {
                destroyBean(this.ownerComponent, parentInstance);
            }
        }

        checkNullInstance(instance);
        checkScopeType();

        return instance;

    }
    
    @SuppressWarnings("unchecked")
    protected <K> void destroyBean(Bean<?> bean, Object instance)
    {
        Bean<K> destroy = (Bean<K>) bean;
        K inst = (K) instance;

        destroy.destroy(inst);
    }
    
    @SuppressWarnings("unchecked")
    protected Object getParentInstance()
    {
        Context context = getManager().getContext(this.ownerComponent.getScopeType());

        return context.get(this.ownerComponent, new CreationalContextImpl());
    }
    
    @Override
    protected void destroyInstance(T instance)
    {
        
    }
    
    public void setProducerField(Field field)
    {
        this.producerField = field;
    }
    
    protected void checkNullInstance(Object instance)
    {
        String errorMessage = "WebBeans producer field : " + producerField.getName() + " return type in the component implementation class : " + this.ownerComponent.getReturnType().getName() + " scope type must be @Dependent to create null instance";
        WebBeansUtil.checkNullInstance(instance, this.getScopeType(), errorMessage);        
    }

    protected void checkScopeType()
    {
        String errorMessage = "WebBeans producer method : " + producerField.getName() + " return type in the component implementation class : " + this.ownerComponent.getReturnType().getName() + " with passivating scope @" + this.getScopeType().getName() + " must be Serializable";
        WebBeansUtil.checkSerializableScopeType(this.getScopeType(), this.isSerializable(), errorMessage);

    }
    
}
