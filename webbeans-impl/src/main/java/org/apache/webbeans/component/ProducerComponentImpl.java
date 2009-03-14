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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import javax.context.CreationalContext;
import javax.context.Dependent;
import javax.inject.manager.Bean;

import org.apache.webbeans.context.ContextFactory;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.inject.InjectableMethods;
import org.apache.webbeans.util.WebBeansUtil;

/**
 * Concrete implementation of the {@link AbstractComponent}.
 * <p>
 * It is defined as producer method component.
 * </p>
 * 
 * @author <a href="mailto:gurkanerdogdu@yahoo.com">Gurkan Erdogdu</a>
 * @since 1.0
 */
public class ProducerComponentImpl<T> extends AbstractComponent<T> implements IComponentHasParent
{
    /** Parent component that this producer method belongs */
    protected AbstractComponent<?> parent;

    /** Creator method of the parent component */
    protected Method creatorMethod;

    /** Disposal method */
    protected Method disposalMethod;

    protected boolean fromRealizes;

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
    protected T createInstance(CreationalContext<T> creationalContext)
    {
        T instance = null;
        Object parentInstance = null;
        boolean dependentContext = false;
        
        try
        {
            if (getParent().getScopeType().equals(Dependent.class))
            {
                if(!ContextFactory.checkDependentContextActive())
                {
                    ContextFactory.activateDependentContext();
                    dependentContext = true;
                }
            }
            
            if(!Modifier.isStatic(creatorMethod.getModifiers()))
            {
                parentInstance = getParentInstance();
            }
            
            InjectableMethods<T> m = new InjectableMethods<T>(creatorMethod, parentInstance, this,null);
            instance = m.doInjection();

        }
        finally
        {
            if (getParent().getScopeType().equals(Dependent.class))
            {
                destroyBean(getParent(), parentInstance);
                
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
            Object parentInstance = getParentInstance();
            boolean dependentContext = false;
            try
            {
                if (getParent().getScopeType().equals(Dependent.class))
                {
                    if(!ContextFactory.checkDependentContextActive())
                    {
                        ContextFactory.activateDependentContext();
                        dependentContext = true;
                    }
                }

                InjectableMethods<T> m = new InjectableMethods<T>(disposalMethod, parentInstance, this,null);
                m.doInjection();

            }
            finally
            {
                if (getParent().getScopeType().equals(Dependent.class))
                {
                    destroyBean(getParent(), parentInstance);

                }
                
                if(dependentContext)
                {
                    ContextFactory.passivateDependentContext();
                }                

            }
        }
    }

    protected Object getParentInstance()
    {
        Annotation[] anns = new Annotation[this.parent.getBindings().size()];
        anns = this.parent.getBindings().toArray(anns);
        
        Object parentInstance = getManager().getInstanceByType(this.parent.getReturnType(),anns);

        return parentInstance;
    }

    protected void checkNullInstance(Object instance)
    {
        String errorMessage = "WebBeans producer method : " + creatorMethod.getName() + " return type in the component implementation class : " + this.parent.getReturnType().getName() + " scope type must be @Dependent to create null instance";
        WebBeansUtil.checkNullInstance(instance, this.getScopeType(), errorMessage);
    }

    protected void checkScopeType()
    {
        String errorMessage = "WebBeans producer method : " + creatorMethod.getName() + " return type in the component implementation class : " + this.parent.getReturnType().getName() + " with passivating scope @" + this.getScopeType().getName() + " must be Serializable";
        WebBeansUtil.checkSerializableScopeType(this.getScopeType(), this.isSerializable(), errorMessage);

    }

    /**
     * @return the fromRealizes
     */
    public boolean isFromRealizes()
    {
        return fromRealizes;
    }

    /**
     * @param fromRealizes the fromRealizes to set
     */
    public void setFromRealizes(boolean fromRealizes)
    {
        this.fromRealizes = fromRealizes;
    }

    public String toString()
    {
        return super.toString();
    }
}