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

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Typed;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.BeforeBeanDiscovery;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ProcessInjectionPoint;
import jakarta.inject.Inject;

import org.apache.webbeans.test.AbstractUnitTest;
import org.junit.Assert;
import org.junit.Test;

public class ProcessInjectionPointTest extends AbstractUnitTest
{
    @Test
    public void testBeforeBeanDiscoveryMustNotTriggerProcessInjectionPoint()
    {
        addExtension(new TestExtension2());
        startContainer(X.class);

        TestExtension2 extensionBeanInstance = getInstance(TestExtension2.class);
        Assert.assertNotNull(extensionBeanInstance);
    }

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

        TestExtension extensionBeanInstance = getInstance(TestExtension.class);
        Assert.assertNotNull(extensionBeanInstance);
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

    @Test
    public void testConsumerInterfaceScanning()
    {
        TestExtension extension = new TestExtension();
        addExtension(extension);
        startContainer(SomeInterface.class, SomeImpl.class, ConsumerInterface.class);

        Assert.assertFalse(extension.isXInjectionPointParsed());
        Assert.assertFalse(extension.isAInjectionPointParsed());
        Assert.assertFalse(extension.isBInjectionPointParsed());
        Assert.assertFalse(extension.isOtherInjectionPointParsed());

        Assert.assertTrue(extension.isInterfaceInjectionPointParsed());
        Assert.assertFalse(extension.isImplInjectionPointParsed());
    }

    @Test
    public void testConsumerImplScanning()
    {
        TestExtension extension = new TestExtension();
        addExtension(extension);
        startContainer(SomeInterface.class, SomeImpl.class, ConsumerImpl.class);

        Assert.assertFalse(extension.isXInjectionPointParsed());
        Assert.assertFalse(extension.isAInjectionPointParsed());
        Assert.assertFalse(extension.isBInjectionPointParsed());
        Assert.assertFalse(extension.isOtherInjectionPointParsed());

        Assert.assertTrue(extension.isInterfaceInjectionPointParsed());
        Assert.assertTrue(extension.isImplInjectionPointParsed());
    }

    @Test
    public void testConsumerRawInstanceScanning()
    {
        TestExtension extension = new TestExtension();
        addExtension(extension);
        startContainer(SomeInterface.class, SomeImpl.class, ConsumerRawInstance.class);

        Assert.assertFalse(extension.isXInjectionPointParsed());
        Assert.assertFalse(extension.isAInjectionPointParsed());
        Assert.assertFalse(extension.isBInjectionPointParsed());
        Assert.assertFalse(extension.isOtherInjectionPointParsed());
        Assert.assertFalse(extension.isInterfaceInjectionPointParsed());
        Assert.assertFalse(extension.isImplInjectionPointParsed());

        Assert.assertTrue(extension.isWildcardInstanceInjectionPointParsed());
        Assert.assertTrue(extension.isExplicitInstanceInjectionPointParsed());
        Assert.assertTrue(extension.isRawInstanceInjectionPointParsed());
    }

    public interface SomeInterface
    {
        void doSomething();
    }

    public static class SomeImpl implements SomeInterface
    {
        @Override
        public void doSomething()
        {
            // nop
        }
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

    @Dependent
    public static class ConsumerInterface
    {
        private @Inject SomeInterface someInterface;
    }

    @Dependent
    public static class ConsumerImpl
    {
        private @Inject SomeImpl someImpl;
    }

    @Dependent
    public static class ConsumerRawInstance
    {
        private @Inject Instance<SomeImpl> someImplInstance;
    }


    public static class TestExtension implements Extension
    {
        private boolean xInjectionPointParsed = false;
        private boolean aInjectionPointParsed = false;
        private boolean bInjectionPointParsed = false;
        private boolean otherInjectionPointParsed = false;
        private boolean interfaceInjectionPointParsed = false;
        private boolean implInjectionPointParsed = false;
        private boolean rawInstanceInjectionPointParsed = false;
        private boolean wildcardInstanceInjectionPointParsed = false;
        private boolean explicitInstanceInjectionPointParsed = false;

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

        void testInterface(@Observes ProcessInjectionPoint<?, SomeInterface> pip)
        {
            interfaceInjectionPointParsed = true;
        }

        void testImpl(@Observes ProcessInjectionPoint<?, SomeImpl> pip)
        {
            implInjectionPointParsed = true;
        }

        void testRawInstance(@Observes ProcessInjectionPoint<?, Instance> pip)
        {
            rawInstanceInjectionPointParsed = true;
        }

        void testRawInstanceWithWildCard(@Observes ProcessInjectionPoint<?, Instance<?>> pip)
        {
            wildcardInstanceInjectionPointParsed = true;
        }

        void testExplicitInstance(@Observes ProcessInjectionPoint<?, Instance<SomeImpl>> pip)
        {
            explicitInstanceInjectionPointParsed = true;
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

        public boolean isInterfaceInjectionPointParsed()
        {
            return interfaceInjectionPointParsed;
        }

        public boolean isImplInjectionPointParsed()
        {
            return implInjectionPointParsed;
        }

        public boolean isRawInstanceInjectionPointParsed()
        {
            return rawInstanceInjectionPointParsed;
        }

        public boolean isWildcardInstanceInjectionPointParsed()
        {
            return wildcardInstanceInjectionPointParsed;
        }

        public boolean isExplicitInstanceInjectionPointParsed()
        {
            return explicitInstanceInjectionPointParsed;
        }
    }

    public static class TestExtension2 implements Extension
    {
        void TestWeirdBeforeBeanDiscovery(@Observes BeforeBeanDiscovery bbd, BeanManager unusedBeanManager)
        {
            // all fine, this is just to test an old problem with firing
            // ProcessInjectionPoint for Extension observers as well.
            // Which created a problem because the Extensions are not yet ready.
        }

        void testExplicitInstance(@Observes ProcessInjectionPoint<?, ?> pip, BeanManager unusedBeanManager)
        {
            // nothing to do
        }


    }
}