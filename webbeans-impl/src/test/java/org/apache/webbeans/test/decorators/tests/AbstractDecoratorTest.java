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

import org.junit.Assert;

import org.apache.webbeans.test.AbstractUnitTest;
import org.apache.webbeans.test.decorators.multiple.Decorator1;
import org.apache.webbeans.test.decorators.multiple.Decorator2;
import org.apache.webbeans.test.decorators.multiple.Decorator3;
import org.apache.webbeans.test.decorators.multiple.Decorator4;
import org.apache.webbeans.test.decorators.multiple.IOutputProvider;
import org.apache.webbeans.test.decorators.multiple.OutputProvider;
import org.apache.webbeans.test.decorators.multiple.RequestStringBuilder;
import org.junit.Test;

public class AbstractDecoratorTest extends AbstractUnitTest
{
    public static final String PACKAGE_NAME = MultipleDecoratorStackTests.class.getPackage().getName();

    @Test
    public void testDecoratorStackWithAbstractAtEnd()
    {
        Collection<Class<?>> classes = new ArrayList<Class<?>>();
        classes.add(Decorator1.class);
        classes.add(Decorator2.class);
        
        //Abstract Decorator with a single method, at the end of the execution order
        classes.add(Decorator3.class);
        
        //Abstract Decorator with no methods, in the middle of the execution order
        classes.add(Decorator4.class);
        
        classes.add(IOutputProvider.class);
        classes.add(OutputProvider.class);
        classes.add(RequestStringBuilder.class);

        Collection<String> xmls = new ArrayList<String>();
        xmls.add(getXmlPath(PACKAGE_NAME, "AbstractDecoratorTest"));

        startContainer(classes, xmls);

        OutputProvider outputProvider = getInstance(OutputProvider.class);
        Assert.assertTrue(outputProvider != null);

        String result = outputProvider.getOutput();
        // Verify that the Decorators were called in order, and in a stack, including the Abstract Decorator 3
        Assert.assertTrue(result.equalsIgnoreCase("Decorator1\nDecorator2\nDecorator3\nOutputProvider\n"));
        
        String hijackedStack = outputProvider.trace();
        // Verify that the a method change in Decorator2 from trace->otherMethod results in the right stack
        Assert.assertEquals("Decorator1/trace,Decorator2/trace,delegate/otherMethod", hijackedStack);   
    }

    @Test
    public void testPureAbstractDecorator() throws Exception
    {
        addDecorator(Decorator3.class);
        startContainer(Decorator3.class, IOutputProvider.class, OutputProvider.class, RequestStringBuilder.class);

        OutputProvider outputProvider = getInstance(OutputProvider.class);
        Assert.assertTrue(outputProvider != null);

        String result = outputProvider.getOutput();
        Assert.assertEquals("Decorator3\nOutputProvider\n", result);

        String trace = outputProvider.trace();
        Assert.assertEquals("delegate/trace", trace);
    }
}
