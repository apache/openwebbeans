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
package org.apache.webbeans.test.configurator;

import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ObserverMethod;
import javax.enterprise.inject.spi.configurator.ObserverMethodConfigurator;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Qualifier;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.webbeans.test.AbstractUnitTest;
import org.junit.Assert;
import org.junit.Test;

public class ObserverMethodConfiguratorAfterBeanDiscoveryTest extends AbstractUnitTest
{

    private static List<String> observedEvents = new ArrayList<>();

    @Test
    public void testManualObservers()
    {
        addExtension(new ObserverAddingExtension());
        startContainer();

        Set<ObserverMethod<? super MyEvent>> observerMethods = getBeanManager().resolveObserverMethods(new MyEvent());
        Assert.assertEquals(2, observerMethods.size());

        observedEvents.clear();

        Event<Object> event = getBeanManager().getEvent();
        Assert.assertNotNull(event);

        event.select(MyEvent.class);
        event.fire(new MyEvent());

        Assert.assertEquals(2, observedEvents.size());
        Assert.assertEquals("event1", observedEvents.get(0));
        Assert.assertEquals("event2", observedEvents.get(1));
    }

    public static class MyEvent
    {

    }

    public static class ObserverAddingExtension implements Extension
    {
        public void addObserverMethod(@Observes AfterBeanDiscovery abd)
        {
            ObserverMethodConfigurator<MyEvent> observerCfg1 = abd.<MyEvent>addObserverMethod();
            Assert.assertNotNull(observerCfg1);

            observerCfg1
                .observedType(MyEvent.class)
                .priority(100)
                .notifyWith(eventContext ->
                {
                    observedEvents.add("event1");
                });

            ObserverMethodConfigurator<MyEvent> observerCfg2 = abd.<MyEvent>addObserverMethod();
            Assert.assertNotNull(observerCfg2);
            observerCfg2
                .observedType(MyEvent.class)
                // with default prio
                .notifyWith(eventContext ->
                {
                    observedEvents.add("event2");
                });

            ObserverMethodConfigurator<MyEvent> observerCfg3 = abd.<MyEvent>addObserverMethod();
            Assert.assertNotNull(observerCfg3);
            observerCfg3
                .observedType(MyEvent.class)
                .priority(3000)
                .addQualifier(new AnnotationLiteral<MyQualifier>()
                    { // with @MyQualifier
                    })
                .notifyWith(eventContext ->
                {
                    observedEvents.add("event3");
                });
        }
    }

    @Qualifier
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER})
    public @interface MyQualifier
    {

    }
}
