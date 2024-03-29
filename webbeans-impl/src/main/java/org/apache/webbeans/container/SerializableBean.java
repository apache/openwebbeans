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
package org.apache.webbeans.container;

import org.apache.webbeans.component.OwbBean;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.exception.WebBeansDeploymentException;

import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.enterprise.inject.spi.PassivationCapable;
import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Set;


/**
 * Wrapper to make all our Beans serializable.
 * This is basically a delegate to an underlying Bean&lt;T&gt;.
 *
 * We use the {@link PassivationCapable#getId()} and
 * {@link jakarta.enterprise.inject.spi.BeanManager#getPassivationCapableBean(String)}
 * for serialisation and deserialisation.
 *
 * @version $Rev$ $Date$
 */
public final class SerializableBean<T> implements Bean<T>, PassivationCapable, Serializable 
{

    private static final long serialVersionUID = -8141263188006177021L;

    /** the delegated bean */
    private Bean<T> bean;


    /**
     * @return the delegated internal Bean. 
     */
    public Bean<T> getBean()
    {
        return bean;
    }

    /**
     * This constructor shall not be invoked directly, but only get called
     * from {@link org.apache.webbeans.container.SerializableBeanVault}
     * @param bean the PassivationCapable bean which should be made Serializable
     */
    SerializableBean(Bean<T> bean)
    {
        this.bean = bean;
    }

    @Override
    public Set<Type> getTypes()
    {
        return bean.getTypes();
    }

    @Override
    public Set<Annotation> getQualifiers()
    {
        return bean.getQualifiers();
    }

    @Override
    public Class<? extends Annotation> getScope()
    {
        return bean.getScope();
    }

    @Override
    public String getName()
    {
        return bean.getName();
    }

    @Override
    public Set<InjectionPoint> getInjectionPoints()
    {
        return bean.getInjectionPoints();
    }

    @Override
    public Class<?> getBeanClass()
    {
        return bean.getBeanClass();
    }

    @Override
    public Set<Class<? extends Annotation>> getStereotypes()
    {
        return bean.getStereotypes();
    }

    @Override
    public boolean isAlternative()
    {
        return bean.isAlternative();
    }

    @Override
    public T create(CreationalContext<T> tCreationalContext)
    {
        return bean.create(tCreationalContext);
    }

    @Override
    public void destroy(T instance, CreationalContext<T> tCreationalContext)
    {
        bean.destroy(instance, tCreationalContext);
    }

    @Override
    public String getId()
    {
        return ((OwbBean<?>) bean).getId();
    }

    private void writeObject(ObjectOutputStream s)
    throws IOException
    {
        String id = getId();
        if (id == null)
        {
            throw new NotSerializableException();
        }
        
        s.writeObject(id);
    }


    @SuppressWarnings("unchecked")
    private void readObject(ObjectInputStream s)
    throws IOException, ClassNotFoundException
    {
        String id = (String) s.readObject();
        Bean<T> b = (Bean<T>) WebBeansContext.currentInstance().getBeanManagerImpl().getPassivationCapableBean(id);
        if (b == null)
        {
            throw new WebBeansDeploymentException("cannot deserialize Bean with PassivationCapable id=" + id);
        }
        if (b instanceof SerializableBean)
        {
            b = ((SerializableBean<T>)b).getBean();
        }
        
        bean = b;
    }

    /**
     * If the other object is a SerializableBean too, we compare the 2 underlying wrapped beans.
     * 
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object other)
    {
        if (other instanceof SerializableBean<?>)
        {
            return bean.equals(((SerializableBean<?>)other).getBean());
        }
        
        return bean.equals(other) || super.equals(other);
    }

    /**
     * We need to return the hashCode of the wrapped underlying bean, otherwise the context
     * won't work.
     * @return hashCode of the underlying bean instance.
     */
    @Override
    public int hashCode()
    {
        return bean.hashCode();
    }

}
