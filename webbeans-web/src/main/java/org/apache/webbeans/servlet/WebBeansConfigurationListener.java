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
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionActivationListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.apache.webbeans.config.OWBLogConst;
import org.apache.webbeans.config.OpenWebBeansConfiguration;
import org.apache.webbeans.conversation.ConversationManager;
import org.apache.webbeans.corespi.ServiceLoader;
import org.apache.webbeans.el.ELContextStore;
import org.apache.webbeans.lifecycle.LifecycleFactory;
import org.apache.webbeans.logger.WebBeansLogger;
import org.apache.webbeans.spi.ContainerLifecycle;
import org.apache.webbeans.spi.FailOverService;
import org.apache.webbeans.util.WebBeansUtil;

/**
 * Initializing the beans container for using in an web application
 * environment.
 * 
 * @version $Rev: 910075 $ $Date: 2010-02-14 23:17:23 +0200 (Sun, 14 Feb 2010) $
 */
public class WebBeansConfigurationListener implements ServletContextListener, ServletRequestListener, HttpSessionListener,HttpSessionActivationListener
{
    /**Logger instance*/
    private static final WebBeansLogger logger = WebBeansLogger.getLogger(WebBeansConfigurationListener.class);
    
    /**Manages the container lifecycle*/
    protected ContainerLifecycle lifeCycle = null;
    
    protected FailOverService failoverService = null;
    
    /**
     * Default constructor
     */
    public WebBeansConfigurationListener()
    {
        failoverService = (FailOverService)
        ServiceLoader.getService(FailOverService.class);
    }

    /**
     * {@inheritDoc}
     */
    public void contextInitialized(ServletContextEvent event)
    {
        this.lifeCycle = LifecycleFactory.getInstance().getLifecycle();

        try
        {
                this.lifeCycle.startApplication(event);  
                event.getServletContext().setAttribute(OpenWebBeansConfiguration.PROPERTY_OWB_APPLICATION, "true");
        }
        catch (Exception e)
        {
             logger.error(OWBLogConst.ERROR_0018, event.getServletContext().getContextPath());
             WebBeansUtil.throwRuntimeExceptions(e);
        }
    }
    
    
    /**
     * {@inheritDoc}
     */
    public void contextDestroyed(ServletContextEvent event)
    {
        this.lifeCycle.stopApplication(event);
        this.lifeCycle = null;
        event.getServletContext().setAttribute(OpenWebBeansConfiguration.PROPERTY_OWB_APPLICATION, "false");
    }

    /**
     * {@inheritDoc}
     */
    public void requestDestroyed(ServletRequestEvent event)
    {
        if (logger.wblWillLogDebug())
        {
            logger.debug("Destroying a request : [{0}]", event.getServletRequest().getRemoteAddr());
        }

        if (failoverService != null && 
                failoverService.isSupportFailOver()) 
        {
            Object request = event.getServletRequest();
            if(request instanceof HttpServletRequest)
            {
                HttpServletRequest httpRequest = (HttpServletRequest)request;
                HttpSession session = httpRequest.getSession(false);
                if (session != null) 
                {
                    failoverService.sessionIsIdle(session);
                }
            }
        }
        
        // clean up the EL caches after each request
        ELContextStore elStore = ELContextStore.getInstance(false);
        if (elStore != null)
        {
            elStore.destroyELContextStore();
        }

        this.lifeCycle.getContextService().endContext(RequestScoped.class, event);
    }

    /**
     * {@inheritDoc}
     */
    public void requestInitialized(ServletRequestEvent event)
    {
        try
        {
            if (logger.wblWillLogDebug())
            {
                logger.debug("Starting a new request : [{0}]", event.getServletRequest().getRemoteAddr());
            }
            
            this.lifeCycle.getContextService().startContext(RequestScoped.class, event);
            
            //Session Conext Must be Active
            Object request = event.getServletRequest();
            if(request instanceof HttpServletRequest)
            {
                HttpServletRequest httpRequest = (HttpServletRequest)request;
                HttpSession currentSession = httpRequest.getSession(false);
                if(currentSession == null)
                {
                    //To activate session context
                    try 
                    {
                        httpRequest.getSession();
                    }
                    catch(Exception e)
                    {
                        logger.error(OWBLogConst.ERROR_0013, e);
                    }
                } 
                else 
                {
                    if (failoverService != null && 
                            failoverService.isSupportFailOver()) 
                    {
                        failoverService.sessionIsInUse(currentSession);
                    }
                }                
            }

        }
        catch (Exception e)
        {
            logger.error(OWBLogConst.ERROR_0019, event.getServletRequest());
            WebBeansUtil.throwRuntimeExceptions(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void sessionCreated(HttpSessionEvent event)
    {
        try
        {
            if (logger.wblWillLogDebug())
            {
                logger.debug("Starting a session with session id : [{0}]", event.getSession().getId());
            }
            this.lifeCycle.getContextService().startContext(SessionScoped.class, event.getSession());
        }
        catch (Exception e)
        {
            logger.error(OWBLogConst.ERROR_0020, event.getSession());
            WebBeansUtil.throwRuntimeExceptions(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void sessionDestroyed(HttpSessionEvent event)
    {
        if (logger.wblWillLogDebug())
        {
            logger.debug("Destroying a session with session id : [{0}]", event.getSession().getId());
        }
        this.lifeCycle.getContextService().endContext(SessionScoped.class, event.getSession());

        ConversationManager conversationManager = ConversationManager.getInstance();
        conversationManager.destroyConversationContextWithSessionId(event.getSession().getId());
    }

    @Override
    public void sessionWillPassivate(HttpSessionEvent event) 
    {
        if (failoverService != null &&
            failoverService.isSupportPassivation())
        {
            HttpSession session = event.getSession();
            failoverService.sessionWillPassivate(session);
        }

    }

    @Override
    public void sessionDidActivate(HttpSessionEvent event)
    {
        if (failoverService.isSupportFailOver() ||
            failoverService.isSupportPassivation())
        {
            HttpSession session = event.getSession();
            failoverService.restoreBeans(session);
        }
    }
}
