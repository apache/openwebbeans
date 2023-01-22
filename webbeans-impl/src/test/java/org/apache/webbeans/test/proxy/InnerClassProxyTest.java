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
package org.apache.webbeans.test.proxy;

import org.apache.webbeans.test.AbstractUnitTest;
import org.junit.Test;

import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InterceptorBinding;
import jakarta.interceptor.InvocationContext;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class InnerClassProxyTest extends AbstractUnitTest {
    @Inject
    private Foo foo;

    @Test
    public void proxy() throws IllegalAccessException, InstantiationException {
        addInterceptor(InInterceptor.class);
        startContainer(Arrays.asList(Foo.class, Inner.class), Collections.<String>emptyList(), true);
        assertNotNull(foo);
        try {
            assertEquals("ok", foo.bar(false));
        } catch (final Exception e) {
            fail();
        }
        try {
            foo.bar(true);
            fail();
        } catch (final Exception e) {
            // no-op: ok
        }
        shutDownContainer();
    }

    @In
    public static class Foo {
        public String bar(final boolean b) throws Inner {
            if (b) {
                throw new Inner("fail");
            }
            return "ok";
        }
    }

    @Interceptor
    @In
    public static class InInterceptor
    {
        @AroundInvoke
        public Object invoke(InvocationContext context) throws Exception
        {
            return context.proceed();
        }
    }

    @InterceptorBinding
    @Retention(RetentionPolicy.RUNTIME)
    @Target( { ElementType.TYPE, ElementType.METHOD })
    public static @interface In {
    }

    public static class Inner extends RuntimeException {
        public Inner() {
            // no-op
        }

        public Inner(final String message) {
            super(message);
        }
    }
}
