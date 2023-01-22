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
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InterceptorBinding;
import jakarta.interceptor.InvocationContext;
import java.io.Serializable;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;

import static org.apache.webbeans.test.util.Serializations.deserialize;
import static org.apache.webbeans.test.util.Serializations.serialize;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DecoratorInterceptorProxySerializationTest extends AbstractUnitTest
{
    @Inject
    private Main client;

    @Test
    public void testProxyMappingConfig() throws Exception
    {
        addDecorator(MyDecorator.class);
        addInterceptor(IBInterceptor.class);
        startContainer(new ArrayList<Class<?>>() {{ add(Main.class); }}, null, true);

        try
        {
            final Main deserializeInit = Main.class.cast(deserialize(serialize(client)));
            assertFalse(deserializeInit.isCalled());
            assertFalse(deserializeInit.isDecoratorCalled());
            assertFalse(deserializeInit.isInterceptorCalled());

            client.aMethod();

            final Main deserializeState = Main.class.cast(deserialize(serialize(client)));
            assertTrue(deserializeState.isCalled());
            assertTrue(deserializeState.isDecoratorCalled());
            assertTrue(deserializeState.isInterceptorCalled());
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
        boolean isInterceptorCalled();
    }

    @InterceptorBinding
    @Target({ElementType.METHOD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    public static @interface IB
    {
    }

    @Interceptor
    @IB
    public static class IBInterceptor implements Serializable
    {
        private boolean called = false; // just here to represent a state in the serialization

        @AroundInvoke
        public Object intercept(final InvocationContext ctx) throws Exception
        {
            final String name = ctx.getMethod().getName();

            if (name.equals("isInterceptorCalled"))
            {
                return called;
            }
            else if (!name.startsWith("is"))
            {
                called = true;
            }

            return ctx.proceed();
        }
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

        @Override
        public boolean isInterceptorCalled()
        {
            return false;
        }
    }

    @IB
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

        @Override
        public boolean isInterceptorCalled()
        {
            return false;
        }
    }
}
