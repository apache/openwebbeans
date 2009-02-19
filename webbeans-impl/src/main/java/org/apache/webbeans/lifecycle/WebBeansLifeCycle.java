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
package org.apache.webbeans.lifecycle;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletRequestEvent;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.jsp.JspApplicationContext;
import javax.servlet.jsp.JspFactory;

import org.apache.webbeans.WebBeansConstants;
import org.apache.webbeans.config.WebBeansContainerDeployer;
import org.apache.webbeans.config.WebBeansFinder;
import org.apache.webbeans.config.WebBeansScanner;
import org.apache.webbeans.container.ManagerImpl;
import org.apache.webbeans.context.ContextFactory;
import org.apache.webbeans.el.WebBeansELResolver;
import org.apache.webbeans.exception.WebBeansException;
import org.apache.webbeans.jsf.ConversationManager;
import org.apache.webbeans.logger.WebBeansLogger;
import org.apache.webbeans.spi.JNDIService;
import org.apache.webbeans.spi.ServiceLoader;
import org.apache.webbeans.xml.WebBeansXMLConfigurator;

public final class WebBeansLifeCycle
{
    private static WebBeansLogger logger = WebBeansLogger.getLogger(WebBeansLifeCycle.class);

    private ScheduledExecutorService service = null;

    private WebBeansScanner scanner = null;

    private WebBeansContainerDeployer deployer = null;

    private WebBeansXMLConfigurator xmlDeployer = null;
    
    private JNDIService jndiService = null;

    public WebBeansLifeCycle()
    {
        this.scanner = new WebBeansScanner();
        this.xmlDeployer = new WebBeansXMLConfigurator();
        this.deployer = new WebBeansContainerDeployer(xmlDeployer);
        this.jndiService = ServiceLoader.getService(JNDIService.class);
        
        ManagerImpl.getManager().setXMLConfigurator(this.xmlDeployer);
    }

    public void requestStarted(ServletRequestEvent event)
    {
        logger.info("Initializing of the Request Context with Remote Address : " + event.getServletRequest().getRemoteAddr());
        ContextFactory.initRequestContext((HttpServletRequest) event.getServletRequest());
    }

    public void requestEnded(ServletRequestEvent event)
    {
        logger.info("Destroying of the Request Context with Remote Address : " + event.getServletRequest().getRemoteAddr());
        ContextFactory.destroyRequestContext((HttpServletRequest) event.getServletRequest());
    }

    public void sessionStarted(HttpSessionEvent event)
    {
        logger.info("Initializing of the Session Context with session id : " + event.getSession().getId());
        ContextFactory.initSessionContext(event.getSession());
    }

    public void sessionEnded(HttpSessionEvent event)
    {
        logger.info("Destroying of the Session Context with session id : " + event.getSession().getId());
        ContextFactory.destroySessionContext(event.getSession());

        ConversationManager conversationManager = ConversationManager.getInstance();
        conversationManager.destroyConversationContextWithSessionId(event.getSession().getId());
    }

    public void applicationStarted(ServletContextEvent event)
    {
        // I do not know this is the correct way, spec is not so explicit.
        service = Executors.newScheduledThreadPool(1);
        service.scheduleWithFixedDelay(new Runnable()
        {

            public void run()
            {
                ConversationManager.getInstance().destroyWithRespectToTimout();

            }

        }, 15000, 15000, TimeUnit.MILLISECONDS);

        logger.info("Starting the WebBeans Container Configuration");
        long begin = System.currentTimeMillis();

        logger.info("Scanning classpaths for WebBeans artifacts is started");

        scanner.scan(event.getServletContext());

        logger.info("Scanning is ended.");

        logger.info("Deploying the scanned WebBeans artifacts.");

        deployer.deploy(this.scanner);

        logger.info("Deploying is ended.");

        long end = System.currentTimeMillis();
        logger.info("WebBeans Container Configuration is ended, takes " + Long.toString(end - begin) + " ms.");

        // Initalize Application Context
        logger.info("Initializing of the Application Context with Context Path : " + event.getServletContext().getContextPath());
        ContextFactory.initApplicationContext(event.getServletContext());

        ServletContext context = event.getServletContext();

        try
        {
            // check this application is JSF application
            URL url = context.getResource("/WEB-INF/faces-config.xml");
            if (url == null)
            {
                JspApplicationContext applicationCtx = JspFactory.getDefaultFactory().getJspApplicationContext(context);
                applicationCtx.addELResolver(new WebBeansELResolver());
            }

        }
        catch (MalformedURLException e)
        {
            logger.error(e);
            throw new WebBeansException(e);
        }

    }

    public void applicationEnded(ServletContextEvent event)
    {
        service.shutdownNow();

        logger.info("Destroying of the Application Context with Context Path : " + event.getServletContext().getContextPath());
        ContextFactory.destroyApplicationContext(event.getServletContext());

        jndiService.unbind(WebBeansConstants.WEB_BEANS_MANAGER_JNDI_NAME);

        this.deployer = null;
        this.scanner = null;
        this.service = null;
        this.xmlDeployer = null;
        
        WebBeansFinder.clearInstances();

    }

}