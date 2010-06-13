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
package org.apache.webbeans.jsf.scopes;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.context.ContextNotActiveException;
import javax.enterprise.context.spi.Context;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;

import javax.faces.bean.ViewScoped;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;
import javax.faces.event.PreDestroyViewMapEvent;
import javax.faces.event.SystemEvent;
import javax.faces.event.SystemEventListener;

import org.apache.webbeans.jsf.JSFUtil;

/**
 * This class provides the contexts lifecycle for the 
 * new JSF-2 @ViewScoped Context
 *
 */
public class ViewScopedContext implements Context, SystemEventListener 
{

    private final static String COMPONENT_MAP_NAME ="openwebbeans.componentInstanceMap";
    private final static String CREATIONAL_MAP_NAME ="openwebbeans.creationalInstanceMap";
    
    private boolean isJsfSubscribed = false;
    
    @Override
    public <T> T get(Contextual<T> component) 
    {
        checkActive();
        
        if(!isJsfSubscribed) 
        {
            FacesContext.getCurrentInstance().getApplication().subscribeToEvent(PreDestroyViewMapEvent.class, this);
            
            isJsfSubscribed = true;
        }

        Map<String, Object> viewMap = JSFUtil.getViewRoot().getViewMap();

        @SuppressWarnings("unchecked")
        ConcurrentHashMap<Contextual<?>, Object> componentInstanceMap = (ConcurrentHashMap<Contextual<?>, Object>) viewMap.get(COMPONENT_MAP_NAME);
        
        if(componentInstanceMap == null)
        {
            return null;
        }
        
        @SuppressWarnings("unchecked")
        T instance = (T) componentInstanceMap.get(component);
        
        return instance;
    }

    @Override
    public <T> T get(Contextual<T> component, CreationalContext<T> creationalContext) 
    {
        checkActive();
        
        Map<String, Object> viewMap = JSFUtil.getViewRoot().getViewMap(true);

        @SuppressWarnings("unchecked")
        ConcurrentHashMap<Contextual<?>, Object> componentInstanceMap = (ConcurrentHashMap<Contextual<?>, Object>) viewMap.get(COMPONENT_MAP_NAME);

        if(componentInstanceMap == null) 
        {
            // TODO we now need to start being carefull with reentrancy...
            componentInstanceMap = new ConcurrentHashMap<Contextual<?>, Object>();
            viewMap.put(COMPONENT_MAP_NAME, componentInstanceMap); 
        }

        @SuppressWarnings("unchecked")
        ConcurrentHashMap<Contextual<?>, CreationalContext<?>> creationalContextMap = (ConcurrentHashMap<Contextual<?>, CreationalContext<?>>) viewMap.get(CREATIONAL_MAP_NAME);
        if(creationalContextMap == null) 
        {
            // TODO we now need to start being carefull with reentrancy...
            creationalContextMap = new ConcurrentHashMap<Contextual<?>, CreationalContext<?>>();
            viewMap.put(CREATIONAL_MAP_NAME, creationalContextMap); 
        }

        @SuppressWarnings("unchecked")
        T instance = (T) componentInstanceMap.get(component);
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
                synchronized (componentInstanceMap)
                {
                    // just to make sure...
                    @SuppressWarnings("unchecked")
                    T i = (T)componentInstanceMap.get(component);
                    if (i != null)
                    {
                        return i;
                    }
                    
                    instance = component.create(creationalContext);
    
                    if (instance != null)
                    {
                        componentInstanceMap.put(component, instance);
                        creationalContextMap.put(component, creationalContext);
                    }
                }
            }            
        }

        return  instance;
    }

    @Override
    public Class<? extends Annotation> getScope() 
    {
        return ViewScoped.class;
    }

    /**
     * The view context is active if a valid ViewRoot could be detected.
     */
    @Override
    public boolean isActive() 
    {
        return JSFUtil.getViewRoot() != null;
    }
    
    private void checkActive()
    {
        if (!isActive())
        {
            throw new ContextNotActiveException("WebBeans context with scope annotation @ViewScoped is not active with respect to the current thread");
        }        
    }

    @Override
    public boolean isListenerForSource(Object source)
    {
        if (source instanceof UIViewRoot)
        {
            return true;
        }
        
        return false;
    }

    /**
     * We get PreDestroyViewMapEvent events from the JSF servlet and destroy our contextual
     * instances. This should (theoretically!) also get fired if the webapp closes, so there 
     * should be no need to manually track all view scopes and destroy them at a shutdown. 
     * 
     * @see javax.faces.event.SystemEventListener#processEvent(javax.faces.event.SystemEvent)
     */
    @Override
    @SuppressWarnings("unchecked")
    public void processEvent(SystemEvent event)
    {
        if (event instanceof PreDestroyViewMapEvent)
        {
            // better use the viewmap we get from the event to prevent concurrent modification problems
            Map<String, Object> viewMap = ((UIViewRoot) event.getSource()).getViewMap();

            ConcurrentHashMap<Contextual<?>, Object> componentInstanceMap 
                    = (ConcurrentHashMap<Contextual<?>, Object>) viewMap.get(COMPONENT_MAP_NAME);

            ConcurrentHashMap<Contextual<?>, CreationalContext<?>> creationalContextMap 
                    = (ConcurrentHashMap<Contextual<?>, CreationalContext<?>>) viewMap.get(CREATIONAL_MAP_NAME);

            if(componentInstanceMap != null)
            {
                for ( Entry<Contextual<?>, Object> componentEntry : componentInstanceMap.entrySet())
                {
                    // there is no nice way to explain the Java Compiler that we are handling the same type T,
                    // therefore we need completely drop the type information :(  
                    Contextual contextual = componentEntry.getKey();
                    Object instance = componentEntry.getValue();
                    CreationalContext creational = creationalContextMap.get(contextual);
                    
                    contextual.destroy(instance, creational);
                }
            }
        }
    }
        
}
