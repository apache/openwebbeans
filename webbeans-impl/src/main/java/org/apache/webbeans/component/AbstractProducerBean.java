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
import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;

import org.apache.webbeans.util.WebBeansUtil;

/**
 * Abstract class for producer components.
 * 
 * @version $Rev$ $Date$
 * @param <T> bean type info
 */
public abstract class AbstractProducerBean<T> extends AbstractOwbBean<T> implements IBeanHasParent<T>
{
    /** Owner of the producer field component */
    protected InjectionTargetBean<?> ownerComponent;
    private Class<T> returnType;

    /**
     * Create a new instance.
     * 
     * @param returnType bean type info
     * @param ownerComponent owner bean
     */
    protected AbstractProducerBean(InjectionTargetBean<?> ownerComponent,
                                   WebBeansType webBeansType,
                                   Set<Type> types,
                                   Set<Annotation> qualifiers,
                                   Class<? extends Annotation> scope,
                                   String name,
                                   boolean nullable,
                                   Class<T> returnType,
                                   Set<Class<? extends Annotation>> stereotypes,
                                   boolean alternative)
    {
        super(ownerComponent.getWebBeansContext(), webBeansType, types, qualifiers, scope, name, nullable, ownerComponent.getBeanClass(), stereotypes, alternative);
        this.ownerComponent = ownerComponent;
        this.returnType = returnType;
    }

    /**
     * {@inheritDoc}
     */
    public InjectionTargetBean<?> getParent()
    {
        return ownerComponent;
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
     * Destroys bean.
     * 
     * @param <K> bean type info
     * @param bean bean info
     * @param instance bean instance
     */
    @SuppressWarnings("unchecked")
    protected <K> void destroyBean(Bean<?> bean, Object instance, CreationalContext<?> creationalContext)
    {
        Bean<K> destroy = (Bean<K>) bean;
        K inst = (K) instance;

        CreationalContext<K> cc = (CreationalContext<K>) creationalContext;
        destroy.destroy(inst, cc);
    }

    /**
     * Returns producer bean's owner bean instance.
     * 
     * @return owner bean instance
     */
    @SuppressWarnings("unchecked")
    protected Object getParentInstance(CreationalContext<?> creationalContext)
    {
        // return getManager().getInstance(this.ownerComponent);

        Object parentInstance;

        Bean<?> specialize = WebBeansUtil.getMostSpecializedBean(getManager(),
                (AbstractOwbBean<T>) ownerComponent);

        if (specialize != null)
        {
            parentInstance = getManager().getReference(specialize, null, creationalContext);
        }
        else
        {
            parentInstance = getManager().getReference(ownerComponent, null, creationalContext);
        }

        return parentInstance;

    }
    
    @SuppressWarnings("unchecked")
    protected Object getParentInstanceFromContext(CreationalContext<?> creationalContext)
    {
        Object parentInstance;

        Bean<?> specialize = WebBeansUtil.getMostSpecializedBean(getManager(),
                (AbstractOwbBean<T>) ownerComponent);

        if (specialize != null)
        {
            parentInstance = getManager().getContext(specialize.getScope()).
                    get((Bean<Object>)specialize,(CreationalContext<Object>) creationalContext);
        }
        else
        {
            parentInstance = getManager().getContext(
                    ownerComponent.getScope()).get((Bean<Object>)ownerComponent,
                                                   (CreationalContext<Object>) creationalContext);
        }

        return parentInstance;

    }

    protected boolean isPassivationCapable(Class<?> returnType, Integer modifiers)
    {
        if(Modifier.isFinal(modifiers) && !(Serializable.class.isAssignableFrom(returnType)))
        {
            return false;
        }
        
        if(returnType.isPrimitive() || Serializable.class.isAssignableFrom(returnType))
        {
            return true;
        }
        
        return false;
    }

    public void validatePassivationDependencies()
    {
        // don't call super.validatePassivationDependencies()!
        // the injection points of producers are the parameters of the producermethod.
        // since CDI-1.1 we must not check those for is serializable anymore.
    }

    @Override
    public int hashCode()
    {
        return super.hashCode() ^ ownerComponent.hashCode();
    }

    public boolean equals(Object object)
    {
        if (!super.equals(object))
        {
            return false;
        }
        AbstractProducerBean<?> other = (AbstractProducerBean<?>) object;
        return ownerComponent.equals(other.ownerComponent);
    }
}
