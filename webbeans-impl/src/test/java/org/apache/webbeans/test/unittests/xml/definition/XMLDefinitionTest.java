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
package org.apache.webbeans.test.unittests.xml.definition;

import java.io.InputStream;

import junit.framework.Assert;

import org.apache.webbeans.component.xml.XMLComponentImpl;
import org.apache.webbeans.test.servlet.TestContext;
import org.apache.webbeans.test.unittests.xml.XMLTest;
import org.apache.webbeans.test.xml.definition.Definition1;
import org.apache.webbeans.test.xml.definition.Definition2;
import org.apache.webbeans.test.xml.definition.MockAsynchronousCreditCardPaymentProcessor;
import org.apache.webbeans.test.xml.definition.PaymentProcessor;
import org.apache.webbeans.test.xml.definition.SystemConfig;
import org.apache.webbeans.test.xml.definition.TstBeanUnnamed;
import org.apache.webbeans.xml.XMLUtil;
import org.dom4j.Element;
import org.junit.Before;
import org.junit.Test;

public class XMLDefinitionTest extends TestContext
{
    public XMLDefinitionTest()
    {
        super(XMLDefinitionTest.class.getName());
    }

    @Before
    public void init()
    {
        initDeploymentTypes();
    }

    @Test
    public void testDefinition1()
    {
        XMLComponentImpl<?> compDef = getWebBeanFromXml("org/apache/webbeans/test/xml/definition/definition1.xml");

        Assert.assertEquals("definition1", compDef.getName());

        Object instance = compDef.create();

        Assert.assertNotNull(instance);
        Assert.assertTrue(instance instanceof Definition1);
    }


    @Test
    public void testDefinition2()
    {
        XMLComponentImpl<?> compDef = getWebBeanFromXml("org/apache/webbeans/test/xml/definition/definition2.xml");

        Object instance = compDef.create();

        Assert.assertNotNull(instance);
        Assert.assertTrue(instance instanceof Definition2);
    }

    @Test
    public void testDefinition3()
    {
        XMLComponentImpl<?> compDef = getWebBeanFromXml("org/apache/webbeans/test/xml/definition/definition3.xml");
        
        // we have to additionally define the PaymentProcessor and SystemConfig
        // which would in real world parsed by the scanner 
        defineSimpleWebBean(PaymentProcessor.class);
        defineSimpleWebBean(SystemConfig.class);
        
        Assert.assertEquals("asyncCreditCardPaymentProcessor", compDef.getName());
        
        Object instance = compDef.create();

        Assert.assertNotNull(instance);
        Assert.assertTrue(instance instanceof MockAsynchronousCreditCardPaymentProcessor);
        
        MockAsynchronousCreditCardPaymentProcessor ccProcessor = (MockAsynchronousCreditCardPaymentProcessor) instance;
        
        SystemConfig config = ccProcessor.getConfig();
        Assert.assertEquals("default", config.getValue());
        
        PaymentProcessor paymentProcesor = ccProcessor.getPaymentProcessor();
        Assert.assertNotNull(paymentProcesor);
    }

    @Test
    public void testWebBeanUnnamed()
    {
        XMLComponentImpl<?> compDef = getWebBeanFromXml("org/apache/webbeans/test/xml/definition/testBeanUnnamed.xml");
        
        // an unnamed bean must not have a name 
        Assert.assertNull(compDef.getName());
        
        Object instance = compDef.create();
        Assert.assertNotNull(instance);
        Assert.assertTrue(instance instanceof TstBeanUnnamed);
    }

    
    /**
     * Private helper function which loads a WebBean definition from the given xmlResourcePath.
     * This will first do a Class lookup and take his annotations as a base, later overlaying
     * it with the definitions from the given XML.
     *  
     * @param xmlResourcePath
     * @return XMLComponentImpl<?> with the WebBean definition 
     */
    private XMLComponentImpl<?> getWebBeanFromXml(String xmlResourcePath)
    {
        InputStream stream = XMLTest.class.getClassLoader().getResourceAsStream(xmlResourcePath);
        Assert.assertNotNull(stream);

        clear();

        Element rootElement = XMLUtil.getRootElement(stream);
        Element beanElement = (Element) rootElement.elements().get(0);

        Class<?> clazz = XMLUtil.getElementJavaType(beanElement);

        defineXMLSimpleWebBeans(clazz, beanElement);

        Assert.assertEquals(1, getComponents().size());
        Object def = getComponents().get(0);
        
        Assert.assertNotNull(def);
        
        Assert.assertTrue(def instanceof XMLComponentImpl);

        return (XMLComponentImpl<?>) def;
    }

}
