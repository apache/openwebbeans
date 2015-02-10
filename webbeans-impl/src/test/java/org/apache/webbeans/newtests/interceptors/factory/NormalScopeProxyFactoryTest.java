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

import org.apache.webbeans.component.OwbBean;
import org.apache.webbeans.component.WebBeansType;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.newtests.AbstractUnitTest;
import org.apache.webbeans.newtests.interceptors.factory.beans.ClassInterceptedClass;
import org.apache.webbeans.newtests.interceptors.factory.beans.SomeBaseClass;
import org.apache.webbeans.proxy.NormalScopeProxyFactory;
import org.junit.Assert;
import org.junit.Test;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.Producer;
import javax.inject.Provider;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Set;
import org.apache.webbeans.newtests.interceptors.factory.beans.PartialBeanClass;

import static org.junit.Assert.assertNotNull;

/**
 * Test for the {@link NormalScopeProxyFactory}
 */
public class NormalScopeProxyFactoryTest extends AbstractUnitTest
{
    @Test
    public void noBeanClassProxy()
    {
        final WebBeansContext webBeansContext = new WebBeansContext();
        final NormalScopeProxyFactory pf = new NormalScopeProxyFactory(webBeansContext);
        final Object proxy = pf.createNormalScopeProxy(new OwbBean<Provider>() {
            @Override
            public Set<Type> getTypes() {
                return null;
            }

            @Override
            public Set<Annotation> getQualifiers() {
                return null;
            }

            @Override
            public Class<? extends Annotation> getScope() {
                return ApplicationScoped.class;
            }

            @Override
            public String getName() {
                return null;
            }

            @Override
            public boolean isNullable() {
                return false;
            }

            @Override
            public Set<InjectionPoint> getInjectionPoints() {
                return null;
            }

            @Override
            public Class<?> getBeanClass() {
                return null; // this is what we test
            }

            @Override
            public Set<Class<? extends Annotation>> getStereotypes() {
                return null;
            }

            @Override
            public boolean isAlternative() {
                return false;
            }

            @Override
            public Provider create(final CreationalContext<Provider> context) {
                return null;
            }

            @Override
            public void destroy(final Provider instance, final CreationalContext<Provider> context) {

            }

            @Override
            public Producer<Provider> getProducer() {
                return null;
            }

            @Override
            public WebBeansType getWebBeansType() {
                return null;
            }

            @Override
            public Class<Provider> getReturnType() {
                return Provider.class;
            }

            @Override
            public void setSpecializedBean(boolean specialized) {

            }

            @Override
            public boolean isSpecializedBean() {
                return false;
            }

            @Override
            public void setEnabled(boolean enabled) {

            }

            @Override
            public boolean isEnabled() {
                return false;
            }

            @Override
            public String getId() {
                return null;
            }

            @Override
            public boolean isPassivationCapable() {
                return false;
            }

            @Override
            public boolean isDependent() {
                return false;
            }

            @Override
            public WebBeansContext getWebBeansContext() {
                return null;
            }
        });
        assertNotNull(proxy);
    }

    @Test
    public void textSimpleProxyCreation() throws Exception
    {
        NormalScopeProxyFactory pf = new NormalScopeProxyFactory(new WebBeansContext());

        // we take a fresh URLClassLoader to not blur the test classpath with synthetic classes.
        ClassLoader classLoader = new URLClassLoader(new URL[0]);

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

    @Test
    public void textPartialBeanProxyCreation() throws Exception
    {
        NormalScopeProxyFactory pf = new NormalScopeProxyFactory(new WebBeansContext());

        // we take a fresh URLClassLoader to not blur the test classpath with synthetic classes.
        ClassLoader classLoader = new URLClassLoader(new URL[0]);

        Class<PartialBeanClass> proxyClass = pf.createProxyClass(classLoader, PartialBeanClass.class);
        Assert.assertNotNull(proxyClass);

        PartialBeanClass internalInstance = new PartialBeanClass()
        {
            @Override
            public String willFail2()
            {
                return "";
            }

            @Override
            public String willFail()
            {
                return "";
            }
        };

        TestContextualInstanceProvider provider = new TestContextualInstanceProvider(internalInstance);

        PartialBeanClass proxy = pf.createProxyInstance(proxyClass, provider);

        proxy.willFail();
        proxy.willFail2();
        proxy.willFail3();
    }

    /**
     * Test if protected and package scope methods are proxied as well.
     * @throws Exception
     */
    @Test
    public void testProtectedMethod() throws Exception
    {
        startContainer(ProtectedUsageBean.class);

        ProtectedUsageBean protectedUsage = getInstance(ProtectedUsageBean.class);
        Assert.assertNotNull(protectedUsage);

        Assert.assertEquals(42, protectedUsage.getPackageMeaningOfLife());
        Assert.assertEquals(42, protectedUsage.getProtectedMeaningOfLife());
        Assert.assertEquals(Integer.valueOf(42), protectedUsage.getProtectedIntegerMeaningOfLife());
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

    @Test
    public void testContainerBoot() throws Exception
    {
        startContainer(ClassInterceptedClass.class, SomeBaseClass.class, SubPackageInterceptedClass.class);
        ClassInterceptedClass instance = getInstance(ClassInterceptedClass.class);
        Assert.assertNotNull(instance);
        instance.getFloat();

        SubPackageInterceptedClass subPackageInstance = getInstance(SubPackageInterceptedClass.class);
        Assert.assertNotNull(subPackageInstance);
        instance.getFloat();
    }
}
