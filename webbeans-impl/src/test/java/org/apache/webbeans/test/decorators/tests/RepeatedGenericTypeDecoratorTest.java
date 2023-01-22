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

import jakarta.decorator.Decorator;
import jakarta.decorator.Delegate;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;

import org.junit.Assert;
import org.apache.webbeans.test.AbstractUnitTest;
import org.junit.Test;

public class RepeatedGenericTypeDecoratorTest extends AbstractUnitTest
{

    @Test
    public void testRepeatedGenericDecorator() throws Exception
    {
        addDecorator(MyServiceDecorator.class);
        startContainer(MyInterface.class, MyService.class, MyServiceDecorator.class);

        MyService myService = getInstance(MyService.class);

        MyServiceDecorator.called = false;
        myService.doSomethingStrange(new MyInterface<MyInterface>()
        {
            @Override
            public void doSomethingStrange(MyInterface param)
            {
                // have no idea here too
            }
        });
        Assert.assertTrue(MyServiceDecorator.called);
    }


    public static interface MyInterface<T>
    {
        void doSomethingStrange(T param);
    }


    public static class MyService implements MyInterface<MyInterface<MyInterface>> {

        @Override
        public void doSomethingStrange(MyInterface<MyInterface> param)
        {
            // don't know what to do here
            // i have no idea for a usecase
            param.doSomethingStrange(param);
        }
    }


    @Decorator
    public static class MyServiceDecorator<T extends MyInterface<T>> implements MyInterface<T>
    {
        private static boolean called;

        @Delegate
        @Inject
        @Any
        private MyInterface<T> delegate;

        @Override
        public void doSomethingStrange(T param)
        {
            called = true;
        }
    }
}
