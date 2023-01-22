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
package org.apache.webbeans.test.unittests.inject;

import java.util.ArrayList;
import java.util.Collection;

import jakarta.enterprise.inject.spi.BeanManager;

import org.junit.Assert;

import org.apache.webbeans.test.AbstractUnitTest;
import org.apache.webbeans.test.component.ContainUserComponent;
import org.apache.webbeans.test.component.UserComponent;
import org.junit.Test;

public class UserComponentTest extends AbstractUnitTest
{
    BeanManager container = null;


    @Test
    public void testTypedComponent() throws Throwable
    {
        Collection<Class<?>> beanClasses = new ArrayList<Class<?>>();
        beanClasses.add(UserComponent.class);
        beanClasses.add(ContainUserComponent.class);

        startContainer(beanClasses, null);

        UserComponent userComponent = getInstance(UserComponent.class);
        userComponent.setName("Gurkan");
        userComponent.setSurname("Erdogdu");

        Assert.assertNotNull(userComponent);

        ContainUserComponent uc = getInstance(ContainUserComponent.class);

        Assert.assertNotNull(uc.echo());
        Assert.assertEquals(uc.echo(), userComponent.getName() + " " + userComponent.getSurname());

        shutDownContainer();
    }

}
