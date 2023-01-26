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
package org.apache.webbeans.lifecycle;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.ConversationScoped;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.context.SessionScoped;
import jakarta.enterprise.event.Startup;
import jakarta.inject.Singleton;
import java.util.Properties;
import java.util.logging.Logger;

import org.apache.webbeans.annotation.InitializedLiteral;
import org.apache.webbeans.config.WebBeansFinder;
import org.apache.webbeans.corespi.se.BaseSeContextsService;
import org.apache.webbeans.el.ELContextStore;
import org.apache.webbeans.logger.WebBeansLoggerFacade;
import org.apache.webbeans.util.WebBeansUtil;

/**
 * Default LifeCycle for a standalone application without a ServletContainer.
 */
public class StandaloneLifeCycle extends AbstractLifeCycle
{
    public StandaloneLifeCycle(Properties properties, Logger logger)
    {
        super(properties);
        this.logger = logger;
    }
    
    public StandaloneLifeCycle()
    {
        this(null, WebBeansLoggerFacade.getLogger(StandaloneLifeCycle.class));
    }

    @Override
    public void beforeStartApplication(Object object)
    {
        webBeansContext.getContextsService().startContext(Singleton.class, null);
        webBeansContext.getContextsService().startContext(ApplicationScoped.class, null);
    }

    @Override
    protected void afterStartApplication(Object startupObject)
    {
        webBeansContext.getContextsService().startContext(RequestScoped.class, null);
        webBeansContext.getContextsService().startContext(SessionScoped.class, null);
        webBeansContext.getContextsService().startContext(ConversationScoped.class, null);

        if (!BaseSeContextsService.class.isInstance(webBeansContext.getContextsService()) ||
                BaseSeContextsService.class.cast(webBeansContext.getContextsService()).fireApplicationScopeEvents())
        {
            // the ApplicationContext is already started, but we fire
            // the event again as the userland beans are only available now
            webBeansContext.getBeanManagerImpl().fireContextLifecyleEvent(
                    new Object(), InitializedLiteral.INSTANCE_APPLICATION_SCOPED);
        }

        // fire Startup event
        webBeansContext.getBeanManagerImpl().fireEvent(new Startup());
    }

    @Override
    public void beforeStopApplication(Object endObject)
    {
        webBeansContext.getContextsService().endContext(RequestScoped.class, null);
        webBeansContext.getContextsService().endContext(ConversationScoped.class, null);
        webBeansContext.getContextsService().endContext(SessionScoped.class, null);
        webBeansContext.getContextsService().endContext(ApplicationScoped.class, null);
        webBeansContext.getContextsService().endContext(Singleton.class, null);

        // clean up the EL caches after each request
        ELContextStore elStore = ELContextStore.getInstance(false);
        if (elStore != null)
        {
            elStore.destroyELContextStore();
        }
    }

    @Override
    protected void afterStopApplication(Object stopObject)
    {
        WebBeansFinder.clearInstances(WebBeansUtil.getCurrentClassLoader());
    }
}
