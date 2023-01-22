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
package org.apache.webbeans.test.producer;

import java.util.ArrayList;
import java.util.Collection;

import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;

import org.apache.webbeans.test.AbstractUnitTest;
import org.junit.Assert;
import org.junit.Test;

public class NamedProducerTest extends AbstractUnitTest
{

    @Test
    public void testNamedProducer()
    {
        Collection<String> beanXmls = new ArrayList<String>();
        Collection<Class<?>> beanClasses = new ArrayList<Class<?>>();

        beanClasses.add(ProducerBean.class);
        beanClasses.add(ConsumerBean.class);
        
        startContainer(beanClasses, beanXmls);        

        Bean<?> consumerBean = getBeanManager().getBeans(ConsumerBean.class).iterator().next();
        CreationalContext<?> context = getBeanManager().createCreationalContext(consumerBean);
        ConsumerBean consumer = (ConsumerBean) getBeanManager().getReference(consumerBean, ConsumerBean.class, context);
        
        Assert.assertEquals("name1", consumer.getName1());
        Assert.assertEquals("name2", consumer.getName2());
        Assert.assertEquals(true, consumer.isName3());
        Assert.assertEquals("name4", consumer.getName4());
        Assert.assertEquals("name5", consumer.getName5());
        Assert.assertEquals(true, consumer.isName6());
        
        shutDownContainer();       
        
    }
}
