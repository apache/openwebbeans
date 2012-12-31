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
package org.apache.webbeans.newtests.interceptors.factory;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.webbeans.newtests.AbstractUnitTest;
import org.apache.webbeans.newtests.interceptors.factory.beans.ClassInterceptedClass;
import org.apache.webbeans.proxy.InterceptorDecoratorProxyFactory;

import org.junit.Assert;
import org.junit.Test;


/**
 * Test the {@link org.apache.webbeans.proxy.InterceptorDecoratorProxyFactory}
 */
public class InterceptorDecoratorProxyFactoryTest extends AbstractUnitTest
{

    @Test
    public void textSimpleProxyCreation() throws Exception
    {
        Collection<Class<?>> beanClasses = new ArrayList<Class<?>>();
        startContainer(beanClasses, null);

        InterceptorDecoratorProxyFactory pf = new InterceptorDecoratorProxyFactory(getWebBeansContext());

        ClassLoader classLoader = this.getClass().getClassLoader();

        Class<ClassInterceptedClass> proxyClass = pf.createInterceptorDecoratorProxyClass(classLoader, ClassInterceptedClass.class);
        Assert.assertNotNull(proxyClass);

        ClassInterceptedClass proxy = pf.createProxyInstance(proxyClass, new ClassInterceptedClass(), null);
        Assert.assertNotNull(proxy);

        // we need to get the field from the proxy via reflection
        // otherwise we will end up seeing the proxied method on the internal state
        Field field = proxy.getClass().getSuperclass().getDeclaredField("defaultCtInvoked");
        Assert.assertNotNull(field);
        field.setAccessible(true);

        Boolean isDefaultCtInvoked = (Boolean) field.get(proxy);
        Assert.assertTrue(isDefaultCtInvoked);

        proxy.setMeaningOfLife(42);

        Assert.assertEquals(42, proxy.getMeaningOfLife());


        shutDownContainer();
    }
}
