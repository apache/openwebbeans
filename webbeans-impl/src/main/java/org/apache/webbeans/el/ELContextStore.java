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

import java.util.HashMap;
import java.util.Map;

import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;

/**
 * The ELContextStore serves two different purposes
 *
 * <ol>
 *  <li>
 *   Store {@link jakarta.enterprise.context.Dependent} objects of the same
 *   invocation. See spec section 6.4.3. <i>Dependent pseudo-scope and Unified EL</i>.
 *   This gets cleaned up with {@link #destroyDependents()} after the whole Expression
 *   got scanned.
 *  </li>
 *  <li>
 *   Store the Contextual Reference for each name per request thread. This is a performance
 *   tuning strategy, because creating a {@link org.apache.webbeans.intercept.NormalScopedBeanInterceptorHandler}
 *   for each and every EL call is very expensive. This needs to be cleaned up with
 *   {@link #destroyELContextStore()} at the end of each request. 
 *  </li>
 * </ol>
 */
public final class ELContextStore
{
    private static ThreadLocal<ELContextStore> contextStores = new ThreadLocal<>();

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

    /**
     * The same Expression must get same instances of &#064;Dependent beans
     */
    private Map<Bean<?>, CreationalStore<?>> dependentObjects = new HashMap<>();
    private Map<String, Bean<?>> beanNameToDependentBeanMapping = new HashMap<>();

    /**
     * Cache for resolved proxies of &#064;NormalScoped beans. This heavily speeds up pages with
     * multiple ELs for the same bean. A typical bean invoke through an EL only accesses 1
     * property. If we wouldn't cache this, every EL call would create a new proxy and
     * drops it after the EL.
     */
    private Map<String, Object> normalScopedObjects = new HashMap<>();

    public Object findBeanByName(String name)
    {
        Object cachedBean = normalScopedObjects.get(name);

        if(cachedBean != null)
        {
            return cachedBean;
        }

        Bean<?> dependentBean = beanNameToDependentBeanMapping.get(name);

        if(dependentBean == null)
        {
            return null;
        }
        CreationalStore cs =  dependentObjects.get(dependentBean);
        if (cs != null)
        {
            return cs.getObject();
        }
        
        return null;
    }

    private static class CreationalStore<T>
    {
        private T object;
        
        private CreationalContext<T> creational;
        
        public CreationalStore(T object, CreationalContext<T> creational)
        {
            this.object = object;
            this.creational = creational;
        }

        /**
         * @return the object
         */
        public T getObject()
        {
            return object;
        }

        /**
         * @return the creational
         */
        public CreationalContext<T> getCreational()
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

    /**
     * Add a @Dependent scoped bean for later use in the <b>same</b> EL.
     * See spec section 6.4.3. <i>Dependent pseudo-scope and Unified EL</i>.
     * @param bean
     * @param dependent
     * @param creationalContext
     */
    public void addDependent(Bean<?> bean, Object dependent, CreationalContext<?> creationalContext)
    {
        dependentObjects.put(bean, new CreationalStore(dependent, creationalContext));
        beanNameToDependentBeanMapping.put(bean.getName(), bean);
    }

    /**
     * @see #addDependent(Bean, Object, CreationalContext)
     * @param bean
     * @return the previously used dependent bean or <code>null</code>
     */
    public Object getDependent(Bean<?> bean)
    {
        CreationalStore sc = dependentObjects.get(bean);

        return sc != null ? sc.getObject() : null;
    }

    /**
     * We cache resolved &#064;NormalScoped bean proxies on the same for speeding up EL.
     * @param beanName
     */
    public void addNormalScoped(String beanName, Object contextualInstance)
    {
        normalScopedObjects.put(beanName, contextualInstance);
    }

    /**
     * This method have to be called after the EL parsing to cleanup the cache
     * for &#064;Dependent scoped beans.
     */
    public void destroyDependents()
    {
        if (dependentObjects.size() > 0)
        {
            for (Map.Entry<Bean<?>, CreationalStore<?>> storeEntry : dependentObjects.entrySet())
            {
                CreationalStore<?> store = storeEntry.getValue();
                Bean contextual = storeEntry.getKey();
                contextual.destroy(store.getObject(), store.getCreational());
            }
            dependentObjects.clear();
        }
        beanNameToDependentBeanMapping.clear();
    }

    /**
     * This needs to be called at the end of each request.
     * Because after the request ends, a server might reuse
     * the Thread to serve other requests (from other WebApps) 
     */
    public void destroyELContextStore()
    {
        normalScopedObjects.clear();
        contextStores.set(null);
        contextStores.remove();
    }
}
