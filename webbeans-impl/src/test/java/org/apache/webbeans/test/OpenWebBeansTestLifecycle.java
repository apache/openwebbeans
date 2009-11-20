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
package org.apache.webbeans.test;

import org.apache.webbeans.container.activity.ActivityManager;
import org.apache.webbeans.context.ContextFactory;
import org.apache.webbeans.exception.WebBeansException;
import org.apache.webbeans.lifecycle.StandaloneLifeCycle;
import org.apache.webbeans.test.mock.MockHttpSession;
import org.apache.webbeans.test.mock.MockServletContextEvent;

/**
 * Ease the writing of the tests. Simulates container
 * startup and stop functionality.
 * @version $Rev$ $Date$
 *
 */
public class OpenWebBeansTestLifecycle extends StandaloneLifeCycle
{
    private MockServletContextEvent servletContextEvent;
    
    private MockHttpSession mockHttpSession;

    public OpenWebBeansTestLifecycle()
    {
        super();        
    }
    
    public void init()
    {
        this.mockHttpSession = new MockHttpSession();
        this.servletContextEvent = new MockServletContextEvent();
        
        ContextFactory.initRequestContext(null);
        ContextFactory.initSessionContext(mockHttpSession);
        ContextFactory.initConversationContext(null);
        ContextFactory.initApplicationContext(this.servletContextEvent.getServletContext());        
        
        this.discoveryService = new OpenWebBeansTestMetaDataDiscoveryService();
        
        this.beanManager.setXMLConfigurator(this.xmlConfig);        
        ActivityManager.getInstance().setRootActivity(this.beanManager);        

    }

    @Override
    public void applicationEnded(Object endObject)
    {
        ContextFactory.destroyRequestContext(null);
        ContextFactory.destroySessionContext(this.mockHttpSession);
        ContextFactory.destroyConversationContext();
        
        super.applicationEnded(this.servletContextEvent);
        
        ContextFactory.destroyApplicationContext(this.servletContextEvent.getServletContext());        
    }

    @Override
    public void applicationStarted(Object startupObject) throws WebBeansException
    {                
        super.applicationStarted(this.servletContextEvent);
    }
        
}
