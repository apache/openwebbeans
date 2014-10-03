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

import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import java.util.Collections;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class GenericProducerTest extends AbstractUnitTest
{
    @Inject
    private Injected injected;

    @Test
    public void check()
    {
        startContainer(asList(TheProducer.class, Injected.class), Collections.<String>emptyList(), true);
        assertNotNull(injected);
        assertNotNull(injected.getGauge());
        assertNotNull(injected.getGauge().getValue());
        assertEquals("ok", injected.getGauge().getValue());
        assertNotNull(injected.getGauge2());
        assertNotNull(injected.getGauge2().getValue());
        assertEquals(1L, injected.getGauge2().getValue().longValue());
    }

    public static class TheProducer
    {
        @Produces
        public <T> Gauge<T> gaugeProducer()
        {
            return new Gauge<T>();
        }
    }

    public static class Injected
    {
        @Inject
        private Gauge<String> gauge;

        @Inject
        private Gauge<Long> gauge2;

        public Gauge<String> getGauge()
        {
            gauge.value = "ok";
            return gauge;
        }

        public Gauge<Long> getGauge2()
        {
            gauge2.value = 1L;
            return gauge2;
        }
    }

    public static class Gauge<T>
    {
        private T value;

        public T getValue()
        {
            return value;
        }
    }
}
