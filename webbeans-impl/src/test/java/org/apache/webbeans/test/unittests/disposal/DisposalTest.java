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
package org.apache.webbeans.test.unittests.disposal;

import javax.persistence.EntityManager;

import org.apache.webbeans.context.ContextFactory;
import org.apache.webbeans.test.component.disposal.Disposal1;
import org.apache.webbeans.test.mock.MockEntityManager;
import org.apache.webbeans.test.mock.MockHttpSession;
import org.apache.webbeans.test.servlet.TestContext;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class DisposalTest extends TestContext
{

    public DisposalTest()
    {
        super(DisposalTest.class.getName());
    }
    
    
    @Before
    public void init()
    {
        initDefaultDeploymentTypes();
    }
    
    
    @Test
    public void testDisposal1()
    {
        clear();
        
        ContextFactory.initRequestContext(null);
        ContextFactory.initSessionContext(new MockHttpSession());
        
        defineSimpleWebBean(Disposal1.class);
        
        EntityManager em = (EntityManager) getManager().getInstanceByName("createEntityManager");
        em.clear();
        Assert.assertNotNull(em);
        
        ContextFactory.destroyRequestContext(null);
        
        Assert.assertTrue(Disposal1.disposeCall);
        
        
    }
}
