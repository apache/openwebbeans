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
package org.apache.webbeans.test.portable;

import org.apache.webbeans.test.AbstractUnitTest;
import org.junit.Test;

import jakarta.enterprise.inject.spi.Bean;
import jakarta.inject.Inject;

import static org.junit.Assert.assertEquals;

public class BeanInjectionTest extends AbstractUnitTest
{
    @Test
    public void run()
    {
        startContainer(Foo.class, Bar.class);
        assertEquals(Foo.class, getInstance(Bar.class).getFoo().getBean().getBeanClass());
    }

    public static class Foo
    {
        @Inject
        private Bean<Foo> bean;

        public Bean<Foo> getBean() {
            return bean;
        }
    }

    public static class Bar
    {
        @Inject
        private Foo foo;

        public Foo getFoo() {
            return foo;
        }
    }
}
