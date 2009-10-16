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
package org.apache.webbeans.context;

import java.lang.annotation.Annotation;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.ContextNotActiveException;
import javax.enterprise.context.ConversationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.context.spi.Context;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;

import org.apache.webbeans.context.type.ContextTypes;

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
public abstract class AbstractContext implements WebBeansContext
{
    /**Context status, active or not*/
    protected boolean active;

    /**Context type*/
    protected ContextTypes type;

    /**Context contextual instances*/
    protected Map<Contextual<?>, Object> componentInstanceMap = null;

    /**Contextual Scope Type*/
    protected Class<? extends Annotation> scopeType;
    
    /**Contextual to CreationalContext Map*/
    protected Map<Contextual<?>, CreationalContext<?>> creationalContextMap = new ConcurrentHashMap<Contextual<?>, CreationalContext<?>>();

    /**
     * Creates a new context instance
     */
    protected AbstractContext()
    {

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
        
        return (T) componentInstanceMap.get(component);
    }

    /**
     * {@inheritDoc}
     */
    public <T> T get(Contextual<T> component, CreationalContext<T> creationalContext)
    {
        checkActive();
        
        return getInstance(component, creationalContext);
    }

    /**
     * {@inheritDoc} 
     */
    @SuppressWarnings("unchecked")
    protected <T> T getInstance(Contextual<T> component, CreationalContext<T> creationalContext)
    {
        T instance = (T)componentInstanceMap.get(component);

        
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
                instance = component.create(creationalContext);

                if (instance != null)
                {
                    this.componentInstanceMap.put(component, instance);
                    this.creationalContextMap.put(component, creationalContext);
                }
                
            }            
        }

        return  instance;
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
        Set<Entry<Contextual<?>, Object>> entrySet = componentInstanceMap.entrySet();
        Iterator<Entry<Contextual<?>, Object>> it = entrySet.iterator();

        Contextual<?> component = null;
        while (it.hasNext())
        {
            component = it.next().getKey();
            
            Object instance = componentInstanceMap.get(component);
            //Get creational context
            CreationalContext<Object> cc = (CreationalContext<Object>)this.creationalContextMap.get(component);

            //Destroy instance
            destroyInstance((Contextual<Object>) component, instance, cc);

        }
        
        //Clear cache
        componentInstanceMap.clear();
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
    public Map<Contextual<?>, Object> getComponentInstanceMap()
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

}