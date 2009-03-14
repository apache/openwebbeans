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
package org.apache.webbeans.test.unittests.inject;


import javax.inject.Instance;
import javax.inject.manager.Bean;

import org.apache.webbeans.context.ContextFactory;
import org.apache.webbeans.test.component.PaymentProcessorComponent;
import org.apache.webbeans.test.component.inject.InstanceInjectedComponent;
import org.apache.webbeans.test.servlet.TestContext;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class InjectedInstanceComponentTest extends TestContext
{
    public InjectedInstanceComponentTest()
    {
        super(InjectedInstanceComponentTest.class.getName());
    }

    @Before
    public void init()
    {
        initDefaultDeploymentTypes();
    }
    
    @Test
    public void testInstanceInjectedComponent()
    {
        ContextFactory.initRequestContext(null);
        Bean<PaymentProcessorComponent> bean =  defineSimpleWebBean(PaymentProcessorComponent.class);
        
        getManager().getInstance(bean);
        
        Bean<InstanceInjectedComponent> injectedBean = defineSimpleWebBean(InstanceInjectedComponent.class);
                
        InstanceInjectedComponent instance = getManager().getInstance(injectedBean);
        
        Assert.assertNotNull(instance);
        Assert.assertNotNull(instance.getInstance());
        Assert.assertNotNull(instance.getPaymentComponent());
        
        Instance<PaymentProcessorComponent> ins = instance.getInstance();
        
        System.out.println(ins.toString());
        
    }
}
