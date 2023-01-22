/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.webbeans.test.injection.typed;

import jakarta.enterprise.inject.spi.Bean;
import java.util.Set;

import org.junit.Assert;
import org.apache.webbeans.test.AbstractUnitTest;
import org.junit.Test;

public class TypedProducerTest extends AbstractUnitTest
{

    @Test
    public void testTypedProducerMethod()
    {
        startContainer(Bird.class, Raven.class, TypedMethodProducer.class);

        Raven raven = getInstance(Raven.class);
        Assert.assertNotNull(raven);

        Set<Bean<?>> birdBeans = getBeanManager().getBeans(Bird.class);
        Assert.assertTrue(birdBeans.isEmpty());
    }

    @Test
    public void testTypedProducerField()
    {
        startContainer(Bird.class, Raven.class, TypedFieldProducer.class);

        Raven raven = getInstance(Raven.class);
        Assert.assertNotNull(raven);

        Set<Bean<?>> birdBeans = getBeanManager().getBeans(Bird.class);
        Assert.assertTrue(birdBeans.isEmpty());
    }
}
