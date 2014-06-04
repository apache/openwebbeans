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
package org.apache.webbeans.test.disposes;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.Typed;

import junit.framework.Assert;

import org.apache.webbeans.test.AbstractUnitTest;
import org.junit.Test;

/**
 * Test if static producer methods and disposal methods do work
 */
public class StaticProducerTest extends AbstractUnitTest
{

    @Test
    public void testStaticProducer() throws Exception
    {
        startContainer(ProducerOwner.class);

        ProducerOwner.destroyed = null;

        MyBean myBean = getInstance(MyBean.class);
        Assert.assertEquals("testval", myBean.getVal());
        Assert.assertNotNull(myBean);
        Assert.assertEquals(Boolean.FALSE, ProducerOwner.destroyed);

        getLifecycle().getContextService().endContext(RequestScoped.class, null);

        // now the bean should be destroyed
        Assert.assertEquals(Boolean.TRUE, ProducerOwner.destroyed);

    }


    public static class ProducerOwner
    {
        public static Boolean destroyed = null;

        @Produces
        @RequestScoped
        public static MyBean createVal()
        {
            destroyed = Boolean.FALSE;
            return new MyBean("testval");
        }

        public static void destroyIt(@Disposes MyBean val)
        {
            destroyed = Boolean.TRUE;
        }

    }


    @Typed()
    public static class MyBean
    {
        private String val;

        public MyBean()
        {
        }

        public MyBean(String val)
        {
            this.val = val;
        }

        public String getVal()
        {
            return val;
        }
    }
}
