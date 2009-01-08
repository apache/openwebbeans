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
        Throwable e = null;
        try
        {
            InputStream stream = XMLTest.class.getClassLoader().getResourceAsStream("org/apache/webbeans/test/xml/definition/definition1.xml");
            Assert.assertNotNull(stream);

            clear();

            Element rootElement = XMLUtil.getRootElement(stream);
            Element beanElement = (Element) rootElement.elements().get(0);

            Class<?> clazz = XMLUtil.getElementJavaType(beanElement);

            defineXMLSimpleWebBeans(clazz, beanElement);

            Assert.assertEquals(1, getComponents().size());
            Object def = getComponents().get(0);

            Assert.assertTrue(def instanceof XMLComponentImpl);

            XMLComponentImpl<?> comp = (XMLComponentImpl<?>) def;

            Assert.assertEquals("definition1", comp.getName());

            Object instance = comp.create();

            Assert.assertNotNull(instance);
            Assert.assertTrue(instance instanceof Definition1);

        } catch (Throwable e1)
        {
            e1.printStackTrace();
            e = e1;
        }

        Assert.assertNull(e);

    }

    @Test
    public void testDefinition2()
    {
        Throwable e = null;
        try
        {
            InputStream stream = XMLTest.class.getClassLoader().getResourceAsStream("org/apache/webbeans/test/xml/definition/definition2.xml");
            Assert.assertNotNull(stream);

            clear();

            Element rootElement = XMLUtil.getRootElement(stream);
            Element beanElement = (Element) rootElement.elements().get(0);

            Class<?> clazz = XMLUtil.getElementJavaType(beanElement);

            defineXMLSimpleWebBeans(clazz, beanElement);

        } catch (Throwable e1)
        {
            e1.printStackTrace();
            e = e1;
        }

        Assert.assertNull(e);

    }

    @Test
    public void testDefinition3()
    {
        Throwable e = null;
        try
        {
            InputStream stream = XMLTest.class.getClassLoader().getResourceAsStream("org/apache/webbeans/test/xml/definition/definition3.xml");
            Assert.assertNotNull(stream);

            clear();

            Element rootElement = XMLUtil.getRootElement(stream);
            Element beanElement = (Element) rootElement.elements().get(0);

            Class<?> clazz = XMLUtil.getElementJavaType(beanElement);

            defineXMLSimpleWebBeans(clazz, beanElement);

        } catch (Throwable e1)
        {
            e1.printStackTrace();
            e = e1;
        }

        Assert.assertNull(e);
    }

}
