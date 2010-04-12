/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.apache.webbeans.component;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.CreationalContext;

import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.inject.InjectableMethods;
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
     * @param parent parent bean
     * @param returnType producer method return type
     */
    public ProducerMethodBean(InjectionTargetBean<?> parent, Class<T> returnType)
    {
        super(WebBeansType.PRODUCERMETHOD, returnType, parent);
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
     * Gets the disposal method of the component.
     * 
     * @return disposal method
     */
    public Method getDisposalMethod()
    {
        return disposalMethod;
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
            throw new WebBeansConfigurationException("There are multiple disposal method for producer method component with name : " + getName() + " with implementation class " + getParent().getReturnType().getName() + " with disposal method name : " + disposalMethod.getName());
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
    /**
     * Gets actual type arguments.
     * 
     * @return actual type arguments
     */
    public Type[] getActualTypeArguments()
    {
        Type type = creatorMethod.getGenericReturnType();
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

    /**
     * {@inheritDoc}
     */
    @Override
    protected T createInstance(CreationalContext<T> creationalContext)
    {
        T instance = null;
        instance = createDefaultInstance(creationalContext);
        // Check null instance
        checkNullInstance(instance);

        // Check scope type
        checkScopeType();
        return instance;
    }

    /**
     * Default producer method creation.
     * 
     * @param creationalContext creational context
     * @return producer method instance
     */
    protected T createDefaultInstance(CreationalContext<T> creationalContext)
    {
        T instance = null;
        Object parentInstance = null;
        CreationalContext<?> parentCreational = null;
        InjectableMethods<T> m = null;
        try
        {
            parentCreational = getManager().createCreationalContext(this.ownerComponent);
            
            if (!Modifier.isStatic(creatorMethod.getModifiers()))
            {
                parentInstance = getParentInstance(parentCreational);
            }

            m = new InjectableMethods<T>(creatorMethod, parentInstance, this, creationalContext);
            //Injection of parameters
            instance = m.doInjection();

        }
        finally
        {
            if (getParent().getScope().equals(Dependent.class))
            {
                destroyBean(getParent(), parentInstance, parentCreational);
            }
            
            //Remove any dependent objects
            m.destroyDependentInjectionPoints();
        }

        return instance;

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void destroyInstance(T instance, CreationalContext<T> creationalContext)
    {
        dispose(instance,creationalContext);
    }

    /**
     * {@inheritDoc}
     */
    public void dispose(T instance, CreationalContext<T> creationalContext)
    {
        disposeDefault(instance, creationalContext);
    }

    /**
     * Default dispose method used.
     * 
     * @param instance bean instance
     */
    protected void disposeDefault(T instance, CreationalContext<T> creationalContext)
    {
        if (disposalMethod != null)
        {
            Object parentInstance = null;
            CreationalContext<?> parentCreational = null;
            InjectableMethods<T> m = null;
            try
            {
                parentCreational = getManager().createCreationalContext(this.ownerComponent);
                
                if (!Modifier.isStatic(disposalMethod.getModifiers()))
                {
                    parentInstance = getParentInstance(parentCreational);
                }

                m = new InjectableMethods<T>(disposalMethod, parentInstance, this.ownerComponent, creationalContext);
                m.setDisposable(true);
                m.setProducerMethodInstance(instance);

                m.doInjection();

            }
            finally
            {
                if (getParent().getScope().equals(Dependent.class))
                {
                    destroyBean(getParent(), parentInstance, parentCreational);
                }
                
                //Remove any dependent objects
                m.destroyDependentInjectionPoints();
            }
        }
    }
    
    /**
     * Check null control.
     * 
     * @param instance bean instance
     */
    protected void checkNullInstance(Object instance)
    {
        String errorMessage = "WebBeans producer method : " + creatorMethod.getName() + " return type in the component implementation class : " + this.ownerComponent.getReturnType().getName() + " scope type must be @Dependent to create null instance";
        WebBeansUtil.checkNullInstance(instance, this.getScope(), errorMessage);
    }

    /**
     * Check passivation check.
     */
    protected void checkScopeType()
    {
        String errorMessage = "WebBeans producer method : " + creatorMethod.getName() + " return type in the component implementation class : " + this.ownerComponent.getReturnType().getName() + " with passivating scope @" + this.getScope().getName() + " must be Serializable";
        WebBeansUtil.checkSerializableScopeType(this.getScope(), this.isSerializable(), errorMessage);

    }
    
    
    
    @Override
    public boolean isPassivationCapable()
    {
        return isPassivationCapable(this.creatorMethod.getReturnType(),this.creatorMethod.getModifiers());
    }

    public String toString()
    {
        return super.toString();
    }
}