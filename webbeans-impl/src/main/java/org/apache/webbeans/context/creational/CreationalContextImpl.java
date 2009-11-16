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

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;

import org.apache.webbeans.util.Asserts;

/** {@inheritDoc} */
public class CreationalContextImpl<T> implements CreationalContext<T>
{
    private Object incompleteInstance = null;
    
    /**Contextual bean dependent instances*/
    private Map<Object, DependentCreationalContext<?>> dependentObjects = new WeakHashMap<Object, DependentCreationalContext<?>>();
     
    private Contextual<T> contextual = null;
    
    private static class DependentCreationalContext<S>
    {
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
     * @param incompleteBean instance owner
     * @return incomplete instance
     */
    public Object get()
    {
        return this.incompleteInstance;
    }
    
    
    /**
     * Removes from creational context.
     * 
     * @param bean owner bean
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

}