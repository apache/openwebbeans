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
package org.apache.webbeans.container;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.context.CreationalContext;
import javax.event.Observer;
import javax.inject.AmbiguousDependencyException;
import javax.inject.DeploymentException;
import javax.inject.TypeLiteral;
import javax.inject.UnsatisfiedDependencyException;
import javax.inject.manager.Bean;
import javax.inject.manager.Decorator;
import javax.inject.manager.InjectionPoint;
import javax.inject.manager.InterceptionType;
import javax.inject.manager.Interceptor;
import javax.inject.manager.Manager;

/**
 * Each Manager can start a new child activity as defined by the JSR-299 spec chapter 11.6
 * 
 * A ChildActivityManager tries to handle the beans, contexts, ... itself 
 * and delegates all other requests to it's parent Manager. 
 *
 */
public class ChildActivityManager extends ManagerImpl
{    
    
    /**
     * the ct will be called by {@code Manager#createActivity()}
     * @param parent is the Manager calling this ct
     */
    public ChildActivityManager(ManagerImpl parent)
    {
        super();
        
        setParent(parent);
    }
    
    /** {@inheritDoc} */
    public Manager addBean(Bean<?> bean)
    {
        if(checkBean(bean, getParent()))
        {
            throw new DeploymentException(bean.toString() + " already registered with parent manager!");
        }
        
        return this;
    }
    
    /**
     * Returns true if bean exist in parent false owise.
     * 
     * @param bean check bean
     * @param parent parent or the child or null if root
     * @return true if bean exist in parent false owise.
     */
    private boolean checkBean(Bean<?> bean, ManagerImpl parent)
    {
        if(parent == null)
        {
            return false;
        }
        else
        {
            Set<Annotation> bindings = bean.getBindings();
            Set<Type> apiTypes = bean.getTypes();
            
            Set<Bean<?>> beans = parent.getBeans();
            boolean found = false;
            for(Bean<?> b : beans)
            {
                Set<Annotation> parentBindings = b.getBindings();
                Set<Type> parentApiTypes = b.getTypes();
                
                if(parentBindings.containsAll(bindings))
                {
                    for(Type t : apiTypes)
                    {
                        
                        if(parentApiTypes.contains(t) && (!t.equals(Object.class)))
                        {
                            found = true;
                            break;
                        }
                    }                
                }
                
                if(found)
                {
                    break;
                }
            }
            
            if(found)
            {
                return true;
            }        
            
            return checkBean(bean, parent.getParent());
        }        
    }

    /** {@inheritDoc} */
    public Manager addDecorator(Decorator decorator)
    {
        throw new UnsupportedOperationException("Decorators may not be registered with a child activity.");
    }

    /** {@inheritDoc} */
    public Manager addInterceptor(Interceptor interceptor)
    {
        throw new UnsupportedOperationException("Interceptors may not be registered with a child activity.");
    }


    /** {@inheritDoc} */
    public void fireEvent(Object event, Annotation... bindings)
    {
        super.fireEvent(event, bindings);
        
        getParent().fireEvent(event, bindings);
    }


    /** {@inheritDoc} */
    public <T> T getInstance(Bean<T> bean)
    {
        T obj = null;
        
        try
        {
            
            obj = super.getInstance(bean);
            
        }catch(UnsatisfiedDependencyException e1)
        {            
        }catch (AmbiguousDependencyException e2) {
        }
        
        
        if (obj == null) 
        {
            obj = getParent().getInstance(bean);
        }
        
        return obj;
    }

    /** {@inheritDoc} */
    public Object getInstanceByName(String name)
    {
        Object obj = null;
        
        try
        {
            
            obj = super.getInstanceByName(name);
            
        }catch(UnsatisfiedDependencyException e1)
        {            
        }catch (AmbiguousDependencyException e2) {
        }
        
        
        if (obj == null) 
        {
            obj = getParent().getInstanceByName(name);
        }
        return obj;
    }

    /** {@inheritDoc} */
    public <T> T getInstanceByType(Class<T> type, Annotation... bindingTypes)
    {
        //X TODO not 100% sure if this is ok. There is some 'double-definition' exception case defined in the spec ...
        T obj = null;
        
        try
        {
            
            obj =  super.getInstanceByType(type, bindingTypes);
            
        }catch(UnsatisfiedDependencyException e1)
        {            
        }catch (AmbiguousDependencyException e2) {
        }
        
        
        if (obj == null) 
        {
            obj = getParent().getInstanceByType(type, bindingTypes);
        }
        return obj;
    }

    /** {@inheritDoc} */
    public <T> T getInstanceByType(TypeLiteral<T> type, Annotation... bindingTypes)
    {
        //X TODO not 100% sure if this is ok. There is some 'double-definition' exception case defined in the spec ...
        T obj = null;
       
        try
        {
            
            obj =  super.getInstanceByType(type, bindingTypes);
            
        }catch(UnsatisfiedDependencyException e1)
        {            
        }catch (AmbiguousDependencyException e2) {
        }
        
        
        if (obj == null) 
        {
            obj = getParent().getInstanceByType(type, bindingTypes);
        }
        return obj;
    }

    /** {@inheritDoc} */
    public <T> T getInstanceToInject(InjectionPoint injectionPoint, CreationalContext<?> context)
    {
        T obj = null;
        
        try
        {
            
            obj =  super.getInstanceToInject(injectionPoint, context); //X ugly <T> due to javac bug 6302954
            
        }catch(UnsatisfiedDependencyException e1)
        {            
        }catch (AmbiguousDependencyException e2) {
        }
        
        
        if (obj == null) 
        {
            obj = getParent().getInstanceToInject(injectionPoint, context); //X ugly <T> due to javac bug 6302954
        }
        return obj;
    }

    /** {@inheritDoc} */
    public Object getInstanceToInject(InjectionPoint injectionPoint)
    {
        Object obj = null;
        
        try
        {
            
            obj = super.getInstanceToInject(injectionPoint); //X ugly <T> due to javac bug 6302954
            
        }catch(UnsatisfiedDependencyException e1)
        {            
        }catch (AmbiguousDependencyException e2) {
        }
        
        
        if (obj == null) 
        {
            obj = getParent().getInstanceToInject(injectionPoint); //X ugly <T> due to javac bug 6302954
        }
        return obj;
    }



    /** {@inheritDoc} */
    public Set<Bean<?>> resolveByName(String name)
    {
        //X TODO not 100% sure if this is ok. There is some 'double-definition' exception case defined in the spec ...
        Set<Bean<?>> set = super.resolveByName(name);
        
        if (set == null || set.isEmpty()) 
        {
            set = getParent().resolveByName(name);
        }
        return set;
    }

    /** {@inheritDoc} */
    public <T> Set<Bean<T>> resolveByType(Class<T> type, Annotation... bindings)
    {
        //X TODO not 100% sure if this is ok. There is some 'double-definition' exception case defined in the spec ...
        Set<Bean<T>> set = super.resolveByType(type, bindings);
       
        if (set == null || set.isEmpty()) 
        {
            set = getParent().resolveByType(type, bindings);
        }
        return set;
    }

    /** {@inheritDoc} */
    public <T> Set<Bean<T>> resolveByType(TypeLiteral<T> apiType, Annotation... bindingTypes)
    {
        //X TODO not 100% sure if this is ok. There is some 'double-definition' exception case defined in the spec ...
        Set<Bean<T>> set = super.resolveByType(apiType, bindingTypes);
       
        if (set == null || set.isEmpty()) 
        {
            set = getParent().resolveByType(apiType, bindingTypes);
        }
        return set;
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    public List<Decorator> resolveDecorators(Set<Type> types, Annotation... bindingTypes)
    {
        return Collections.EMPTY_LIST;    
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    public List<Interceptor> resolveInterceptors(InterceptionType type, Annotation... interceptorBindings)
    {
        return Collections.EMPTY_LIST;    
    }

    /** {@inheritDoc} */
    public <T> Set<Observer<T>> resolveObservers(T event, Annotation... bindings)
    {
        //X TODO not 100% sure if this is ok. There is some 'double-definition' exception case defined in the spec ...
        Set<Observer<T>> set = super.resolveObservers(event, bindings);
       
        if (set == null || set.isEmpty()) 
        {
            set = getParent().resolveObservers(event, bindings);
        }
        return set;
    }

 }