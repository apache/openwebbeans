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
package org.apache.webbeans.test.contexts;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jakarta.inject.Singleton;

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.corespi.se.StandaloneContextsService;
import org.apache.webbeans.spi.ContextsService;
import org.apache.webbeans.test.AbstractUnitTest;
import org.junit.Test;

public class StandaloneContextsServiceTest extends AbstractUnitTest
{
    @Test
    public void ensureSingletonScopeIsUnique() throws ExecutionException, InterruptedException
    {
        addService(ContextsService.class, new StandaloneContextsService(WebBeansContext.getInstance()));
        startContainer(Unique.class);
        final Unique instance = getInstance(Unique.class);
        final long id = instance.id(); // ensure it can be called = there is a context
        final ExecutorService es = Executors.newSingleThreadExecutor();

        // ContextNotActiveException: @Singleton does not exist within current thread with default
        assertEquals(id, es.submit(() -> getInstance(Unique.class).id()).get().longValue());
        es.shutdownNow();
    }

    @Singleton
    public static class Unique
    {
        private final long instance = System.identityHashCode(this);

        public long id()
        {
            return instance;
        }
    }
}
