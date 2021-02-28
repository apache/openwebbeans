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
package org.apache.webbeans.test.events.observer;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.apache.webbeans.test.AbstractUnitTest;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class EventTest extends AbstractUnitTest {

    @Test
    public void multipleObserverMethodsWithSameName()
    {
        startContainer(Painter.class);

        final Orange orange = new Orange();
        getBeanManager().fireEvent(orange);

        final Green green = new Green();
        getBeanManager().fireEvent(green);

        final Painter painter = getInstance(Painter.class);
        Assert.assertEquals(2, painter.getObserved().size());
        Assert.assertSame(orange, painter.getObserved().get(0));
        Assert.assertSame(green, painter.getObserved().get(1));

        shutDownContainer();
    }

    @Test
    public void testOverriddenObserverMethodsInSubclasses()
    {
        startContainer(Superclass.class, BeanA.class);

        TestEvent testEvent = new TestEvent();
        getBeanManager().fireEvent(testEvent);

        Assert.assertEquals(1, testEvent.getCalledObservers().size());
        Assert.assertTrue(testEvent.getCalledObservers().iterator().next().equals("BeanA"));

        shutDownContainer();
    }

    @Test
    public void testEventViaEventSource()
    {
        startContainer(Superclass.class, BeanA.class, EventSourceOwnerBean.class);

        PrivateTestEvent testEvent = new PrivateTestEvent();
        getInstance(EventSourceOwnerBean.class).fireForMe(testEvent);

        Assert.assertEquals(2, testEvent.getCalledObservers().size());
        Assert.assertTrue(testEvent.getCalledObservers().contains("BeanA"));
        Assert.assertTrue(testEvent.getCalledObservers().contains("BeanA[Superclass]"));

        shutDownContainer();
    }

    @Test
    public void testSubclassRemovesObserverAnnotationByOverriding()
    {
        startContainer(Superclass.class, BeanB.class);

        TestEvent testEvent = new TestEvent();
        getBeanManager().fireEvent(testEvent);

        Assert.assertEquals(0, testEvent.getCalledObservers().size());

        shutDownContainer();
    }
    
    @Test
    public void testObserverOnPrivateMethod() {
        startContainer(Superclass.class, BeanA.class);

        PrivateTestEvent testEvent = new PrivateTestEvent();
        getBeanManager().fireEvent(testEvent);

        Assert.assertEquals(2, testEvent.getCalledObservers().size());
        Assert.assertTrue(testEvent.getCalledObservers().contains("BeanA"));
        Assert.assertTrue(testEvent.getCalledObservers().contains("BeanA[Superclass]"));

        shutDownContainer();

    }
    
    @Test
    public void testPrivateMethodCannotBeOverridden() {
        startContainer(Superclass.class, BeanB.class);

        PrivateTestEvent testEvent = new PrivateTestEvent();
        getBeanManager().fireEvent(testEvent);

        Assert.assertEquals(1, testEvent.getCalledObservers().size());
        Assert.assertEquals("BeanB[Superclass]", testEvent.getCalledObservers().iterator().next());

        shutDownContainer();

    }
    
    @Test
    public void testObserverOnStaticMethod() {
        startContainer(Superclass.class, BeanA.class);

        StaticTestEvent testEvent = new StaticTestEvent();
        getBeanManager().fireEvent(testEvent);

        Assert.assertEquals(2, testEvent.getCalledObservers().size());
        Assert.assertTrue(testEvent.getCalledObservers().contains("BeanA"));
        Assert.assertTrue(testEvent.getCalledObservers().contains("Superclass"));

        shutDownContainer();

    }
    
    @Test
    public void testStaticMethodCannotBeOverridden() {
        startContainer(Superclass.class, BeanB.class);

        StaticTestEvent testEvent = new StaticTestEvent();
        getBeanManager().fireEvent(testEvent);

        Assert.assertEquals(1, testEvent.getCalledObservers().size());
        Assert.assertEquals("Superclass", testEvent.getCalledObservers().iterator().next());

        shutDownContainer();
    }

    @Test
    @Ignore("only for manual performance testing and debugging")
    public void testEventPerformance()
    {
        startContainer(Painter.class, Litographer.class);

        final Orange orange = new Orange();

        Logger.getLogger(EventTest.class.getName()).info("Starting performance test");

        long start = System.nanoTime();
        for (int i = 0; i < 5_000_000; i++)
        {
            getBeanManager().fireEvent(orange);
        }

        long end = System.nanoTime();

        Logger.getLogger(EventTest.class.getName())
            .info("firing Events took " + TimeUnit.NANOSECONDS.toMillis(end - start));

        shutDownContainer();
    }


}
