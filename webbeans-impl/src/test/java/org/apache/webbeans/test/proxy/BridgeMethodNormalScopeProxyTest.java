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

import jakarta.enterprise.context.ApplicationScoped;

import org.junit.Assert;
import org.junit.Test;

import org.apache.webbeans.proxy.NormalScopeProxyFactory;
import org.apache.webbeans.proxy.OwbNormalScopeProxy;
import org.apache.webbeans.test.AbstractUnitTest;

/**
 * OWB-1234: interface dispatch uses JVM bridge methods; normal-scope proxies must delegate those too.
 */
public class BridgeMethodNormalScopeProxyTest extends AbstractUnitTest
{
    public interface IntegerContract
    {
        void doIt(Integer value);
    }

    public static class BaseNumberBean<T extends Number>
    {
        static volatile Object lastDoItReceiver;

        public void doIt(T param)
        {
            lastDoItReceiver = this;
        }
    }

    /** Compiler-generated bridge {@code doIt(Integer)} only (no explicit override). */
    @ApplicationScoped
    public static class BridgeOnlyBean extends BaseNumberBean<Integer> implements IntegerContract
    {
    }

    /** Explicit {@code doIt(Integer)} — always worked before OWB-1234 fix. */
    @ApplicationScoped
    public static class ExplicitOverrideBean extends BaseNumberBean<Integer> implements IntegerContract
    {
        @Override
        public void doIt(Integer param)
        {
            super.doIt(param);
        }
    }

    @Test
    public void bridgeMethodInvocationDelegatesToContextualInstance()
    {
        BaseNumberBean.lastDoItReceiver = null;
        startContainer(BridgeOnlyBean.class);

        IntegerContract handler = getInstance(IntegerContract.class);
        Assert.assertTrue(handler instanceof OwbNormalScopeProxy);

        Object contextual = NormalScopeProxyFactory.unwrapInstance(handler);
        handler.doIt(4711);

        Assert.assertSame(
                "call through interface must use proxy delegation (contextual instance as receiver)",
                contextual,
                BaseNumberBean.lastDoItReceiver);
    }

    @Test
    public void explicitOverrideStillDelegatesToContextualInstance()
    {
        BaseNumberBean.lastDoItReceiver = null;
        startContainer(ExplicitOverrideBean.class);

        IntegerContract handler = getInstance(IntegerContract.class);
        Assert.assertTrue(handler instanceof OwbNormalScopeProxy);

        Object contextual = NormalScopeProxyFactory.unwrapInstance(handler);
        handler.doIt(7);

        Assert.assertSame(contextual, BaseNumberBean.lastDoItReceiver);
    }
}
