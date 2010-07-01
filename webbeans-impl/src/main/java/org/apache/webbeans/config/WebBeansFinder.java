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

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.webbeans.exception.WebBeansException;
import org.apache.webbeans.util.Asserts;
import org.apache.webbeans.util.ClassUtil;
import org.apache.webbeans.util.WebBeansUtil;

/**
 * Holds singletons based on the deployment
 * class loader.
 * 
 * @version $Rev$ $Date$
 *
 */
public final class WebBeansFinder
{   
    /**
     * Keys --> ClassLoaders
     * Values --> Maps of singleton class name with object
     */
    private static Map<ClassLoader, Map<String, Object>> singletonMap = new HashMap<ClassLoader, Map<String,Object>>();
    
    private static Map<Object, ClassLoader> objectToClassLoaderMap = new IdentityHashMap<Object, ClassLoader>();

    /**
     * No instantiate.
     */
    private WebBeansFinder()
    {
        //No action
    }
    
    /**
     * Gets signelton instance.
     * @param singletonName singleton class name
     * @return singleton instance
     */
    public static Object getSingletonInstance(String singletonName)
    {
       return getSingletonInstance(singletonName, WebBeansUtil.getCurrentClassLoader());
    }
    
    /**
     * Gets singleton instance for deployment.
     * @param singletonName singleton class name
     * @param classLoader classloader of the deployment
     * @return signelton instance for this deployment
     */
    public static Object getSingletonInstance(String singletonName, ClassLoader classLoader)
    {
        Object object = null;

        synchronized (singletonMap)
        {
            Map<String, Object> managerMap = singletonMap.get(classLoader);

            if (managerMap == null)
            {
                managerMap = new HashMap<String, Object>();
                singletonMap.put(classLoader, managerMap);
            }
            
            object = managerMap.get(singletonName);
            /* No singleton for this application, create one */
            if (object == null)
            {
                try
                {
                    //Load class
                    Class<?> clazz = ClassUtil.getClassFromName(singletonName);
                    if(clazz == null)
                    {
                        throw new ClassNotFoundException("Class with name: " + singletonName + " is not found in the system");
                    }
                    
                    //Create instance
                    object = clazz.newInstance();

                    //Save it
                    managerMap.put(singletonName, object);
                    
                    //Save it object --> classloader
                    objectToClassLoaderMap.put(object, classLoader);

                }
                catch (InstantiationException e)
                {
                    throw new WebBeansException("Unable to instantiate class : " + singletonName, e);
                }
                catch (IllegalAccessException e)
                {
                    throw new WebBeansException("Illegal access exception in creating instance with class : " + singletonName, e);
                }
                catch (ClassNotFoundException e)
                {
                    throw new WebBeansException("Class not found exception in creating instance with class : " + singletonName, e);
                }
            }
        }

        return object;
    }
    
    /**
     * Gets singleton instance if one already exists
     * @param singletonName singleton class name
     * @param cl classloader of the deployment
     * @return singleton instance or null if one doesn't already exist
     */
    public static Object getExistingSingletonInstance(String singletonName, ClassLoader cl)
    {
        Object object = null;
        synchronized (singletonMap)
        {
            Map<String, Object> managerMap = singletonMap.get(cl);
            if (managerMap == null)
            {
                return null;
            }
            else
            {
                object = managerMap.get(singletonName);
            }
        }
        return object;
    }
    
    /**
     * Clear all deployment instances when the application is undeployed.
     * @param classLoader of the deployment
     */
    public static void clearInstances(ClassLoader classLoader)
    {
        Asserts.assertNotNull(classLoader, "classloader is null");
        synchronized (singletonMap)
        {
            Map<String, Object> objects = singletonMap.remove(classLoader);
            if(objects != null)
            {
                for(Entry<String, Object> entry : objects.entrySet())
                {
                    objectToClassLoaderMap.remove(entry.getValue());
                }
            }
        }
    }
    
    /**
     * Gets classloader with given singelton instance.
     * @param object singleton instance
     * @return the classloader that instance is created within
     */
    public static ClassLoader getSingletonClassLoader(Object object)
    {
        Asserts.assertNotNull(object, "object is null");
        synchronized (objectToClassLoaderMap)
        {
            if(objectToClassLoaderMap.containsKey(object))
            {
                return objectToClassLoaderMap.get(object);
            }              
        }
        
        return null;
    }

}