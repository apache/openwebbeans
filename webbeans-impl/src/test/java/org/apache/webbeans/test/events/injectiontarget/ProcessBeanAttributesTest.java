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
package org.apache.webbeans.test.events.injectiontarget;

import org.apache.webbeans.test.AbstractUnitTest;
import org.junit.Test;

import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.Annotated;
import jakarta.enterprise.inject.spi.AnnotatedField;
import jakarta.enterprise.inject.spi.AnnotatedMethod;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ProcessBeanAttributes;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ProcessBeanAttributesTest extends AbstractUnitTest
{
    @Test
    public void checkTypeIsTakenIntoAccount() throws Exception
    {
        final ProcessBeanAttributesExtension processBeanAttributesExtension = new ProcessBeanAttributesExtension();
        addExtension(processBeanAttributesExtension);
        startContainer(SomeBean.class, SomeOtherBean.class);

        assertEquals(1, processBeanAttributesExtension.someBean.size());
        assertEquals(1, processBeanAttributesExtension.someOtherBean.size());
        assertEquals(1, processBeanAttributesExtension.producer.size());
    }

    @Test
    public void checkProducerMethod()
    {
        ProcessBeanAttributesExtension extension = new ProcessBeanAttributesExtension();
        addExtension(extension);
        startContainer(SomeOtherBean.class);

        assertNotNull(extension.annotatedMethod);
        assertTrue(extension.annotatedMethod instanceof AnnotatedMethod);
        assertEquals("producer", ((AnnotatedMethod) extension.annotatedMethod).getJavaMember().getName());
        assertEquals(String.class, ((AnnotatedMethod) extension.annotatedMethod).getJavaMember().getReturnType());
    }

    @Test
    public void checkProducerField()
    {
        ProcessBeanAttributesExtension extension = new ProcessBeanAttributesExtension();
        addExtension(extension);
        startContainer(SomeOtherBean.class);

        assertNotNull(extension.annotatedField);
        assertTrue(extension.annotatedField instanceof AnnotatedField);
        assertEquals("realMeaningOfLife", ((AnnotatedField) extension.annotatedField).getJavaMember().getName());
        assertEquals(Integer.class, ((AnnotatedField) extension.annotatedField).getJavaMember().getType());

    }

    public static class ProcessBeanAttributesExtension implements Extension
    {
        private Collection<ProcessBeanAttributes<?>> someBean = new ArrayList<ProcessBeanAttributes<?>>();
        private Collection<ProcessBeanAttributes<?>> someOtherBean = new ArrayList<ProcessBeanAttributes<?>>();
        private Collection<ProcessBeanAttributes<?>> producer = new ArrayList<ProcessBeanAttributes<?>>();

        private Annotated annotatedMethod;
        private Annotated annotatedField;

        public void someBean(@Observes ProcessBeanAttributes<SomeBean> pba)
        {
            someBean.add(pba);
        }

        public void producer(@Observes ProcessBeanAttributes<String> pba)
        {
            producer.add(pba);
            annotatedMethod = pba.getAnnotated();
        }

        public void otherBean(@Observes ProcessBeanAttributes<SomeOtherBean> pba)
        {
            someOtherBean.add(pba);
        }

        public void fieldProducer(@Observes ProcessBeanAttributes<Integer> pba)
        {
            annotatedField = pba.getAnnotated();
        }
    }

    @RequestScoped
    public static class SomeBean
    {
        private @Inject SomeOtherBean someOtherBean;

        public SomeOtherBean getSomeOtherBean()
        {
            return someOtherBean;
        }
    }

    @RequestScoped
    public static class SomeOtherBean
    {
        @Produces
        protected Integer realMeaningOfLife = 30;

        private int meaningOfLife = 42;


        public int getMeaningOfLife()
        {
            return meaningOfLife;
        }

        @Produces
        public String producer() {
            return "producer";
        }

        public void setMeaningOfLife(int meaningOfLife)
        {
            this.meaningOfLife = meaningOfLife;
        }
    }
}
