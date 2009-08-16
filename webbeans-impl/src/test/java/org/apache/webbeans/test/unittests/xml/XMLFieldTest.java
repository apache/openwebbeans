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
package org.apache.webbeans.test.unittests.xml;

import java.io.InputStream;

import javax.enterprise.inject.spi.BeanManager;

import junit.framework.Assert;

import org.apache.webbeans.test.servlet.TestContext;
import org.apache.webbeans.test.xml.ComponentForField;
import org.junit.Before;
import org.junit.Test;

public class XMLFieldTest extends TestContext
{
    BeanManager container = null;

    public XMLFieldTest()
    {
        super(XMLFieldTest.class.getSimpleName());
    }

    @Before
    public void init()
    {
        super.init();
    }

    @Test
    public void nameSpacesNotDeclared()
    {
        InputStream stream = XMLFieldTest.class.getClassLoader().getResourceAsStream("org/apache/webbeans/test/xml/fieldTest.xml");
        Assert.assertNotNull(stream);

        /* Clear the manager component list */
        clear();

        this.xmlConfigurator.configureOwbSpecific(stream, "fieldTest.xml");

        ComponentForField cff = (ComponentForField) getManager().getInstanceByName("componentForField");

        Assert.assertNotNull(cff);
        Assert.assertEquals(35, cff.getIntField());
        Assert.assertEquals(35.3f, cff.getFloatField());
        Assert.assertEquals(35.5d, cff.getDoubleField());
        Assert.assertEquals('a', cff.getCharField());
        Assert.assertEquals(37, cff.getLongField());
        Assert.assertEquals(1, cff.getByteField());
        Assert.assertEquals(5, cff.getShortField());
        Assert.assertEquals(true, cff.isBooleanField());
        Assert.assertEquals("ENUM1", cff.getEnum1().name());
        Assert.assertEquals("dskfj", cff.getStrField());
        Assert.assertNotNull(cff.getDateField());
        Assert.assertNotNull(cff.getCalendarField());
        Assert.assertEquals(ComponentForField.class, cff.getClazzField());
        Assert.assertNotNull(cff.getListStrField());
        Assert.assertNotNull(cff.getListEnumField());
    }

}
