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
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;

import org.apache.webbeans.util.Asserts;

/** {@inheritDoc} */
public class CreationalContextImpl<T> implements CreationalContext<T>
{
    /**Map of bean with its incomplete instance*/
    private Map<Contextual<?>,Object> incompleteInstancesMap = new ConcurrentHashMap<Contextual<?>, Object>();

    /**Contextual bean*/
    private Contextual<T> incompleteBean = null;
    
    /**Contextual bean dependent instances*/
    private Map<Object, Contextual<?>> dependentObjects = new WeakHashMap<Object, Contextual<?>>();
    
    /**
     * Package private
     */
    CreationalContextImpl()
    {
        //Empty
    }
    
    /**
     * Returns new creational context.
     * 
     * @param incompleteBean incomplete instance owner
     * @return new creational context
     */
    protected CreationalContextImpl<T> getCreationalContextImpl(Contextual<T> incompleteBean)
    {
        CreationalContextImpl<T> impl = new CreationalContextImpl<T>();        
        
        impl.incompleteBean = incompleteBean;
        impl.incompleteInstancesMap = this.incompleteInstancesMap;
        
        return impl;
        
    }
    
    /**
     * Save this incomplete instance.
     * 
     * @param incompleteInstance incomplete bean instance
     */
    public void push(T incompleteInstance)
    {
        this.incompleteInstancesMap.put(this.incompleteBean, incompleteInstance);
        
    }
    
    
    /**
     * Adds given dependent instance to the map.
     * 
     * @param dependent dependent contextual
     * @param instance dependent instance
     */
    public <K> void addDependent(Contextual<K> dependent, Object instance)
    {
        Asserts.assertNotNull(dependent,"dependent parameter cannot be null");
        
        if(instance != null)
        {
            synchronized (this.dependentObjects)
            {
                this.dependentObjects.put(instance, dependent);   
            }            
        }
    }
    
    /**
     * Returns incomplete instance.
     * 
     * @param incompleteBean instance owner
     * @return incomplete instance
     */
    public Object get(Contextual<?> incompleteBean)
    {
        return incompleteInstancesMap.get(incompleteBean);
    }
    
    
    /**
     * Removes from creational context.
     * 
     * @param bean owner bean
     */
    public void  remove()
    {
        if(this.incompleteInstancesMap.containsKey(this.incompleteBean))
        {
            this.incompleteInstancesMap.remove(this.incompleteBean);
            this.incompleteInstancesMap = null;
        }        
    }
    
    /**
     * Removes dependent objects.
     */
    @SuppressWarnings("unchecked")
    private void  removeDependents()
    {
        //Clear its dependence objects
        synchronized (this.dependentObjects)
        {
            Collection<?> values = this.dependentObjects.keySet();
            Iterator<?> iterator = values.iterator();
            
            while(iterator.hasNext())
            {
                T instance = (T)iterator.next();
                Contextual<T> dependent = (Contextual<T>)this.dependentObjects.get(instance);
                dependent.destroy(instance, (CreationalContext<T>)this);                
            }
            
            this.dependentObjects.clear();
        }
        
    }
    
    /**
     * Clear registry.
     */
    public void clear()
    {
        this.incompleteInstancesMap.clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void release()
    {
        removeDependents();        
        
    }

}