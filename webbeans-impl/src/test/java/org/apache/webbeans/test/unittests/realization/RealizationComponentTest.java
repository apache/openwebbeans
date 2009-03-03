/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.webbeans.test.unittests.realization;

import javax.inject.manager.Bean;

import org.apache.webbeans.context.ContextFactory;
import org.apache.webbeans.test.component.UserComponent;
import org.apache.webbeans.test.component.library.BookShop;
import org.apache.webbeans.test.component.realization.RealizationComponent1;
import org.apache.webbeans.test.component.realization.RealizationProducerFieldInjecterComponent;
import org.apache.webbeans.test.mock.MockHttpSession;
import org.apache.webbeans.test.servlet.TestContext;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class RealizationComponentTest extends TestContext
{
    public RealizationComponentTest()
    {
        super(RealizationComponentTest.class.getName());
    }
    
    @Before
    public void init()
    {
        initDefaultDeploymentTypes();
    }

    @Test
    public void testRealizationComponent1()
    {
        ContextFactory.initRequestContext(null);
        ContextFactory.initSessionContext(new MockHttpSession());
        
        Bean<UserComponent> userComponent = defineSimpleWebBean(UserComponent.class);
        defineSimpleWebBean(BookShop.class);
        
        UserComponent component = getManager().getInstance(userComponent);
        
        Assert.assertNotNull(component);
        
        Bean<RealizationComponent1> bean = defineSimpleWebBean(RealizationComponent1.class);
        RealizationComponent1 comp = getManager().getInstance(bean);
        
        Assert.assertNotNull(comp);        
        Assert.assertEquals(null, comp.getUserComponent());
        
        comp.setUserComponent(component);
        
        Bean<RealizationProducerFieldInjecterComponent> inejtecer = defineSimpleWebBean(RealizationProducerFieldInjecterComponent.class);
        RealizationProducerFieldInjecterComponent instance = getManager().getInstance(inejtecer);
        
        //Assert.assertSame(component, instance.getComponent());
    }
}
