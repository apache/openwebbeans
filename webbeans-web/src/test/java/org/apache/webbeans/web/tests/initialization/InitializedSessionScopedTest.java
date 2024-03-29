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
package org.apache.webbeans.web.tests.initialization;

import java.util.ArrayList;
import java.util.Collection;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.context.SessionScoped;
import jakarta.servlet.ServletRequestEvent;
import org.junit.Assert;
import org.apache.webbeans.test.AbstractUnitTest;
import org.apache.webbeans.web.lifecycle.test.MockServletContext;
import org.apache.webbeans.web.tests.MockHttpSession;
import org.apache.webbeans.web.tests.MockServletRequest;
import org.junit.Test;

public class InitializedSessionScopedTest extends AbstractUnitTest
{
    @Test
    public void testse() throws Exception
    {
        Collection<Class<?>> beanClasses = new ArrayList<Class<?>>();
        beanClasses.add(MySession.class);
        beanClasses.add(MySessionHandler.class);
        
        startContainer(beanClasses, null);
        
        final MockServletContext mockServletContext = new MockServletContext();
        final MockServletRequest mockServletRequest = new MockServletRequest();
        final ServletRequestEvent servletRequestEvent = new ServletRequestEvent(mockServletContext, mockServletRequest);

        MockHttpSession mockSession = new MockHttpSession();
        
        getWebBeansContext().getContextsService().startContext(RequestScoped.class, servletRequestEvent);
        
        MySessionHandler mySessionHandler = getInstance(MySessionHandler.class);
        Assert.assertFalse(mySessionHandler.isInitialized());
        
        getWebBeansContext().getContextsService().startContext(SessionScoped.class, mockSession);
        
        getWebBeansContext().getContextsService().endContext(SessionScoped.class, mockSession);
        
        Assert.assertTrue(mySessionHandler.isInitialized());
        
        getWebBeansContext().getContextsService().endContext(RequestScoped.class, servletRequestEvent);
        
        shutDownContainer();
    }
}
