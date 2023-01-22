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

import org.apache.webbeans.annotation.InitializedLiteral;
import org.apache.webbeans.config.OWBLogConst;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.context.AbstractContextsService;
import org.apache.webbeans.el.ELContextStore;
import org.apache.webbeans.exception.WebBeansException;
import org.apache.webbeans.lifecycle.AbstractLifeCycle;
import org.apache.webbeans.logger.WebBeansLoggerFacade;
import org.apache.webbeans.spi.ContextsService;
import org.apache.webbeans.spi.ResourceInjectionService;
import org.apache.webbeans.spi.adaptor.ELAdaptor;
import org.apache.webbeans.web.context.WebContextsService;
import org.apache.webbeans.web.util.ServletCompatibilityUtil;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.ConversationScoped;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.context.SessionScoped;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Singleton;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.jsp.JspApplicationContext;
import javax.servlet.jsp.JspFactory;
import java.util.logging.Level;

/**
 * Manages container lifecycle.
 *
 * <p>
 * Behaves according to the request, session, and application
 * contexts of the web application.
 * </p>
 *
 * @version $Rev: 911764 $ $Date: 2010-02-19 11:52:54 +0200 (Fri, 19 Feb 2010) $
 * @see org.apache.webbeans.servlet.WebBeansConfigurationListener
 */
public class WebContainerLifecycle extends AbstractLifeCycle
{

    /**
     * Creates a new lifecycle instance and initializes
     * the instance variables.
     */
    public WebContainerLifecycle()
    {
        super(null);
        this.logger = WebBeansLoggerFacade.getLogger(WebContainerLifecycle.class);
    }

    /**
     * Creates a new lifecycle instance and initializes
     * the instance variables.
     */
    public WebContainerLifecycle(WebBeansContext webBeansContext)
    {
        super(null, webBeansContext);
        this.logger = WebBeansLoggerFacade.getLogger(WebContainerLifecycle.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void startApplication(Object startupObject)
    {
        ServletContext servletContext = getServletContext(startupObject);
        // this flag is just there in case some integration does it already so we should be able to switch it off
        if ("true".equalsIgnoreCase(getWebBeansContext().getOpenWebBeansConfiguration().getProperty("org.apache.webbeans.web.add-beans", "true")))
        {
            webBeansContext.getBeanManagerImpl().addInternalBean(new ServletContextBean(webBeansContext, servletContext));

            ContextsService contextsService = webBeansContext.getContextsService();
            if (WebContextsService.class.isInstance(contextsService))
            {
                webBeansContext.getBeanManagerImpl().addInternalBean(new ServletRequestBean(webBeansContext, WebContextsService.class.cast(contextsService)));
            }
        }
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
    @Override
    protected void afterStartApplication(Object startupObject)
    {
        //Application is configured as JSP
        if(getWebBeansContext().getOpenWebBeansConfiguration().isJspApplication())
        {
            ELAdaptor elAdaptor = getWebBeansContext().getService(ELAdaptor.class);

            logger.log(Level.FINE, "Application is configured as JSP. Adding EL Resolver.");

            setJspELFactory((ServletContext) startupObject, elAdaptor.getOwbELResolver());
        }

        ServletContext servletContext =  null;
        if (startupObject instanceof ServletContext)
        {
            servletContext = (ServletContext)(startupObject);

            // Add BeanManager to the 'jakarta.enterprise.inject.spi.BeanManager' servlet context attribute
            servletContext.setAttribute(BeanManager.class.getName(), getBeanManager());
        }

        // fire @Initialized(ApplicationScoped.class) if any observer for it exists
        if (webBeansContext.getNotificationManager().
            hasContextLifecycleObserver(InitializedLiteral.INSTANCE_APPLICATION_SCOPED))
        {
            // we need to temporarily start the ReqeustContext
            webBeansContext.getContextsService().startContext(RequestScoped.class, null);

            webBeansContext.getBeanManagerImpl().fireEvent(
                servletContext != null ? servletContext : new Object(), InitializedLiteral.INSTANCE_APPLICATION_SCOPED);

            // shut down the RequestContext again
            webBeansContext.getContextsService().endContext(RequestScoped.class, null);
        }
    }

    @Override
    protected void beforeStartApplication(Object startupObject)
    {
        this.scannerService.init(startupObject);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void beforeStopApplication(Object stopObject)
    {
        final ContextsService contextsService = webBeansContext.getContextsService();
        contextsService.endContext(RequestScoped.class, null);
        if (AbstractContextsService.class.isInstance(contextsService) &&
                AbstractContextsService.class.cast(contextsService).isSupportsConversation())
        {
            contextsService.endContext(ConversationScoped.class, null);
        }
        contextsService.endContext(SessionScoped.class, null);
        contextsService.endContext(ApplicationScoped.class, null);
        contextsService.endContext(Singleton.class, null);

        // clean up the EL caches after each request
        ELContextStore elStore = ELContextStore.getInstance(false);
        if (elStore != null)
        {
            elStore.destroyELContextStore();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void afterStopApplication(Object stopObject)
    {
        ServletContext servletContext;

        if(stopObject instanceof ServletContext)
        {
            servletContext = (ServletContext)stopObject;
        }
        else
        {
            servletContext = getServletContext(stopObject);
        }

        //Clear the resource injection service
        ResourceInjectionService injectionServices = getWebBeansContext().getService(ResourceInjectionService.class);
        if(injectionServices != null)
        {
            injectionServices.clear();
        }

        //Comment out for commit OWB-502
        //ContextFactory.cleanUpContextFactory();

        this.cleanupShutdownThreadLocals();

        if (logger.isLoggable(Level.INFO))
        {
          logger.log(Level.INFO, OWBLogConst.INFO_0002, ServletCompatibilityUtil.getServletInfo(servletContext));
        }
    }

  /**
     * Ensures that all ThreadLocals, which could have been set in this
     * (shutdown-) Thread, are removed in order to prevent memory leaks.
     */
    private void cleanupShutdownThreadLocals()
    {
        contextsService.removeThreadLocals();
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
                throw new WebBeansException(WebBeansLoggerFacade.getTokenString(OWBLogConst.EXCEPT_0018));
            }
        }

        throw new IllegalArgumentException("ServletContextEvent object but found null");
    }

    protected void setJspELFactory(ServletContext startupObject, Object resolver)
    {
        JspFactory factory = JspFactory.getDefaultFactory();
        if (factory == null)
        {
            try
            {
                try
                {   // no need of using the tccl since in OSGi it is init elsewhere and using container shortcut can just make it faster
                    Class.forName("org.apache.jasper.servlet.JasperInitializer", true, WebContainerLifecycle.class.getClassLoader());
                }
                catch (Throwable th)
                {
                    Class.forName("org.apache.jasper.compiler.JspRuntimeContext", true, WebContainerLifecycle.class.getClassLoader());
                }
                factory = JspFactory.getDefaultFactory();
            }
            catch (Exception e)
            {
                // ignore
            }

        }

        if (factory != null)
        {
            JspApplicationContext applicationCtx = factory.getJspApplicationContext(startupObject);
            applicationCtx.addELResolver(jakarta.el.ELResolver.class.cast(resolver));
        }
        else
        {
            logger.log(Level.FINE, "Default JSPFactroy instance has not found");
        }
    }

}
