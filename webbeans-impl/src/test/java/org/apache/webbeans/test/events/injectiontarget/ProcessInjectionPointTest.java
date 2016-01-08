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
package org.apache.webbeans.test.events.injectiontarget;

import javax.enterprise.context.Dependent;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Typed;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessInjectionPoint;
import javax.inject.Inject;

import org.apache.webbeans.test.AbstractUnitTest;
import org.junit.Assert;
import org.junit.Test;

public class ProcessInjectionPointTest extends AbstractUnitTest
{

    @Test
    public void testConsumerAScanning()
    {
        TestExtension extension = new TestExtension();
        addExtension(extension);
        startContainer(X.class, A.class, B.class, ConsumerA.class);

        Assert.assertTrue(extension.isXInjectionPointParsed());
        Assert.assertTrue(extension.isAInjectionPointParsed());

        Assert.assertFalse(extension.isBInjectionPointParsed());
        Assert.assertFalse(extension.isOtherInjectionPointParsed());
    }

    @Test
    public void testConsumerBScanning()
    {
        TestExtension extension = new TestExtension();
        addExtension(extension);
        startContainer(X.class, A.class, B.class, ConsumerB.class);

        Assert.assertTrue(extension.isXInjectionPointParsed());
        Assert.assertTrue(extension.isAInjectionPointParsed());
        Assert.assertTrue(extension.isBInjectionPointParsed());

        Assert.assertFalse(extension.isOtherInjectionPointParsed());
    }


    @Dependent
    public static class X
    {
        // no content
    }

    @Dependent
    public static class A extends X
    {
        // no content
    }

    @Dependent
    @Typed(B.class)
    public static class B extends A
    {

    }

    @Dependent
    public static class ConsumerA
    {
        private @Inject A a;
    }

    @Dependent
    public static class ConsumerB
    {
        private @Inject B b;
    }

    public static class TestExtension implements Extension
    {
        private boolean xInjectionPointParsed = false;
        private boolean aInjectionPointParsed = false;
        private boolean bInjectionPointParsed = false;
        private boolean otherInjectionPointParsed = false;

        void testX(@Observes ProcessInjectionPoint<?, X> pip)
        {
            xInjectionPointParsed = true;
        }

        void testA(@Observes ProcessInjectionPoint<?, A> pip)
        {
            aInjectionPointParsed = true;
        }

        void testB(@Observes ProcessInjectionPoint<?, B> pip)
        {
            bInjectionPointParsed = true;
        }

        void testOther(@Observes ProcessInjectionPoint<?, String> pip)
        {
            otherInjectionPointParsed = true;
        }


        public boolean isXInjectionPointParsed()
        {
            return xInjectionPointParsed;
        }

        public boolean isAInjectionPointParsed()
        {
            return aInjectionPointParsed;
        }


        public boolean isBInjectionPointParsed()
        {
            return bInjectionPointParsed;
        }

        public boolean isOtherInjectionPointParsed()
        {
            return otherInjectionPointParsed;
        }
    }
}
