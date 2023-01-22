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

import org.apache.webbeans.test.AbstractUnitTest;
import org.apache.webbeans.test.decorators.multiple.IOutputProvider;
import org.apache.webbeans.test.decorators.multiple.MultipleCallDecorator;
import org.apache.webbeans.test.decorators.multiple.OutputProvider;
import org.apache.webbeans.test.decorators.multiple.RequestStringBuilder;
import org.junit.Test;

import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.util.AnnotationLiteral;
import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.assertTrue;

public class MultipleCallDecoratorTest extends AbstractUnitTest
{
    public static final String PACKAGE_NAME = MultipleDecoratorStackTests.class.getPackage().getName();

    @Test
    // this test is just a standard decorator calling a bunch of method
    // it tests we don't have recusive issues in DelegateHandler
    public void testDecoratorStackWithAbstractAtEnd()
    {
        final Collection<Class<?>> classes = new ArrayList<Class<?>>();
        classes.add(MultipleCallDecorator.class);
        classes.add(IOutputProvider.class);
        classes.add(OutputProvider.class);
        classes.add(RequestStringBuilder.class);

        final Collection<String> xmls = new ArrayList<String>();
        xmls.add(getXmlPath(PACKAGE_NAME, "MultipleCallDecoratorTest"));

        startContainer(classes, xmls);

        final Bean<?> bean = getBeanManager().getBeans(OutputProvider.class, new AnnotationLiteral<Default>()
        {
        }).iterator().next();

        final IOutputProvider instance = (IOutputProvider) getBeanManager().getReference(bean, IOutputProvider.class, getBeanManager().createCreationalContext(bean));
        final String expected = "delegate/traceorg.apache.webbeans.test.decorators.multiple.OutputProvider@.*delegate/trace";
        final String actual = instance.trace();
        assertTrue("'" + actual + "' doesn't match with '" + expected + "'", actual.matches(expected));
    }
}