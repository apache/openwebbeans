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

import java.lang.annotation.Annotation;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.enterprise.inject.spi.BeanManager;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletRequestEvent;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.jsp.JspApplicationContext;
import javax.servlet.jsp.JspFactory;

import org.apache.webbeans.WebBeansConstants;
import org.apache.webbeans.config.OpenWebBeansConfiguration;
import org.apache.webbeans.config.BeansDeployer;
import org.apache.webbeans.config.WebBeansFinder;
import org.apache.webbeans.container.BeanManagerImpl;
import org.apache.webbeans.context.ContextFactory;
import org.apache.webbeans.conversation.ConversationManager;
import org.apache.webbeans.el.WebBeansELResolver;
import org.apache.webbeans.exception.WebBeansException;
import org.apache.webbeans.logger.WebBeansLogger;
import org.apache.webbeans.plugins.PluginLoader;
import org.apache.webbeans.portable.events.ExtensionLoader;
import org.apache.webbeans.portable.events.discovery.BeforeShutdownImpl;
import org.apache.webbeans.servlet.WebBeansConfigurationListener;
import org.apache.webbeans.spi.JNDIService;
import org.apache.webbeans.spi.Lifecycle;
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
public final class EnterpriseLifeCycle implements Lifecycle
{
	//Logger instance
    private static final WebBeansLogger logger = WebBeansLogger.getLogger(EnterpriseLifeCycle.class);

    /**Manages unused conversations*/
    private ScheduledExecutorService service = null;

    /**Discover bean classes*/
    private MetaDataDiscoveryService discovery = null;

    /**Deploy discovered beans*/
    private final BeansDeployer deployer;

    /**XML discovery. */
    //XML discovery is removed from the specification. It is here for next revisions of spec.
    private final WebBeansXMLConfigurator xmlDeployer;
    
    /**Using for lookup operations*/
    private final JNDIService jndiService;
    
    /**Root container.*/
    //Activities are removed from the specification.
    private final BeanManagerImpl rootManager;

    /**
     * Creates a new lifecycle instance and initializes
     * the instance variables.
     */
    public EnterpriseLifeCycle()
    {
        this.rootManager = (BeanManagerImpl) WebBeansFinder.getSingletonInstance(WebBeansFinder.SINGLETON_MANAGER);
        this.xmlDeployer = new WebBeansXMLConfigurator();
        this.deployer = new BeansDeployer(xmlDeployer);
        this.jndiService = ServiceLoader.getService(JNDIService.class);
        
        init();
    }

    public void init()
    {
        rootManager.setXMLConfigurator(this.xmlDeployer);        
    }
    
    public void requestStarted(ServletRequestEvent event)
    {
        logger.debug("Starting a new request : " + event.getServletRequest().getRemoteAddr());
        
        ContextFactory.initializeThreadLocals();
        
        //Init Request Context
        ContextFactory.initRequestContext(event);
        
        //Session Conext Must be Active
        Object request = event.getServletRequest();
        if(request instanceof HttpServletRequest)
        {
            HttpServletRequest httpRequest = (HttpServletRequest)request;
            HttpSession currentSession = httpRequest.getSession(false);
            if(currentSession == null)
            {
                //To activate session context
                httpRequest.getSession();
            }
        }
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

    public void applicationStarted(Object startupObject)
    {
        if(startupObject != null && !(ServletContextEvent.class.isAssignableFrom(startupObject.getClass())))
        {
            throw new WebBeansException("Wrong initialization object");
        }
        
        ServletContextEvent event = (ServletContextEvent)startupObject; 
        
        // Initalize Application Context
        logger.info("OpenWebBeans Container is starting");
        
        long begin = System.currentTimeMillis();

        //Application Context initialization
        ContextFactory.initApplicationContext(event.getServletContext());
        
        //Singleton context
        ContextFactory.initSingletonContext(event.getServletContext());

        this.discovery = ServiceLoader.getService(MetaDataDiscoveryService.class);
        this.discovery.init(event.getServletContext());

        // load all optional plugins
        PluginLoader.getInstance().startUp();

        String strDelay = OpenWebBeansConfiguration.getInstance().getProperty(OpenWebBeansConfiguration.CONVERSATION_PERIODIC_DELAY,"150000");
        long delay = Long.parseLong(strDelay);
        
        service = Executors.newScheduledThreadPool(1);
        service.scheduleWithFixedDelay(new ConversationCleaner(), delay, delay, TimeUnit.MILLISECONDS);

        logger.info("Scanning classpaths for beans artifacts");

        this.discovery.scan();

        logger.info("Deploying scanned beans");

        deployer.deploy(this.discovery);
        
        //Application is configured as JSP
        if(OpenWebBeansConfiguration.getInstance().isJspApplication())
        {
            logger.debug("Application is configured as JSP. Adding EL Resolver");
            
            ServletContext context = event.getServletContext();

            JspApplicationContext applicationCtx = JspFactory.getDefaultFactory().getJspApplicationContext(context);
            applicationCtx.addELResolver(new WebBeansELResolver());            
        }
        
        long end = System.currentTimeMillis();
        
        logger.info("OpenWebBeans Container is started, it takes " + Long.toString(end - begin) + " ms.");
    }

    public void applicationEnded(Object endObject)
    {
        logger.info("OpenWebBeans Container is stopping");
        
        if(endObject != null && !(ServletContextEvent.class.isAssignableFrom(endObject.getClass())))
        {
            throw new WebBeansException("Wrong ended object");
        }
        
        ServletContextEvent event = (ServletContextEvent)endObject;
        
        //Fire shut down
        this.rootManager.fireEvent(new BeforeShutdownImpl(), new Annotation[0]);
                
        service.shutdownNow();

        ContextFactory.destroyApplicationContext(event.getServletContext());
        
        ContextFactory.destroySingletonContext(event.getServletContext());

        jndiService.unbind(WebBeansConstants.WEB_BEANS_MANAGER_JNDI_NAME);

        // finally free all plugin resources
        PluginLoader.getInstance().shutDown();
        
        //Clear extensions
        ExtensionLoader.getInstance().clear();
        
        //Clear singleton list
        WebBeansFinder.clearInstances();
                
        logger.info("OpenWebBeans Container is stopped for context path, " + event.getServletContext().getContextPath());        
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

    @Override
    public BeanManager getBeanManager()
    {
        return this.rootManager;
    }

}