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

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.CreationException;
import javax.enterprise.inject.spi.Bean;

import org.apache.webbeans.context.ContextFactory;
import org.apache.webbeans.util.WebBeansUtil;

/**
 * Defines the producer field component implementation.
 * 
 * @param <T> Type of the field decleration
 */
public class ProducerFieldComponent<T> extends AbstractComponent<T> implements IComponentHasParent
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
        Object parentInstance = null;
        boolean dependentContext = false;
        try
        {
            if (this.ownerComponent.getScopeType().equals(Dependent.class))
            {
                if(!ContextFactory.checkDependentContextActive())
                {
                    ContextFactory.activateDependentContext();
                    dependentContext = true;
                }

            }
            
            if(!producerField.isAccessible())
            {
                producerField.setAccessible(true);
            }
            
            if(Modifier.isStatic(producerField.getModifiers()))
            {
                instance =  (T)producerField.get(null);
            }
            else
            {
                parentInstance = getParentInstance();
                instance =  (T)producerField.get(parentInstance);
            }
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
                
                if(dependentContext)
                {
                    ContextFactory.passivateDependentContext();
                }

            }
        }

        checkNullInstance(instance);
        checkScopeType();

        return instance;

    }
    
    public Field getCreatorField()
    {
    	return this.producerField;
    }
    
    public Type[] getActualTypeArguments()
    {
        Type type = producerField.getGenericType();
        if (type instanceof ParameterizedType)
        {
            ParameterizedType pType = (ParameterizedType) type;
            return pType.getActualTypeArguments();
        }

        else
        {
            return new Type[0];
        }

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
        //return getManager().getInstance(this.ownerComponent);
        
        Object parentInstance = null;
        
        //Added for most specialized bean
        Annotation[] anns = new Annotation[this.ownerComponent.getBindings().size()];
        anns = this.ownerComponent.getBindings().toArray(anns);
        
        AbstractComponent<T> specialize = WebBeansUtil.getMostSpecializedBean(getManager(), (AbstractComponent<T>)this.ownerComponent);
        
        if(specialize != null)
        {
            parentInstance = getManager().getInstance(specialize);
        }
        else
        {
            parentInstance = getManager().getInstance(this.ownerComponent);   
        }
        
        return parentInstance;
        
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

    /* (non-Javadoc)
     * @see org.apache.webbeans.component.IComponentHasParent#getParent()
     */
    public AbstractComponent<?> getParent()
    {
        return this.ownerComponent;
    }
    
    public String toString()
    {
        return super.toString();
    }
}
