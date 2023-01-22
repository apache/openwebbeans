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

import org.junit.Assert;
import org.apache.webbeans.test.AbstractUnitTest;
import org.junit.Test;

import jakarta.annotation.Priority;
import jakarta.decorator.Decorator;
import jakarta.decorator.Delegate;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;
import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Test to reproduce OWB-1024
 */
public class ExtendedMulitpleGenericDecoratorTest extends AbstractUnitTest
{

    @Test
    public void testMultipleTypeParameters() throws Exception
    {
        startContainer(Interface.class,
                       ExtendingInterface.class,
                       Service.class,
                       ExtendingInterfaceDecorator.class,
                       SwapedExtendingInterfaceDecorator.class,
                       SwapedService.class,
                       SwapedParameterExtendingInterface.class);

        Service service = getInstance(Service.class);
        SwapedService swapedService = getInstance(SwapedService.class);

        Assert.assertNotNull(service);
        Assert.assertNotNull(swapedService);

        Number result = service.doSomethingOthers("aString");

        Assert.assertNotNull(result);
        Assert.assertEquals(2, result.intValue());

        Assert.assertTrue(ExtendingInterfaceDecorator.called);
        Assert.assertEquals(1, ExtendingInterfaceDecorator.callCount.get());

        String swapedResult = swapedService.doSomethingWithWrappedTypeParams(5L);

        Assert.assertNotNull(swapedResult);
        Assert.assertEquals("doSomethingWithWrappedTypeParams: your number is 5", swapedResult);

        Assert.assertTrue(SwapedExtendingInterfaceDecorator.called);
        Assert.assertEquals(1, SwapedExtendingInterfaceDecorator.callCount.get());


        // shutdown is called @After in AbstractUnitTest
    }



    public static interface Interface<P, T> {
        T doSomething(P param);
    }

    public static interface ExtendingInterface<P extends Serializable> extends Interface<P, Number> {
        Number doSomethingOthers(P param);
    }

    public static interface SwapedParameterExtendingInterface<T extends Serializable> extends Interface<Number, T>
    {
        T doSomethingWithWrappedTypeParams(Number param);
    }

    public static class Service implements ExtendingInterface<String>
    {

        @Override
        public Number doSomethingOthers(String param)
        {
            return param.length() % 5;
        }

        @Override
        public Number doSomething(String param)
        {
            return (param.length() / 2) * 6;
        }
    }

    public static class SwapedService implements SwapedParameterExtendingInterface<String> {

        @Override
        public String doSomethingWithWrappedTypeParams(Number param)
        {
            return "doSomethingWithWrappedTypeParams: your number is " + param.longValue();
        }

        @Override
        public String doSomething(Number param)
        {
            return "doSomething: parameter = " + param;
        }
    }

    @Decorator
    @Priority(value = 1)
    public abstract static class ExtendingInterfaceDecorator<T extends Serializable> implements ExtendingInterface<T>
    {
        public static boolean called;
        public static AtomicInteger callCount = new AtomicInteger(0);


        @Inject
        @Any
        @Delegate
        private ExtendingInterface<T> delegate;


        @Override
        public Number doSomethingOthers(T param)
        {
            called = true;
            callCount.incrementAndGet();

            return delegate.doSomethingOthers(param);
        }
    }

    @Decorator
    @Priority(value = 1)
    public abstract static class SwapedExtendingInterfaceDecorator<T extends Serializable> implements SwapedParameterExtendingInterface<T>
    {
        public static boolean called;
        public static AtomicInteger callCount = new AtomicInteger(0);


        @Inject
        @Any
        @Delegate
        private SwapedParameterExtendingInterface<T> delegete;


        @Override
        public T doSomethingWithWrappedTypeParams(Number param)
        {
            called = true;
            callCount.incrementAndGet();

            return delegete.doSomethingWithWrappedTypeParams(param);
        }
    }
}
