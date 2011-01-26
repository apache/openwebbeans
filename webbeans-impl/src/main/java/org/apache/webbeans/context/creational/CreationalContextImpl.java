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
package org.apache.webbeans.context.creational;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Decorator;
import javax.enterprise.inject.spi.Interceptor;

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.context.creational.DependentCreationalContext.DependentType;
import org.apache.webbeans.util.Asserts;
import org.apache.webbeans.util.WebBeansUtil;

/** {@inheritDoc} */
public class CreationalContextImpl<T> implements CreationalContext<T>, Serializable
{
    //Default serial id
    private static final long serialVersionUID = 1L;

    /**Contextual bean dependent instances*/
    //contextual instance --> dependents
    private Map<Object, List<DependentCreationalContext<?>>> dependentObjects =
        Collections.synchronizedMap(new WeakHashMap<Object, List<DependentCreationalContext<?>>>());
     
    /**Contextual bean*/
    private volatile Contextual<T> contextual = null;
        
    /**Ejb interceptors*/
    //contextual instance --> interceptors
    private ConcurrentMap<Object, List<EjbInterceptorContext>> ejbInterceptors = null;

    /**When bean object is destroyed it is set*/
    public static ThreadLocal<Object> currentRemoveObject = new ThreadLocal<Object>();

    private WebBeansContext webBeansContext;

    /**
     * Package private
     */
    CreationalContextImpl(Contextual<T> contextual, WebBeansContext webBeansContext)
    {
        this.contextual = contextual;
        this.webBeansContext = webBeansContext;
    }
    
    /**
     * Add interceptor instance.
     * @param ownerInstance
     * @param instance interceptor instance
     */
    public void addEjbInterceptor(Object ownerInstance, EjbInterceptorContext instance)
    {
        Asserts.assertNotNull(ownerInstance,"Owner instance parameter can not be null");
        Asserts.assertNotNull(instance,"Instance parameter can not be null");
        
        List<EjbInterceptorContext> list = null;
        if (this.ejbInterceptors == null)
        {
            this.ejbInterceptors =  new ConcurrentHashMap<Object, List<EjbInterceptorContext>>();
        }
        else
        {
            list = this.ejbInterceptors.get(ownerInstance);
        }

        if(list == null)
        {
            list = new ArrayList<EjbInterceptorContext>();
            List<EjbInterceptorContext> oldList = this.ejbInterceptors.putIfAbsent(ownerInstance, list);
            if(oldList != null)
            {
                list = oldList;
            }
        }
        
        
        list.add(instance);
    }
    
    /**
     * Gets interceptor instance.
     * @param clazz interceptor class
     * @return interceptor instance
     */
    public EjbInterceptorContext getEjbInterceptor(Object ownerInstance,Class<?> clazz)
    {
        Asserts.assertNotNull(ownerInstance,"Owner instance can not be null");

        if (this.ejbInterceptors == null)
        {
            return null;
        }
        
        List<EjbInterceptorContext> ejbInterceptors = this.ejbInterceptors.get(ownerInstance);
        if(ejbInterceptors != null)
        {
            for(EjbInterceptorContext ejbInterceptor : ejbInterceptors)
            {
                if(ejbInterceptor.getInterceptorClass() == clazz)
                {
                    return ejbInterceptor;
                }
            }            
        }
        
        return null;
    }
    
    
    /**
     * Save this incomplete instance.
     * 
     * @param incompleteInstance incomplete bean instance
     */
    public void push(T incompleteInstance)
    {
        //No-action
    }
        
    /**
     * Adds given dependent instance to the map.
     * 
     * @param dependent dependent contextual
     * @param instance dependent instance
     */
    public <K> void addDependent(Object ownerInstance, Contextual<K> dependent, Object instance)
    {
        Asserts.assertNotNull(dependent,"dependent parameter cannot be null");
        
        if(instance != null)
        {
           DependentCreationalContext<K> dependentCreational = new DependentCreationalContext<K>(dependent);
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
           List<DependentCreationalContext<?>> dependentList = this.dependentObjects.get(ownerInstance);
           if(dependentList == null)
           {
               dependentList = new ArrayList<DependentCreationalContext<?>>();
               this.dependentObjects.put(ownerInstance, dependentList);
           }
           
           dependentList.add(dependentCreational);   
        }
    }
    
    /**
     * Gets bean interceptor instance.
     * @param interceptor interceptor bean
     * @return bean interceptor instance
     */
    public Object getDependentInterceptor(Object ownerInstance, Contextual<?> interceptor)
    {
        Asserts.assertNotNull(interceptor,"Interceptor parameter can not be null");
        
        if(ownerInstance != null)
        {
            List<DependentCreationalContext<?>> dcs = getDependentInterceptors(ownerInstance);
            for(DependentCreationalContext<?> dc : dcs)
            {
                if(dc.getContextual().equals(interceptor))
                {
                    return dc.getInstance();
                }
            }            
        }
        
        return null;
    }
    
    /**
     * Gets bean decorator instance.
     * @param decorator decorator bean
     * @return bean decorator instance
     */
    public Object getDependentDecorator(Object ownerInstance, Contextual<?> decorator)
    {
        Asserts.assertNotNull(decorator, "Decorator parameter can not be null");
        List<DependentCreationalContext<?>> dcs = getDependentDecorators(ownerInstance);
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
    private List<DependentCreationalContext<?>> getDependentInterceptors(Object ownerInstance)
    {
        List<DependentCreationalContext<?>> list = new ArrayList<DependentCreationalContext<?>>();
        
        if(ownerInstance != null)
        {
            List<DependentCreationalContext<?>> values = this.dependentObjects.get(ownerInstance);
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
        }
        
        return list;
    }
    
    /**
     * Gets list of dependent decorators context.
     * @return list of dependent decorators context
     */
    private List<DependentCreationalContext<?>> getDependentDecorators(Object ownerInstance)
    {
        List<DependentCreationalContext<?>> list = new ArrayList<DependentCreationalContext<?>>();

        if (ownerInstance != null)
        {
            List<DependentCreationalContext<?>> values = this.dependentObjects.get(ownerInstance);
            if (values != null && values.size() > 0)
            {
                Iterator<DependentCreationalContext<?>> it = values.iterator();
                while (it.hasNext())
                {
                    DependentCreationalContext<?> dc = it.next();
                    if (dc.getDependentType().equals(DependentType.DECORATOR))
                    {
                        list.add(dc);
                    }
                }
            }
        }
        
        return list;
    }
    
    /**
     * Removes dependent objects.
     */
    @SuppressWarnings("unchecked")
    public void  removeDependents(Object ownerInstance)
    {
        if(ownerInstance == null)
        {
            return;
        }
        
        List<DependentCreationalContext<?>> values = this.dependentObjects.get(ownerInstance);
        if(values != null)
        {
            final CreationalContextFactory contextFactory = webBeansContext.getCreationalContextFactory();
            Iterator<?> iterator = values.iterator();        
            while(iterator.hasNext())
            {
                DependentCreationalContext<T> dependent = (DependentCreationalContext<T>)iterator.next();
                dependent.getContextual().destroy((T)dependent.getInstance(), contextFactory.getCreationalContext(dependent.getContextual()));
            }

            this.dependentObjects.remove(ownerInstance);                        
        }

        if (this.ejbInterceptors != null)
        {
            List<EjbInterceptorContext> interceptors = this.ejbInterceptors.get(ownerInstance);
            if(interceptors != null)
            {
                for(EjbInterceptorContext intereptor : interceptors)
                {
                    intereptor.getInjectorInstance().destroy();
                }
            }

            this.ejbInterceptors.remove(ownerInstance);
        }
    }
    
    @SuppressWarnings("unchecked")
    public void removeAllDependents()
    {
        WebBeansContext webBeansContext = this.webBeansContext;
        Collection<List<DependentCreationalContext<?>>> values = this.dependentObjects.values();
        if(values != null)
        {
            for(List<DependentCreationalContext<?>> value : values)
            {
                if(values != null)
                {
                    Iterator<?> iterator = value.iterator();        
                    while(iterator.hasNext())
                    {
                        DependentCreationalContext<T> dependent = (DependentCreationalContext<T>)iterator.next();
                        final CreationalContextFactory contextFactory = webBeansContext.getCreationalContextFactory();
                        dependent.getContextual().destroy((T)dependent.getInstance(), contextFactory.getCreationalContext(dependent.getContextual()));
                    }                        
                }                
            }
        }
        
        this.dependentObjects.clear();
        
        Collection<List<EjbInterceptorContext>> interceptorValues = null;
        if (this.ejbInterceptors != null)
        {
            interceptorValues = this.ejbInterceptors.values();
        }

        if(interceptorValues != null)
        {
            for(List<EjbInterceptorContext> interceptors : interceptorValues)
            {
                if(interceptors != null)
                {
                    for(EjbInterceptorContext intereptor : interceptors)
                    {
                        intereptor.getInjectorInstance().destroy();
                    }
                }                
            }

            this.ejbInterceptors.clear();
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void release()
    {
        if(currentRemoveObject.get() == null)
        {
            removeAllDependents();
        }
        else
        {
            removeDependents(currentRemoveObject.get());   
        }
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
     * Write Object. 
     */
    private synchronized void writeObject(ObjectOutputStream s)
    throws IOException
    {
        // we have to remap into a standard HashMap because WeakHashMap is not serializable
        HashMap<Object, List<DependentCreationalContext<?>>> depo
                = new HashMap<Object, List<DependentCreationalContext<?>>>(dependentObjects);
        s.writeObject(depo);

        String id = WebBeansUtil.isPassivationCapable(contextual);
        if (contextual != null && id != null)
        {
            s.writeObject(id);
        }
        else
        {
            s.writeObject(null);
        }
        s.writeObject(ejbInterceptors);
    }


    /**
     * Read object. 
     */
    @SuppressWarnings("unchecked")
    private synchronized void readObject(ObjectInputStream s)
    throws IOException, ClassNotFoundException
    {
        this.webBeansContext = WebBeansContext.currentInstance();
        HashMap<Object, List<DependentCreationalContext<?>>> depo = (HashMap<Object, List<DependentCreationalContext<?>>>)s.readObject();
        dependentObjects = new WeakHashMap<Object, List<DependentCreationalContext<?>>>(depo);

        String id = (String) s.readObject();
        if (id != null)
        {
            contextual = (Contextual<T>) webBeansContext.getBeanManagerImpl().getPassivationCapableBean(id);
        }
                
        ejbInterceptors = (ConcurrentMap<Object, List<EjbInterceptorContext>>) s.readObject();
    }

}
