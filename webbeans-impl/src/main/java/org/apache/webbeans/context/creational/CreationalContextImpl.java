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
package org.apache.webbeans.context.creational;

import java.io.*;
import java.util.*;

import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.PassivationCapable;

import org.apache.webbeans.container.BeanManagerImpl;
import org.apache.webbeans.util.Asserts;

/** {@inheritDoc} */
public class CreationalContextImpl<T> implements CreationalContext<T>, Serializable
{
    private static final long serialVersionUID = -3416834742959340960L;

    private transient Object incompleteInstance = null;
    
    private Object proxyInstance = null;
    
    /**Contextual bean dependent instances*/
    private Map<Object, DependentCreationalContext<?>> dependentObjects = new WeakHashMap<Object, DependentCreationalContext<?>>();
     
    private Contextual<T> contextual = null;
    
    private CreationalContextImpl<?> ownerCreational = null;
    
    private static class DependentCreationalContext<S> implements Serializable
    {
        private static final long serialVersionUID = 7107949019995422165L;

        private CreationalContext<S> creationalContext;
        
        private Contextual<S> contextual;
        
        /**
         * @return the creationalContext
         */
        public CreationalContext<S> getCreationalContext()
        {
            return creationalContext;
        }

        /**
         * @param creationalContext the creationalContext to set
         */
        public void setCreationalContext(CreationalContext<S> creationalContext)
        {
            this.creationalContext = creationalContext;
        }

        /**
         * @return the contextual
         */
        public Contextual<S> getContextual()
        {
            return contextual;
        }

        /**
         * @param contextual the contextual to set
         */
        public void setContextual(Contextual<S> contextual)
        {
            this.contextual = contextual;
        }

        public DependentCreationalContext(CreationalContext<S> cc, Contextual<S> contextual)
        {
            this.contextual = contextual;
            this.creationalContext = cc;
        }
    }
    
    
    /**
     * Package private
     */
    CreationalContextImpl(Contextual<T> contextual)
    {
        this.contextual = contextual;
    }
    
    
    /**
     * Save this incomplete instance.
     * 
     * @param incompleteInstance incomplete bean instance
     */
    public void push(T incompleteInstance)
    {
        this.incompleteInstance = incompleteInstance;
        
    }
    
    public void setProxyInstance(Object proxyInstance)
    {
        this.proxyInstance = proxyInstance;
    }
    
    public Object getProxyInstance()
    {
        return this.proxyInstance;
    }
    
    /**
     * Adds given dependent instance to the map.
     * 
     * @param dependent dependent contextual
     * @param instance dependent instance
     */
    public <K> void addDependent(Contextual<K> dependent, Object instance, CreationalContext<K> cc)
    {
        Asserts.assertNotNull(dependent,"dependent parameter cannot be null");
        
        if(instance != null)
        {
           this.dependentObjects.put(instance, new DependentCreationalContext<K>(cc,dependent));   
        }
    }
    
    /**
     * Returns incomplete instance.
     * 
     * @return incomplete instance
     */
    public Object get()
    {
        return this.incompleteInstance;
    }
    
    
    /**
     * Removes from creational context.
     */
    public void  remove()
    {
        this.incompleteInstance = null;
    }
    
    /**
     * Removes dependent objects.
     */
    @SuppressWarnings("unchecked")
    private void  removeDependents()
    {
        Collection<?> values = this.dependentObjects.keySet();
        Iterator<?> iterator = values.iterator();
        
        while(iterator.hasNext())
        {
            T instance = (T)iterator.next();
            DependentCreationalContext<T> dependent = (DependentCreationalContext<T>)this.dependentObjects.get(instance);
            dependent.getContextual().destroy(instance, (CreationalContext<T>)dependent.getCreationalContext());                
        }
        
        this.dependentObjects.clear();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void release()
    {
        removeDependents();        
        
    }
    
    
    public Contextual<T> getBean()
    {
        return this.contextual;
    }


    /**
     * @return the ownerCreational
     */
    public CreationalContextImpl<?> getOwnerCreational()
    {
        return ownerCreational;
    }


    /**
     * @param ownerCreational the ownerCreational to set
     */
    public void setOwnerCreational(CreationalContextImpl<?> ownerCreational)
    {
        this.ownerCreational = ownerCreational;
    }

    private synchronized void writeObject(ObjectOutputStream s)
    throws IOException
    {
        s.writeObject(incompleteInstance);
        s.writeObject(proxyInstance);

        // we have to remap into a standard HashMap because WeakHashMap is not serializable
        HashMap<Object, DependentCreationalContext<?>> depo
                = new HashMap<Object, DependentCreationalContext<?>>(dependentObjects);
        s.writeObject(depo);

        if (contextual != null && contextual instanceof PassivationCapable)
        {
            s.writeObject(((PassivationCapable)contextual).getId());
        }
        else
        {
            s.writeObject(null);
        }
        s.writeObject(ownerCreational);
    }


    @SuppressWarnings("unchecked")
    private synchronized void readObject(ObjectInputStream s)
    throws IOException, ClassNotFoundException
    {
        incompleteInstance = s.readObject();
        proxyInstance = s.readObject();

        HashMap<Object, DependentCreationalContext<?>> depo = (HashMap<Object, DependentCreationalContext<?>>)s.readObject();
        dependentObjects = new WeakHashMap<Object, DependentCreationalContext<?>>(depo);

        String id = (String) s.readObject();
        if (id != null)
        {
            contextual = (Contextual<T>) BeanManagerImpl.getManager().getPassivationCapableBean(id);
        }
        ownerCreational = (CreationalContextImpl<?>) s.readObject();
    }

}