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
package org.apache.webbeans.test.unittests.inject;

import java.util.List;

import javax.servlet.http.HttpSession;

import junit.framework.Assert;

import org.apache.webbeans.component.AbstractBean;
import org.apache.webbeans.context.ContextFactory;
import org.apache.webbeans.test.TestContext;
import org.apache.webbeans.test.component.ContaintsCurrentComponent;
import org.apache.webbeans.test.component.CurrentBindingComponent;
import org.apache.webbeans.test.component.service.ITyped2;
import org.apache.webbeans.test.component.service.Typed2;
import org.junit.Before;
import org.junit.Test;

public class CurrentInjectedComponentTest extends TestContext
{
    public CurrentInjectedComponentTest()
    {
        super(CurrentInjectedComponentTest.class.getSimpleName());
    }

    @Before
    public void init()
    {
        super.init();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testTypedComponent() throws Throwable
    {
        clear();

        defineManagedBean(Typed2.class);
        defineManagedBean(CurrentBindingComponent.class);
        defineManagedBean(ContaintsCurrentComponent.class);
        List<AbstractBean<?>> comps = getComponents();

        HttpSession session = getSession();
        ContextFactory.initRequestContext(null);
        ContextFactory.initSessionContext(session);

        Assert.assertEquals(3, comps.size());

        getManager().getInstance(comps.get(0));
        getManager().getInstance(comps.get(1));

        Object object = getManager().getInstance(comps.get(2));

        Assert.assertTrue(object instanceof ContaintsCurrentComponent);

        ContaintsCurrentComponent i = (ContaintsCurrentComponent) object;

        Assert.assertTrue(i.getInstance() instanceof CurrentBindingComponent);

        Object obj2 = getManager().getInstance(comps.get(1));

        Assert.assertSame(i.getInstance(), obj2);

        CurrentBindingComponent bc = (CurrentBindingComponent) obj2;
        ITyped2 typed2 = bc.getTyped2();

        Assert.assertNotNull(typed2);

        ContextFactory.destroyRequestContext(null);
        ContextFactory.destroySessionContext(session);
    }

}
