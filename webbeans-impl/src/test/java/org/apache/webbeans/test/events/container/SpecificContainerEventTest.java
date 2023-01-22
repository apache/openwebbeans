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
package org.apache.webbeans.test.events.container;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.atomic.AtomicInteger;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ProcessBean;

import org.apache.webbeans.annotation.DefaultLiteral;
import org.apache.webbeans.test.AbstractUnitTest;
import org.junit.Test;

public class SpecificContainerEventTest extends AbstractUnitTest
{
    @Test
    public void captureSpecific()
    {
        final AtomicInteger counter = new AtomicInteger(0);
        addExtension(new Extension()
        {
            void onProcessBean(@Observes final ProcessBean<Foo> foo)
            {
                counter.incrementAndGet();
            }

            void onAfterBeanDiscovery(@Observes final AfterBeanDiscovery afterBeanDiscovery)
            {
                afterBeanDiscovery.addBean()
                        .addType(Bar.class)
                        .addQualifier(DefaultLiteral.INSTANCE)
                        .createWith(c -> new Bar());
            }
        });
        startContainer(Foo.class);
        assertEquals(1, counter.get());
    }

    @ApplicationScoped
    public static class Foo
    {
    }

    public static class Bar
    {
    }
}
