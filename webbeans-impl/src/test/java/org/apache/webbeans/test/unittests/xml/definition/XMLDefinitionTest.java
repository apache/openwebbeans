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
package org.apache.webbeans.test.unittests.xml.definition;

import junit.framework.Assert;

import org.apache.webbeans.component.AbstractOwbBean;
import org.apache.webbeans.component.xml.XMLManagedBean;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.context.ContextFactory;
import org.apache.webbeans.test.TestContext;
import org.apache.webbeans.test.xml.definition.Definition1;
import org.apache.webbeans.test.xml.definition.Definition2;
import org.apache.webbeans.test.xml.definition.TstBeanConstructor;
import org.apache.webbeans.test.xml.definition.TstBeanUnnamed;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

@SuppressWarnings("unchecked")
public class XMLDefinitionTest extends TestContext
{
    public XMLDefinitionTest()
    {
        super(XMLDefinitionTest.class.getName());
    }

    @Before
    public void init()
    {
        initDependentContext();

        // initialize the plugin. There are no plugins, but we should not get any NPEs
        WebBeansContext.getInstance().getPluginLoader().startUp();
    }

    @After
    public void shutDown()
    {
        WebBeansContext.getInstance().getPluginLoader().shutDown();
    }
    
    @Test
    public void testDefinition1()
    {
        clear();

        XMLManagedBean<?> compDef = getWebBeanFromXml("org/apache/webbeans/test/xml/definition/definition1.xml");

        Assert.assertEquals("definition1", compDef.getName());

        Object instance = compDef.create(WebBeansContext.getInstance().getCreationalContextFactory().getCreationalContext(compDef));

        Assert.assertNotNull(instance);
        Assert.assertTrue(instance instanceof Definition1);
    }

    @Test
    public void testDefinition2()
    {
        clear();

        XMLManagedBean<?> compDef = getWebBeanFromXml("org/apache/webbeans/test/xml/definition/definition2.xml");

        Object instance = compDef.create(WebBeansContext.getInstance().getCreationalContextFactory().getCreationalContext(compDef));

        Assert.assertNotNull(instance);
        Assert.assertTrue(instance instanceof Definition2);
    }

    @Test
    public void testDefinition3()
    {
//        clear();
//
//        XMLComponentImpl<?> compDef = getWebBeanFromXml("org/apache/webbeans/test/xml/definition/definition3.xml");
//
//        // we have to additionally define the PaymentProcessor and SystemConfig
//        // which would in real world parsed by the scanner
//        defineSimpleWebBean(PaymentProcessor.class);
//        defineSimpleWebBean(SystemConfig.class);
//
//        Assert.assertEquals("asyncCreditCardPaymentProcessor", compDef.getName());
//
//        Object instance = compDef.create(CreationalContextFactory.getInstance().getCreationalContext(compDef));
//
//        Assert.assertNotNull(instance);
//        Assert.assertTrue(instance instanceof MockAsynchronousCreditCardPaymentProcessor);
//
//        MockAsynchronousCreditCardPaymentProcessor ccProcessor = (MockAsynchronousCreditCardPaymentProcessor) instance;

//        SystemConfig config = ccProcessor.getConfig();
        //Assert.assertEquals("default", config.getValue());

        //PaymentProcessor paymentProcesor = ccProcessor.getPaymentProcessor();
        //Assert.assertNotNull(paymentProcesor);
    }

    @Test
    public void testWebBeanUnnamed()
    {
        clear();

        XMLManagedBean<?> compDef = getWebBeanFromXml("org/apache/webbeans/test/xml/definition/testBeanUnnamed.xml");

        // an unnamed bean must not have a name
        Assert.assertNull(compDef.getName());

        Object instance = compDef.create(WebBeansContext.getInstance().getCreationalContextFactory().getCreationalContext(compDef));
        Assert.assertNotNull(instance);
        Assert.assertTrue(instance instanceof TstBeanUnnamed);
    }

    @Test
    public void testConstructorInjection1()
    {
        //TODO AbstractInjetable has changed
//        clear();
//
//        AbstractComponent<?> compDef = getWebBeanFromXml("org/apache/webbeans/test/xml/definition/testBeanConstructor1.xml", TstBeanConstructor.class);
//
//        Object instance = compDef.create(CreationalContextFactory.getInstance().getCreationalContext(compDef));
//        Assert.assertNotNull(instance);
//        Assert.assertTrue(instance instanceof TstBeanConstructor);
//
//        TstBeanConstructor tbc = (TstBeanConstructor) instance;
//        Assert.assertEquals(4200, tbc.getVal1());
//        Assert.assertEquals(13, tbc.getVal2());
    }

    @Test
    public void testConstructorInjection2()
    {
        clear();

        ContextFactory.initRequestContext(null);

        AbstractOwbBean<?> compDef = getWebBeanFromXml("org/apache/webbeans/test/xml/definition/testBeanConstructor2.xml", TstBeanConstructor.class);

        Object instance = compDef.create(WebBeansContext.getInstance().getCreationalContextFactory().getCreationalContext(compDef));
        Assert.assertNotNull(instance);
        Assert.assertTrue(instance instanceof TstBeanConstructor);

        TstBeanConstructor tbc = (TstBeanConstructor) instance;
        Assert.assertEquals(0, tbc.getVal1());
        Assert.assertEquals(40, tbc.getVal2());
    }
}
