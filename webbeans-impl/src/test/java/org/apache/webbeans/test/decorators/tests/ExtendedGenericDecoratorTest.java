/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.webbeans.test.decorators.tests;

import javax.annotation.Priority;
import javax.decorator.Decorator;
import javax.decorator.Delegate;
import javax.enterprise.inject.Any;
import javax.inject.Inject;

import junit.framework.Assert;
import org.apache.webbeans.test.AbstractUnitTest;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Test to reproduce OWB-992
 */
public class ExtendedGenericDecoratorTest extends AbstractUnitTest
{
    @Test
    @Ignore
    public void testExtendedGenericDecorator() throws Exception
    {
        addDecorator(ExtendedInterfaceDecorator.class);

        startContainer(Interface.class, ExtendedInterface.class, LongService.class, IntegerService.class);

        Assert.assertFalse(LongService.testMethodCalled);
        Assert.assertFalse(LongService.anotherMethodCalled);
        Assert.assertFalse(IntegerService.testMethodCalled);
        Assert.assertFalse(IntegerService.anotherMethodCalled);
        Assert.assertFalse(ExtendedInterfaceDecorator.testMethodDecorated);
        Assert.assertFalse(ExtendedInterfaceDecorator.anotherMethodDecorated);

        ExtendedInterface<Long> service = getInstance(LongService.class);

        // call method from interface
        service.anotherMethod(2L);

        Assert.assertTrue(LongService.anotherMethodCalled);
        Assert.assertTrue(ExtendedInterfaceDecorator.anotherMethodDecorated);

        // call method from super interface
        service.test(1L);

        Assert.assertTrue(LongService.testMethodCalled);
        Assert.assertTrue(ExtendedInterfaceDecorator.testMethodDecorated);

        shutDownContainer();

    }


    public static interface Interface<T> {
        void test(T input);
    }

    public static interface ExtendedInterface<T extends Number> extends Interface<T> {
        void anotherMethod(T input);
    }

    public static class LongService implements ExtendedInterface<Long> {

        public static boolean anotherMethodCalled = false;
        public static boolean testMethodCalled = false;

        @Override
        public void anotherMethod(Long input)
        {
            anotherMethodCalled = true;
        }

        @Override
        public void test(Long input)
        {
            testMethodCalled = true;
        }
    }

    public static class IntegerService implements ExtendedInterface<Integer>
    {

        public static boolean anotherMethodCalled = false;
        public static boolean testMethodCalled = false;

        @Override
        public void anotherMethod(Integer input)
        {
            anotherMethodCalled = true;
        }

        @Override
        public void test(Integer input)
        {
            testMethodCalled = true;
        }
    }

    @Decorator
    @Priority(value = 10)
    public static class ExtendedInterfaceDecorator<T extends Number> implements ExtendedInterface<T> {

        public static boolean testMethodDecorated = false;
        public static boolean anotherMethodDecorated = false;

        @Delegate
        @Inject
        @Any
        private ExtendedInterface<T> delegate;


        @Override
        public void anotherMethod(T input)
        {
            anotherMethodDecorated = true;
            delegate.anotherMethod(input);
        }

        @Override
        public void test(T input)
        {
            testMethodDecorated = true;
            delegate.test(input);
        }
    }
}
