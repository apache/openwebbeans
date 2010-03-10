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
package org.apache.webbeans.spi;

import org.apache.webbeans.config.OpenWebBeansConfiguration;
import org.apache.webbeans.config.WebBeansFinder;
import org.apache.webbeans.logger.WebBeansLogger;

/**
 * Loads any Service Provider Interface implementation declared in the 
 * {@link OpenWebBeansConfiguration}.
 * 
 * The Configuration for Services looks like
 * <pre>
 * org.apache.webbeans.spi.MyServiceInterface=my.package.webbeans.spi.MyServiceImplementation
 * </pre>
 * 
 * 
 */
public class ServiceLoader
{   
    private static final WebBeansLogger logger = WebBeansLogger.getLogger(ServiceLoader.class);
    
    /**
     * Get a new service singleton instance for the given interface.
     * 
     * @param serviceInterface
     * @return the new service implementation
     */
    @SuppressWarnings("unchecked")
    public static <T> T getService(Class<T> serviceInterface)
    {
        String implName = OpenWebBeansConfiguration.getInstance().getProperty(serviceInterface.getName());
        if (implName == null)
        {
            logger.warn("Unable to find service with class name : " + serviceInterface.getName());
            return null;
        }
        return (T) WebBeansFinder.getSingletonInstance(implName);
    }
    
    /**
     * Get a new service singleton instance for the given interface.
     * 
     * @param serviceInterface
     * @param classloader to be used for lookup
     * @return the new service implementation
     */
    @SuppressWarnings("unchecked")
    public static <T> T getService(Class<T> serviceInterface, ClassLoader cl)
    {
        String implName = OpenWebBeansConfiguration.getInstance().getProperty(serviceInterface.getName());
        if (implName == null)
        {
            logger.warn("Unable to find service with class name : " + serviceInterface.getName());
            return null;
        }
        return (T) WebBeansFinder.getSingletonInstance(implName, cl);
    }
}
