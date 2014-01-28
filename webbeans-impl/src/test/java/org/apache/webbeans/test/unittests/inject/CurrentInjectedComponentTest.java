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


import junit.framework.Assert;

import org.apache.webbeans.newtests.AbstractUnitTest;
import org.apache.webbeans.test.component.ContaintsCurrentComponent;
import org.apache.webbeans.test.component.CurrentBindingComponent;
import org.apache.webbeans.test.component.service.ITyped2;
import org.apache.webbeans.test.component.service.Typed2;
import org.junit.Test;

public class CurrentInjectedComponentTest extends AbstractUnitTest
{

    @SuppressWarnings("unchecked")
    @Test
    public void testTypedComponent() throws Throwable
    {
        startContainer(Typed2.class, CurrentBindingComponent.class, ContaintsCurrentComponent.class);


        ContaintsCurrentComponent i = getInstance(ContaintsCurrentComponent.class);

        Assert.assertTrue(i.getInstance() instanceof CurrentBindingComponent);

        Object obj2 = getInstance(CurrentBindingComponent.class);

        Assert.assertSame(i.getInstance().getTyped2(), ((CurrentBindingComponent) obj2).getTyped2());

        CurrentBindingComponent bc = (CurrentBindingComponent) obj2;
        ITyped2 typed2 = bc.getTyped2();

        Assert.assertNotNull(typed2);
    }

}
