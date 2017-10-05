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
package org.apache.webbeans.context.control;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.control.RequestContextController;

import org.apache.webbeans.spi.ContextsService;
import org.apache.webbeans.test.AbstractUnitTest;
import org.junit.Test;

public class OwbRequestContextControllerTest extends AbstractUnitTest
{
    @Test
    public void check()
    {
        startContainer();
        final ContextsService cs = getWebBeansContext().getContextsService();
        cs.endContext(RequestScoped.class, null); // it is started by AbstractUnitTest

        final RequestContextController controller = getInstance(RequestContextController.class);
        assertNull(cs.getCurrentContext(RequestScoped.class));
        assertTrue(controller.activate());
        assertFalse(controller.activate());
        assertFalse(getInstance(RequestContextController.class).activate());
        assertTrue(cs.getCurrentContext(RequestScoped.class).isActive());
        controller.deactivate();
        assertNull(cs.getCurrentContext(RequestScoped.class));
    }
}
