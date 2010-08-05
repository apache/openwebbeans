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
package org.apache.webbeans.config;

import org.apache.webbeans.corespi.DefaultSingletonService;
import org.apache.webbeans.spi.SingletonService;
import org.apache.webbeans.util.WebBeansUtil;

/**
 * Holds singletons based on the deployment
 * class loader.
 * 
 * @version $Rev$ $Date$
 *
 */
public final class WebBeansFinder implements SingletonService
{   
    //How you use singleton provider ,
    //As a default we use ClassLoader --> Object
    private SingletonService singletonService = new DefaultSingletonService();
    
    //VM based singleton finder instance
    private static final WebBeansFinder FINDER = new WebBeansFinder();
    
    /**
     * No instantiate.
     */
    private WebBeansFinder()
    {
        //No action
    }
    
    public static Object getSingletonInstance(String singletonName)
    {
        return getSingletonInstance(singletonName, WebBeansUtil.getCurrentClassLoader());
    }
    
    public static Object getSingletonInstance(String singletonName, ClassLoader classLoader)
    {
        return FINDER.get(classLoader, singletonName);
    }
    
    
    public static Object getExistingSingletonInstance(String singletonName, ClassLoader cl)
    {
        return FINDER.getExist(cl, singletonName);
    }
    
    public static void clearInstances(ClassLoader classLoader)
    {
        FINDER.clear(classLoader);
    }
    
    public static Object getSingletonClassLoader(Object object)
    {
        return FINDER.getKey(object);
    }
    
    //Thirdt pary frameworks can set singleton instance
    //For example, OpenEJB could provide its own provider
    //Based on deployment
    public synchronized void setSingletonService(SingletonService singletonService)
    {
        FINDER.singletonService = singletonService;
    }

    @Override
    public void clear(Object key)
    {
        this.singletonService.clear(key);
    }

    @Override
    public Object get(Object key, String singletonClassName)
    {
        return this.singletonService.get(key, singletonClassName);
    }

    @Override
    public Object getExist(Object key, String singletonClassName)
    {
        return this.singletonService.getExist(key, singletonClassName);
    }

    @Override
    public Object getKey(Object singleton)
    {
        return this.singletonService.getKey(singleton);
    }

    @Override
    public boolean isExist(Object key, String singletonClassName)
    {
        return this.singletonService.isExist(key, singletonClassName);
    }
    
    

}