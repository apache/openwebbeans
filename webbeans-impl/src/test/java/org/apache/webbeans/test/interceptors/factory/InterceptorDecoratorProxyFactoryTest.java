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
package org.apache.webbeans.test.interceptors.factory;

import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.InjectionPoint;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.configurator.AnnotatedTypeConfiguratorImpl;
import org.apache.webbeans.exception.WebBeansException;
import org.apache.webbeans.intercept.InterceptorResolutionService;
import org.apache.webbeans.portable.AnnotatedTypeImpl;
import org.apache.webbeans.test.AbstractUnitTest;
import org.apache.webbeans.test.component.intercept.webbeans.TransactionalInterceptor;
import org.apache.webbeans.test.interceptors.factory.beans.ClassInterceptedClass;
import org.apache.webbeans.proxy.InterceptorDecoratorProxyFactory;

import org.apache.webbeans.proxy.InterceptorHandler;
import org.apache.webbeans.proxy.OwbInterceptorProxy;
import org.apache.webbeans.test.interceptors.factory.beans.TonsOfMethodsInterceptedClass;
import org.apache.webbeans.util.Asserts;
import org.apache.webbeans.util.ClassUtil;
import org.apache.webbeans.test.util.CustomBaseType;
import org.apache.webbeans.test.util.CustomType;
import org.apache.webbeans.test.util.ExtendedSpecificClass;
import org.apache.webbeans.test.util.GenericInterface;
import org.apache.webbeans.test.util.SpecificClass;
import org.apache.webbeans.util.WebBeansUtil;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertSame;


/**
 * Test the {@link org.apache.webbeans.proxy.InterceptorDecoratorProxyFactory}
 */
public class InterceptorDecoratorProxyFactoryTest extends AbstractUnitTest
{

    @Test
    public void testEnsureOneProxyPerAT()
    {
        // we take a fresh URLClassLoader to not blur the test classpath with synthetic classes.
        ClassLoader classLoader = new URLClassLoader(new URL[0]);

        final WebBeansContext context = WebBeansContext.currentInstance();
        final InterceptorDecoratorProxyFactory factory = context.getInterceptorDecoratorProxyFactory();

        // create first annotated type and it's associated proxy
        final var at = new AnnotatedTypeImpl<DummyBean>(context, context.getBeanManagerImpl().createAnnotatedType(DummyBean.class));
        final var configurator = new AnnotatedTypeConfiguratorImpl<DummyBean>(context, at);
        final AnnotatedTypeImpl<DummyBean> newAnnotatedType = configurator.getNewAnnotatedType();
        final InterceptorResolutionService.BeanInterceptorInfo interceptorInfo = context.getInterceptorResolutionService()
                   .calculateInterceptorInfo(newAnnotatedType.getTypeClosure(), Collections.emptySet(), newAnnotatedType, true);
        final Class<DummyBean> subClass = factory.getCachedProxyClass(interceptorInfo, newAnnotatedType, classLoader);

        Asserts.assertNotNull(subClass);

        // create second annotated type and it's associated proxy
        final var at2 = new AnnotatedTypeImpl<DummyBean>(context, context.getBeanManagerImpl().createAnnotatedType(DummyBean.class));
        final var configurator2 = new AnnotatedTypeConfiguratorImpl<DummyBean>(context, at2);
        final AnnotatedTypeImpl<DummyBean> newAnnotatedType2 = configurator2.getNewAnnotatedType();
        final InterceptorResolutionService.BeanInterceptorInfo interceptorInfo2 = context.getInterceptorResolutionService()
                   .calculateInterceptorInfo(newAnnotatedType2.getTypeClosure(), Collections.emptySet(), newAnnotatedType2, true);
        final Class<DummyBean> subClass2 = factory.getCachedProxyClass(interceptorInfo2, newAnnotatedType2, classLoader);

        Asserts.assertNotNull(subClass2);

        // the 2 proxy instances should be the same and the cache in the factory should be filed in with one proxy only
        assertSame(subClass, subClass2);
    }

    @Test
    public void testSimpleProxyCreation() throws Exception
    {
        InterceptorDecoratorProxyFactory pf = new InterceptorDecoratorProxyFactory(new WebBeansContext());

        // we take a fresh URLClassLoader to not blur the test classpath with synthetic classes.
        ClassLoader classLoader = new URLClassLoader(new URL[0]);

        List<Method> methods = ClassUtil.getNonPrivateMethods(ClassInterceptedClass.class, true);

        Method[] interceptedMethods = methods.toArray(new Method[methods.size()]);
        Method[] nonInterceptedMethods = null;

        Bean dummyBean = new DummyBean();

        Class<ClassInterceptedClass> proxyClass = pf.createProxyClass(dummyBean, classLoader, ClassInterceptedClass.class, interceptedMethods, nonInterceptedMethods);
        Assert.assertNotNull(proxyClass);

        ClassInterceptedClass internalInstance = new ClassInterceptedClass();
        internalInstance.init();

        TestInterceptorHandler testInvocationHandler = new TestInterceptorHandler(internalInstance);

        ClassInterceptedClass proxy = pf.createProxyInstance(proxyClass, internalInstance, testInvocationHandler);
        Assert.assertNotNull(proxy);

        Assert.assertTrue(proxy instanceof OwbInterceptorProxy);

        proxy.setMeaningOfLife(42);

        Assert.assertEquals(42, proxy.getMeaningOfLife());
        Assert.assertEquals(internalInstance.getFloat(), proxy.getFloat(), 0f);
        Assert.assertEquals('c', proxy.getChar());
        Assert.assertEquals(internalInstance, proxy.getSelf());

        Assert.assertEquals(5, testInvocationHandler.invokedMethodNames.size());
    }

    @Test
    public void testGenericProxyGeneration()
    {
        InterceptorDecoratorProxyFactory pf = new InterceptorDecoratorProxyFactory(new WebBeansContext());

        // we take a fresh URLClassLoader to not blur the test classpath with synthetic classes.
        ClassLoader classLoader = new URLClassLoader(new URL[0]);

        List<Method> methods = ClassUtil.getNonPrivateMethods(ExtendedSpecificClass.class, true);
        methods.removeIf(Method::isBridge);

        Method[] interceptedMethods = methods.toArray(new Method[methods.size()]);
        Method[] nonInterceptedMethods = null;

        Bean dummyBean = new DummyBean();

        Class<ExtendedSpecificClass> proxyClass = pf.createProxyClass(dummyBean, classLoader, ExtendedSpecificClass.class, interceptedMethods, nonInterceptedMethods);
        Assert.assertNotNull(proxyClass);

        ExtendedSpecificClass internalInstance = new ExtendedSpecificClass();
        internalInstance.init();

        TestInterceptorHandler testInvocationHandler = new TestInterceptorHandler(internalInstance);

        ExtendedSpecificClass extendedSpecificProxyInstance = pf.createProxyInstance(proxyClass, internalInstance, testInvocationHandler);
        SpecificClass<CustomType> specificProxyInstance = extendedSpecificProxyInstance;
        GenericInterface<CustomBaseType> interfaceProxyInstance = extendedSpecificProxyInstance;
        Assert.assertNotNull(extendedSpecificProxyInstance.newInstance());
        Assert.assertNotNull(specificProxyInstance.newInstance());
        Assert.assertNotNull(interfaceProxyInstance.newInstance());

        Assert.assertTrue(extendedSpecificProxyInstance instanceof OwbInterceptorProxy);
        Assert.assertNotNull(internalInstance.newInstance()); 
    }

    /**
     * We originally did have a bug in our proxy code which
     * blew up if we did have > 127 methods in an intercepted class.
     */
    @Test
    public void testManyMethodsInterceptor() throws Exception
    {
        addInterceptor(TransactionalInterceptor.class);
        startContainer(TonsOfMethodsInterceptedClass.class);

        TonsOfMethodsInterceptedClass instance = getInstance(TonsOfMethodsInterceptedClass.class);

        for (int i = 0; i < 130; i++)
        {
            String methodName = "method" + i;
            Method m = instance.getClass().getDeclaredMethod(methodName);
            m.invoke(instance);
        }
    }



    public static class DummyBean implements Bean {
        @Override
        public Object create(CreationalContext context)
        {
            return null;
        }

        @Override
        public Set<Type> getTypes()
        {
            return null;
        }

        @Override
        public Set<Annotation> getQualifiers()
        {
            return null;
        }

        @Override
        public Class<? extends Annotation> getScope()
        {
            return null;
        }

        @Override
        public String getName()
        {
            return null;
        }

        @Override
        public Set<InjectionPoint> getInjectionPoints()
        {
            return null;
        }

        @Override
        public Class<?> getBeanClass()
        {
            return null;
        }

        @Override
        public Set<Class<? extends Annotation>> getStereotypes()
        {
            return null;
        }

        @Override
        public boolean isAlternative()
        {
            return false;
        }

        @Override
        public void destroy(Object instance, CreationalContext context)
        {
        }
    }

    public static class TestInterceptorHandler implements InterceptorHandler
    {
        public List<String> invokedMethodNames = new ArrayList<String>();

        private Object instance;

        public TestInterceptorHandler(Object instance)
        {
            this.instance = instance;
        }

        @Override
        public Object invoke(Method method, Object[] args)
        {
            Assert.assertFalse(method.isBridge());
            if (!method.getName().equals("toString"))
            {
                invokedMethodNames.add(method.getName());
            }

            System.out.println("TestInvocationHandler got properly invoked for method " + method.getName());

            try
            {
                return method.invoke(instance, args);
            }
            catch (IllegalAccessException | InvocationTargetException e)
            {
                throw new WebBeansException(e);
            }
        }
    }
}
