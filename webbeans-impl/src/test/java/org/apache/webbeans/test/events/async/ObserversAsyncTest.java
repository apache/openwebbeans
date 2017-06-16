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

import javax.enterprise.context.RequestScoped;
import javax.enterprise.event.ObservesAsync;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.webbeans.test.AbstractUnitTest;
import org.junit.Assert;
import org.junit.Test;

public class ObserversAsyncTest extends AbstractUnitTest
{
    @Test
    public void testAsyncEventExceptionHandling() throws ExecutionException, InterruptedException
    {
        startContainer(Observer1.class, Observer2.class);

        final AtomicReference<Throwable> observerException = new AtomicReference<>();

        BlockingQueue<Throwable> queue = new LinkedBlockingQueue<>();

        long start = System.nanoTime();

        VisitorCollectorEvent event = new VisitorCollectorEvent();
        CompletableFuture<VisitorCollectorEvent> completionStage = getBeanManager().getEvent().fireAsync(event)
            .exceptionally(e ->
            {
                observerException.set(e);
                return null;
            })
            .toCompletableFuture();

        VisitorCollectorEvent visitorCollectorEvent = completionStage.get();

        Assert.assertEquals(2, visitorCollectorEvent.getVisitors().size());

        long end = System.nanoTime();
        long durationMs = TimeUnit.NANOSECONDS.toMillis(end - start);
        System.out.println("took ms: " + durationMs);
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

    @RequestScoped
    public static class Observer1
    {
        public void visit(@ObservesAsync VisitorCollectorEvent visitorCollector)
        {
            sleep(100L);
            visitorCollector.visiting(getClass().getSimpleName());
            throw new IllegalStateException("Observer1");
        }
    }

    @RequestScoped
    public static class Observer2
    {
        public void visit(@ObservesAsync VisitorCollectorEvent visitorCollector)
        {
            sleep(2000L);
            visitorCollector.visiting(getClass().getSimpleName());
            //X throw new IllegalStateException("Observer2");
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
}
