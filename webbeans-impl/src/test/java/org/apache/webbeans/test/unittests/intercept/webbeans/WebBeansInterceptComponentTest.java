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
package org.apache.webbeans.test.unittests.intercept.webbeans;

import org.junit.Assert;

import org.apache.webbeans.test.AbstractUnitTest;
import org.apache.webbeans.test.component.intercept.webbeans.ActionInterceptor;
import org.apache.webbeans.test.component.intercept.webbeans.TransactionalInterceptor2;
import org.apache.webbeans.test.component.intercept.webbeans.WInterceptorComponent;
import org.apache.webbeans.test.component.intercept.webbeans.WMetaInterceptorComponent;
import org.junit.Test;

public class WebBeansInterceptComponentTest extends AbstractUnitTest
{
    @Test
    public void testInterceptedComponent()
    {
        addInterceptor(TransactionalInterceptor2.class);
        startContainer(TransactionalInterceptor2.class, WInterceptorComponent.class);
    }

    @Test
    public void testInterceptorCalls()
    {
        addInterceptor(TransactionalInterceptor2.class);
        startContainer(TransactionalInterceptor2.class, WInterceptorComponent.class);

        WInterceptorComponent comp = getInstance(WInterceptorComponent.class);
        int s = comp.hello();

        Assert.assertEquals(5, s);
    }

    @Test
    public void testMetaInterceptorCalls()
    {
        addInterceptor(TransactionalInterceptor2.class);
        addInterceptor(ActionInterceptor.class);
        startContainer(TransactionalInterceptor2.class, ActionInterceptor.class, WMetaInterceptorComponent.class);
        WMetaInterceptorComponent comp = getInstance(WMetaInterceptorComponent.class);
        int s = comp.hello();

        Assert.assertEquals(5, s);

        s = comp.hello2();

        Assert.assertEquals(10, s);
    }
}
