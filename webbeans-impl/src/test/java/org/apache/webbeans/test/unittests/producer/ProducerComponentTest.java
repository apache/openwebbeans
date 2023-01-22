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
package org.apache.webbeans.test.unittests.producer;

import java.util.List;

import jakarta.enterprise.util.TypeLiteral;

import org.junit.Assert;

import org.apache.webbeans.test.AbstractUnitTest;
import org.apache.webbeans.test.component.producer.ParametrizedModel1;
import org.apache.webbeans.test.component.producer.ParametrizedModel2;
import org.apache.webbeans.test.component.producer.ParametrizedProducer;
import org.apache.webbeans.test.component.producer.Producer4;
import org.apache.webbeans.test.component.producer.Producer4ConsumerComponent;
import org.junit.Test;

public class ProducerComponentTest extends AbstractUnitTest
{

    @Test
    public void testParametrizedProducer()
    {
        startContainer(ParametrizedProducer.class);
        TypeLiteral<List<ParametrizedModel1>> model1 = new TypeLiteral<List<ParametrizedModel1>>()
        {
        };

        List<ParametrizedModel1> instance = getInstance(model1.getType());
        Assert.assertNull(instance);
        Assert.assertTrue(ParametrizedProducer.callModel1);
        Assert.assertTrue(!ParametrizedProducer.callModel2);

        TypeLiteral<List<ParametrizedModel2>> model2 = new TypeLiteral<List<ParametrizedModel2>>()
        {
        };
        List<ParametrizedModel2> instance2 = getInstance(model2.getType());

        Assert.assertNull(instance2);
        Assert.assertTrue(ParametrizedProducer.callModel2);
    }

    @Test
    public void testProducer4()
    {
        startContainer(Producer4.class, Producer4ConsumerComponent.class);

        Producer4ConsumerComponent instance = getInstance(Producer4ConsumerComponent.class);

        int count = instance.count();

        Assert.assertEquals(1, count);
    }

}
