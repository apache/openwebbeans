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

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessBean;
import javax.enterprise.inject.spi.ProcessBeanAttributes;
import java.util.function.Consumer;

import org.apache.webbeans.test.AbstractUnitTest;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BeanAttributesConfiguratorImplTest extends AbstractUnitTest
{

    @Test
    public void testAddScope()
    {
        checkBeanAttributes(
            pba ->
                pba.configureBeanAttributes()
                    .scope(ApplicationScoped.class),
            pb ->
                assertEquals(ApplicationScoped.class, pb.getBean().getScope()),
            EmptyBean.class);
    }

    private void checkBeanAttributes(Consumer<ProcessBeanAttributes<Marker>> beanAttributeConfigurator,
                                     Consumer<ProcessBean<Marker>> beanConsumer,
                                     Class<?> classToCheck)
    {
        CheckBeanAttributesExtension extension
            = new CheckBeanAttributesExtension(beanAttributeConfigurator, beanConsumer);
        addExtension(extension);
        startContainer(classToCheck);
        shutdown();
    }

    public static class CheckBeanAttributesExtension implements Extension
    {
        private final Consumer<ProcessBeanAttributes<Marker>> beanAttributeConfigurator;
        private final Consumer<ProcessBean<Marker>> beanConsumer;

        public CheckBeanAttributesExtension(Consumer<ProcessBeanAttributes<Marker>> beanAttributeConfigurator, Consumer<ProcessBean<Marker>> beanConsumer)
        {
            this.beanAttributeConfigurator = beanAttributeConfigurator;
            this.beanConsumer = beanConsumer;
        }

        public void processBeanAttributes(@Observes ProcessBeanAttributes<Marker> processBeanAttributes)
        {
            beanAttributeConfigurator.accept(processBeanAttributes);
        }

        public void processBean(@Observes ProcessBean<Marker> processBean)
        {
            beanConsumer.accept(processBean);
        }
    }

    public interface Marker
    {
    }


    public static class EmptyBean implements Marker
    {

    }

}
