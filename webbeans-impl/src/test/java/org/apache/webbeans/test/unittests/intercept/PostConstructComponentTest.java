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
package org.apache.webbeans.test.unittests.intercept;


import junit.framework.Assert;

import org.apache.webbeans.test.AbstractUnitTest;
import org.apache.webbeans.test.component.CheckWithCheckPayment;
import org.apache.webbeans.test.component.PostConstructComponent;
import org.junit.Test;

public class PostConstructComponentTest extends AbstractUnitTest
{
    @SuppressWarnings("unchecked")
    @Test
    public void testTypedComponent() throws Throwable
    {
        startContainer(CheckWithCheckPayment.class, PostConstructComponent.class);

        CheckWithCheckPayment object = getInstance("checkWithCheckPayment");
        PostConstructComponent pcc = getInstance(PostConstructComponent.class);

        CheckWithCheckPayment chk = (CheckWithCheckPayment) pcc.getP();

        Assert.assertNotNull(pcc.getP());
        Assert.assertSame(object.getValue(), chk.getValue());
    }

}
