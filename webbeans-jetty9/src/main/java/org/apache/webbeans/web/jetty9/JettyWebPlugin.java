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
package org.apache.webbeans.web.jetty9;

import java.util.EventListener;

import jakarta.servlet.Filter;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletContextAttributeListener;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.ServletRequestAttributeListener;
import jakarta.servlet.ServletRequestListener;
import jakarta.servlet.http.HttpSessionActivationListener;
import jakarta.servlet.http.HttpSessionAttributeListener;
import jakarta.servlet.http.HttpSessionBindingListener;
import jakarta.servlet.http.HttpSessionListener;

import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.spi.SecurityService;
import org.apache.webbeans.spi.plugins.AbstractOwbPlugin;

/**
 * Jetty plugin for OWB.
 */
public class JettyWebPlugin extends AbstractOwbPlugin
{
    //Security service implementation.
    private final JettySecurityService securityService = new JettySecurityService();

    /**
     * Default constructor.
     */
    public JettyWebPlugin()
    {

    }

    /**
     * {@inheritDoc}
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


    /**
     * {@inheritDoc}
     */
    @Override
    public void isManagedBean(Class<?> clazz)
    {
        if (isServletSpecClass(clazz))
        {
            throw new WebBeansConfigurationException("Given class  : " + clazz.getName() + " is not managed bean");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean supportsJavaEeComponentInjections(Class<?> clazz)
    {
        return isServletSpecClass(clazz);
    }

    private boolean isServletSpecClass(Class<?> clazz)
    {
        if (Servlet.class.isAssignableFrom(clazz) ||
                Filter.class.isAssignableFrom(clazz))
        {
            return true;
        }

        if (EventListener.class.isAssignableFrom(clazz))
        {
            return ServletContextListener.class.isAssignableFrom(clazz) ||
                    ServletContextAttributeListener.class.isAssignableFrom(clazz) ||
                    HttpSessionActivationListener.class.isAssignableFrom(clazz) ||
                    HttpSessionAttributeListener.class.isAssignableFrom(clazz) ||
                    HttpSessionBindingListener.class.isAssignableFrom(clazz) ||
                    HttpSessionListener.class.isAssignableFrom(clazz) ||
                    ServletRequestListener.class.isAssignableFrom(clazz) ||
                    ServletRequestAttributeListener.class.isAssignableFrom(clazz);
        }
        return false;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean supportService(Class<?> serviceClass)
    {
        return serviceClass.equals(SecurityService.class);
    }

}
