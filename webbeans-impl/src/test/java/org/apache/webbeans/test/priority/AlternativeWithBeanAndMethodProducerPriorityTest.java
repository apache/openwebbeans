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
package org.apache.webbeans.test.priority;

import org.apache.webbeans.component.ProducerMethodBean;
import org.apache.webbeans.test.AbstractUnitTest;
import org.junit.Test;

import jakarta.annotation.Priority;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.Vetoed;

import static org.junit.Assert.assertTrue;

public class AlternativeWithBeanAndMethodProducerPriorityTest extends AbstractUnitTest
{
    @Test
    public void run()
    {
        startContainer(Prod.class, Injected1.class, Injected2.class, Injected3.class);
        assertTrue(ProducerMethodBean.class.isInstance(getBean(Injected1.class)));
    }

    @Priority(2001)
    public static class Prod {
        @Produces
        @Alternative
        public Injected1 getGreeting()
        {
            return new Injected1() {};
        }
    }

    @Vetoed
    public static class Injected1
    {
        // nothing
    }

    @Priority(1999)
    @Alternative
    public static class Injected2 extends Injected1
    {
        // nothing
    }

    @Priority(2000)
    @Alternative
    public static class Injected3 extends Injected1
    {
        // nothing
    }
}
