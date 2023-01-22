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
package org.apache.webbeans.test.unittests.event.component;

import org.junit.Assert;

import org.apache.webbeans.test.AbstractUnitTest;
import org.apache.webbeans.test.component.event.broken.BrokenObserverComponent1;
import org.apache.webbeans.test.component.event.broken.BrokenObserverComponent2;
import org.apache.webbeans.test.component.event.broken.BrokenObserverComponent3;
import org.apache.webbeans.test.component.event.broken.BrokenObserverComponent4;
import org.apache.webbeans.test.component.event.broken.BrokenObserverComponent5;
import org.apache.webbeans.test.component.event.broken.BrokenObserverComponent6;
import org.junit.Test;

import jakarta.enterprise.inject.spi.DefinitionException;

public class BrokenComponentTest extends AbstractUnitTest
{
    @Test
    public void test1()
    {
        Exception exc = null;

        try
        {
            startContainer(BrokenObserverComponent1.class);

        }
        catch (Exception e)
        {
            System.out.println(e.getMessage());
            exc = e;
        }
    }

    @Test
    public void test2()
    {
        try
        {
            startContainer(BrokenObserverComponent2.class);
            Assert.fail("DefinitionException expected");
        }
        catch (DefinitionException e)
        {
            return; // all ok
        }
    }

    @Test
    public void test3()
    {
        try
        {
            startContainer(BrokenObserverComponent3.class);
            Assert.fail("DefinitionException expected");

        }
        catch (DefinitionException e)
        {
            return; // all ok
        }
    }

    @Test
    public void test4()
    {
        try
        {
            startContainer(BrokenObserverComponent4.class);
            Assert.fail("DefinitionException expected");

        }
        catch (DefinitionException e)
        {
            return; // all ok
        }
    }

    @Test
    public void test5()
    {
        try
        {
            startContainer(BrokenObserverComponent5.class);
            Assert.fail("DefinitionException expected");
        }
        catch (DefinitionException e)
        {
            return; // all ok
        }
    }

    @Test
    public void test6()
    {
        try
        {
            startContainer(BrokenObserverComponent6.class);
            Assert.fail("DefinitionException expected");
        }
        catch (DefinitionException e)
        {
            return; // all ok
        }
    }

}
