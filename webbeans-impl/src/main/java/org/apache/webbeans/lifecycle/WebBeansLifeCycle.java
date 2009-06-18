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
import org.apache.webbeans.config.OpenWebBeansConfiguration;
import org.apache.webbeans.config.WebBeansContainerDeployer;
import org.apache.webbeans.config.WebBeansFinder;
import org.apache.webbeans.container.ManagerImpl;
import org.apache.webbeans.container.activity.ActivityManager;
import org.apache.webbeans.context.ContextFactory;
import org.apache.webbeans.conversation.ConversationManager;
import org.apache.webbeans.el.WebBeansELResolver;
import org.apache.webbeans.exception.WebBeansException;
import org.apache.webbeans.logger.WebBeansLogger;
import org.apache.webbeans.plugins.PluginLoader;
import org.apache.webbeans.servlet.WebBeansConfigurationListener;
import org.apache.webbeans.spi.JNDIService;
import org.apache.webbeans.spi.ServiceLoader;
import org.apache.webbeans.spi.deployer.MetaDataDiscoveryService;
import org.apache.webbeans.xml.WebBeansXMLConfigurator;

/**
 * Manages container lifecycle.
 * 
 * <p>
 * Behaves according to the request, session, and application
 * contexts of the web application. 
 * </p>
 * 
 * @version $Rev$ $Date$
 * @see WebBeansConfigurationListener
 */
public final class WebBeansLifeCycle
{
	//Logger instance
    private static final WebBeansLogger logger = WebBeansLogger.getLogger(WebBeansLifeCycle.class);

    /**Manages unused conversations*/
    private ScheduledExecutorService service = null;

    /**Discover bean classes*/
    private MetaDataDiscoveryService discovery = null;

    /**Deploy discovered beans*/
    private final WebBeansContainerDeployer deployer;

    /**XML discovery. */
    //XML discovery is removed from the specification. It is here for next revisions of spec.
    private final WebBeansXMLConfigurator xmlDeployer;
    
    /**Using for lookup operations*/
    private final JNDIService jndiService;
    
    /**Root container.*/
    //Activities are removed from the specification.
    private final ManagerImpl rootManager;

    /**
     * Creates a new lifecycle instance and initializes
     * the instance variables.
     */
    public WebBeansLifeCycle()
    {
        this.rootManager = new ManagerImpl();
        this.xmlDeployer = new WebBeansXMLConfigurator();
        this.deployer = new WebBeansContainerDeployer(xmlDeployer);
        this.jndiService = ServiceLoader.getService(JNDIService.class);
        
        rootManager.setXMLConfigurator(this.xmlDeployer);
        
        ActivityManager.getInstance().setRootActivity(this.rootManager);
    }

    
    public void requestStarted(ServletRequestEvent event)
    {
        logger.debug("Starting a new request : " + event.getServletRequest().getRemoteAddr());
        ContextFactory.initRequestContext(event);
    }

    public void requestEnded(ServletRequestEvent event)
    {
    	logger.debug("Destroying a request : " + event.getServletRequest().getRemoteAddr());
        ContextFactory.destroyRequestContext((HttpServletRequest) event.getServletRequest());
    }

    public void sessionStarted(HttpSessionEvent event)
    {
        logger.debug("Starting a session with session id : " + event.getSession().getId());
        ContextFactory.initSessionContext(event.getSession());
    }

    public void sessionEnded(HttpSessionEvent event)
    {
    	logger.debug("Destroying a session with session id : " + event.getSession().getId());
        ContextFactory.destroySessionContext(event.getSession());

        ConversationManager conversationManager = ConversationManager.getInstance();
        conversationManager.destroyConversationContextWithSessionId(event.getSession().getId());
    }

    public void applicationStarted(ServletContextEvent event)
    {
        this.discovery = ServiceLoader.getService(MetaDataDiscoveryService.class);
        this.discovery.init(event.getServletContext());

        // load all optional plugins
        PluginLoader.getInstance().startUp();

        String strDelay = OpenWebBeansConfiguration.getInstance().getProperty(OpenWebBeansConfiguration.CONVERSATION_PERIODIC_DELAY,"150000");
        long delay = Long.parseLong(strDelay);
        
        service = Executors.newScheduledThreadPool(1);
        service.scheduleWithFixedDelay(new ConversationCleaner(), delay, delay, TimeUnit.MILLISECONDS);

        logger.info("Starting the dependency injection container configuration");
        long begin = System.currentTimeMillis();

        logger.info("Scanning classpaths for beans artifacts is started");

        this.discovery.scan();

        logger.info("Scanning is ended");

        logger.info("Deploying the scanned beans artifacts");

        deployer.deploy(this.discovery);

        logger.info("Deploying is ended");

        long end = System.currentTimeMillis();
        logger.info("Dependency injection container configuration is ended, takes " + Long.toString(end - begin) + " ms.");

        // Initalize Application Context
        logger.info("Initializing of the application context");
        ContextFactory.initApplicationContext(event.getServletContext());

        ServletContext context = event.getServletContext();

        try
        {
            // check this application is JSF application,this must be extended.
        	//In JSF 2.0, faces-config.xml may not be necessary!
            URL url = context.getResource("/WEB-INF/faces-config.xml");
            URL urlWeb = context.getResource("/WEB-INF/web.xml");
            if (url == null && urlWeb != null)
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
        
    	logger.info("Dependency injection container is started for context path : " + event.getServletContext().getContextPath());
    }

    public void applicationEnded(ServletContextEvent event)
    {
        service.shutdownNow();

        ContextFactory.destroyApplicationContext(event.getServletContext());

        jndiService.unbind(WebBeansConstants.WEB_BEANS_MANAGER_JNDI_NAME);

        // finally free all plugin resources
        PluginLoader.getInstance().shutDown();
        
        WebBeansFinder.clearInstances();
        
        logger.info("Dependency injection container is stopped for context path : " + event.getServletContext().getContextPath());        
    }
    
    public void sessionPassivated(HttpSessionEvent event)
    {
    	logger.info("Session is passivated. Session id : [ " + event.getSession().getId()+" ]");
    }
    
    public void sessionActivated(HttpSessionEvent event)
    {
    	logger.info("Session is activated. Session id : [ " + event.getSession().getId()+" ]");
    }
    
    private static class ConversationCleaner implements Runnable
    {
    	public ConversationCleaner()
    	{
    		
    	}
    	
        public void run()
        {
            ConversationManager.getInstance().destroyWithRespectToTimout();

        }    	
    }

}