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
package org.apache.webbeans.test.performance;

import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.spi.BeanManager;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.apache.webbeans.test.AbstractUnitTest;
import org.junit.Test;

/**
 * Test to benchmark the performance of bean resolving
 */
public class BeanResolvingPerformanceTest extends AbstractUnitTest
{
    private static final Logger logger = Logger.getLogger(BeanResolvingPerformanceTest.class.getName());

    private static final int WARMUP_ITERATIONS = 10000;

    // tune up to larger values to
    private static final int BENCHMARK_ITERATIONS = 1000000;

    /**
     * original times with 10000000 iterations on my MBP
     * started with 9885,9776,9868
     * after qualifier hashCode tuning: 6177,6144,6075
     * after qualifier-Comparator tuning: 1573,1453,1471
     */
    @Test
    public void testBeanResolverPerformance()
    {
        startContainer(BeanWithDefaultQualifier.class);
        BeanManager bm = getBeanManager();

        for (int i= 0; i < WARMUP_ITERATIONS; i++)
        {
            getBeans(bm);
        }

        long start = System.nanoTime();
        for (int i= 0; i < BENCHMARK_ITERATIONS; i++)
        {
            getBeans(bm);
        }
        long end = System.nanoTime();
        logger.info("Resolving a bean " + BENCHMARK_ITERATIONS + " times took ms: " + TimeUnit.NANOSECONDS.toMillis(end - start));
    }

    private void getBeans(BeanManager bm)
    {
        bm.getBeans(BeanWithDefaultQualifier.class);
    }


    @RequestScoped
    public static class BeanWithDefaultQualifier
    {
        // no content needed
    }
}
