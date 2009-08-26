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
package org.apache.webbeans.test.unittests.portable.events;

import javax.enterprise.inject.spi.Bean;

import junit.framework.Assert;

import org.apache.webbeans.annotation.CurrentLiteral;
import org.apache.webbeans.context.ContextFactory;
import org.apache.webbeans.portable.events.ExtensionLoader;
import org.apache.webbeans.test.component.library.BookShop;
import org.apache.webbeans.test.component.portable.events.MyExtension;
import org.apache.webbeans.test.mock.MockServletContext;
import org.apache.webbeans.test.servlet.TestContext;
import org.junit.Before;
import org.junit.Test;

public class ExtensionTest extends TestContext
{
    public ExtensionTest()
    {
        super(ExtensionTest.class.getName());
    }

    @Before
    public void init()
    {
        super.init();
    }
    
    @Test
    @SuppressWarnings("unchecked")
    public void testExtensionServices()
    {
        ExtensionLoader.getInstance().loadExtensionServices();
        
        MockServletContext servletContext = new MockServletContext();
        ContextFactory.initApplicationContext(servletContext);
        Bean<MyExtension> extension = (Bean<MyExtension>)getManager().resolveByType(MyExtension.class, new CurrentLiteral()).iterator().next();
        
        MyExtension ext = getManager().getInstance(extension);
        System.out.println(ext.toString());
        
        defineSimpleWebBean(BookShop.class);
        
        Assert.assertNotNull(MyExtension.event);
        
        ContextFactory.destroyApplicationContext(servletContext);
    }
}
