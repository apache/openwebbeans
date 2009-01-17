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
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import javax.webbeans.Dependent;
import javax.webbeans.IllegalProductException;
import javax.webbeans.ScopeType;
import javax.webbeans.manager.Bean;

import org.apache.webbeans.container.ManagerImpl;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.inject.InjectableMethods;

/**
 * Concrete implementation of the {@link AbstractComponent}.
 * <p>
 * It is defined as producer method component.
 * </p>
 * 
 * @author <a href="mailto:gurkanerdogdu@yahoo.com">Gurkan Erdogdu</a>
 * @since 1.0
 */
public class ProducerComponentImpl<T> extends AbstractComponent<T>
{
    /** Parent component that this producer method belongs */
    protected AbstractComponent<?> parent;

    /** Creator method of the parent component */
    protected Method creatorMethod;

    /** Disposal method */
    protected Method disposalMethod;

    /*
     * Constructor
     */
    public ProducerComponentImpl(AbstractComponent<?> parent, Class<T> returnType)
    {
        super(WebBeansType.PRODUCER, returnType);
        this.parent = parent;
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
     * Gets the producer method owner web bean.
     * 
     * @return web bean component defines producer method
     */
    public AbstractComponent<?> getParent()
    {
        return parent;
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

    /*
     * (non-Javadoc)
     * @see org.apache.webbeans.component.AbstractComponent#createInstance()
     */
    @Override
    protected T createInstance()
    {
        T instance = null;
        Object parentInstance = getParentInstance();

        try
        {
            InjectableMethods<T> m = new InjectableMethods<T>(creatorMethod, parentInstance, this);
            instance = m.doInjection();

        }
        finally
        {
            if (getParent().getScopeType().equals(Dependent.class))
            {
                destroyBean(getParent(), parentInstance);
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

    /*
     * (non-Javadoc)
     * @see
     * org.apache.webbeans.component.AbstractComponent#destroyInstance(java.
     * lang.Object)
     */
    @Override
    protected void destroyInstance(T instance)
    {
        if (disposalMethod != null)
        {

            Object object = getParentInstance();

            InjectableMethods<T> m = new InjectableMethods<T>(disposalMethod, object, this);
            m.doInjection();

        }

        instance = null;

    }

    protected Object getParentInstance()
    {
        Object parentInstance = ManagerImpl.getManager().getInstance(this.parent);

        return parentInstance;
    }

    protected void checkNullInstance(Object instance)
    {
        if (instance == null)
        {
            if (!this.getScopeType().equals(Dependent.class))
            {
                throw new IllegalProductException("WebBeans producer method : " + creatorMethod.getName() + " return type in the component implementation class : " + this.parent.getReturnType().getName() + " scope type must be @Dependent to create null instance");
            }
        }
    }

    protected void checkScopeType()
    {
        // Scope type check
        ScopeType scope = this.getScopeType().getAnnotation(ScopeType.class);
        if (scope.passivating())
        {
            if (!this.isSerializable())
            {
                throw new IllegalProductException("WebBeans producer method : " + creatorMethod.getName() + " return type in the component implementation class : " + this.parent.getReturnType().getName() + " with passivating scope @" + scope.annotationType().getName() + " must be Serializable");
            }
        }
    }
}