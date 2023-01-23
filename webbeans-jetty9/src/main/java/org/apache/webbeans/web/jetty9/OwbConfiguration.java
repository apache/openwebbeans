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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.EventListener;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;

import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.DecoratedObjectFactory;
import org.eclipse.jetty.webapp.ClasspathPattern;
import org.eclipse.jetty.webapp.Configuration;
import org.eclipse.jetty.webapp.WebAppContext;

/**
 * A Jetty module configurator for use in embedded mode or a Jetty module.
 */
public class OwbConfiguration implements Configuration
{
    private static final String LISTENER_CLASS_NAME = "org.apache.webbeans.servlet.WebBeansConfigurationListener";

    static void addOwb(WebAppContext ctx) throws ServletException
    {
        URL url = getBeansXml(ctx.getServletContext());
        if (url != null)
        {
            //Registering ELResolver with JSP container
            System.setProperty("org.apache.webbeans.application.jsp", "true");
            addOwbListeners((ServletContextHandler.Context) ctx.getServletContext());
            addOwbFilters(ctx.getServletContext());
            DecoratedObjectFactory decObjFact = ctx.getObjectFactory();
            decObjFact.addDecorator(new JettyDecorator(ctx.getClassLoader()));
        }
    }

    private static void addOwbListeners(ServletContextHandler.Context context) throws ServletException
    {
        for (EventListener eventListener : context.getContextHandler().getEventListeners())
            {
                if (eventListener.getClass().getName().equals(LISTENER_CLASS_NAME))
                {
                    return;
                }
            }
        context.addListener(LISTENER_CLASS_NAME);
    }

    private static void addOwbFilters(ContextHandler.Context context)
    {
        // we currently add all other filters via web-fragment.xml
    }

    private static URL getBeansXml(ServletContext ctx) throws ServletException
    {
        try
        {
            URL url = ctx.getResource("/WEB-INF/beans.xml");
            if (url == null)
            {
                url = ctx.getResource("/WEB-INF/classes/META-INF/beans.xml");
            }
            return url;
        }
        catch (MalformedURLException e)
        {
            throw new ServletException(e);
        }
    }

    @Override
    public void preConfigure(WebAppContext ctx) throws Exception
    {
        final ClasspathPattern classpathPattern = ctx.getServerClasspathPattern();
        classpathPattern.add("-org.eclipse.jetty.util.Decorator");
        classpathPattern.add("-org.eclipse.jetty.util.DecoratedObjectFactory");
        classpathPattern.add("-org.eclipse.jetty.server.handler.ContextHandler");
        classpathPattern.add("-org.eclipse.jetty.servlet.ServletContextHandler$Context");
        classpathPattern.add("-org.eclipse.jetty.servlet.ServletContextHandler");
        classpathPattern.add("-org.eclipse.jetty.webapp.WebAppContext");
    }

    @Override
    public void configure(WebAppContext ctx) throws Exception
    {
       addOwb(ctx);
    }

    @Override
    public void postConfigure(WebAppContext ctx) throws Exception
    {

    }

    @Override
    public void deconfigure(WebAppContext ctx) throws Exception
    {

    }

    @Override
    public void destroy(WebAppContext ctx) throws Exception
    {

    }

    @Override
    public void cloneConfigure(WebAppContext template, WebAppContext ctx) throws Exception
    {

    }


}
