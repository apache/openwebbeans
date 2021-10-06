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
package org.apache.webbeans.service;

import static java.util.Collections.emptyMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.util.Properties;

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.proxy.NormalScopeProxyFactory;
import org.apache.webbeans.spi.DefiningClassService;
import org.junit.Test;

public class ClassLoaderProxyServiceTest
{
    @Test
    public void defineInProxy() throws NoSuchMethodException
    {
        final Properties config = new Properties();
        config.setProperty(DefiningClassService.class.getName(), ClassLoaderProxyService.class.getName());
        final WebBeansContext context = new WebBeansContext(emptyMap(), config);
        final NormalScopeProxyFactory factory = new NormalScopeProxyFactory(context);
        final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        final Class<MyBean> proxyClass = factory.createProxyClass(contextClassLoader, MyBean.class);
        assertNotEquals(contextClassLoader, proxyClass.getClassLoader());
        final ClassLoader proxyLoader = context.getService(DefiningClassService.class).getProxyClassLoader(proxyClass);
        assertEquals(proxyLoader, proxyClass.getClassLoader());
        proxyClass.getMethod("ok", String.class); // this line would fail if not here, no assert needed

        // when using ClassLoaderProxyService, we don't use Unsafe to allocate the instance
        // the regular reflection method newInstance is called and therefore the constructor gets called
        // final Bean<MyBean> bean =
        // final MyBean beanInstance = factory.createProxyInstance(proxyClass, factory.getInstanceProvider(proxyLoader, bean));
        // assertTrue(beanInstance.isConstructorInvoked);
    }

    public static class MyBean
    {
        private final boolean constructorInvoked;

        public MyBean() {
            this.constructorInvoked = true;
        }

        public String ok(final String value)
        {
            return ">" + value + "<";
        }

        public boolean isConstructorInvoked() {
            return constructorInvoked;
        }
    }
}
