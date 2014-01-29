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
package org.apache.webbeans.test.injection.constructor;

import org.apache.webbeans.test.AbstractUnitTest;
import org.apache.webbeans.test.injection.constructor.beans.Administrator;
import org.apache.webbeans.test.injection.constructor.beans.BeanWithSingleParamInjectConstructor;
import org.apache.webbeans.test.injection.constructor.beans.BeanWithTwoParamInjectConstructor;
import org.apache.webbeans.test.injection.constructor.beans.User;

import org.junit.Assert;
import org.junit.Test;

/**
 * Test &#064;Inject constructors.
 */
public class ConstructorInjectionTest extends AbstractUnitTest
{

    @Test
    public void testSingleParamCtInjection() throws Exception
    {
        startContainer(BeanWithSingleParamInjectConstructor.class, User.class);

        BeanWithSingleParamInjectConstructor instance = getInstance(BeanWithSingleParamInjectConstructor.class);
        Assert.assertNotNull(instance);
        Assert.assertNotNull(instance.getUser());

        User user = getInstance(User.class);
        Assert.assertNotNull(user);

        Assert.assertTrue(user.getSelf() == instance.getUser().getSelf());
    }

    @Test
    public void testTwoParamCtInjection() throws Exception
    {
        startContainer(BeanWithTwoParamInjectConstructor.class, User.class, Administrator.class);

        BeanWithTwoParamInjectConstructor instance = getInstance(BeanWithTwoParamInjectConstructor.class);
        Assert.assertNotNull(instance);
        Assert.assertNotNull(instance.getUser());
        Assert.assertNotNull(instance.getAdministrator());

        User user = getInstance(User.class);
        Assert.assertNotNull(user);
        Assert.assertTrue(user.getSelf() == instance.getUser().getSelf());

        Administrator administrator = getInstance(Administrator.class);
        Assert.assertNotNull(administrator);
        Assert.assertTrue(administrator.getSelf() == instance.getAdministrator().getSelf());
    }
}
