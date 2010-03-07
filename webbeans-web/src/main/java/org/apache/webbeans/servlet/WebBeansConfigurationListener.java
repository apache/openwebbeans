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
import org.apache.webbeans.conversation.ConversationManager;
import org.apache.webbeans.lifecycle.LifecycleFactory;
import org.apache.webbeans.logger.WebBeansLogger;
import org.apache.webbeans.spi.ContainerLifecycle;

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
    private ContainerLifecycle lifeCycle = null;

    /**
     * Default constructor
     */
    public WebBeansConfigurationListener()
    {
        
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
        }
        catch (Exception e)
        {
             logger.error("Error is occured while starting application context path : " + event.getServletContext().getContextPath());
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public void contextDestroyed(ServletContextEvent event)
    {
        this.lifeCycle.stopApplication(event);
        this.lifeCycle = null;
    }

	/**
	 * {@inheritDoc}
	 */
    public void requestDestroyed(ServletRequestEvent event)
    {
        logger.debug("Destroying a request : " + event.getServletRequest().getRemoteAddr());
        this.lifeCycle.getContextService().endContext(RequestScoped.class, event);        
    }

	/**
	 * {@inheritDoc}
	 */
    public void requestInitialized(ServletRequestEvent event)
    {
        try
        {
            logger.debug("Starting a new request : " + event.getServletRequest().getRemoteAddr());
            
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
                    catch(Exception e) {
                        logger.error(OWBLogConst.ERROR_0013, e);
                    }
                }
            }

        }
        catch (Exception e)
        {
            logger.error("Error is occured while starting request : " + event.getServletRequest());        
        }
    }

	/**
	 * {@inheritDoc}
	 */
    public void sessionCreated(HttpSessionEvent event)
    {
        try
        {
            logger.debug("Starting a session with session id : " + event.getSession().getId());
            this.lifeCycle.getContextService().startContext(SessionScoped.class, event.getSession());
        }
        catch (Exception e)
        {
            logger.error("Error is occured while starting session : " + event.getSession());
        }
    }

	/**
	 * {@inheritDoc}
	 */
    public void sessionDestroyed(HttpSessionEvent event)
    {
        logger.debug("Destroying a session with session id : " + event.getSession().getId());
        this.lifeCycle.getContextService().endContext(SessionScoped.class, event.getSession());

        ConversationManager conversationManager = ConversationManager.getInstance();
        conversationManager.destroyConversationContextWithSessionId(event.getSession().getId());
    }

	/**
	 * {@inheritDoc}
	 */    
	@Override
	public void sessionDidActivate(HttpSessionEvent event) 
	{
	    //TODO activation
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void sessionWillPassivate(HttpSessionEvent event) 
	{
	    //TODO Passivation
	}

}