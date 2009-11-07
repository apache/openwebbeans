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

import javax.enterprise.inject.spi.BeanManager;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.apache.webbeans.common.TestContext;
import org.apache.webbeans.component.AbstractBean;
import org.apache.webbeans.container.BeanManagerImpl;
import org.apache.webbeans.context.ContextFactory;
import org.apache.webbeans.test.component.resource.TstResourcePersistenceBean;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ResourceInjectionTest extends TestContext
{
    BeanManager container = null;

    public ResourceInjectionTest()
    {
        super(ResourceInjectionTest.class.getSimpleName());
    }
    
    @Before
    public void init()
    {
        super.init();
        this.container = BeanManagerImpl.getManager();
        clear();
    }

    @Test
    public void testPersistenceContextInjection() throws Exception
    {
        AbstractBean<?> tstComponent = defineManagedBean(TstResourcePersistenceBean.class);
        Assert.assertNotNull(tstComponent);
        
        ContextFactory.initRequestContext(null);
        ContextFactory.initApplicationContext(null);
        
//        TstResourcePersistenceBean persBean = (TstResourcePersistenceBean) getInstanceByName("tstResourcePersistenceBean");
//        Assert.assertNotNull(persBean);
//        
//        // test field injection
//        EntityManagerFactory emf = persBean.getFieldInjectedEntityManagerFactory();
//        Assert.assertNotNull(emf);
//
//        EntityManager em = persBean.getFieldInjectedEntityManager();
//        Assert.assertNotNull(em);
//    
//        EntityManager em2 = persBean.getFieldInjectedEntityManager2();
//        Assert.assertNotNull(em2);
//
//        Assert.assertTrue(em != em2);
//Spec seems does not support this type of injection, See SECTION 3.5 and See SECTION 5 First Paragraph
//        // test method injection
//        EntityManagerFactory emf2 = persBean.getMethodInjectedEntityManagerFactory();
//        Assert.assertNotNull(emf2);
//        
//        EntityManager em3 = persBean.getMethodInjectedEntityManager();
//        Assert.assertNotNull(em3);
    }
}
