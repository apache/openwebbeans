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
package org.apache.webbeans.el.test;

import jakarta.el.ELContext;
import jakarta.el.ELResolver;
import jakarta.el.FunctionMapper;
import jakarta.el.ValueExpression;
import jakarta.el.VariableMapper;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Named;
import org.apache.el.ExpressionFactoryImpl;
import org.apache.webbeans.el22.WrappedExpressionFactory;
import org.apache.webbeans.el22.WrappedValueExpressionNode;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;

public class DotNamedBeansTest extends AbstractUnitTest
{

    @Test
    public void testNonResolved() throws Exception
    {
        Collection<Class<?>> classes = new ArrayList<Class<?>>();

        classes.add(GoldenFish.class);

        startContainer(classes);
        final Object node = getBeanManager().getELResolver().getValue(new MockELContext(), null, "bla");
        Assert.assertNull(node);

        shutDownContainer();
    }

    @Test
    public void testIntermediateNode() throws Exception
    {
        Collection<Class<?>> classes = new ArrayList<Class<?>>();

        classes.add(GoldenFish.class);

        startContainer(classes);
        final Object node = getBeanManager().getELResolver().getValue(new MockELContext(), null, "magic");
        Assert.assertNotNull(node);
        Assert.assertTrue(node instanceof WrappedValueExpressionNode);

        shutDownContainer();
    }

    @Test
    public void testLeaf() throws Exception
    {
        Collection<Class<?>> classes = new ArrayList<Class<?>>();

        classes.add(GoldenFish.class);

        startContainer(classes);
        final GoldenFish goldenFish = evaluateValueExpression(getBeanManager(), "#{magic.golden.fish}", GoldenFish.class);
        Assert.assertNotNull(goldenFish);

        shutDownContainer();
    }

    @Test(expected = WebBeansConfigurationException.class)
    public void testConflictingName() throws Exception
    {
        Collection<Class<?>> classes = new ArrayList<Class<?>>();

        classes.add(GoldenFish.class);
        classes.add(ConflictingGoldenFish.class);

        startContainer(classes);
    }

    @Test
    public void testSameBeginning() throws Exception
    {
        Collection<Class<?>> classes = new ArrayList<Class<?>>();

        classes.add(GoldenFish.class);
        classes.add(BlueGoldenFish.class);

        startContainer(classes);

        Assert.assertNotNull(evaluateValueExpression(getBeanManager(), "#{magic.golden.fish}", GoldenFish.class));
        Assert.assertNotNull(evaluateValueExpression(getBeanManager(), "#{magic.blue}", BlueGoldenFish.class));

        shutDownContainer();
    }

    public static <T> T evaluateValueExpression(final BeanManager beanManager, final String expression, final Class<T> expectedType)
    {
        final WrappedExpressionFactory wrappedExpressionFactory = new WrappedExpressionFactory(new ExpressionFactoryImpl());
        final ELContext elContext = new ELContext() {

            @Override
            public ELResolver getELResolver() {
                return beanManager.getELResolver();
            }

            @Override
            public FunctionMapper getFunctionMapper() {
                return null;
            }

            @Override
            public VariableMapper getVariableMapper() {
                return null;
            }
        };
        final ValueExpression valueExpression = wrappedExpressionFactory.createValueExpression(elContext, expression, expectedType);
        final Object object = valueExpression.getValue(elContext);

        return (T)object;
    }

    @Named("magic.golden.fish")
    @Dependent
    public static class GoldenFish {
    }

    @Named("magic.golden")
    @Dependent
    public static class ConflictingGoldenFish {
    }

    @Named("magic.blue")
    @Dependent
    public static class BlueGoldenFish {
    }
}
