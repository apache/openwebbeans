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

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.enterprise.inject.spi.InterceptionFactory;
import jakarta.inject.Qualifier;
import org.apache.webbeans.annotation.EmptyAnnotationLiteral;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.exception.WebBeansException;
import org.apache.webbeans.proxy.InterceptorDecoratorProxyFactory;
import org.apache.webbeans.proxy.InterceptorHandler;
import org.apache.webbeans.proxy.OwbInterceptorProxy;
import org.apache.webbeans.test.AbstractUnitTest;
import org.apache.webbeans.test.component.intercept.webbeans.TransactionalInterceptor;
import org.apache.webbeans.test.interceptors.factory.beans.ClassInterceptedClass;
import org.apache.webbeans.test.interceptors.factory.beans.TonsOfMethodsInterceptedClass;
import org.apache.webbeans.test.util.CustomBaseType;
import org.apache.webbeans.test.util.CustomType;
import org.apache.webbeans.test.util.ExtendedSpecificClass;
import org.apache.webbeans.test.util.GenericInterface;
import org.apache.webbeans.test.util.SpecificClass;
import org.apache.webbeans.util.ClassUtil;
import org.junit.Assert;
import org.junit.Test;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;


/**
 * Test the {@link org.apache.webbeans.proxy.InterceptorDecoratorProxyFactory}
 */
public class InterceptorDecoratorProxyFactoryTest extends AbstractUnitTest
{

    @ApplicationScoped
    public static class IFProducer {
        @Produces
        public Runnable wrap1(final InterceptionFactory<Runnable> interceptionFactory) {
            return interceptionFactory.createInterceptedInstance(() -> {});
        }

        @Produces
        @SimpleQualifier
        public Runnable wrap2(final InterceptionFactory<Runnable> interceptionFactory) {
            interceptionFactory.configure().add(new EmptyAnnotationLiteral<SimpleQualifier>(){});
            return interceptionFactory.createInterceptedInstance(() -> {});
        }
    }

    @Test
    public void testEnsureOneProxyPerAT() {
        startContainer(IFProducer.class);
        final var simpleQualifier = new EmptyAnnotationLiteral<SimpleQualifier>(){};
        final var r11 = getInstance(Runnable.class);
        final var r12 = getInstance(Runnable.class);
        assertEquals(1, getWebBeansContext().getWebBeansUtil().getInterceptionFactoryCache().size());
        final var r2 = getInstance(Runnable.class, simpleQualifier);
        assertEquals(2, getWebBeansContext().getWebBeansUtil().getInterceptionFactoryCache().size());
        assertSame(r11.getClass(), r12.getClass());
        assertNotSame(r2.getClass(), r11.getClass());
        assertSame(getInstance(Runnable.class, simpleQualifier).getClass(), r2.getClass());
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

    @Target({ METHOD })
    @Retention(RUNTIME)
    @Documented
    @Qualifier
    public @interface SimpleQualifier
    {
    }
}
