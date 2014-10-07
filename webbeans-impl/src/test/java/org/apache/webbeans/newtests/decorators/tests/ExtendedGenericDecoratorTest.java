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
package org.apache.webbeans.newtests.decorators.tests;

import javax.decorator.Decorator;
import javax.decorator.Delegate;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.spi.Bean;
import javax.inject.Inject;

import junit.framework.Assert;
import org.apache.webbeans.annotation.Decorated;
import org.apache.webbeans.newtests.AbstractUnitTest;
import org.junit.Test;

/**
 * Test to reproduce OWB-992
 */
public class ExtendedGenericDecoratorTest extends AbstractUnitTest
{
    @Test
    public void testExtendedGenericDecorator() throws Exception
    {
        addDecorator(ExtendedInterfaceDecorator.class);

        startContainer(Interface.class, ExtendedInterface.class, LongService.class, IntegerService.class, StringService.class);

        Assert.assertFalse(LongService.testMethodCalled);
        Assert.assertFalse(LongService.anotherMethodCalled);
        Assert.assertFalse(IntegerService.testMethodCalled);
        Assert.assertFalse(IntegerService.anotherMethodCalled);
        Assert.assertFalse(ExtendedInterfaceDecorator.anotherMethodDecoratedWithLong);
        Assert.assertFalse(ExtendedInterfaceDecorator.anotherMethodDecoratedWithInteger);
        Assert.assertFalse(StringService.testMethodCalled);

        // LongService
        ExtendedInterface<Long> service = getInstance(LongService.class);
        service.anotherMethod(2L); // call method from interface - shall be decorated
        service.test(1L); // call method from super interface - shall not be decorated

        // StringService shall not be decorated
        StringService stringService = getInstance(StringService.class);
        stringService.test("test");

        // IntegerService
        IntegerService integerService = getInstance(IntegerService.class);
        integerService.anotherMethod(3); // call method from interface - shall be decorated
        integerService.test(4); // call method from super interface - shall not be decorated


        Assert.assertTrue(LongService.anotherMethodCalled);
        Assert.assertTrue(LongService.testMethodCalled);
        Assert.assertTrue(ExtendedInterfaceDecorator.anotherMethodDecoratedWithLong);
        Assert.assertTrue(IntegerService.anotherMethodCalled);
        Assert.assertTrue(IntegerService.testMethodCalled);
        Assert.assertTrue(ExtendedInterfaceDecorator.anotherMethodDecoratedWithInteger);
        Assert.assertTrue(StringService.testMethodCalled);


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

    public static class StringService implements Interface<String> {

        public static boolean testMethodCalled = false;

        @Override
        public void test(String input)
        {
            testMethodCalled = true;
        }
    }

    @Decorator
    public abstract static class ExtendedInterfaceDecorator<T extends Number> implements ExtendedInterface<T> {

        public static boolean anotherMethodDecoratedWithLong = false;
        public static boolean anotherMethodDecoratedWithInteger = false;

        public static boolean unexpectedDecorated = false;

        @Delegate
        @Inject
        @Any
        private ExtendedInterface<T> delegate;

        @Inject
        @Decorated
        private Bean<Interface<T>> decorated;


        @Override
        public void anotherMethod(T input)
        {
            Class<?> beanClass = decorated.getBeanClass();
            if (beanClass.equals(LongService.class))
            {
                anotherMethodDecoratedWithLong = true;
            }
            else if (beanClass.equals(IntegerService.class))
            {
                anotherMethodDecoratedWithInteger = true;
            }
            else
            {
                unexpectedDecorated = true;
            }

            delegate.anotherMethod(input);
        }
    }
}
