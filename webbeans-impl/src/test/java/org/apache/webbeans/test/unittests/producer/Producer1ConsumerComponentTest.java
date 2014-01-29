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
package org.apache.webbeans.test.unittests.producer;

import junit.framework.Assert;

import org.apache.webbeans.test.AbstractUnitTest;
import org.apache.webbeans.test.component.producer.Producer1;
import org.apache.webbeans.test.component.service.IService;
import org.apache.webbeans.test.component.service.Producer1ConsumerComponent;
import org.apache.webbeans.test.component.service.ServiceImpl1;
import org.junit.Test;

public class Producer1ConsumerComponentTest extends AbstractUnitTest
{
    @Test
    public void testTypedComponent() throws Throwable
    {
        startContainer(IService.class, ServiceImpl1.class, Producer1.class, Producer1ConsumerComponent.class);

        IService svc1Named = getInstance("service");
        Assert.assertNotNull(svc1Named);
        Assert.assertEquals("ServiceImpl1", svc1Named.service());

        Producer1ConsumerComponent single = getInstance(Producer1ConsumerComponent.class);

        IService service = single.getService();
        Assert.assertNotNull(service);
        Assert.assertEquals("ServiceImpl1", service.service());
    }
}
