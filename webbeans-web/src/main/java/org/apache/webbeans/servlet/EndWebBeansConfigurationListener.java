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

import javax.enterprise.context.ConversationScoped;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.context.spi.Context;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.el.ELContextStore;
import org.apache.webbeans.logger.WebBeansLoggerFacade;
import org.apache.webbeans.spi.ContainerLifecycle;
import org.apache.webbeans.web.context.WebContextsService;

/**
 * This listener should be the last in the invocation chain.
 *
 * @see BeginWebBeansConfigurationListener
 * @see WebBeansConfigurationListener
 */
public class EndWebBeansConfigurationListener implements ServletContextListener, ServletRequestListener, HttpSessionListener
{
    /**Logger instance*/
    private static final Logger logger = WebBeansLoggerFacade.getLogger(WebBeansConfigurationListener.class);

    private WebBeansContext webBeansContext;
    private ContainerLifecycle lifeCycle;

    /**
     * Default constructor
     */
    public EndWebBeansConfigurationListener()
    {
        webBeansContext = WebBeansContext.getInstance();
    }

    /**
     * Constructor for manual creation
     */
    public EndWebBeansConfigurationListener(WebBeansContext webBeansContext)
    {
        this.webBeansContext = webBeansContext;
    }

    @Override
    public void contextInitialized(ServletContextEvent sce)
    {
        // this must return the booted OWB container as the BeginWebBeansConfigurationListener did already run
        this.lifeCycle = webBeansContext.getService(ContainerLifecycle.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void contextDestroyed(ServletContextEvent event)
    {
        lifeCycle.stopApplication(event);

        // just to be sure that we didn't lazily create anything...
        cleanupRequestThreadLocals();
    }

    /**
     * {@inheritDoc}
     */
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


    /**
     * {@inheritDoc}
     */
    @Override
    public void sessionDestroyed(HttpSessionEvent event)
    {
        if (logger.isLoggable(Level.FINE))
        {
            logger.log(Level.FINE, "Destroying a session with session id : [{0}]", event.getSession().getId());
        }
        boolean mustDestroy = ensureRequestScope();

        this.lifeCycle.getContextService().endContext(SessionScoped.class, event.getSession());
        this.lifeCycle.getContextService().endContext(ConversationScoped.class, event.getSession());

        if (mustDestroy)
        {
            requestDestroyed(null);
        }
    }

    @Override
    public void sessionCreated(HttpSessionEvent se)
    {
        // nothing to do, init is done in BeginWebBeansConfigurationListener
    }

    @Override
    public void requestInitialized(ServletRequestEvent sre)
    {
        // nothing to do, init is done in BeginWebBeansConfigurationListener
    }

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
        WebContextsService.removeThreadLocals();
    }

}
