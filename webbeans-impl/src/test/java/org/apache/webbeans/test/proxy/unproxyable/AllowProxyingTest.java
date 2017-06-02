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
package org.apache.webbeans.test.proxy.unproxyable;

import javax.enterprise.context.Dependent;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.UnproxyableResolutionException;
import javax.enterprise.inject.spi.DefinitionException;
import javax.inject.Inject;
import java.util.HashMap;

import org.junit.Assert;
import org.apache.webbeans.config.OpenWebBeansConfiguration;
import org.apache.webbeans.test.AbstractUnitTest;
import org.junit.Before;
import org.junit.Test;

public class AllowProxyingTest extends AbstractUnitTest
{

    @Before
    public void resetSettings()
    {
        System.setProperty(OpenWebBeansConfiguration.ALLOW_PROXYING_PARAM, "");
        // there is no portable way to set environment variables from within java.
        // and this is probably a good thing ;)
    }


    @Test(expected = UnproxyableResolutionException.class)
    public void testNonProxyableException()
    {
        startContainer(SomeClassWithFinalMethods.class);

        // should blow up with UnproxyableResolutionException wrapped in a DefinitionException
        getInstance(SomeClassWithFinalMethods.class);
    }

    @Test
    public void testAllowProxyingDefaults()
    {
        startContainer(ProducerOwner.class, HashMapConsumer.class);
        HashMapConsumer consumer = getInstance(HashMapConsumer.class);
        Assert.assertEquals("B", consumer.getA());
    }

    @Test
    public void testAllowProxyingViaSystemProps()
    {
        System.setProperty(OpenWebBeansConfiguration.ALLOW_PROXYING_PARAM,
            "some.other.Class," + SomeClassWithFinalMethods.class.getName());

        startContainer(SomeClassWithFinalMethods.class);

        // should blow up with UnproxyableResolutionException wrapped in a DefinitionException
        SomeClassWithFinalMethods instance = getInstance(SomeClassWithFinalMethods.class);
        Assert.assertEquals("X", instance.getX());
    }

    @Test
    public void testAllowProxyingViaBeansXml()
    {
        startContainer("org/apache/webbeans/test/xml/allowproxying/allowproxying.xml", SomeClassWithFinalMethods.class);

        // should blow up with UnproxyableResolutionException wrapped in a DefinitionException
        SomeClassWithFinalMethods instance = getInstance(SomeClassWithFinalMethods.class);
        Assert.assertEquals("X", instance.getX());
    }



    @Dependent
    public static class ProducerOwner
    {
        @Produces
        @RequestScoped
        public HashMap produceHashMap()
        {
            HashMap hm = new HashMap();
            hm.put("A", "B");
            return hm;
        }
    }

    @RequestScoped
    public static class HashMapConsumer
    {
        private @Inject HashMap theHashMap;

        public String getA()
        {
            return (String) theHashMap.get("A");
        }
    }

    @RequestScoped
    public static class SomeClassWithFinalMethods
    {
        public String getX()
        {
            return "X";
        }

        final void crazyFinalMethod()
        {
            // nothing to do
        }
    }
}
