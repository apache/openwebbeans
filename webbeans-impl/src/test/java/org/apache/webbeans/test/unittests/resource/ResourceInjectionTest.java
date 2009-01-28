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
package org.apache.webbeans.test.unittests.resource;

import javax.inject.manager.Manager;
import javax.persistence.EntityManagerFactory;

import org.apache.webbeans.component.AbstractComponent;
import org.apache.webbeans.container.ManagerImpl;
import org.apache.webbeans.context.ContextFactory;
import org.apache.webbeans.test.component.resource.TstResourcePersistenceBean;
import org.apache.webbeans.test.servlet.TestContext;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ResourceInjectionTest extends TestContext
{
    Manager container = null;

    public ResourceInjectionTest()
    {
        super(ResourceInjectionTest.class.getSimpleName());
    }
    
    @Before
    public void init()
    {
        super.init();
        this.container = ManagerImpl.getManager();
        clear();
    }

    @Test
    public void testPersistenceContextInjection() throws Exception
    {
        AbstractComponent<?> tstComponent = defineSimpleWebBean(TstResourcePersistenceBean.class);
        Assert.assertNotNull(tstComponent);
        
        ContextFactory.initRequestContext(null);
        ContextFactory.initApplicationContext(null);
        
        TstResourcePersistenceBean persBean = (TstResourcePersistenceBean) getInstanceByName("tstResourcePersistenceBean");
        Assert.assertNotNull(persBean);
        
        EntityManagerFactory emf = persBean.getEntityManagerFactory();
        Assert.assertNotNull(emf);
     }
}
