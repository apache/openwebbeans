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
package org.apache.webbeans.newtests.decorators.tests;

import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.newtests.AbstractUnitTest;
import org.apache.webbeans.newtests.decorators.multiple.Decorator1;
import org.apache.webbeans.newtests.decorators.multiple.OutputProvider;
import org.apache.webbeans.newtests.decorators.multiple.RequestStringBuilder;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;

public class DecoratorConfigurationTest extends AbstractUnitTest
{
    public static final String PACKAGE_NAME = DecoratorConfigurationTest.class.getPackage().getName();

    @Test(expected = WebBeansConfigurationException.class)
    public void testMultipleDecoratorsInSameFile()
    {
        Collection<String> beanXmls = new ArrayList<String>();
        beanXmls.add(getXmlPath(PACKAGE_NAME, "SameDecorator_broken"));

        Collection<Class<?>> beanClasses = new ArrayList<Class<?>>();
        beanClasses.add(Decorator1.class);
        beanClasses.add(OutputProvider.class);

        startContainer(beanClasses, beanXmls, true);
        Assert.fail("should have thrown a deployment error");
    }

    @Test
    public void testMultipleDecoratorsInMultipleFiles()
    {
        Collection<String> beanXmls = new ArrayList<String>();
        beanXmls.add(getXmlPath(PACKAGE_NAME, "SimpleDecorator_1"));
        beanXmls.add(getXmlPath(PACKAGE_NAME, "SimpleDecorator_2"));

        Collection<Class<?>> beanClasses = new ArrayList<Class<?>>();
        beanClasses.add(Decorator1.class);
        beanClasses.add(OutputProvider.class);
        beanClasses.add(RequestStringBuilder.class);

        startContainer(beanClasses, beanXmls, true);

        OutputProvider op = getInstance(OutputProvider.class);
        Assert.assertNotNull(op);
        op.getOutput();

    }
}

