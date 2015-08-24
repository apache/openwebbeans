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
package org.apache.webbeans.test.injection.generics;

import org.apache.webbeans.component.InstanceBean;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class MyBean
{
    @Inject
    private Instance<MyInterface> stringBean;

    @Inject
    private Instance<MyAbstract> stringAbstractBean;

    public void checkAll() {
        assertNotNull(stringBean);
        assertNotNull(stringAbstractBean);
        assertEquals(StringBean.class.getName(), stringBean.get().getValue());
        assertEquals(StringBeanAbstract.class.getName(), stringAbstractBean.get().getValue());
    }
}
