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

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpSessionActivationListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.apache.webbeans.lifecycle.DefaultLifecycle;

/**
 * Initializing the beans container for using in an web application
 * environment.
 * 
 * @version $Rev$ $Date$
 */
public class WebBeansConfigurationListener implements ServletContextListener, ServletRequestListener, HttpSessionListener,HttpSessionActivationListener
{
	/**Manages the container lifecycle*/
    private DefaultLifecycle lifeCycle = null;

    /**
     * Default constructor
     */
    public WebBeansConfigurationListener()
    {
    	super();
    }
    
	/**
	 * {@inheritDoc}
	 */
    public void contextDestroyed(ServletContextEvent event)
    {
        this.lifeCycle.applicationEnded(event);
        this.lifeCycle = null;
    }

	/**
	 * {@inheritDoc}
	 */
    public void contextInitialized(ServletContextEvent event)
    {
        this.lifeCycle = new DefaultLifecycle();
        this.lifeCycle.applicationStarted(event);
    }

	/**
	 * {@inheritDoc}
	 */
    public void requestDestroyed(ServletRequestEvent event)
    {
        this.lifeCycle.requestEnded(event);
    }

	/**
	 * {@inheritDoc}
	 */
    public void requestInitialized(ServletRequestEvent event)
    {
        this.lifeCycle.requestStarted(event);
    }

	/**
	 * {@inheritDoc}
	 */
    public void sessionCreated(HttpSessionEvent event)
    {
        this.lifeCycle.sessionStarted(event);
    }

	/**
	 * {@inheritDoc}
	 */
    public void sessionDestroyed(HttpSessionEvent event)
    {
        this.lifeCycle.sessionEnded(event);
    }

	/**
	 * {@inheritDoc}
	 */    
	@Override
	public void sessionDidActivate(HttpSessionEvent event) 
	{
		this.lifeCycle.sessionActivated(event);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void sessionWillPassivate(HttpSessionEvent event) 
	{
		this.lifeCycle.sessionPassivated(event);
	}

}