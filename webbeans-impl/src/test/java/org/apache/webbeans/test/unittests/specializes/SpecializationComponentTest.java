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
package org.apache.webbeans.test.unittests.specializes;

import javax.enterprise.inject.spi.Bean;

import org.apache.webbeans.common.TestContext;
import org.apache.webbeans.context.ContextFactory;
import org.apache.webbeans.test.annotation.binding.Mock;
import org.apache.webbeans.test.component.specializes.AsynhrounousSpecalizesService;
import org.apache.webbeans.test.component.specializes.MockSpecializesService;
import org.apache.webbeans.test.component.specializes.SpecializesServiceInjectorComponent;
import org.apache.webbeans.util.WebBeansUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class SpecializationComponentTest extends TestContext
{
    public SpecializationComponentTest()
    {
        super(SpecializationComponentTest.class.getName());
    }

    @Before
    public void init()
    {
        initDefaultDeploymentTypes();
        initializeDeploymentType(Mock.class, 3);
    }
    
    @Test
    public void testMockService()
    {
        clear();
        
        ContextFactory.initRequestContext(null);
        
        Bean<MockSpecializesService> bean1 = defineManagedBean(MockSpecializesService.class);
        Bean<AsynhrounousSpecalizesService> bean2 = defineManagedBean(AsynhrounousSpecalizesService.class);
        Bean<SpecializesServiceInjectorComponent> bean3 = defineManagedBean(SpecializesServiceInjectorComponent.class);
        
        WebBeansUtil.configureSpecializations(MockSpecializesService.class);
        
        MockSpecializesService instanceMock =getManager().getInstance(bean1);
        AsynhrounousSpecalizesService instanceOther = getManager().getInstance(bean2);
        
        Assert.assertNotNull(instanceMock);
        Assert.assertNotNull(instanceOther);
        
        SpecializesServiceInjectorComponent instance = getManager().getInstance(bean3);
        
        Assert.assertTrue(instance.getService() instanceof MockSpecializesService);
        
    }
}
