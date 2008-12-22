/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.webbeans.servlet;


import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.apache.webbeans.lifecycle.WebBeansLifeCycle;

/**
 * Configures the all web beans components that are defined in the EAR or WAR
 * file.
 * @author <a href="mailto:gurkanerdogdu@yahoo.com">Gurkan Erdogdu</a>
 * @since 1.0
 */
public class WebBeansConfigurationListener implements ServletContextListener,ServletRequestListener,HttpSessionListener
{
	private WebBeansLifeCycle lifeCycle = null;

	/**
	 * Performed when the servlet context destroyed.
	 */
	public void contextDestroyed(ServletContextEvent event)
	{
		this.lifeCycle.applicationEnded(event);
	}

	/**
	 * Performed when the servlet context started.
	 */
	public void contextInitialized(ServletContextEvent event)
	{
		this.lifeCycle = new WebBeansLifeCycle();
		this.lifeCycle.applicationStarted(event);
	}

	/**
	 * Destroy request context
	 */
	public void requestDestroyed(ServletRequestEvent event)
	{
		this.lifeCycle.requestEnded(event);
	}

	/**
	 * Initializes the request context
	 */
	public void requestInitialized(ServletRequestEvent event)
	{
		this.lifeCycle.requestStarted(event);
	}

	/**
	 * Initializes session context
	 */
	public void sessionCreated(HttpSessionEvent event)
	{
		this.lifeCycle.sessionStarted(event);
	}

	/**
	 * Destroy session context
	 */
	public void sessionDestroyed(HttpSessionEvent event)
	{
		this.lifeCycle.sessionEnded(event);
	}
	
}
