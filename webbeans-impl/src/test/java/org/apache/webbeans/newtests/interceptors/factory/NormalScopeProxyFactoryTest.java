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

import javax.inject.Provider;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

import org.apache.webbeans.newtests.AbstractUnitTest;
import org.apache.webbeans.newtests.interceptors.factory.beans.ClassInterceptedClass;
import org.apache.webbeans.proxy.NormalScopeProxyFactory;
import org.apache.webbeans.util.ClassUtil;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test for the {@link NormalScopeProxyFactory}
 */
public class NormalScopeProxyFactoryTest extends AbstractUnitTest
{

    @Test
    public void textSimpleProxyCreation() throws Exception
    {
        NormalScopeProxyFactory pf = new NormalScopeProxyFactory();

        // we take a fresh URLClassLoader to not blur the test classpath with synthetic classes.
        ClassLoader classLoader = new URLClassLoader(new URL[0]);

        List<Method> methods = ClassUtil.getNonPrivateMethods(ClassInterceptedClass.class);

        Method[] nonInterceptedMethods = methods.toArray(new Method[methods.size()]);;
        //X Method[] nonInterceptedMethods = new Method[]{methods.get(0)};

        Class<ClassInterceptedClass> proxyClass = pf.createProxyClass(classLoader, ClassInterceptedClass.class);
        Assert.assertNotNull(proxyClass);

        ClassInterceptedClass internalInstance = new ClassInterceptedClass();
        internalInstance.init();

        TestContextualInstanceProvider provider = new TestContextualInstanceProvider(internalInstance);

        ClassInterceptedClass proxy = pf.createProxyInstance(proxyClass, provider);

        Assert.assertEquals(42, proxy.getMeaningOfLife());
        Assert.assertTrue(provider.gotInvoked());

        Assert.assertEquals(internalInstance.getFloat(), proxy.getFloat(), 0f);
        Assert.assertTrue(provider.gotInvoked());

        Assert.assertEquals('c', proxy.getChar());
        Assert.assertTrue(provider.gotInvoked());

        Assert.assertEquals(internalInstance, proxy.getSelf());
        Assert.assertTrue(provider.gotInvoked());

        try
        {
            proxy.doThaBlowup();
            Assert.fail("NumberFormatException expected!");
        }
        catch (NumberFormatException nfe)
        {
            Assert.assertEquals("should fit", nfe.getMessage());
        }

    }


    public static class TestContextualInstanceProvider<T> implements Provider<T>
    {
        private T instance;
        private boolean gotInvoked = false;

        public TestContextualInstanceProvider(T instance)
        {
            this.instance = instance;
        }

        public boolean gotInvoked()
        {
            boolean invoked = gotInvoked;
            gotInvoked = false;

            return invoked;
        }

        @Override
        public T get()
        {
            System.out.println("TestContextualInstanceProvider#get() got invoked!");
            gotInvoked = true;

            return instance;
        }
    }
}
