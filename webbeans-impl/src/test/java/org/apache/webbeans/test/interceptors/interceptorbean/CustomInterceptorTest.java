/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.webbeans.test.interceptors.interceptorbean;

import jakarta.enterprise.context.ApplicationScoped;
import org.apache.webbeans.test.AbstractUnitTest;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Test for programmatically added Interceptors
 */
public class CustomInterceptorTest extends AbstractUnitTest
{
    @Test
    public void testCustomInterceptor() throws Exception
    {
        BigBrotheredExtension bbe = new BigBrotheredExtension();
        addExtension(bbe);
        startContainer(LittleJohnDoe.class);

        final LittleJohnDoe joe = getInstance(LittleJohnDoe.class);

        joe.makeACoffee();
        assertTrue(BigBrotherInterceptor.isObserved());

        joe.watchTv();
        assertTrue(BigBrotherInterceptor.isObserved());
    }

    @ApplicationScoped
    @BigBrothered
    public static class LittleJohnDoe
    {
        public void makeACoffee() {
            System.out.println("Making a coffee");
        }

        public void watchTv() {
            System.out.println("Watching TV");
        }
    }
}
