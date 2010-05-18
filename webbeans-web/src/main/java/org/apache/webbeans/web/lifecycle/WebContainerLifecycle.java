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
package org.apache.webbeans.web.lifecycle;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.el.ELContextListener;
import javax.el.ELResolver;
import javax.enterprise.inject.spi.BeanManager;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.jsp.JspApplicationContext;
import javax.servlet.jsp.JspFactory;

import org.apache.webbeans.config.OpenWebBeansConfiguration;
import org.apache.webbeans.config.OWBLogConst;
import org.apache.webbeans.conversation.ConversationManager;
import org.apache.webbeans.corespi.ServiceLoader;
import org.apache.webbeans.exception.WebBeansException;
import org.apache.webbeans.lifecycle.AbstractLifeCycle;
import org.apache.webbeans.logger.WebBeansLogger;
import org.apache.webbeans.spi.adaptor.ELAdaptor;

/**
 * Manages container lifecycle.
 * 
 * <p>
 * Behaves according to the request, session, and application
 * contexts of the web application. 
 * </p>
 * 
 * @version $Rev: 911764 $ $Date: 2010-02-19 11:52:54 +0200 (Fri, 19 Feb 2010) $
 * @see WebBeansConfigurationListener
 */
public final class WebContainerLifecycle extends AbstractLifeCycle
{
	//Logger instance
    private static final WebBeansLogger logger = WebBeansLogger.getLogger(WebContainerLifecycle.class);

    /**Manages unused conversations*/
    private ScheduledExecutorService service = null;
    

    /**
     * Creates a new lifecycle instance and initializes
     * the instance variables.
     */
    public WebContainerLifecycle()
    {
        super(null,logger);
    }
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void startApplication(Object startupObject) throws Exception
    {
        ServletContext servletContext = getServletContext(startupObject);        
        super.startApplication(servletContext);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void stopApplication(Object endObject)
    {
        ServletContext servletContext = getServletContext(endObject);        
        super.stopApplication(servletContext);
    }
    
    /**
     * {@inheritDoc}
     */
    protected void afterStartApplication(Object startupObject) throws Exception    
    {
        String strDelay = OpenWebBeansConfiguration.getInstance().getProperty(OpenWebBeansConfiguration.CONVERSATION_PERIODIC_DELAY,"150000");
        long delay = Long.parseLong(strDelay);
        
        service = Executors.newScheduledThreadPool(1);
        service.scheduleWithFixedDelay(new ConversationCleaner(), delay, delay, TimeUnit.MILLISECONDS);
        
        ELAdaptor elAdaptor = ServiceLoader.getService(ELAdaptor.class);
        ELResolver resolver = elAdaptor.getOwbELResolver();
        ELContextListener elContextListener = elAdaptor.getOwbELContextListener();
        //Application is configured as JSP
        if(OpenWebBeansConfiguration.getInstance().isJspApplication())
        {
            logger.debug("Application is configured as JSP. Adding EL Resolver.");
            
            JspApplicationContext applicationCtx = JspFactory.getDefaultFactory().getJspApplicationContext((ServletContext)(startupObject));
            applicationCtx.addELResolver(resolver);  
            
            logger.debug("Application is configured as JSP. Adding EL Listener.");
            
            //Adding listener
            applicationCtx.addELContextListener(elContextListener);                        
        }              
        
        // Add BeanManager to the 'javax.enterprise.inject.spi.BeanManager' servlet context attribute
        ServletContext servletContext = (ServletContext)(startupObject); 
        servletContext.setAttribute(BeanManager.class.getName(), getBeanManager());
        
    }
    
    protected void beforeStartApplication(Object startupObject)
    {
        this.scannerService.init(startupObject);
    }
    

    /**
     * {@inheritDoc}
     */
    protected void beforeStopApplication(Object stopObject) throws Exception
    {
        if(service != null)
        {
            service.shutdownNow();   
        }                
    }

    /**
     * {@inheritDoc}
     */
    protected void afterStopApplication(Object stopObject) throws Exception
    {                
        ServletContext servletContext = null;
        
        if(stopObject instanceof ServletContext)
        {
            servletContext = (ServletContext)stopObject;
        }
        else
        {
            servletContext = getServletContext(stopObject);
        }
        
        logger.info(OWBLogConst.INFO_0003, new Object[]{servletContext != null ? servletContext.getContextPath() : null});
    }
    
    /**
     * Returns servelt context otherwise throws exception.
     * @param object object
     * @return servlet context
     */
    private ServletContext getServletContext(Object object)
    {
        if(object != null)
        {
            if(object instanceof ServletContextEvent)
            {
                object = ((ServletContextEvent) object).getServletContext();
                return (ServletContext)object;
            }
            else
            {
                throw new WebBeansException(logger.getTokenString(OWBLogConst.EXCEPT_0002));
            }
        }                
        
        throw new IllegalArgumentException("Must give ServletContext object but found null");
    }
    
    /**
     * Conversation cleaner thread, that
     * clears unused conversations.
     *
     */
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
