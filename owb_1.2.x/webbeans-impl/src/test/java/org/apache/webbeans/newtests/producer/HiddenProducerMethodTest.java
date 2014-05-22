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
package org.apache.webbeans.newtests.producer;

import org.apache.webbeans.newtests.AbstractUnitTest;

import org.apache.webbeans.newtests.producer.beans.PrivateProducedBean;
import org.apache.webbeans.newtests.producer.beans.ProtectedProducedBean;
import org.apache.webbeans.newtests.producer.beans.SampleProducerOwner;
import org.apache.webbeans.newtests.producer.beans.SomeUserBean;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test for protected and private producer methods
 */
public class HiddenProducerMethodTest extends AbstractUnitTest
{
    @Test
    public void testHiddenProducerMethods()
    {
        startContainer(PrivateProducedBean.class, ProtectedProducedBean.class, SampleProducerOwner.class, SomeUserBean.class);

        PrivateProducedBean privateProducedBean = getInstance(PrivateProducedBean.class);
        Assert.assertNotNull(privateProducedBean);
        Assert.assertEquals(42, privateProducedBean.getMeaningOfLife());

        ProtectedProducedBean protectedProducedBean = getInstance(ProtectedProducedBean.class);
        Assert.assertNotNull(protectedProducedBean);
        Assert.assertEquals(42, privateProducedBean.getMeaningOfLife());
    }

}
