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
package org.apache.webbeans.test.unittests.newcomp;

import java.util.List;

import javax.context.RequestScoped;
import javax.servlet.ServletContext;

import junit.framework.Assert;

import org.apache.webbeans.component.AbstractComponent;
import org.apache.webbeans.context.ContextFactory;
import org.apache.webbeans.test.component.CheckWithCheckPayment;
import org.apache.webbeans.test.component.IPayment;
import org.apache.webbeans.test.component.dependent.DependentComponent;
import org.apache.webbeans.test.component.dependent.DependentOwnerComponent;
import org.apache.webbeans.test.component.newcomp.NewComponent;
import org.apache.webbeans.test.component.newcomp.ProducerNewComponent;
import org.apache.webbeans.test.servlet.TestContext;
import org.junit.Before;
import org.junit.Test;

public class NewComponentTest extends TestContext
{
    public NewComponentTest()
    {
        super(NewComponentTest.class.getName());
    }

    public void endTests(ServletContext ctx)
    {
        // TODO Auto-generated method stub

    }

    @Before
    public void init()
    {
        super.init();

    }

    public void startTests(ServletContext ctx)
    {
        // TODO Auto-generated method stub

    }

    @Test
    public void testDependent()
    {
        clear();

        defineSimpleWebBean(DependentComponent.class);
        defineSimpleWebBean(DependentOwnerComponent.class);
        defineSimpleWebBean(NewComponent.class);

        ContextFactory.initRequestContext(null);

        List<AbstractComponent<?>> comps = getComponents();

        Assert.assertEquals(3, comps.size());

        NewComponent comp = (NewComponent) getContext(RequestScoped.class).get(comps.get(2), true);

        DependentOwnerComponent own = comp.owner();

        Assert.assertNotNull(own);

        ContextFactory.destroyRequestContext(null);
    }

    @Test
    public void testDepedent2()
    {
        clear();
        defineSimpleWebBean(CheckWithCheckPayment.class);
        defineSimpleWebBean(ProducerNewComponent.class);

        ContextFactory.initRequestContext(null);
        Assert.assertEquals(3, getDeployedComponents());

        IPayment payment = (IPayment) getManager().getInstanceByName("paymentProducer");
        Assert.assertNotNull(payment);

        IPayment payment2 = (IPayment) getManager().getInstanceByName("paymentProducer");

        Assert.assertNotSame(payment, payment2);
    }
}
