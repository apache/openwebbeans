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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Decorator;
import javax.enterprise.inject.spi.Interceptor;

import org.apache.webbeans.container.BeanManagerImpl;
import org.apache.webbeans.context.creational.DependentCreationalContext.DependentType;
import org.apache.webbeans.util.Asserts;
import org.apache.webbeans.util.WebBeansUtil;

/** {@inheritDoc} */
public class CreationalContextImpl<T> implements CreationalContext<T>, Serializable
{
    private static final long serialVersionUID = -3416834742959340960L;

    /**Actual bean instance*/
    private transient volatile Object incompleteInstance = null;
    
    /**Bean proxy*/
    private volatile Object proxyInstance = null;
    
    /**Contextual bean dependent instances*/
    private Map<Object, DependentCreationalContext<?>> dependentObjects = 
        Collections.synchronizedMap(new WeakHashMap<Object, DependentCreationalContext<?>>());
     
    /**Owner bean*/
    private volatile Contextual<T> contextual = null;
    
    /**Owner creational context*/
    private volatile CreationalContextImpl<?> ownerCreational = null;
    
    /**Ejb interceptors*/
    private ConcurrentMap<Class<?>, EjbInterceptorContext> ejbInterceptors = new ConcurrentHashMap<Class<?>, EjbInterceptorContext>();
    
    /**
     * Package private
     */
    CreationalContextImpl(Contextual<T> contextual)
    {
        this.contextual = contextual;
    }
    
    /**
     * Add interceptor instance.
     * @param clazz interceptor class
     * @param instance interceptor instance
     */
    public void addEjbInterceptor(Class<?> clazz, EjbInterceptorContext instance)
    {
        this.ejbInterceptors.putIfAbsent(clazz, instance);
    }
    
    /**
     * Gets interceptor instance.
     * @param clazz interceptor class
     * @return interceptor instance
     */
    public EjbInterceptorContext getEjbInterceptor(Class<?> clazz)
    {
        return this.ejbInterceptors.get(clazz);
    }
    
    
    /**
     * Save this incomplete instance.
     * 
     * @param incompleteInstance incomplete bean instance
     */
    public void push(T incompleteInstance)
    {
        if(this.incompleteInstance != null)
        {
            this.incompleteInstance = incompleteInstance;   
        }
        
    }
    
    /**
     * Sets bean instance proxy.
     * @param proxyInstance proxy
     */
    public void setProxyInstance(Object proxyInstance)
    {
        if(this.proxyInstance != null)
        {
            this.proxyInstance = proxyInstance;   
        }
    }
    
    /**
     * Gets bean proxy.
     * @return bean proxy
     */
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
           DependentCreationalContext<K> dependentCreational = new DependentCreationalContext<K>(cc,dependent);
           if(dependent instanceof Interceptor)
           {
               dependentCreational.setDependentType(DependentType.INTERCEPTOR);
           }
           else if(dependent instanceof Decorator)
           {
               dependentCreational.setDependentType(DependentType.DECORATOR);
           }
           else
           {
               dependentCreational.setDependentType(DependentType.BEAN);
           }
           
           dependentCreational.setInstance(instance);
           this.dependentObjects.put(instance, dependentCreational);   
        }
    }
    
    /**
     * Gets bean interceptor instance.
     * @param interceptor interceptor bean
     * @return bean interceptor instance
     */
    public Object getDependentInterceptor(Contextual<?> interceptor)
    {
        List<DependentCreationalContext<?>> dcs = getDependentInterceptors();
        for(DependentCreationalContext<?> dc : dcs)
        {
            if(dc.getContextual().equals(interceptor))
            {
                return dc.getInstance();
            }
        }
        
        return null;
    }
    
    /**
     * Gets bean decorator instance.
     * @param decorator decorator bean
     * @return bean decorator instance
     */
    public Object getDependentDecorator(Contextual<?> decorator)
    {
        List<DependentCreationalContext<?>> dcs = getDependentDecorators();
        for(DependentCreationalContext<?> dc : dcs)
        {
            if(dc.getContextual().equals(decorator))
            {
                return dc.getInstance();
            }
        }
        
        return null;
    }    
    
    /**
     * Gets list of dependent interceptors context.
     * @return list of dependent interceptors context
     */
    private List<DependentCreationalContext<?>> getDependentInterceptors()
    {
        List<DependentCreationalContext<?>> list = new ArrayList<DependentCreationalContext<?>>();
        Collection<DependentCreationalContext<?>> values = this.dependentObjects.values();
        if(values != null && values.size() > 0)
        {
            Iterator<DependentCreationalContext<?>> it = values.iterator();
            while(it.hasNext())
            {
                DependentCreationalContext<?> dc = it.next();
                if(dc.getDependentType().equals(DependentType.INTERCEPTOR))
                {
                    list.add(dc);
                }
            }
        }
        
        return list;
    }
    
    /**
     * Gets list of dependent decorators context.
     * @return list of dependent decorators context
     */
    private List<DependentCreationalContext<?>> getDependentDecorators()
    {
        List<DependentCreationalContext<?>> list = new ArrayList<DependentCreationalContext<?>>();
        Collection<DependentCreationalContext<?>> values = this.dependentObjects.values();
        if(values != null && values.size() > 0)
        {
            Iterator<DependentCreationalContext<?>> it = values.iterator();
            while(it.hasNext())
            {
                DependentCreationalContext<?> dc = it.next();
                if(dc.getDependentType().equals(DependentType.DECORATOR))
                {
                    list.add(dc);
                }
            }
        }
        
        return list;
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
    public void  removeDependents()
    {
        Collection<?> values = this.dependentObjects.keySet();
        Iterator<?> iterator = values.iterator();
        
        while(iterator.hasNext())
        {
            T instance = (T)iterator.next();
            DependentCreationalContext<T> dependent = (DependentCreationalContext<T>)this.dependentObjects.get(instance);
            dependent.getContextual().destroy(instance, dependent.getCreationalContext());
        }
        
        this.dependentObjects.clear();
        
        Collection<EjbInterceptorContext> interceptors = this.ejbInterceptors.values();
        if(interceptors != null)
        {
            for(EjbInterceptorContext intereptor : interceptors)
            {
                intereptor.getInjectorInstance().destroy();
            }
        }
        
        this.ejbInterceptors.clear();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void release()
    {
        removeDependents();
        this.incompleteInstance = null;
    }
    
    /**
     * Gets owner bean.
     * @return bean
     */
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
        if(this.ownerCreational != null)
        {
            this.ownerCreational = ownerCreational;   
        }
    }

    /**
     * Write Object. 
     */
    private synchronized void writeObject(ObjectOutputStream s)
    throws IOException
    {
        s.writeObject(proxyInstance);

        // we have to remap into a standard HashMap because WeakHashMap is not serializable
        HashMap<Object, DependentCreationalContext<?>> depo
                = new HashMap<Object, DependentCreationalContext<?>>(dependentObjects);
        s.writeObject(depo);

        String id = null;
        if (contextual != null && (id = WebBeansUtil.isPassivationCapable(contextual)) != null)
        {
            s.writeObject(id);
        }
        else
        {
            s.writeObject(null);
        }
        s.writeObject(ownerCreational);
    }


    /**
     * Read object. 
     */
    @SuppressWarnings("unchecked")
    private synchronized void readObject(ObjectInputStream s)
    throws IOException, ClassNotFoundException
    {
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