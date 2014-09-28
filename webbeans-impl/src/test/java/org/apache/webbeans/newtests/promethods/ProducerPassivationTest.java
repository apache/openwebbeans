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
package org.apache.webbeans.newtests.promethods;

import javax.enterprise.context.Dependent;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.newtests.AbstractUnitTest;
import org.junit.Test;

/**
 * Tests passivation capability detection for producerMethods
 */
public class ProducerPassivationTest extends AbstractUnitTest
{
    @Test
    public void testNonSerializableInterfaceWithSerializableImplWorks()
    {
        List<Class<?>> classes = new ArrayList<Class<?>>();
        classes.add(SerializableImplProducerOwner.class);
        startContainer(classes);

        NonSerializableInterface instance = getInstance(NonSerializableInterface.class);
        Assert.assertNotNull(instance);
    }

    @Test
    public void testNonSerializableBaseClassWithSerializableImplWorks()
    {
        List<Class<?>> classes = new ArrayList<Class<?>>();
        classes.add(SerializableExtendedClassProducerOwner.class);
        startContainer(classes);

        NonSerializableBaseClass instance = getInstance(NonSerializableBaseClass.class);
        Assert.assertNotNull(instance);
    }

    @Test
    public void testNonSerializableInterfaceInjectionWorks()
    {
        List<Class<?>> classes = new ArrayList<Class<?>>();
        classes.add(SerializableImplProducerOwner.class);
        classes.add(NonSerializableImplHolder.class);
        startContainer(classes);

        NonSerializableImplHolder instance = getInstance(NonSerializableImplHolder.class);
        Assert.assertNotNull(instance);
        Assert.assertNotNull(instance.getInstance());
    }

    @Test
    public void testDependentNonSerializableInterfaceInjectionWorks()
    {
        List<Class<?>> classes = new ArrayList<Class<?>>();
        classes.add(DependentSerializableImplProducerOwner.class);
        classes.add(NonSerializableImplHolder.class);
        startContainer(classes);

        NonSerializableImplHolder instance = getInstance(NonSerializableImplHolder.class);
        Assert.assertNotNull(instance);
        Assert.assertNotNull(instance.getInstance());
    }

    @Test(expected = WebBeansConfigurationException.class)
    public void testBrokenNonPassivatingProducer()
    {
        List<Class<?>> classes = new ArrayList<Class<?>>();
        classes.add(NonSerializableFinalImplProducerOwner.class);
        startContainer(classes);
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


    public static class NonSerializableImplHolder
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
