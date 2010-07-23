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

import java.util.Properties;

import org.apache.webbeans.config.WebBeansFinder;
import org.apache.webbeans.context.ContextFactory;
import org.apache.webbeans.el.ELContextStore;
import org.apache.webbeans.logger.WebBeansLogger;
import org.apache.webbeans.util.WebBeansUtil;

/**
 * Default LifeCycle for a standalone application without a ServletContainer.
 */
public class StandaloneLifeCycle extends AbstractLifeCycle
{
    private static final WebBeansLogger logger = WebBeansLogger.getLogger(StandaloneLifeCycle.class);
        
    public StandaloneLifeCycle(Properties properties, WebBeansLogger logger)
    {
        super(properties, logger);        
    }
    
    public StandaloneLifeCycle()
    {
        this(null, logger);        
    }
    
    @Override
    public void beforeInitApplication(Properties properties)
    {
        WebBeansFinder.clearInstances(WebBeansUtil.getCurrentClassLoader());
    }
    
    @Override
    public void beforeStartApplication(Object object)
    {
        ContextFactory.initRequestContext(null);
        ContextFactory.initSessionContext(null);
        ContextFactory.initConversationContext(null);
        ContextFactory.initApplicationContext(null);       
        ContextFactory.initSingletonContext(null);
    }
        
    
    @Override
    public void beforeStopApplication(Object endObject)
    {
        ContextFactory.destroyRequestContext(null);
        ContextFactory.destroySessionContext(null);
        ContextFactory.destroyConversationContext();
        ContextFactory.destroyApplicationContext(null);
        ContextFactory.destroySingletonContext(null);

        ContextFactory.cleanUpContextFactory();

        // clean up the EL caches after each request
        ELContextStore elStore = ELContextStore.getInstance(false);
        if (elStore != null)
        {
            elStore.destroyELContextStore();
        }
    }
    
}