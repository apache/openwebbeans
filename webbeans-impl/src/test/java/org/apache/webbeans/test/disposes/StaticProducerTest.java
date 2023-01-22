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

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.Typed;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.util.AnnotationLiteral;

import org.junit.Assert;

import org.apache.webbeans.test.AbstractUnitTest;
import org.apache.webbeans.test.annotation.binding.Users;
import org.junit.Test;

/**
 * Test if static producer methods and disposal methods do work
 */
public class StaticProducerTest extends AbstractUnitTest
{

    @Test
    public void testStaticProducerOnBean() throws Exception
    {
        startContainer(ProducerOwner.class);

        ProducerOwner.destroyed = null;

        MyBean myBean = getInstance(MyBean.class);
        Assert.assertNotNull(myBean);
        Assert.assertEquals("testval", myBean.getVal());
        Assert.assertEquals(Boolean.FALSE, ProducerOwner.destroyed);

        getLifecycle().getContextService().endContext(RequestScoped.class, null);

        // now the bean should be destroyed
        Assert.assertEquals(Boolean.TRUE, ProducerOwner.destroyed);

        getLifecycle().getContextService().startContext(RequestScoped.class, null);


        ProducerOwner.destroyed = null;
        Bean<MyBean> bean = getBean(MyBean.class);
        CreationalContext<MyBean> cc = getBeanManager().createCreationalContext(bean);
        MyBean myBean1 = (MyBean) getBeanManager().getReference(bean, MyBean.class, cc);
        Assert.assertEquals("testval", myBean1.getVal());
        Assert.assertNotNull(myBean1);
        Assert.assertEquals(Boolean.FALSE, ProducerOwner.destroyed);

        bean.destroy(myBean1, cc);
        // now the bean should be destroyed
        Assert.assertEquals(Boolean.TRUE, ProducerOwner.destroyed);
    }

    @Test
    public void testStaticProducerOnString() throws Exception
    {
        startContainer(ProducerOwner.class);

        ProducerOwner.destroyed = null;
        Bean<String> bean = getBean(String.class);
        CreationalContext<String> cc = getBeanManager().createCreationalContext(bean);
        String myVal = (String) getBeanManager().getReference(bean, String.class, cc);
        Assert.assertNotNull(myVal);
        Assert.assertEquals("testval", myVal);
        Assert.assertEquals(Boolean.FALSE, ProducerOwner.destroyed);

        bean.destroy(myVal, cc);
        // now the bean should be destroyed
        Assert.assertEquals(Boolean.TRUE, ProducerOwner.destroyed);
    }

    @Test
    public void testStaticUserProducerOnString() throws Exception
    {
        startContainer(ProducerOwner.class);

        ProducerOwner.destroyed = null;
        Bean<String> bean = getBean(String.class, new AnnotationLiteral<Users>() {});
        CreationalContext<String> cc = getBeanManager().createCreationalContext(bean);
        String myVal = (String) getBeanManager().getReference(bean, String.class, cc);
        Assert.assertNotNull(myVal);
        Assert.assertEquals("user", myVal);
        Assert.assertEquals(Boolean.FALSE, ProducerOwner.destroyed);

        bean.destroy(myVal, cc);
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

        public static void destroyMyBean(@Disposes MyBean val)
        {
            destroyed = Boolean.TRUE;
        }


        @Produces
        @Dependent
        public static String createString()
        {
            destroyed = Boolean.FALSE;
            return "testval";
        }

        public static void destroyString(@Disposes String val)
        {
            destroyed = Boolean.TRUE;
        }

        @Produces
        @Dependent
        @Users
        public static String createUserString()
        {
            destroyed = Boolean.FALSE;
            return "user";
        }

        public static void destroyUserString(@Disposes @Users String val)
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
