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
package org.apache.webbeans.test.proxy;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InterceptorBinding;
import jakarta.interceptor.InvocationContext;

import org.junit.Assert;
import org.junit.Test;

import org.apache.webbeans.proxy.InterceptorDecoratorProxyFactory;
import org.apache.webbeans.proxy.NormalScopeProxyFactory;
import org.apache.webbeans.proxy.OwbNormalScopeProxy;
import org.apache.webbeans.test.AbstractUnitTest;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Regression coverage aligned with historical bridge-method bugs in interceptors / scoped proxies.
 *
 * <p><a href="https://issues.apache.org/jira/browse/OWB-923">OWB-923</a> — wrong handling when a
 * bean inherits the implementation from a superclass but implements a parameterized interface
 * (compiler bridge methods).</p>
 *
 * <p><a href="https://issues.apache.org/jira/browse/OWB-828">OWB-828</a> — with normal-scoping and
 * interceptor subclasses, interface dispatch via a bridge could leave the interceptor proxy as
 * {@code this} in the business method (broken injection / wrong receiver) instead of the target
 * instance.</p>
 */
public class BridgeMethodOwb923And828Test extends AbstractUnitTest
{

    // --- OWB-923 shape: interface ValueContract&lt;T&gt; + abstract base with String getValue() ---

    public interface ValueContract923<T>
    {
        T getValue();
    }

    public abstract static class ValueBase923
    {
        static volatile Object lastGetValueReceiver;

        public String getValue()
        {
            lastGetValueReceiver = this;
            return "owb923";
        }
    }

    @ApplicationScoped
    public static class Owb923StyleBean extends ValueBase923 implements ValueContract923<String>
    {
    }

    @Test
    public void owb923_inheritedGetterThroughParameterizedInterface_delegatesToContextualInstance()
    {
        ValueBase923.lastGetValueReceiver = null;
        startContainer(Owb923StyleBean.class);

        ValueContract923<String> viaIface = getInstance(Owb923StyleBean.class);
        Assert.assertTrue(viaIface instanceof OwbNormalScopeProxy);

        Object contextual = NormalScopeProxyFactory.unwrapInstance(viaIface);
        Assert.assertEquals("owb923", viaIface.getValue());
        Assert.assertSame(
                "OWB-923: call through parameterized interface must reach the contextual instance",
                contextual,
                ValueBase923.lastGetValueReceiver);
    }

    // --- OWB-828 shape: @ApplicationScoped + interceptor proxy under normal-scope proxy + bridge ---

    @InterceptorBinding
    @Retention(RUNTIME)
    @Target({ TYPE, METHOD })
    public @interface Owb828InterceptorHitBinding
    {
    }

    @Interceptor
    @Owb828InterceptorHitBinding
    public static class Owb828HitInterceptor
    {
        static final AtomicInteger aroundInvokeCount = new AtomicInteger();

        @AroundInvoke
        public Object aroundInvoke(InvocationContext ctx) throws Exception
        {
            aroundInvokeCount.incrementAndGet();
            return ctx.proceed();
        }
    }

    public interface ActionContract828<T>
    {
        void doSomething(T value);
    }

    public abstract static class ActionBase828
    {
        static volatile Object lastDoSomethingReceiver;

        public void doSomething(String value)
        {
            lastDoSomethingReceiver = this;
        }
    }

    /**
     * Method-level interceptor binding on an overriding method (see
     * {@link org.apache.webbeans.test.interceptors.resolution.InterceptBridgeMethodTest})
     * matches the historical OWB-828 reproducer: normal-scoped client reference, interceptor subclass,
     * and interface dispatch through a bridge.
     */
    @ApplicationScoped
    public static class Owb828AppScopedBean extends ActionBase828 implements ActionContract828<String>
    {
        @Override
        @Owb828InterceptorHitBinding
        public void doSomething(String value)
        {
            super.doSomething(value);
        }
    }

    @Test
    public void owb828_applicationScopedInterceptor_bridgeDispatchReachesContextualInstance()
    {
        Owb828HitInterceptor.aroundInvokeCount.set(0);
        ActionBase828.lastDoSomethingReceiver = null;

        addInterceptor(Owb828HitInterceptor.class);
        startContainer(Owb828AppScopedBean.class);

        ActionContract828<String> viaIface = getInstance(Owb828AppScopedBean.class);
        Assert.assertTrue(viaIface instanceof OwbNormalScopeProxy);

        Object afterNormalScope = NormalScopeProxyFactory.unwrapInstance(viaIface);
        InterceptorDecoratorProxyFactory intDecFactory = getWebBeansContext().getInterceptorDecoratorProxyFactory();
        Object contextual = intDecFactory.unwrapInstance(afterNormalScope);

        viaIface.doSomething("owb828");

        Assert.assertEquals(
                "OWB-828: interceptor must run exactly once for the bridged interface dispatch",
                1,
                Owb828HitInterceptor.aroundInvokeCount.get());
        Assert.assertSame(
                "OWB-828: business method 'this' must be the real bean behind scope + intercept proxies",
                contextual,
                ActionBase828.lastDoSomethingReceiver);
    }
}
