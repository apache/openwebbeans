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
package org.apache.webbeans.test.unittests.scopes;


import org.junit.Assert;
import org.apache.webbeans.test.AbstractUnitTest;
import org.apache.webbeans.test.component.Singleton;
import org.apache.webbeans.test.component.service.ITyped2;
import org.apache.webbeans.test.component.service.Typed2;
import org.junit.Test;

public class SingletonComponentTest extends AbstractUnitTest
{
    @Test
    public void testTypedComponent() throws Throwable
    {
        startContainer(Typed2.class, Singleton.class);

        Singleton single = getInstance("singletonInstance");

        Assert.assertEquals("debug", single.logDebug());
        Assert.assertEquals("info", single.logInfoo());

        ITyped2 t = single.getType();

        Assert.assertNotNull(t);
    }

}
