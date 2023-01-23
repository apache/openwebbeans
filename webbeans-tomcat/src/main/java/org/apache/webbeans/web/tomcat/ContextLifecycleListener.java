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
package org.apache.webbeans.web.tomcat;

import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.core.StandardContext;
import org.apache.tomcat.InstanceManager;
import org.apache.webbeans.exception.WebBeansException;
import org.apache.webbeans.servlet.WebBeansConfigurationListener;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextAttributeEvent;
import jakarta.servlet.ServletContextAttributeListener;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;


/**
 * Context lifecycle listener. Adapted from
 * OpenEJB Tomcat and updated.
 * 
 * @version $Rev$ $Date$
 *
 */
public class ContextLifecycleListener implements LifecycleListener, ServletContextAttributeListener
{
    public ContextLifecycleListener()
    {
    }

    @Override
    public void lifecycleEvent(LifecycleEvent event)
    {
        try
        {
            if (event.getSource() instanceof StandardContext)
            {
                StandardContext context = (StandardContext) event.getSource();

                if (event.getType().equals(Lifecycle.CONFIGURE_START_EVENT))
                {
                    ServletContext scontext = context.getServletContext();
                    URL url = getBeansXml(scontext);
                    if (url != null)
                    {
                        //Registering ELResolver with JSP container
                        System.setProperty("org.apache.webbeans.application.jsp", "true");

                        addOwbListeners(context);
                        addOwbFilters(context);

                        context.addApplicationListener(TomcatSecurityFilter.class.getName());
                        context.addApplicationEventListener(this);
                    }
                }
            }
        }
        catch(Exception e)
        {
            throw new RuntimeException(e);
        }        
    }


    private void addOwbListeners(StandardContext context)
    {
        String[] oldListeners = context.findApplicationListeners();
        LinkedList<String> listeners = new LinkedList<>();

        listeners.addFirst(WebBeansConfigurationListener.class.getName());

        for(String listener : oldListeners)
        {
            listeners.add(listener);
            context.removeApplicationListener(listener);
        }

        for(String listener : listeners)
        {
            context.addApplicationListener(listener);
        }
    }

    private void addOwbFilters(StandardContext context)
    {
        // we currently add all filters via web-fragment.xml
    }

    private URL getBeansXml(ServletContext scontext) throws MalformedURLException
    {
        URL url = scontext.getResource("/WEB-INF/beans.xml");
        if (url == null)
        {
            url = scontext.getResource("/WEB-INF/classes/META-INF/beans.xml");
        }
        return url;
    }


    private void wrapInstanceManager(StandardContext context)
    {
        if (context.getInstanceManager() instanceof TomcatInstanceManager)
        {
            return;
        }

        InstanceManager processor = context.getInstanceManager();
        InstanceManager custom = new TomcatInstanceManager(context.getLoader().getClassLoader(), processor);
        context.setInstanceManager(custom);

        context.getServletContext().setAttribute(InstanceManager.class.getName(), custom);
    }

    @Override
    public void attributeAdded(ServletContextAttributeEvent servletContextAttributeEvent)
    {
        if (InstanceManager.class.getName().equals(servletContextAttributeEvent.getName()))
        { // used as a hook to know we can override eagerly the InstanceManager
            try
            {
                StandardContext context = (StandardContext) getContext(servletContextAttributeEvent.getServletContext());
                wrapInstanceManager(context);
            }
            catch (NoSuchFieldException | IllegalAccessException e)
            {
                throw new WebBeansException(e.getMessage(), e);
            }
        }
    }

    private static Object getContext(Object o) throws NoSuchFieldException, IllegalAccessException
    {
        Field getContext = o.getClass().getDeclaredField("context");
        boolean acc = getContext.isAccessible();
        getContext.setAccessible(true);
        try
        {
            Object retVal =  getContext.get(o);
            if (! (retVal instanceof StandardContext))
            {
                retVal = getContext(retVal);
            }
            return retVal;
        }
        finally
        {
            getContext.setAccessible(acc);
        }
    }

    @Override
    public void attributeRemoved(ServletContextAttributeEvent servletContextAttributeEvent)
    {
        // nothing to do
    }

    @Override
    public void attributeReplaced(ServletContextAttributeEvent servletContextAttributeEvent)
    {
        // nothing to do
    }
}
