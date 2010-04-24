/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.webbeans.web.tomcat;

import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletContextAttributeListener;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRequestAttributeListener;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpSessionActivationListener;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingListener;
import javax.servlet.http.HttpSessionListener;

import org.apache.webbeans.spi.SecurityService;
import org.apache.webbeans.spi.plugins.AbstractOwbPlugin;
import org.apache.webbeans.spi.plugins.OpenWebBeansWebPlugin;

public class TomcatWebPlugin extends AbstractOwbPlugin implements OpenWebBeansWebPlugin
{
    private final TomcatSecurityService securityService = new TomcatSecurityService();
    
    public TomcatWebPlugin()
    {
        
    }
    
    

    /* (non-Javadoc)
     * @see org.apache.webbeans.plugins.AbstractOwbPlugin#getSupportedService(java.lang.Class)
     */
    @Override
    public <T> T getSupportedService(Class<T> serviceClass)
    {
        if(serviceClass.equals(SecurityService.class))
        {
            return serviceClass.cast(this.securityService);
        }
        
        return null;
    }



    @Override
    public void isManagedBean(Class<?> clazz) throws Exception
    {
        if(Servlet.class.isAssignableFrom(clazz) ||
                Filter.class.isAssignableFrom(clazz) ||
                ServletContextListener.class.isAssignableFrom(clazz) ||
                ServletContextAttributeListener.class.isAssignableFrom(clazz) ||
                HttpSessionActivationListener.class.isAssignableFrom(clazz) ||
                HttpSessionAttributeListener.class.isAssignableFrom(clazz) ||
                HttpSessionBindingListener.class.isAssignableFrom(clazz) ||
                HttpSessionListener.class.isAssignableFrom(clazz) ||
                ServletRequestListener.class.isAssignableFrom(clazz) ||
                ServletRequestAttributeListener.class.isAssignableFrom(clazz) )
        {
            throw new Exception("Given class  : " + clazz.getName() + " is not managed bean");
        }
    }

    /* (non-Javadoc)
     * @see org.apache.webbeans.plugins.AbstractOwbPlugin#supportService(java.lang.Class)
     */
    @Override
    public boolean supportService(Class<?> serviceClass)
    {
        if(serviceClass.equals(SecurityService.class))
        {
            return true;
        }
        
        return false;
    }

}
