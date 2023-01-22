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
package org.apache.webbeans.test.producer;

import org.apache.webbeans.test.AbstractUnitTest;
import org.junit.Assert;
import org.junit.Test;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.SessionScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.DefinitionException;
import jakarta.inject.Inject;
import java.io.Serializable;

/**
 * Tests passivation capability detection for producerMethods
 */
public class ProducerPassivationTest extends AbstractUnitTest
{
    @Test
    public void testNonSerializableInterfaceWithSerializableImplWorks()
    {
        startContainer(SerializableImplProducerOwner.class);

        NonSerializableInterface instance = getInstance(NonSerializableInterface.class);
        Assert.assertNotNull(instance);
    }

    @Test
    public void testNonSerializableBaseClassWithSerializableImplWorks()
    {
        startContainer(SerializableExtendedClassProducerOwner.class);

        NonSerializableBaseClass instance = getInstance(NonSerializableBaseClass.class);
        Assert.assertNotNull(instance);
    }

    @Test
    public void testNonSerializableInterfaceInjectionWorks()
    {
        startContainer(SerializableImplProducerOwner.class, NonSerializableImplHolder.class);

        NonSerializableImplHolder instance = getInstance(NonSerializableImplHolder.class);
        Assert.assertNotNull(instance);
        Assert.assertNotNull(instance.getInstance());
    }

    @Test
    public void testDependentNonSerializableInterfaceInjectionWorks()
    {
        startContainer(DependentSerializableImplProducerOwner.class, NonSerializableImplHolder.class);

        NonSerializableImplHolder instance = getInstance(NonSerializableImplHolder.class);
        Assert.assertNotNull(instance);
        Assert.assertNotNull(instance.getInstance());
    }

    @Test(expected = DefinitionException.class)
    public void testBrokenNonPassivatingProducer()
    {
        startContainer(NonSerializableFinalImplProducerOwner.class);
    }



    public static interface NonSerializableInterface
    {
    }

    public static class SerializableImpl implements NonSerializableInterface, Serializable
    {
    }

    public static class SerializableImplProducerOwner
    {
        @Produces
        @SessionScoped
        public NonSerializableInterface createSerializableImpl()
        {
            return new SerializableImpl(); // all fine as the actual impl is Serializable at runtime
        }
    }

    public static class DependentSerializableImplProducerOwner
    {
        @Produces
        @Dependent
        public NonSerializableInterface createSerializableImpl()
        {
            return new SerializableImpl(); // all fine as the actual impl is Serializable at runtime
        }
    }

    @SessionScoped
    public static class NonSerializableImplHolder implements Serializable
    {
        private @Inject NonSerializableInterface instance;

        public NonSerializableInterface getInstance()
        {
            return instance;
        }
    }

    public static final class NonSerializableImpl implements NonSerializableInterface
    {
    }

    public static class NonSerializableFinalImplProducerOwner
    {
        // this should get detected at boot time already
        @Produces
        @SessionScoped
        public NonSerializableImpl createNonSerializableFinalImpl()
        {
            return new NonSerializableImpl();
        }
    }

    public static class NonSerializableBaseClass
    {
    }

    public static class SerializableExtendedClass extends NonSerializableBaseClass
    {
    }

    public static class SerializableExtendedClassProducerOwner
    {
        // this should get detected at boot time already
        @Produces
        @SessionScoped
        public NonSerializableBaseClass createSerializableExtendedClass()
        {
            return new SerializableExtendedClass();
        }
    }

}
