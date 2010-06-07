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
package org.apache.webbeans.context;

import java.io.*;
import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentMap;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.ContextNotActiveException;
import javax.enterprise.context.ConversationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.context.spi.Context;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;

import org.apache.webbeans.container.SerializableBean;
import org.apache.webbeans.container.SerializableBeanVault;
import org.apache.webbeans.context.creational.BeanInstanceBag;
import org.apache.webbeans.context.type.ContextTypes;
import org.apache.webbeans.util.Asserts;

/**
 * Abstract implementation of the {@link WebBeansContext} interfaces.
 * 
 * @see Context
 * @see RequestContext
 * @see DependentContext
 * @see SessionContext
 * @see ApplicationContext
 * @see ConversationContext
 */
public abstract class AbstractContext implements WebBeansContext, Serializable
{
    /**Context status, active or not*/
    protected volatile boolean active;

    /**Context type*/
    protected ContextTypes type;

    /**Context contextual instances*/
    protected Map<Contextual<?>, BeanInstanceBag<?>> componentInstanceMap = null;

    /**Contextual Scope Type*/
    protected Class<? extends Annotation> scopeType;
    
    /**
     * Creates a new context instance
     */
    protected AbstractContext()
    {

    }
    

    public <T> void initContextualBag(Contextual<T> contextual, CreationalContext<T> creationalContext)
    {
        createContextualBag(contextual, creationalContext);
    }

    @SuppressWarnings("unchecked")
    private <T> void createContextualBag(Contextual<T> contextual, CreationalContext<T> creationalContext)
    {
        BeanInstanceBag<T> bag = new BeanInstanceBag<T>(creationalContext);
        
        if(this.componentInstanceMap instanceof ConcurrentMap)
        {
            T exist = (T) ((ConcurrentMap) this.componentInstanceMap).putIfAbsent(contextual, bag);
            //no instance
            if(exist == null)
            {
                this.componentInstanceMap.put(contextual, bag);
            }
        }
        else
        {
            this.componentInstanceMap.put(contextual , bag);
        }                
    }
    
    /**
     * Creates a new context with given scope type.
     * 
     * @param scopeType context scope type
     */
    protected AbstractContext(Class<? extends Annotation> scopeType)
    {
        this.scopeType = scopeType;
        setComponentInstanceMap();

    }

    /**
     * Creates a new context with given context type.
     * 
     * @param type context type
     */
    protected AbstractContext(ContextTypes type)
    {
        this.type = type;
        configureScopeType(type);
        setComponentInstanceMap();
    }

    /**
     * Configures scope type from context type.
     * 
     * @param type context type
     */
    private void configureScopeType(ContextTypes type)
    {
        if (type.equals(ContextTypes.APPLICATION))
        {
            this.scopeType = ApplicationScoped.class;
        }
        else if (type.equals(ContextTypes.SESSION))
        {
            this.scopeType = SessionScoped.class;
        }
        else if (type.equals(ContextTypes.REQUEST))
        {
            this.scopeType = RequestScoped.class;
        }
        else if (type.equals(ContextTypes.DEPENDENT))
        {
            this.scopeType = Dependent.class;
        }
        else if (type.equals(ContextTypes.CONVERSATION))
        {
            this.scopeType = ConversationScoped.class;
        }
        else if (type.equals(ContextTypes.SINGLETON))
        {
            this.scopeType = ConversationScoped.class;
        }
        else
        {
            throw new IllegalArgumentException("Not known scope type : " + type.toString());
        }

    }
    
    

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public <T> T get(Contextual<T> component)
    {
        checkActive();
        
        if(componentInstanceMap.get(component) != null)
        {
            return (T) componentInstanceMap.get(component).getBeanInstance();    
        }
        
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public <T> T get(Contextual<T> contextual, CreationalContext<T> creationalContext)
    {
        checkActive();
        
        return getInstance(contextual, creationalContext);
    }

    /**
     * {@inheritDoc} 
     */
    @SuppressWarnings("unchecked")
    protected <T> T getInstance(Contextual<T> contextual, CreationalContext<T> creationalContext)
    {
        T instance = null;
        
        //Look for bag
        BeanInstanceBag<T> bag = (BeanInstanceBag<T>)componentInstanceMap.get(contextual);        
        if(bag == null)
        {
            createContextualBag(contextual, creationalContext);
        }
        
        //Look for instance
        instance = (T)componentInstanceMap.get(contextual).getBeanInstance();        
        if (instance != null)
        {
            return instance;
        }

        else
        {
            if(creationalContext == null)
            {
                return null;
            }
            
            else
            {                
                //No instance
                if(instance == null)
                {
                    instance = contextual.create(creationalContext);    
                }
                
                //If succesfull creation
                if (instance != null)
                {
                    bag = (BeanInstanceBag<T>)this.componentInstanceMap.get(contextual);
                    bag.setBeanInstance(instance);
                }
                
            }            
        }

        return  instance;
    }
    
    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public <T> CreationalContext<T> getCreationalContext(Contextual<T> contextual)
    {
        Asserts.assertNotNull(contextual);
        if(this.componentInstanceMap.containsKey(contextual))
        {
            return (CreationalContext<T>)this.componentInstanceMap.get(contextual).getBeanCreationalContext();
        }
        
        return null;
    }

    /**
     * Destroy the given web beans component instance.
     * 
     * @param <T>
     * @param component web beans component
     * @param instance component instance
     */
    private <T> void destroyInstance(Contextual<T> component, T instance,CreationalContext<T> creationalContext)
    {
        //Destroy component
        component.destroy(instance,creationalContext);
    }
    
    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public void destroy()
    {
        Set<Entry<Contextual<?>, BeanInstanceBag<?>>> entrySet = componentInstanceMap.entrySet();
        Iterator<Entry<Contextual<?>, BeanInstanceBag<?>>> it = entrySet.iterator();

        Contextual<?> contextual = null;
        while (it.hasNext())
        {
            contextual = it.next().getKey();
            
            BeanInstanceBag<?> instance = componentInstanceMap.get(contextual);
            //Get creational context
            CreationalContext<Object> cc = (CreationalContext<Object>)instance.getBeanCreationalContext();

            //Destroy instance
            destroyInstance((Contextual<Object>) contextual, instance.getBeanInstance(), cc);
        }
        
        //Clear context map
        this.componentInstanceMap.clear();        
    }

    /**
     * Gets context active flag.
     * 
     * @return active flag
     */
    public boolean isActive()
    {
        return active;
    }

    /**
     * Set component active flag.
     * 
     * @param active active flag
     */
    public void setActive(boolean active)
    {
        this.active = active;
    }

    /**
     * Type of the context.
     * 
     * @return type of the context
     * @see ContextTypes
     */
    public ContextTypes getType()
    {
        return type;
    }

    /**
     * {@inheritDoc}
     */
    public Map<Contextual<?>, BeanInstanceBag<?>> getComponentInstanceMap()
    {
        return componentInstanceMap;
    }

    /**
     * {@inheritDoc}
     */
    public Class<? extends Annotation> getScope()
    {

        return this.scopeType;
    }

    /**
     * {@inheritDoc}
     */
    protected abstract void setComponentInstanceMap();
    
    /**
     * Check that context is active or throws exception.
     */
    protected void checkActive()
    {
        if (!active)
        {
            throw new ContextNotActiveException("WebBeans context with scope annotation @" + getScope().getName() + " is not active with respect to the current thread");
        }        
    }

    /**
     * Write Object.
     */
    private synchronized void writeObject(ObjectOutputStream s)
    throws IOException
    {
        s.writeObject(type);
        s.writeObject(scopeType);

        // we need to repack the Contextual<T> from the componentInstanceMap into Serializable ones
        if (componentInstanceMap != null)
        {
            SerializableBeanVault sbv = SerializableBeanVault.getInstance();

            Map<Contextual<?>, BeanInstanceBag<?>> serializableInstanceMap =
                    new HashMap<Contextual<?>, BeanInstanceBag<?>>();

            for (Map.Entry<Contextual<?>, BeanInstanceBag<?>> componentInstanceMapEntry : componentInstanceMap.entrySet())
            {
                serializableInstanceMap.put(sbv.getSerializableBean(componentInstanceMapEntry.getKey()),
                                            componentInstanceMapEntry.getValue());
            }
            
            s.writeObject(serializableInstanceMap);
        }
        else
        {
            s.writeObject(null);
        }

    }

    /**
     * Read object.
     */
    @SuppressWarnings("unchecked")
    private synchronized void readObject(ObjectInputStream s)
    throws IOException, ClassNotFoundException
    {
        type = (ContextTypes) s.readObject();
        scopeType = (Class<? extends Annotation>) s.readObject();

        HashMap<Contextual<?>, BeanInstanceBag<?>> serializableInstanceMap =
                (HashMap<Contextual<?>, BeanInstanceBag<?>>) s.readObject();

        if (serializableInstanceMap != null)
        {
            setComponentInstanceMap();
            if (componentInstanceMap == null)
            {
                throw new NotSerializableException("componentInstanceMap not initialized!");
            }

            for (Map.Entry<Contextual<?>, BeanInstanceBag<?>> serializableInstanceMapEntry : serializableInstanceMap.entrySet())
            {
                Contextual<?> bean = serializableInstanceMapEntry.getKey();
                if (bean instanceof SerializableBean)
                {
                    componentInstanceMap.put(((SerializableBean<?>)bean).getBean(), serializableInstanceMapEntry.getValue());
                }
                else
                {
                    componentInstanceMap.put(bean, serializableInstanceMapEntry.getValue());
                }
            }
        }
    }
}