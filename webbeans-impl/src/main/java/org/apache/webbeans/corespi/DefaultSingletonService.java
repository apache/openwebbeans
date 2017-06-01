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
package org.apache.webbeans.corespi;

import java.util.Map;
import java.util.WeakHashMap;

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.spi.SingletonService;
import org.apache.webbeans.util.Asserts;

public class DefaultSingletonService implements SingletonService<WebBeansContext>
{
    /**
     * Keys --> ClassLoaders
     * Values --> WebBeansContext
     */
    private final Map<ClassLoader, WebBeansContext> singletonMap = new WeakHashMap<ClassLoader, WebBeansContext>();
    
    /**
     * Gets singleton instance for deployment.
     * @return signelton instance for this deployment
     */
    @Override
    public WebBeansContext get(Object key)
    {
        assertClassLoaderKey(key);
        ClassLoader classLoader = (ClassLoader) key;
        synchronized (singletonMap)
        {
            //util.Track.sync(key);
            
            WebBeansContext webBeansContext = singletonMap.get(classLoader);
            //util.Track.get(key);

            if (webBeansContext == null)
            {
                webBeansContext = new WebBeansContext();
                singletonMap.put(classLoader, webBeansContext);
            }

            return webBeansContext;

        }
    }

    public void register(final ClassLoader key, final WebBeansContext context)
    {
        if (singletonMap.containsKey(key))
        {
            throw new IllegalArgumentException(key + " is already registered");
        }
        singletonMap.putIfAbsent(key, context);
    }

    /**
     * Clear all deployment instances when the application is undeployed.
     * @param classLoader of the deployment
     */
    public void clearInstances(ClassLoader classLoader)
    {
        Asserts.assertNotNull(classLoader, "classloader");
        synchronized (singletonMap)
        {
            singletonMap.remove(classLoader);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear(Object classLoader)
    {
        assertClassLoaderKey(classLoader);
        clearInstances((ClassLoader)classLoader);
    }

    /**
     * Assert that key is classloader instance.
     * @param key key
     */
    private void assertClassLoaderKey(Object key)
    {
        if(!(key instanceof ClassLoader))
        {
            throw new IllegalArgumentException("Key instance must be ClassLoader for using DefaultSingletonService");
        }
    }

}
