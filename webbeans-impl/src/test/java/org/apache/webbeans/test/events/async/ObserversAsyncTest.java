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
package org.apache.webbeans.test.events.async;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.Extension;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.webbeans.test.AbstractUnitTest;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ObserversAsyncTest extends AbstractUnitTest
{


    @Test
    public void testAsyncEventExceptionHandling_handle() throws ExecutionException, InterruptedException
    {
        final int count = 5 + ForkJoinPool.getCommonPoolParallelism() * 5;

        final VisitorCollectorEvent event = new VisitorCollectorEvent();

        addExtension(new ParallelObserveExtension(count));
        startContainer();

        BlockingQueue<Throwable> queue = new LinkedBlockingQueue<>();

        long start = System.nanoTime();

        getBeanManager().getEvent().fireAsync(event)
            .handle((e, t) -> queue.offer(t));

        Throwable t = queue.poll(20, TimeUnit.SECONDS);

        long end = System.nanoTime();
        long durationMs = TimeUnit.NANOSECONDS.toMillis(end - start);
        System.out.println("took ms: " + durationMs);

        Assert.assertNotNull(t);
        Assert.assertTrue(t instanceof CompletionException);
        CompletionException ce = (CompletionException) t;

        if (count != ce.getSuppressed().length)
        {
            Stream.of(ce.getSuppressed())
                .sorted(Comparator.comparing(Throwable::getMessage))
                .forEach(throwable -> System.out.println(throwable.getMessage()));
        }
        Assert.assertEquals(count, ce.getSuppressed().length);

    }

    @Test
    public void testAsyncEventExceptionHandling_CompletableFuture() throws ExecutionException, InterruptedException
    {
        final int count = 10 + ForkJoinPool.getCommonPoolParallelism() * 5;

        final VisitorCollectorEvent event = new VisitorCollectorEvent();

        addExtension(new ParallelObserveExtension(count));
        startContainer();

        final AtomicReference<Throwable> observerException = new AtomicReference<>();

        long start = System.nanoTime();

        CompletableFuture<VisitorCollectorEvent> completionStage = getBeanManager().getEvent().fireAsync(event)
            .exceptionally(e ->
            {
                observerException.set(e);
                return event;
            })
            .toCompletableFuture();

        VisitorCollectorEvent visitorCollectorEvent = completionStage.get();

        assertNotNull(observerException.get());
        assertNotNull(visitorCollectorEvent);
        assertEquals(count, visitorCollectorEvent.getVisitors().size());

        long end = System.nanoTime();
        long durationMs = TimeUnit.NANOSECONDS.toMillis(end - start);
        System.out.println("took ms: " + durationMs);
    }

    @Test
    public void testNoObserver() throws ExecutionException, InterruptedException
    {
        startContainer();
        final CompletableFuture<ObserversAsyncTest> completionStage = getBeanManager()
                .getEvent()
                .fireAsync(new ObserversAsyncTest())
                .handle((r, e) ->
                {
                    if (r != null)
                    {
                        return r;
                    }
                    fail();
                    return null;
                })
                .toCompletableFuture();

        assertTrue(completionStage.isDone());
        assertTrue(ObserversAsyncTest.class.isInstance(completionStage.get()));
    }

    public static class VisitorCollectorEvent
    {
        private List<String> visitors = Collections.synchronizedList(new ArrayList<>());

        public void visiting(String visitor)
        {
            visitors.add(visitor);
        }

        public List<String> getVisitors()
        {
            return visitors;
        }
    }

    private static void sleep(long time)
    {
        try
        {
            Thread.sleep(time);
        }
        catch (InterruptedException e)
        {
            // ignore
        }
    }

    private class ParallelObserveExtension implements Extension
    {
        private final int count;

        public ParallelObserveExtension(int count)
        {
            this.count = count;
        }

        void addABunchOfObserversAtLeastMoreThanThreads(@Observes final AfterBeanDiscovery afterBeanDiscovery)
        {
            IntStream.range(0, count)
                .forEach(i -> afterBeanDiscovery.<VisitorCollectorEvent>addObserverMethod()
                    .observedType(VisitorCollectorEvent.class)
                    .async(true)
                    .notifyWith(e ->
                    {
                        if (i % 2 == 0 && (i < 30 || i > 70))
                        {
                            sleep(200);
                        }

                        final String name = String.format("%s_%03d", "Observer", i);
                        e.getEvent().visiting(name);
                        throw new IllegalStateException(name);
                    }));
        }
    }
}
