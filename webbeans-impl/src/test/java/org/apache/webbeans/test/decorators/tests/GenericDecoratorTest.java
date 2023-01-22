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
package org.apache.webbeans.test.decorators.tests;

import java.util.ArrayList;
import java.util.Collection;

import jakarta.enterprise.inject.spi.DefinitionException;

import org.junit.Assert;

import org.apache.webbeans.test.AbstractUnitTest;
import org.apache.webbeans.test.decorators.common.Garphly;
import org.apache.webbeans.test.decorators.common.GarphlyDecorator;
import org.apache.webbeans.test.decorators.generic.DecoratedBean;
import org.apache.webbeans.test.decorators.generic.ExtendedDecoratedBean;
import org.apache.webbeans.test.decorators.generic.ExtendedGenericInterface;
import org.apache.webbeans.test.decorators.generic.ExtendedSampleDecorator;
import org.apache.webbeans.test.decorators.generic.GenericInterface;
import org.apache.webbeans.test.decorators.generic.SampleDecorator;
import org.junit.Test;

public class GenericDecoratorTest extends AbstractUnitTest
{
    public static final String PACKAGE_NAME = GenericDecoratorTest.class.getPackage().getName();
    
    
    @Test
    public void testGenericDecorator()
    {
        
        Collection<Class<?>> classes = new ArrayList<Class<?>>();
        classes.add(Garphly.class);
        classes.add(GarphlyDecorator.class);
        
        Collection<String> xmls = new ArrayList<String>();
        xmls.add(getXmlPath(PACKAGE_NAME, "GenericDecoratorTest"));

        try
        {
            startContainer(classes, xmls);
            Assert.fail("DefinitionException expected");
        }
        catch (DefinitionException e)
        {
            // expected, all ok
            shutDownContainer();
        }
    }

    @Test
    public void injection() throws Exception {
        Collection<Class<?>> classes = new ArrayList<Class<?>>();
        classes.add(DecoratedBean.class);
        classes.add(GenericInterface.class);
        classes.add(SampleDecorator.class);

        Collection<String> xmls = new ArrayList<String>();
        xmls.add(getXmlPath(PACKAGE_NAME, "GenericDecoratorTest"));

        startContainer(classes, xmls);

        DecoratedBean decoratedBean = getInstance(DecoratedBean.class);
        Assert.assertTrue(decoratedBean.isDecoratorCalled());
    }
    
    @Test
    public void extendedInjection() throws Exception {
        Collection<Class<?>> classes = new ArrayList<Class<?>>();
        classes.add(ExtendedDecoratedBean.class);
        classes.add(ExtendedGenericInterface.class);
        classes.add(ExtendedSampleDecorator.class);

        Collection<String> xmls = new ArrayList<String>();
        xmls.add(getXmlPath(PACKAGE_NAME, "GenericDecoratorTest"));

        startContainer(classes, xmls);

        ExtendedDecoratedBean decoratedBean = getInstance(ExtendedDecoratedBean.class);
        Assert.assertTrue(decoratedBean.isDecoratorCalled());
    }
}
