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
package org.apache.webbeans.test.producer;

import org.apache.webbeans.test.AbstractUnitTest;
import org.junit.Test;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

import java.util.Collections;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class GenericProducerTest extends AbstractUnitTest
{
    @Inject
    private Injected injected;

    @Test
    public void checkParameterizedProducerMethod()
    {
        startContainer(asList(TheParameterizedProducer.class, Injected.class), Collections.<String>emptyList(), true);
        assertNotNull(injected);
        assertNotNull(injected.getGauge());
        assertNotNull(injected.getGauge().getValue());
        assertEquals("ok", injected.getGauge().getValue());
        assertNotNull(injected.getGauge2());
        assertNotNull(injected.getGauge2().getValue());
        assertEquals(1L, injected.getGauge2().getValue().longValue());
    }

    /**
     * This was explicitly specified to work in CDI-1.0.
     * We need it for backward compatibility!
     */
    @Test
    public void checkRawProducerMethod()
    {
        startContainer(asList(TheRawProducer.class, Injected.class), Collections.<String>emptyList(), true);
        assertNotNull(injected);
        assertNotNull(injected.getGauge());
        assertNotNull(injected.getGauge().getValue());
        assertEquals("ok", injected.getGauge().getValue());
        assertNotNull(injected.getGauge2());
        assertNotNull(injected.getGauge2().getValue());
        assertEquals(1L, injected.getGauge2().getValue().longValue());
    }

    /**
     * This was explicitly specified to work in CDI-1.0.
     * We need it for backward compatibility!
     */
    @Test
    public void checkMixedProducerMethods()
    {
        startContainer(asList(TheRawProducer.class, TheParameterizedProducer.class, Injected.class), Collections.<String>emptyList(), true);
        assertNotNull(injected);
        assertNotNull(injected.getGauge());
        assertNotNull(injected.getGauge().getValue());
        assertEquals("ok", injected.getGauge().getValue());
        assertNotNull(injected.getGauge2());
        assertNotNull(injected.getGauge2().getValue());
        assertEquals(1L, injected.getGauge2().getValue().longValue());
    }

    public static class TheParameterizedProducer
    {
        @Produces
        public <T, X> Gauge<T, X> gaugeProducer()
        {
            return new Gauge<T, X>();
        }
    }

    public static class TheRawProducer
    {
        @Produces
        public Gauge gaugeProducer()
        {
            return new Gauge();
        }
    }

    public static class Injected
    {
        @Inject
        private Gauge<String, Integer> gauge;

        @Inject
        private Gauge<Long, Integer> gauge2;

        public Gauge<String, Integer> getGauge()
        {
            gauge.value = "ok";
            return gauge;
        }

        public Gauge<Long, Integer> getGauge2()
        {
            gauge2.value = 1L;
            return gauge2;
        }
    }

    public static class Gauge<T, X>
    {
        private T value;

        public T getValue()
        {
            return value;
        }
    }



}
