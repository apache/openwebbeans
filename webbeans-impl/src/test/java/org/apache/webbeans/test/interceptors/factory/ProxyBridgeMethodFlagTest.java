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

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.proxy.InterceptorDecoratorProxyFactory;
import org.apache.webbeans.proxy.NormalScopeProxyFactory;
import org.apache.webbeans.test.util.CustomBaseType;
import org.apache.webbeans.test.util.CustomType;
import org.apache.webbeans.test.util.SpecificParameterClass;
import org.apache.webbeans.util.ClassUtil;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

/**
 * Proxies which materialize JVM bridge methods must keep the {@code ACC_BRIDGE} flag
 * on the generated method.
 * <p>
 * {@link SpecificParameterClass} implements {@code GenericParameterInterface&lt;CustomType&gt;},
 * so javac generates the bridge method {@code consume(CustomBaseType)} next to the declared
 * {@code consume(CustomType)}. When a subclass proxy re-declares that bridge method
 * <em>without</em> the {@code ACC_BRIDGE} flag, the proxy class exposes two regular methods
 * named {@code consume} with assignable parameter types. Expression language implementations
 * resolve overloaded methods by preferring the non-bridge variant (see
 * {@code jakarta.el}/Tomcat {@code org.apache.el.util.ReflectionUtil}); with the flag dropped
 * the invocation of e.g. {@code #{bean.consume(x)}} fails with
 * "{@code MethodNotFoundException: Unable to find unambiguous method}".
 */
public class ProxyBridgeMethodFlagTest
{

    @Test
    public void normalScopeProxyPreservesBridgeMethodFlag() throws Exception
    {
        NormalScopeProxyFactory pf = new NormalScopeProxyFactory(new WebBeansContext());

        // we take a fresh URLClassLoader to not blur the test classpath with synthetic classes.
        ClassLoader classLoader = new URLClassLoader(new URL[0]);

        Class<SpecificParameterClass> proxyClass = pf.createProxyClass(classLoader, SpecificParameterClass.class);
        Assert.assertNotNull(proxyClass);

        assertBridgeFlagPreserved(proxyClass);
    }

    /**
     * Documents the currently correct behaviour of the {@link InterceptorDecoratorProxyFactory}:
     * bridge methods are skipped entirely ({@code unproxyableMethod}), so the proxy inherits the
     * properly flagged bridge method from the proxied superclass. This test passes and guards
     * against regressions; only {@link NormalScopeProxyFactory} re-declares bridge methods and
     * loses the flag.
     */
    @Test
    public void interceptorProxyPreservesBridgeMethodFlag() throws Exception
    {
        InterceptorDecoratorProxyFactory pf = new InterceptorDecoratorProxyFactory(new WebBeansContext());

        // we take a fresh URLClassLoader to not blur the test classpath with synthetic classes.
        ClassLoader classLoader = new URLClassLoader(new URL[0]);

        List<Method> methods = ClassUtil.getNonPrivateMethods(SpecificParameterClass.class, true, true);

        List<Method> interceptedMethods = new ArrayList<>();
        List<Method> nonInterceptedMethods = new ArrayList<>();
        for (Method m : methods)
        {
            if (m.isBridge())
            {
                // bridge methods only get delegated, they are never intercepted (OWB-1234)
                nonInterceptedMethods.add(m);
            }
            else
            {
                interceptedMethods.add(m);
            }
        }

        Class<SpecificParameterClass> proxyClass = pf.createProxyClass(
                new InterceptorDecoratorProxyFactoryTest.DummyBean(), classLoader, SpecificParameterClass.class,
                interceptedMethods.toArray(new Method[interceptedMethods.size()]),
                nonInterceptedMethods.toArray(new Method[nonInterceptedMethods.size()]));
        Assert.assertNotNull(proxyClass);

        assertBridgeFlagPreserved(proxyClass);
    }

    private void assertBridgeFlagPreserved(Class<? extends SpecificParameterClass> proxyClass) throws Exception
    {
        // sanity check the fixture: javac generated the bridge method on the proxied class
        Method originalBridge = SpecificParameterClass.class.getDeclaredMethod("consume", CustomBaseType.class);
        Assert.assertTrue(originalBridge.isBridge());
        Method originalSpecific = SpecificParameterClass.class.getDeclaredMethod("consume", CustomType.class);
        Assert.assertFalse(originalSpecific.isBridge());

        // exactly like on the proxied class itself, only ONE non-bridge consume method may be visible
        List<Method> nonBridgeMethods = new ArrayList<>();
        for (Method m : proxyClass.getMethods())
        {
            if ("consume".equals(m.getName()) && m.getParameterCount() == 1 && !m.isBridge())
            {
                nonBridgeMethods.add(m);
            }
        }

        Assert.assertEquals("the proxy must not expose the JVM bridge method consume(CustomBaseType) as a"
                + " regular method - overload resolution (e.g. jakarta.el MethodExpressions on proxied beans)"
                + " relies on the ACC_BRIDGE flag to disambiguate, but found: " + nonBridgeMethods,
                1, nonBridgeMethods.size());
    }
}
