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

import org.apache.webbeans.newtests.interceptors.factory.beans.ClassInterceptedClass;
import org.apache.webbeans.proxy.asm.InterceptorDecoratorProxyFactory;

import org.junit.Assert;
import org.junit.Test;


/**
 * Test the {@link org.apache.webbeans.proxy.asm.InterceptorDecoratorProxyFactory}
 */
public class InterceptorDecoratorProxyFactoryTest
{

    @Test
    public void textSimpleProxyCreation() throws Exception
    {
        InterceptorDecoratorProxyFactory pf = new InterceptorDecoratorProxyFactory();

        ClassLoader classLoader = this.getClass().getClassLoader();

        Class<ClassInterceptedClass> proxyClass = pf.createInterceptorDecoratorProxyClass(classLoader, ClassInterceptedClass.class);
        Assert.assertNotNull(proxyClass);

        ClassInterceptedClass instance = proxyClass.newInstance();
        Assert.assertNotNull(instance);
        Assert.assertTrue(instance.defaultCtInvoked);

        instance.setMeaningOfLife(42);

        Assert.assertEquals(42, instance.getMeaningOfLife());
    }
}
