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

import jakarta.el.ArrayELResolver;
import jakarta.el.BeanELResolver;
import jakarta.el.CompositeELResolver;
import jakarta.el.ELContext;
import jakarta.el.ELResolver;
import jakarta.el.FunctionMapper;
import jakarta.el.ListELResolver;
import jakarta.el.MapELResolver;
import jakarta.el.PropertyNotFoundException;
import jakarta.el.StaticFieldELResolver;
import jakarta.el.ValueExpression;
import jakarta.el.VariableMapper;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Named;
import org.apache.el.ExpressionFactoryImpl;
import org.apache.el.lang.FunctionMapperImpl;
import org.apache.el.lang.VariableMapperImpl;
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
        classes.add(BlueGoldenFish.class);

        startContainer(classes);
        final Object node = getBeanManager().getELResolver().getValue(new MockELContext(), null, "magic");
        Assert.assertNotNull(node);
        Assert.assertTrue(node instanceof WrappedValueExpressionNode);

        shutDownContainer();
    }

    @Test
    public void testUniqueIntermediateNode() throws Exception
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

    @Test
    public void testNonDottedChildren() throws Exception
    {
        Collection<Class<?>> classes = new ArrayList<Class<?>>();

        classes.add(Dogs.class);
        classes.add(Dog.class);
        classes.add(BlueGoldenFish.class);
        classes.add(GoldenFish.class);

        startContainer(classes);

        Dog dog = evaluateValueExpression(getBeanManager(), "#{dog}", Dog.class);
        dog.setParent(new Parent());
        dog.getParent().setName("Hans");

        String parentName = evaluateValueExpression(getBeanManager(), "#{dog.parent.name}", String.class);
        Assert.assertEquals("Hans", parentName);

        shutDownContainer();
    }

    @Test
    public void testChildren() throws Exception
    {
        Collection<Class<?>> classes = new ArrayList<Class<?>>();

        classes.add(Dog.class);
        classes.add(BlueGoldenFish.class);
        classes.add(GoldenFish.class);

        startContainer(classes);

        GoldenFish goldenFish = evaluateValueExpression(getBeanManager(), "#{magic.golden.fish}", GoldenFish.class);
        goldenFish.setParent(new Parent());
        goldenFish.getParent().setName("Hans");

        String parentName = evaluateValueExpression(getBeanManager(), "#{magic.golden.fish.parent.name}", String.class);
        Assert.assertEquals("Hans", parentName);

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

    @Test
    public void testNoMatch() throws Exception
    {
        Collection<Class<?>> classes = new ArrayList<Class<?>>();

        classes.add(Dog.class);

        startContainer(classes);

        Assert.assertThrows(PropertyNotFoundException.class,
                            () -> evaluateValueExpression(getBeanManager(), "#{magic.golden.fish}", GoldenFish.class));

        shutDownContainer();
    }

    public static <T> T evaluateValueExpression(final BeanManager beanManager, final String expression, final Class<T> expectedType)
    {
        final WrappedExpressionFactory wrappedExpressionFactory = new WrappedExpressionFactory(new ExpressionFactoryImpl());
        final ELContext elContext = new ELContext() {

            private CompositeELResolver elResolver;
            private FunctionMapper functionMapper;
            private VariableMapper variableMapper;

            @Override
            public ELResolver getELResolver() {
                if (elResolver == null)
                {
                    elResolver = new CompositeELResolver();
                    elResolver.add(beanManager.getELResolver());
                    elResolver.add(new StaticFieldELResolver());
                    elResolver.add(new MapELResolver());
                    elResolver.add(new ListELResolver());
                    elResolver.add(new ArrayELResolver());
                    elResolver.add(new BeanELResolver());
                }
                return elResolver;
            }

            @Override
            public FunctionMapper getFunctionMapper() {
                if (functionMapper == null)
                {
                    functionMapper = new FunctionMapperImpl();
                }
                return functionMapper;
            }

            @Override
            public VariableMapper getVariableMapper() {
                if (variableMapper == null)
                {
                    variableMapper = new VariableMapperImpl();
                }
                return variableMapper;
            }
        };
        final ValueExpression valueExpression = wrappedExpressionFactory.createValueExpression(elContext, expression, expectedType);
        final Object object = valueExpression.getValue(elContext);

        return (T)object;
    }

    @Named("magic.golden.fish")
    @RequestScoped
    public static class GoldenFish {
        private Parent parent;

        public Parent getParent() {
            return parent;
        }

        public void setParent(Parent parent) {
            this.parent = parent;
        }
    }

    @Named("magic.golden")
    @Dependent
    public static class ConflictingGoldenFish {
    }

    @Named("magic.blue")
    @Dependent
    public static class BlueGoldenFish {
    }

    @Named("dogs")
    @RequestScoped
    public static class Dogs {

    }

    @Named("dog")
    @RequestScoped
    public static class Dog {
        private Parent parent;

        public Parent getParent() {
            return parent;
        }

        public void setParent(Parent parent) {
            this.parent = parent;
        }
    }

    public static class Parent {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
