/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.webbeans.test.instance;

import org.apache.webbeans.annotation.DefaultLiteral;
import org.apache.webbeans.test.AbstractUnitTest;
import org.junit.Test;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Typed;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;

import static junit.framework.Assert.assertNotNull;

public class InstanceWithTypedTest extends AbstractUnitTest
{
    @Test
    public void testTypedIsRespected()
    {
        Collection<Class<?>> beanClasses = new ArrayList<Class<?>>();
        beanClasses.add(TypedBean.class);
        beanClasses.add(InstanceHolder.class);
        beanClasses.add(RealRunnable.class);

        startContainer(beanClasses, null);

        InstanceHolder instance = getInstance(InstanceHolder.class);

        assertNotNull(instance);
        assertNotNull(instance.getBean());

        shutDownContainer();
    }

    @Test
    public void testDynamicInstanceResolving() {
        startContainer(RealRunnable.class, TypedBean.class, InstanceHolder.class);

        InstanceHolder ih = getInstance(InstanceHolder.class);
        assertNotNull(ih);
        assertNotNull(ih.getBean());

        // now try to trigger the resolving programmatically
        Instance<Runnable> instance = ih.getInstance();
        Runnable r1 = instance.select(DefaultLiteral.INSTANCE).get();
        assertNotNull(r1);

        Instance<Runnable> instance2 = ih.getDefaultRunnableInstance();
        Runnable r2 = instance2.select(DefaultLiteral.INSTANCE).get();
        assertNotNull(r2);
    }

    public static class InstanceHolder
    {
        @Inject
        @Any
        private Instance<Runnable> bean;

        @Inject
        private Instance<Runnable> defaultRunnableInstance;

        public Runnable getBean()
        {
            return bean.get();
        }

        public Instance<Runnable> getInstance()
        {
            return bean;
        }

        public Instance<Runnable> getDefaultRunnableInstance()
        {
            return defaultRunnableInstance;
        }
    }

    public static class RealRunnable implements Runnable
    {
        @Override
        public void run()
        {
            // no-op
        }
    }

    @Typed({ TypedBean.class })
    public static class TypedBean<X> implements Runnable
    {
        @Override
        public void run() {
            // no-op
        }
    }
}
