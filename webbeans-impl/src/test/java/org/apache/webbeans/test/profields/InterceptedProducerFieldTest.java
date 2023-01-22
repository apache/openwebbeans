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
package org.apache.webbeans.test.profields;

import org.apache.webbeans.test.AbstractUnitTest;
import org.apache.webbeans.test.profields.beans.classproducer.MyProductBean;
import org.junit.Test;

import jakarta.enterprise.inject.Produces;
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
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class InterceptedProducerFieldTest extends AbstractUnitTest {
    @Inject
    private MyProductBean bean;

    @Test
    public void testProductProducer() {
        addInterceptor(BoundInterceptor.class);
        startContainer(Collections.<Class<?>>singletonList(Producing.class), Collections.<String>emptyList(), true);

        BoundInterceptor.CALLED.set(false);

        assertEquals(1234, bean.getI());
        assertFalse(BoundInterceptor.CALLED.get()); // we access the field so no interception
        getInstance(Producing.class).intercepted();
        assertTrue(BoundInterceptor.CALLED.get()); // ensure interceptors were correctly setup
    }

    @Bound
    public static class Producing {
        @Produces
        MyProductBean product = new MyProductBean() {
            @Override
            public int getI() {
                return 1234;
            }
        };

        public void intercepted() {
        }
    }


    @InterceptorBinding
    @Target({ElementType.METHOD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Bound {
    }

    @Bound
    @Interceptor
    public static class BoundInterceptor implements Serializable {
        private static final AtomicBoolean CALLED = new AtomicBoolean();

        @AroundInvoke
        public Object measure(final InvocationContext ctx) throws Exception {
            CALLED.set(true);
            return ctx.proceed();
        }
    }
}
