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
package org.apache.webbeans.test.unittests.producer.broken;

import org.apache.webbeans.test.AbstractUnitTest;
import org.apache.webbeans.test.component.producer.broken.BrokenProducerComponent1;
import org.apache.webbeans.test.component.producer.broken.BrokenProducerComponent2;
import org.apache.webbeans.test.component.producer.broken.BrokenProducerComponent3;
import org.apache.webbeans.test.component.producer.broken.BrokenProducerComponent4;
import org.apache.webbeans.test.component.producer.broken.BrokenProducerComponent5;
import org.apache.webbeans.test.component.producer.broken.BrokenProducerComponent6;
import org.junit.Assert;
import org.junit.Test;

import jakarta.enterprise.inject.spi.DefinitionException;

public class BrokenProducerTest extends AbstractUnitTest
{
    @Test
    public void testBroken1()
    {
        try
        {
            startContainer(BrokenProducerComponent1.class);
            Assert.fail("DefinitionException expected");
        }
        catch (DefinitionException e)
        {
            return; // all ok
        }
    }

    @Test
    public void testBroken2()
    {
        try
        {
            startContainer(BrokenProducerComponent2.class);
            Assert.fail("DefinitionException expected");
        }
        catch (DefinitionException e)
        {
            return; // all ok
        }
    }

    @Test
    public void testBroken3()
    {
        try
        {
            startContainer(BrokenProducerComponent3.class);
            Assert.fail("DefinitionException expected");
        }
        catch (DefinitionException e)
        {
            return; // all ok
        }
    }

    @Test
    public void testBroken4()
    {
        try
        {
            startContainer(BrokenProducerComponent4.class);
            Assert.fail("DefinitionException expected");
        }
        catch (DefinitionException e)
        {
            return; // all ok
        }
    }

    @Test
    public void testBroken5()
    {
        try
        {
            startContainer(BrokenProducerComponent5.class);
            Assert.fail("DefinitionException expected");
        }
        catch (DefinitionException e)
        {
            return; // all ok
        }
    }

    @Test
    public void testBroken6()
    {
        try
        {
            startContainer(BrokenProducerComponent6.class);
            Assert.fail("DefinitionException expected");
        }
        catch (DefinitionException e)
        {
            return; // all ok
        }
    }

}
