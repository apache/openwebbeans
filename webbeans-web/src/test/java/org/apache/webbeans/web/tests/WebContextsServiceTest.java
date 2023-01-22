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
package org.apache.webbeans.web.tests;

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.spi.ContextsService;
import org.apache.webbeans.web.context.WebContextsService;
import org.junit.Test;

import jakarta.enterprise.context.ContextNotActiveException;
import jakarta.enterprise.context.SessionScoped;
import java.util.HashMap;
import java.util.Properties;

import static org.junit.Assert.fail;

public class WebContextsServiceTest
{
    /**
     * Without the fix it was failing this way:
     *
     * java.lang.NullPointerException
     * at org.apache.webbeans.web.context.WebContextsService.lazyStartSessionContext(WebContextsService.java:816)
     */
    @Test
    public void issue_OWB1124()
    {
        final WebBeansContext webBeansContext = new WebBeansContext(new HashMap<Class<?>, Object>(), new Properties()
        {{
            setProperty(ContextsService.class.getName(), WebContextsService.class.getName());
        }});

        try
        {
            webBeansContext.getBeanManagerImpl().getContext(SessionScoped.class);
            fail();
        }
        catch (final ContextNotActiveException cnae)
        {
            // ok
        }
    }
}
