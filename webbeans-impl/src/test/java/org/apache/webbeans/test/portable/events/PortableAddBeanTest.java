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
package org.apache.webbeans.test.portable.events;

import static java.util.Collections.emptyList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.util.ArrayList;
import java.util.Collection;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.BeforeBeanDiscovery;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.inject.Inject;

import org.junit.Assert;

import org.apache.webbeans.test.AbstractUnitTest;
import org.apache.webbeans.test.portable.events.extensions.AddBeanExtension;
import org.junit.Test;

public class PortableAddBeanTest extends AbstractUnitTest
{

    @Test
    public void testAddBeanExtension()
    {
        Collection<String> beanXmls = new ArrayList<String>();
        
        Collection<Class<?>> beanClasses = new ArrayList<Class<?>>();

        addExtension(new AddBeanExtension());  
        addExtension(new AddBeanExtension.MyBeanExtension());
        
        startContainer(beanClasses, beanXmls);
        
        Assert.assertNotNull(AddBeanExtension.MyBeanExtension.myBean);
        
        shutDownContainer();
    }

    @Test
    public void testAddBeanExtensionWithProduceInsteadOfCreateCallback()
    {
        final MyBean myBean = new MyBean();
        addExtension(new Extension()
        {
            void addType(@Observes BeforeBeanDiscovery beforeBeanDiscovery)
            {
                beforeBeanDiscovery.addAnnotatedType(Injected.class, Injected.class.getName());
            }

            void addBean(@Observes AfterBeanDiscovery event)
            {
                event.addBean()
                        .scope(Dependent.class)
                        .addTypes(MyBean.class, Object.class)
                        .beanClass(MyBean.class)
                        .produceWith(i -> {
                            assertNull(myBean.ip);
                            myBean.ip = i.select(InjectionPoint.class).get();
                            return myBean;
                        });
            }
        });
        startContainer(emptyList(), emptyList());

        final Injected instance = getInstance(Injected.class);
        assertSame(myBean, instance.bean);
        assertNotNull(instance.bean.ip);
        assertEquals(MyBean.class, instance.bean.ip.getType());
        assertEquals("bean", instance.bean.ip.getMember().getName());
    }

    public static class Injected
    {
        @Inject
        private MyBean bean;
    }

    public static class MyBean
    {
        private InjectionPoint ip;
    }
}
