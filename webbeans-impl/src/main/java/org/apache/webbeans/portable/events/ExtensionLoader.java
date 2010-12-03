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
package org.apache.webbeans.portable.events;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.Extension;

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.config.WebBeansFinder;
import org.apache.webbeans.container.BeanManagerImpl;
import org.apache.webbeans.exception.WebBeansException;
import org.apache.webbeans.util.Asserts;
import org.apache.webbeans.util.WebBeansUtil;

/**
 * Loads META-INF/services/javax.enterprise.inject.spi.Extension
 * services.
 * 
 * @version $Rev$ $Date$
 *
 */
public class ExtensionLoader
{
    /**Map of extensions*/
    private final  Map<Bean<?>, Object> extensions = new ConcurrentHashMap<Bean<?>, Object>();
    private final Set<Class<? extends Extension>> extensionClasses = new HashSet<Class<? extends Extension>>();
    private final BeanManagerImpl manager;

    /**
     * Creates a new loader instance.
     * @param webBeansContext
     */
    public ExtensionLoader(WebBeansContext webBeansContext)
    {

        manager = webBeansContext.getBeanManagerImpl();
    }

    /**
     * Returns singleton <code>ExtensionLoader</code> instance.
     * 
     * @return singleton instance
     */
    public static ExtensionLoader getInstance()
    {
        ExtensionLoader loader = (ExtensionLoader) WebBeansFinder.getSingletonInstance(ExtensionLoader.class.getName());
        
        return loader;
    }
    
    /**
     * Load extension services.
     */
    public void loadExtensionServices()
    {
        ServiceLoader<Extension> loader = ServiceLoader.load(Extension.class, WebBeansUtil.getCurrentClassLoader());
        Iterator<Extension> iterator = loader.iterator();
        while(iterator.hasNext())
        {
            Extension ext = iterator.next();
            if (!extensionClasses.contains(ext.getClass()))
            {
                extensionClasses.add(ext.getClass());
                try
                {
                    addExtension(ext);
                }
                catch (Exception e)
                {
                    throw new WebBeansException("Error occurred while reading Extension service list",e);
                }
            }
        }        
    }
    
    /**
     * Returns service bean instance.
     * 
     * @param bean service bean
     * @return service bean instance
     */
    @SuppressWarnings("unchecked")
    public <T> T getBeanInstance(Bean<T> bean)
    {
        Asserts.assertNotNull(bean,"bean parameter cannot be null");
        
        if(this.extensions.containsKey(bean))
        {
            return (T)this.extensions.get(bean);
        }
        
        return null;
    }


    /**
     * Add a CDI Extension to our internal list.
     * @param ext Extension to add
     */
    public void addExtension(Extension ext)
    {
        Bean<?> bean = WebBeansUtil.createExtensionComponent(ext.getClass());
        this.extensions.put(bean, ext);

        manager.addBean(bean);
    }

    /**
     * Clear service list.
     * TODO since this doesn't remove the beans from the BeanManager it's unlikely to allow you to call loadExtensionServices again
     */
    public void clear()
    {
        this.extensions.clear();
        this.extensionClasses.clear();
    }
}
