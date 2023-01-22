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
package org.apache.openwebbeans.junit5.reusable;

import org.apache.openwebbeans.junit5.bean.MyService;
import org.apache.webbeans.config.WebBeansContext;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertEquals;

abstract class Base {
    private static WebBeansContext current;

    @Inject
    private MyService service;

    @Test
    void test1()
    {
        doTest();
    }

    @Test
    void test2()
    {
        doTest();
    }

    private void doTest()
    {
        if (current == null)
        {
            current = WebBeansContext.currentInstance();
        }
        else
        {
            assertEquals(current, WebBeansContext.currentInstance());
        }
        assertEquals("ok", service.ok());
    }
}
