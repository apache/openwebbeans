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
package org.apache.webbeans.config;

import javax.enterprise.inject.spi.Bean;

import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.plugins.OpenWebBeansEjbPlugin;
import org.apache.webbeans.plugins.PluginLoader;

public final class EJBWebBeansConfigurator
{
    private EJBWebBeansConfigurator()
    {

    }

    /**
     * Returns true if given class is an deployed ejb bean class, false otherwise.
     * @param clazz bean class
     * @return true if given class is an deployed ejb bean class
     * @throws WebBeansConfigurationException if any exception occurs
     */
    public static boolean isSessionBean(Class<?> clazz) throws WebBeansConfigurationException
    {
        PluginLoader loader = PluginLoader.getInstance();
        OpenWebBeansEjbPlugin ejbPlugin = loader.getEjbPlugin();
        
        //There is no ejb container
        if(ejbPlugin == null)
        {
            return false;
        }
        
        return ejbPlugin.isSessionBean(clazz);
    }
    
    /**
     * Returns ejb bean.
     * @param <T> bean class info
     * @param clazz bean class
     * @return ejb bean
     */
    public static <T> Bean<T> defineEjbBean(Class<T> clazz)
    {
        PluginLoader loader = PluginLoader.getInstance();
        OpenWebBeansEjbPlugin ejbPlugin = loader.getEjbPlugin();
        
        if(ejbPlugin == null)
        {
            throw new IllegalStateException("There is no provided EJB plugin. Unable to define session bean for class : " + clazz.getName());
        }
        
        return ejbPlugin.defineSessionBean(clazz);
    }
    
}
