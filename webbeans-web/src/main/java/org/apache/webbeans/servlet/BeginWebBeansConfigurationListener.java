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

import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.SessionScoped;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.webbeans.config.OWBLogConst;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.logger.WebBeansLoggerFacade;
import org.apache.webbeans.spi.ContainerLifecycle;
import org.apache.webbeans.util.WebBeansUtil;
import org.apache.webbeans.web.util.ServletCompatibilityUtil;

/**
 * As the ordering of servlet listener invocations is the same for all
 * *Initialized events (e.g. contextInitialized, requestInitialized)
 * and for all *Destroyed events (e.g. contextDestroyed, requestDestroyed)
 * we need a different listener for start and end events.
 *
 * The {@link BeginWebBeansConfigurationListener} needs to be invoked as
 * very first listener in the chain whereas the
 * {@link EndWebBeansConfigurationListener} needs to be invoked as last
 * in the chain.
 *
 * The {@link WebBeansConfigurationListener} exists for backward compatibility
 * reasons and simply delegates through to the 2 other listeners.
 *
 * Note: You only need the separate Begin and End listeners if your environment
 * supports injection into ServletListeners and Filters or if you use a manual
 * BeanManager lookup in any of these.
 *
 * @see EndWebBeansConfigurationListener
 * @see WebBeansConfigurationListener
 */
public class BeginWebBeansConfigurationListener implements ServletContextListener, ServletRequestListener, HttpSessionListener
{

    /**Logger instance*/
    private static final Logger logger = WebBeansLoggerFacade.getLogger(WebBeansConfigurationListener.class);

    /**Manages the container lifecycle*/
    protected ContainerLifecycle lifeCycle = null;

    private WebBeansContext webBeansContext;

    /**
     * Default constructor
     */
    public BeginWebBeansConfigurationListener()
    {
        webBeansContext = WebBeansContext.getInstance();
    }

    /**
     * Constructor for manual creation
     */
    public BeginWebBeansConfigurationListener(WebBeansContext webBeansContext)
    {
        this.webBeansContext = webBeansContext;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void contextInitialized(ServletContextEvent event)
    {
        this.lifeCycle = webBeansContext.getService(ContainerLifecycle.class);

        try
        {
            this.lifeCycle.startApplication(event);
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
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     */
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
    public void contextDestroyed(ServletContextEvent sce)
    {
        // nothing to do, cleanup is done in EndWebBeansConfigurationListener
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se)
    {
        // nothing to do, cleanup is done in EndWebBeansConfigurationListener
    }

    @Override
    public void requestDestroyed(ServletRequestEvent sre)
    {
        // nothing to do, cleanup is done in EndWebBeansConfigurationListener
    }
}
