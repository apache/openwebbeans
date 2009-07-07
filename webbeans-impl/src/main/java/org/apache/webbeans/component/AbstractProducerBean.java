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
import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.Producer;

import org.apache.webbeans.util.WebBeansUtil;

/**
 * Abstract class for producer components.
 * 
 * @version $Rev$ $Date$
 * @param <T> bean type info
 */
public abstract class AbstractProducerBean<T> extends AbstractBean<T> implements IBeanHasParent<T>
{
    /** Owner of the producer field component */
    protected AbstractBean<?> ownerComponent;

    /** Producer instance responsible for produce and dispose */
    protected Producer<T> producer;

    /**
     * Create a new instance.
     * 
     * @param type webbeans typr
     * @param returnType bean type info
     * @param ownerComponent owner bean
     */
    protected AbstractProducerBean(WebBeansType type, Class<T> returnType, AbstractBean<?> ownerComponent)
    {
        super(type, returnType);
        this.ownerComponent = ownerComponent;
    }

    /**
     * {@inheritDoc}
     */
    public AbstractBean<?> getParent()
    {
        return this.ownerComponent;
    }

    /**
     * {@inheritDoc}
     */
    public void dispose(T instance)
    {
        // Do nothing
    }

    /**
     * Sets producer instance.
     * 
     * @param producer producer instance
     */
    public void setProducer(Producer<T> producer)
    {
        this.producer = producer;
    }

    /**
     * Gets producer instance.
     * 
     * @return producer instance
     */
    public Producer<T> getProducer()
    {
        return this.producer;
    }

    /**
     * Returns true if producer is set.
     * 
     * @return true if producer instance is set
     */
    protected boolean isProducerSet()
    {
        return producer != null ? true : false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected T createInstance(CreationalContext<T> creationalContext)
    {
        return null;
    }

    /**
     * Destroys bean.
     * 
     * @param <K> bean type info
     * @param bean bean info
     * @param instance bean instance
     */
    @SuppressWarnings("unchecked")
    protected <K> void destroyBean(Bean<?> bean, Object instance)
    {
        Bean<K> destroy = (Bean<K>) bean;
        K inst = (K) instance;

        CreationalContext<K> cc = (CreationalContext<K>) this.creationalContext;
        destroy.destroy(inst, cc);
    }

    /**
     * Returns producer bean's owner bean instance.
     * 
     * @return owner bean instance
     */
    @SuppressWarnings("unchecked")
    protected Object getParentInstance()
    {
        // return getManager().getInstance(this.ownerComponent);

        Object parentInstance = null;

        // Added for most specialized bean
        Annotation[] anns = new Annotation[this.ownerComponent.getBindings().size()];
        anns = this.ownerComponent.getBindings().toArray(anns);

        Bean<?> specialize = WebBeansUtil.getMostSpecializedBean(getManager(), (AbstractBean<T>) this.ownerComponent);

        if (specialize != null)
        {
            parentInstance = getManager().getReference(specialize, null, null);
        }
        else
        {
            parentInstance = getManager().getReference(this.ownerComponent, null, null);
        }

        return parentInstance;

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void destroyInstance(T instance)
    {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<InjectionPoint> getInjectionPoints()
    {
        if (isProducerSet())
        {
            return getProducer().getInjectionPoints();
        }

        return super.getInjectionPoints();
    }

}