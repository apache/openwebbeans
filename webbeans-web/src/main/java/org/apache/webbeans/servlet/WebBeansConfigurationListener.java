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
package org.apache.webbeans.servlet;

import org.apache.webbeans.config.WebBeansContext;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

/**
 * Initializing the beans container for using in an web application
 * environment.
 *
 * This is the main entry point for starting the CDI container
 * for a servlet.
 *
 *
 * If you have a container with &lt; Servlet-2.5 then use
 * {@link WebBeansConfigurationFilter} and {@link WebBeansConfigurationHttpSessionListener}
 * instead.
 */
public class WebBeansConfigurationListener implements ServletContextListener, ServletRequestListener, HttpSessionListener
{
    private WebBeansContext webBeansContext;
    private BeginWebBeansConfigurationListener beginWebBeansConfigurationListener;
    private EndWebBeansConfigurationListener endWebBeansConfigurationListener;

    /**
     * Default constructor
     */
    public WebBeansConfigurationListener()
    {
        webBeansContext = WebBeansContext.getInstance();
        beginWebBeansConfigurationListener = new BeginWebBeansConfigurationListener(webBeansContext);
        endWebBeansConfigurationListener = new EndWebBeansConfigurationListener(webBeansContext);
    }


    @Override
    public void contextInitialized(ServletContextEvent sce)
    {
        beginWebBeansConfigurationListener.contextInitialized(sce);

        // for setting the lifecycle
        endWebBeansConfigurationListener.contextInitialized(sce);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce)
    {
        endWebBeansConfigurationListener.contextDestroyed(sce);
    }

    @Override
    public void sessionCreated(HttpSessionEvent se)
    {
        beginWebBeansConfigurationListener.sessionCreated(se);
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se)
    {
        endWebBeansConfigurationListener.sessionDestroyed(se);
    }


    @Override
    public void requestInitialized(ServletRequestEvent sre)
    {
        beginWebBeansConfigurationListener.requestInitialized(sre);
    }

    @Override
    public void requestDestroyed(ServletRequestEvent sre)
    {
        endWebBeansConfigurationListener.requestDestroyed(sre);
    }

}
