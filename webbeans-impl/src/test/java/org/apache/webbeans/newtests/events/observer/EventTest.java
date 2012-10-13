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
package org.apache.webbeans.newtests.events.observer;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.webbeans.newtests.AbstractUnitTest;
import org.junit.Assert;
import org.junit.Test;

public class EventTest extends AbstractUnitTest {

    @Test
    public void testOverriddenObserverMethodsInSubclasses()
    {
        Collection<Class<?>> beanClasses = new ArrayList<Class<?>>();
        beanClasses.add(Superclass.class);
        beanClasses.add(BeanA.class);
        startContainer(beanClasses, null);

        TestEvent testEvent = new TestEvent();
        getBeanManager().fireEvent(testEvent);

        Assert.assertEquals(1, testEvent.getCalledObservers().size());
        Assert.assertTrue(testEvent.getCalledObservers().iterator().next().endsWith(":[subclass]"));

        shutDownContainer();
    }

    @Test
    public void testSubclassRemovesObserverAnnotationByOverriding()
    {
        Collection<Class<?>> beanClasses = new ArrayList<Class<?>>();
        beanClasses.add(Superclass.class);
        beanClasses.add(BeanB.class);
        startContainer(beanClasses, null);

        TestEvent testEvent = new TestEvent();
        getBeanManager().fireEvent(testEvent);

        Assert.assertEquals(0, testEvent.getCalledObservers().size());

        shutDownContainer();
    }
}
