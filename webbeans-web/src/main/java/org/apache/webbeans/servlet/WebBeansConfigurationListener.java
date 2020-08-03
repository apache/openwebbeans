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

import org.apache.webbeans.config.OWBLogConst;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.el.ELContextStore;
import org.apache.webbeans.logger.WebBeansLoggerFacade;
import org.apache.webbeans.spi.ContainerLifecycle;
import org.apache.webbeans.spi.ContextsService;
import org.apache.webbeans.util.WebBeansUtil;
import org.apache.webbeans.web.util.ServletCompatibilityUtil;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.context.spi.Context;
import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

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
 *
 */
public class WebBeansConfigurationListener implements ServletContextListener, ServletRequestListener, HttpSessionListener
{
    /**Logger instance*/
    private static final Logger logger = WebBeansLoggerFacade.getLogger(WebBeansConfigurationListener.class);


    /**Manages the container lifecycle*/
    protected ContainerLifecycle lifeCycle;

    /**Application {@link WebBeansContext} instance*/
    private WebBeansContext webBeansContext;
    
    /**Application {@link ContextsService}*/
    private ContextsService contextsService;

    /**
     * Default constructor
     */
    public WebBeansConfigurationListener()
    {
        webBeansContext = WebBeansContext.getInstance();
        contextsService = webBeansContext.getContextsService();
    }

    @Override
    public void contextInitialized(ServletContextEvent event)
    {
        doStart(event);
    }


    @Override
    public void requestInitialized(ServletRequestEvent event)
    {
        try
        {
            if (logger.isLoggable(Level.FINE))
            {
                logger.log(Level.FINE, "Starting a new request : [{0}]", event == null ? "null" : event.getServletRequest().getRemoteAddr());
            }

            this.lifeCycle.getContextService().startContext(RequestScoped.class, event);

            // we don't initialise the Session here but do it lazily if it gets requested
            // the first time. See OWB-457
        }
        catch (Exception e)
        {
            logger.log(Level.SEVERE,
                WebBeansLoggerFacade.constructMessage(OWBLogConst.ERROR_0019, event == null ? "null" : event.getServletRequest()));
            WebBeansUtil.throwRuntimeExceptions(e);
        }
    }

    @Override
    public void sessionCreated(HttpSessionEvent event)
    {
        try
        {
            if (logger.isLoggable(Level.FINE))
            {
                logger.log(Level.FINE, "Starting a session with session id : [{0}]", event.getSession().getId());
            }
            this.lifeCycle.getContextService().startContext(SessionScoped.class, event.getSession());
        }
        catch (Exception e)
        {
            logger.log(Level.SEVERE,
                WebBeansLoggerFacade.constructMessage(OWBLogConst.ERROR_0020, event.getSession()));
            WebBeansUtil.throwRuntimeExceptions(e);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent event)
    {
        if (lifeCycle != null)
        {
            lifeCycle.stopApplication(event);
        }

        // just to be sure that we didn't lazily create anything...
        cleanupRequestThreadLocals();
    }

    @Override
    public void requestDestroyed(ServletRequestEvent event)
    {
        if (logger.isLoggable(Level.FINE))
        {
            logger.log(Level.FINE, "Destroying a request : [{0}]", event == null ? "null" : event.getServletRequest().getRemoteAddr());
        }

        // clean up the EL caches after each request
        ELContextStore elStore = ELContextStore.getInstance(false);
        if (elStore != null)
        {
            elStore.destroyELContextStore();
        }

        this.lifeCycle.getContextService().endContext(RequestScoped.class, event);

        this.cleanupRequestThreadLocals();
    }


    @Override
    public void sessionDestroyed(HttpSessionEvent event)
    {
        if (logger.isLoggable(Level.FINE))
        {
            logger.log(Level.FINE, "Destroying a session with session id : [{0}]", event.getSession().getId());
        }
        boolean mustDestroy = ensureRequestScope();

        this.lifeCycle.getContextService().endContext(SessionScoped.class, event.getSession());

        if (mustDestroy)
        {
            requestDestroyed(null);
        }
    }

    /**
     * Initializing logic for initializing the context.
     * @param event {@link ServletContextEvent}
     */
    private void doStart(final ServletContextEvent event)
    {
        if (event.getServletContext().getAttribute(getClass().getName()) != null)
        {
            return;
        }

        this.lifeCycle = webBeansContext.getService(ContainerLifecycle.class);

        try
        {
            this.lifeCycle.startApplication(event);
            event.getServletContext().setAttribute(getClass().getName(), true);
        }
        catch (Exception e)
        {
            logger.log(Level.SEVERE,
                    WebBeansLoggerFacade.constructMessage(
                            OWBLogConst.ERROR_0018,
                            ServletCompatibilityUtil.getServletInfo(event.getServletContext())));
            WebBeansUtil.throwRuntimeExceptions(e);
        }
    }

    /**
     * Returns true if the request must be destroyed false otherwise.
     * Ensure that we have a {@link RequestScoped} context.
     * @return true if the request must be destroyed false otherwise
     */
    private boolean ensureRequestScope()
    {
        Context context = this.lifeCycle.getContextService().getCurrentContext(RequestScoped.class);

        if (context == null || !context.isActive())
        {
            requestInitialized(null);
            return true;
        }
        return false;
    }

    /**
     * Ensures that all ThreadLocals, which could have been set in this
     * requests Thread, are removed in order to prevent memory leaks.
     */
    private void cleanupRequestThreadLocals()
    {
        if (contextsService != null)
        {
            contextsService.removeThreadLocals();
        }
    }

    /**
     * Auto initialization class for servers supporting
     * the {@link ServletContainerInitializer}
     */
    public static class Auto implements ServletContainerInitializer
    {
        @Override
        public void onStartup(final Set<Class<?>> set, final ServletContext servletContext)
        {
            final String key = "openwebbeans.web.sci.active";
            
            if (!Boolean.parseBoolean(System.getProperty(key, servletContext.getInitParameter(key))))
            {
                return;
            }
            
            final WebBeansConfigurationListener listener = new WebBeansConfigurationListener();
            listener.doStart(new ServletContextEvent(servletContext));
            servletContext.addListener(listener);
        }
    }
}
