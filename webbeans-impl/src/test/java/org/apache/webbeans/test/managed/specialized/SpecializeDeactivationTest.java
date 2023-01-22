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
package org.apache.webbeans.test.managed.specialized;

import org.apache.webbeans.test.AbstractUnitTest;
import org.junit.Before;
import org.junit.Test;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.Specializes;
import jakarta.inject.Inject;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SpecializeDeactivationTest extends AbstractUnitTest
{
    @Inject
    private Init init;

    @Before
    public void reset() {
        Impl1.called = false;
        Impl2.called = false;
        SpeImpl1.called = false;
    }

    @Test
    public void normal()
    {
        startContainer(Arrays.<Class<?>>asList(Init.class, API.class, Impl1.class, Impl2.class),
            Collections.<String>emptyList(), true);

        assertEquals(2, init.init());
        assertTrue(Impl1.called);
        assertTrue(Impl2.called);
    }

    @Test
    public void specialize()
    {
        startContainer(Arrays.<Class<?>>asList(Init.class, API.class, Impl1.class, Impl2.class, SpeImpl1.class),
            Collections.<String>emptyList(), true);

        assertEquals(2, init.init());
        assertTrue(SpeImpl1.called);
        assertTrue(Impl2.called);
    }

    @Test
    public void specializeProducers()
    {
        startContainer(Arrays.asList(Init.class, API.class, Prod1.class, Prod2.class, Spe1.class, Prod3.class, Prod4.class),
            Collections.emptyList(), true);

        assertEquals(4, init.init());
        assertTrue(SpeImpl1.called);
        assertTrue(Impl2.called);
    }

    public interface API
    {
        void init();
    }

    public interface API2
    {
        void init();
    }

    public static class Prod1
    {
        @Produces
        public API api1()
        {
            return new Impl1();
        }

        @Produces
        public API2 api2()
        {
            return new Impl1();
        }
    }

    public static class Prod3
    {
        @Produces
        public API api1()
        {
            return new Impl1();
        }

    }

    public static class Prod4
    {
        @Produces
        public API api1()
        {
            return new Impl1();
        }

    }

    public static class Spe1 extends Prod1
    {
        @Produces
        @Override
        @Specializes
        public API api1()
        {
            return new SpeImpl1();
        }

        @Produces
        @Override
        @Specializes
        public API2 api2()
        {
            return new SpeImpl1();
        }
    }

    public static class Prod2
    {
        @Produces
        public API api1()
        {
            return new Impl2();
        }

        @Produces
        public API2 api2()
        {
            return new Impl2();
        }
    }

    public static class Impl1 implements API, API2
    {
        public static boolean called = false;

        @Override
        public void init()
        {
            called = true;
        }
    }

    @Specializes
    public static class SpeImpl1 extends Impl1
    {
        public static boolean called = false;

        @Override
        public void init()
        {
            called = true;
        }
    }

    public static class Impl2 implements API, API2
    {
        public static boolean called = false;

        @Override
        public void init()
        {
            called = true;
        }
    }

    public static class Init
    {
        @Inject
        @Any
        private Instance<API> impls;

        public int init()
        {
            int i = 0;
            for (final API api : impls)
            {
                api.init();
                i++;
            }
            return i;
        }
    }
}
