/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.apache.webbeans.test.unittests;

import java.util.List;

import javax.enterprise.inject.spi.BeanManager;
import javax.servlet.http.HttpSession;

import junit.framework.Assert;

import org.apache.webbeans.component.AbstractComponent;
import org.apache.webbeans.container.ManagerImpl;
import org.apache.webbeans.context.ContextFactory;
import org.apache.webbeans.test.component.service.ITyped2;
import org.apache.webbeans.test.component.service.Typed2;
import org.apache.webbeans.test.component.service.TypedInjection;
import org.apache.webbeans.test.component.service.TypedInjectionWithoutArguments;
import org.apache.webbeans.test.servlet.TestContext;
import org.junit.Before;
import org.junit.Test;

public class TypedInjectedComponentTest extends TestContext
{
    BeanManager container = null;

    public TypedInjectedComponentTest()
    {
        super(TypedInjectedComponentTest.class.getSimpleName());
    }

    @Before
    public void init()
    {
        super.init();
        this.container = ManagerImpl.getManager();
    }

    @Test
    public void testTypedComponent() throws Throwable
    {
        clear();

        defineSimpleWebBean(Typed2.class);
        defineSimpleWebBean(TypedInjection.class);
        List<AbstractComponent<?>> comps = getComponents();

        HttpSession session = getSession();

        ContextFactory.initSessionContext(session);

        Assert.assertEquals(2, comps.size());

        getManager().getInstance(comps.get(0));

        Object object = getManager().getInstance(comps.get(1));

        Assert.assertTrue(object instanceof TypedInjection);

        TypedInjection i = (TypedInjection) object;

        Assert.assertTrue(i.getV() instanceof ITyped2);

        Object obj2 = getManager().getInstance(comps.get(0));

        Assert.assertSame(i.getV(), obj2);

        ContextFactory.destroySessionContext(session);
    }

    @Test
    public void testTypedComponentWithoutArgument() throws Throwable
    {
        clear();

        defineSimpleWebBean(Typed2.class);
        defineSimpleWebBean(TypedInjectionWithoutArguments.class);
        List<AbstractComponent<?>> comps = getComponents();

        HttpSession session = getSession();

        ContextFactory.initSessionContext(session);

        Assert.assertEquals(2, comps.size());

        getManager().getInstance(comps.get(0));
        Object object = getManager().getInstance(comps.get(1));

        Assert.assertTrue(object instanceof TypedInjectionWithoutArguments);

        TypedInjectionWithoutArguments i = (TypedInjectionWithoutArguments) object;

        Assert.assertTrue(i.getV() instanceof ITyped2);

        Object obj2 = getManager().getInstance(comps.get(0));

        Assert.assertSame(i.getV(), obj2);

        ContextFactory.destroySessionContext(session);
    }

}
