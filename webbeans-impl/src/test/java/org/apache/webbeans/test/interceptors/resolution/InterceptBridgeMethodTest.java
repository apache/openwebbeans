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
package org.apache.webbeans.test.interceptors.resolution;

import java.util.ArrayList;
import java.util.Collection;

import jakarta.enterprise.inject.spi.Bean;

import org.apache.webbeans.test.AbstractUnitTest;
import org.apache.webbeans.test.interceptors.resolution.beans.Foo;
import org.apache.webbeans.test.interceptors.resolution.beans.FooImpl;
import org.apache.webbeans.test.interceptors.resolution.beans.UtilitySampleBean;
import org.apache.webbeans.test.interceptors.resolution.interceptors.TestIntercepted1;
import org.apache.webbeans.test.interceptors.resolution.interceptors.TestInterceptor1;
import org.apache.webbeans.test.interceptors.resolution.interceptors.TestInterceptorParent;
import org.junit.Assert;
import org.junit.Test;

public class InterceptBridgeMethodTest extends AbstractUnitTest
{

    @Test
    public final void testMethodLevelInterceptorOnBridgeMethod()
    {
        Collection<String> beanXmls = new ArrayList<String>();
        beanXmls.add(getXmlPath(this.getClass().getPackage().getName(), InterceptorResolutionServiceTest.class.getSimpleName()));

        Collection<Class<?>> beanClasses = new ArrayList<Class<?>>();
        beanClasses.add(TestIntercepted1.class);
        beanClasses.add(TestInterceptor1.class);
        beanClasses.add(TestInterceptorParent.class);
        beanClasses.add(UtilitySampleBean.class);

        beanClasses.add(Foo.class);
        beanClasses.add(FooImpl.class);

        startContainer(beanClasses, beanXmls);

        final Bean<FooImpl> bean = getBean(FooImpl.class);
        FooImpl instance = FooImpl.class.cast(getBeanManager().getReference(bean, FooImpl.class, null));
        Assert.assertNotNull(instance);

        instance.doSomething("test1");

        // Ensure the method call was intercepted, and value was set
        Assert.assertEquals(TestInterceptor1.invocationCount, 1);
        Assert.assertEquals(instance.getValue(), "test1");

        // Cast FooImpl to Foo, testing if the call becoming doSomething(Object)
        // breaks the interception
        Foo castInstance = (Foo) instance;
        castInstance.doSomething("test2");

        // Ensure the method call on to doSomethign(Object) was intercepted, and
        // value was set
        Assert.assertEquals(TestInterceptor1.invocationCount, 2);
        Assert.assertEquals(instance.getValue(), "test2");
        
        shutDownContainer();
    }
}
