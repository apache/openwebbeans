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

import javax.inject.manager.Manager;
import javax.servlet.http.HttpSession;

import junit.framework.Assert;

import org.apache.webbeans.component.AbstractComponent;
import org.apache.webbeans.container.ManagerImpl;
import org.apache.webbeans.context.ContextFactory;
import org.apache.webbeans.test.component.ContainUserComponent;
import org.apache.webbeans.test.component.UserComponent;
import org.apache.webbeans.test.servlet.TestContext;
import org.junit.Before;
import org.junit.Test;

public class UserComponentTest extends TestContext
{
    Manager container = null;

    public UserComponentTest()
    {
        super(UserComponentTest.class.getSimpleName());
    }

    @Before
    public void init()
    {
        this.container = ManagerImpl.getManager();
        super.init();
    }

    @Test
    public void testTypedComponent() throws Throwable
    {
        clear();

        defineSimpleWebBean(UserComponent.class);
        defineSimpleWebBean(ContainUserComponent.class);
        List<AbstractComponent<?>> comps = getComponents();

        HttpSession session = getSession();
        ContextFactory.initRequestContext(null);
        ContextFactory.initSessionContext(session);

        Assert.assertEquals(2, comps.size());

        UserComponent userComponent = (UserComponent) getManager().getInstance(comps.get(0));
        userComponent.setName("Gurkan");
        userComponent.setSurname("Erdogdu");

        Assert.assertNotNull(userComponent);

        Object object = getManager().getInstance(comps.get(1));
        Assert.assertNotNull(object);
        Assert.assertTrue(object instanceof ContainUserComponent);

        ContainUserComponent uc = (ContainUserComponent) object;

        Assert.assertNotNull(uc.echo());
        Assert.assertEquals(uc.echo(), userComponent.getName() + " " + userComponent.getSurname());

        ContextFactory.destroyRequestContext(null);
        ContextFactory.destroySessionContext(session);
    }

}
