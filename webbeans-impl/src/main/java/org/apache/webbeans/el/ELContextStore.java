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
package org.apache.webbeans.el;

import org.apache.webbeans.container.BeanManagerImpl;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;

/**
 * The ELContextStore serves two different purposes
 *
 * <ol>
 *  <li>
 *   Store {@link javax.enterprise.context.Dependent} objects of the same
 *   invocation. See spec section 6.4.3. <i>Dependent pseudo-scope and Unified EL</i>.
 *  </li>
 *  <li>
 *   Store the Contextual Reference for each name per thread. This is a performance
 *   tuning strategy, because creating a {@link org.apache.webbeans.intercept.NormalScopedBeanInterceptorHandler}
 *   for each and every EL call is very expensive.
 *  </li>
 * </ol>
 */
public class ELContextStore
{
    //X TODO MUST NOT BE PUBLIC!
    private static ThreadLocal<ELContextStore> contextStores = new ThreadLocal<ELContextStore>();

    /**
     * @param createIfNotExist if <code>false</code> doesn't create a new ELContextStore if none exists
     * @return
     */
    public static ELContextStore getInstance(boolean createIfNotExist)
    {
        ELContextStore store = contextStores.get();

        if (store == null && createIfNotExist)
        {
            store = new ELContextStore();
            contextStores.set(store);
        }

        return store;
    }

    private Map<Bean<?>, CreationalStore> dependentObjects = new HashMap<Bean<?>, CreationalStore>();
    private Map<Bean<?>, Object>          normalScopedObjects = new HashMap<Bean<?>, Object>();

    private BeanManagerImpl beanManager;

    private static class CreationalStore
    {
        private Object object;
        
        private CreationalContext<?> creational;
        
        public CreationalStore(Object object, CreationalContext<?> creational)
        {
            this.object = object;
            this.creational = creational;
        }

        /**
         * @return the object
         */
        public Object getObject()
        {
            return object;
        }

        /**
         * @return the creational
         */
        public CreationalContext<?> getCreational()
        {
            return creational;
        }
        
        
    }

    /**
     * This class can only get constructed via {@link #getInstance(boolean)}
     */
    private ELContextStore()
    {
    }
    
    public void addDependent(Bean<?> bean, Object dependent, CreationalContext<?> creationalContext)
    {
        if(bean.getScope().equals(Dependent.class))
        {
            this.dependentObjects.put(bean, new CreationalStore(dependent,creationalContext));   
        }
    }
    
    public Object getDependent(Bean<?> bean)
    {
        CreationalStore sc = this.dependentObjects.get(bean);

        return sc != null ? sc.getObject() : null;
    }

    public Object getNormalScoped(Bean<?> bean)
    {
        return normalScopedObjects.get(bean);
    }

    public void addNormalScoped(Bean<?> bean, Object contextualInstance)
    {
        normalScopedObjects.put(bean, contextualInstance);
    }
    
    public BeanManagerImpl getBeanManager()
    {
        if (beanManager == null)
        {
            beanManager = BeanManagerImpl.getManager();
        }
        return beanManager;
    }

    
    @SuppressWarnings("unchecked")
    public void destroyDependents()
    {
        Set<Bean<?>> beans = this.dependentObjects.keySet();
        for(Bean<?> bean : beans)
        {
            Bean<Object> o = (Bean<Object>)bean;
            CreationalStore store = this.dependentObjects.get(bean);
            o.destroy(store.getObject(), (CreationalContext<Object>)store.getCreational());
        }
        
        this.dependentObjects.clear();
    }

    /**
     * This needs to be called at the end of each request.
     * Because after the request ends, a server might reuse
     * the Thread to serve other requests (from other WebApps) 
     */
    public void destroyELContextStore()
    {
        beanManager = null;
        normalScopedObjects.clear();
        contextStores.set(null);
        contextStores.remove();
    }
}
