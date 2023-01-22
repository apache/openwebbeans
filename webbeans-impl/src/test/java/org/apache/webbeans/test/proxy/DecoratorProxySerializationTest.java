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
package org.apache.webbeans.test.proxy;

import org.apache.webbeans.test.AbstractUnitTest;
import org.junit.Test;

import jakarta.decorator.Decorator;
import jakarta.decorator.Delegate;
import jakarta.inject.Inject;
import java.io.Serializable;
import java.util.ArrayList;

import static org.apache.webbeans.test.util.Serializations.deserialize;
import static org.apache.webbeans.test.util.Serializations.serialize;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DecoratorProxySerializationTest extends AbstractUnitTest
{
    @Inject
    private Main client;

    @Test
    public void testProxyMappingConfig() throws Exception
    {
        addDecorator(MyDecorator.class);
        startContainer(new ArrayList<Class<?>>() {{ add(Main.class); }}, null, true);

        try
        {
            final Main deserializeInit = Main.class.cast(deserialize(serialize(client)));
            assertFalse(deserializeInit.isCalled());
            assertFalse(deserializeInit.isDecoratorCalled());

            client.aMethod();

            final Main deserializeState = Main.class.cast(deserialize(serialize(client)));
            assertTrue(deserializeState.isCalled());
            assertTrue(deserializeState.isDecoratorCalled());
        }
        finally
        {
            shutDownContainer();
        }
    }

    public static interface StupidClass
    {
        void aMethod();
        boolean isCalled();
        boolean isDecoratorCalled();
    }

    @Decorator
    public static class MyDecorator implements Serializable, StupidClass
    {
        private boolean called = false; // just here to represent a state in the serialization

        @Inject @Delegate
        private StupidClass delegate;

        @Override
        public void aMethod() {
            called = true;
            delegate.aMethod();
        }

        @Override
        public boolean isCalled() {
            return delegate.isCalled();
        }

        @Override
        public boolean isDecoratorCalled() {
            return called;
        }
    }

    public static class Main implements StupidClass, Serializable
    {
        private boolean called = false; // just here to represent a state in the serialization

        @Override
        public void aMethod()
        {
            called = true;
        }

        @Override
        public boolean isCalled()
        {
            return called;
        }

        @Override
        public boolean isDecoratorCalled()
        {
            return false;
        }
    }
}
