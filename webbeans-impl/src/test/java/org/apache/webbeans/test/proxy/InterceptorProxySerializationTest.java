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
import java.util.Arrays;

import static org.apache.webbeans.test.util.Serializations.deserialize;
import static org.apache.webbeans.test.util.Serializations.serialize;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class InterceptorProxySerializationTest extends AbstractUnitTest
{
    @Inject
    private Intercepted client;

    @Inject
    private AutoIntercepted auto;

    @Test
    public void testProxyMappingConfig() throws Exception
    {
        addInterceptor(IBInterceptor.class);
        startContainer(Arrays.<Class<?>>asList(Intercepted.class, AutoIntercepted.class, InjectMeInInterceptor.class), null, true);

        try
        {
            final Intercepted deserializeInit = Intercepted.class.cast(deserialize(serialize(client)));
            assertFalse(deserializeInit.isCalled());
            assertFalse(deserializeInit.isInterceptorCalled());

            client.intercepted();

            final Intercepted deserializeState = Intercepted.class.cast(deserialize(serialize(client)));
            assertTrue(deserializeState.isCalled());
            assertTrue(deserializeState.isInterceptorCalled());
        }
        finally
        {
            shutDownContainer();
        }
    }

    @Test
    public void testSerializableEvenIfAutoIntercepted() throws Exception
    {
        addInterceptor(IBInterceptor.class);
        startContainer(Arrays.<Class<?>>asList(Intercepted.class, AutoIntercepted.class, InjectMeInInterceptor.class), null, true);

        try
        {
            AutoIntercepted.called = false;
            auto.touch();
            assertTrue(AutoIntercepted.called);

            final AutoIntercepted deserializeInit = AutoIntercepted.class.cast(deserialize(serialize(auto)));
            AutoIntercepted.called = false;
            deserializeInit.touch();
            assertTrue(AutoIntercepted.called);

            final AutoIntercepted deserializeState = AutoIntercepted.class.cast(deserialize(serialize(deserializeInit)));
            AutoIntercepted.called = false;
            deserializeState.touch();
            assertTrue(AutoIntercepted.called);
        }
        finally
        {
            shutDownContainer();
        }
    }

    @InterceptorBinding
    @Target({ElementType.METHOD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    public static @interface IB
    {
    }

    public static class InjectMeInInterceptor implements Serializable {
        public void touch() {}
    }

    @Interceptor @IB
    public static class IBInterceptor implements Serializable
    {
        private boolean called = false; // just here to represent a state in the serialization

        @Inject
        private InjectMeInInterceptor injected;

        @AroundInvoke
        public Object intercept(final InvocationContext ctx) throws Exception
        {
            injected.touch(); // will throw NPE if wrongly serialized

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

    @IB
    public static class Intercepted implements Serializable
    {
        private boolean called = false; // just here to represent a state in the serialization

        public void intercepted()
        {
            called = true;
        }

        public boolean isCalled()
        {
            return called;
        }

        public boolean isInterceptorCalled()
        {
            return false;
        }
    }

    @IB
    public static class AutoIntercepted implements Serializable
    {
        public static boolean called = false;

        @AroundInvoke
        public Object auto(final InvocationContext ic)  throws Exception {
            called = true;
            return ic.proceed();
        }

        public void touch() {}
    }
}
