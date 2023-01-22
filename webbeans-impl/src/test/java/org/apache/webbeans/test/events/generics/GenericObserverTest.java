/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.webbeans.test.events.generics;

import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.webbeans.test.AbstractUnitTest;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests assignability of raw and parameterized event types.
 * See CDI-1.2 spec 10.3.1. "Assignability of type variables, raw and parameterized types"
 */
public class GenericObserverTest extends AbstractUnitTest
{

    @Test
    public void testGenericObserverAssignability()
    {
        startContainer(GenericEventBean1.class);

        GenericEventBean1 instance = getInstance(GenericEventBean1.class);
        instance.fireEvent();

        Assert.assertTrue(instance.isIntegerObserved());
        Assert.assertFalse(instance.isUnintendedListObserved());
        Assert.assertFalse(instance.isNumberObserved());
        Assert.assertTrue(instance.isExtendsNumberObserved());
    }

    @Test
    public void testOwb1066()
    {
        startContainer(GenericEventBean1.class);

        GenericEventBean1 instance = getInstance(GenericEventBean1.class);
        instance.fireOwb1066_Event();

        Assert.assertTrue(instance.isIntegerObserved());
        Assert.assertFalse(instance.isUnintendedListObserved());
        Assert.assertFalse(instance.isNumberObserved());
        Assert.assertTrue(instance.isExtendsNumberObserved());
    }


    @RequestScoped
    public static class GenericEventBean1
    {
        private boolean integerObserved = false;
        private boolean numberObserved = false;
        private boolean extendsNumberObserved = false;
        private boolean unintendedListObserved = false;


        private @Inject Event<List<Integer>> integerEvent;

        public void fireEvent()
        {
            List<Integer> list = new ArrayList<Integer>(Arrays.asList(1,2,3));
            integerEvent.fire(list);
        }

        /**
         * Test for OWB-1066. Previously created an endless loop
         */
        public void fireOwb1066_Event()
        {
            List<Integer> list = Arrays.asList(1,2,3);
            integerEvent.fire(list);
        }

        public void observeTotallyDifferentList(@Observes List<Map> mapList)
        {
            unintendedListObserved = true;
        }

        public void observeParameterizedEventInteger(@Observes List<Integer> integerList)
        {
            integerObserved = true;
        }

        public void observeParameterizedEventNumber(@Observes List<Number> integerList)
        {
            numberObserved = true;
        }

        public void observeParameterizedEventExtendsNumber(@Observes List<? extends Number> integerList)
        {
            extendsNumberObserved = true;
        }


        public boolean isIntegerObserved()
        {
            return integerObserved;
        }

        public boolean isNumberObserved()
        {
            return numberObserved;
        }

        public boolean isExtendsNumberObserved()
        {
            return extendsNumberObserved;
        }

        public boolean isUnintendedListObserved()
        {
            return unintendedListObserved;
        }
    }
}
