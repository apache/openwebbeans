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

import org.apache.webbeans.test.AbstractUnitTest;
import org.junit.Test;

import jakarta.inject.Inject;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

import static org.junit.Assert.assertNotNull;

// http://openejb.979440.n4.nabble.com/ArrayIndexOutOfBoundsException-on-TomEE-1-6-0-td4665080.html
public class ArrayOuttOfBouncExceptionFromMLTest extends AbstractUnitTest
{
    @Inject
    private TwoKeyHashMap<String, Integer, Value> injected;

    @Test
    public void testGenericBeanInjection()
    {
        startContainer(Arrays.<Class<?>>asList(TwoKeyHashMap.class), Collections.<String>emptyList(), true);
        
        assertNotNull(injected);

        shutDownContainer();
    }

    public static class TwoKeyHashMap<K1, K2, V extends TwoKeyHashMap.TwoKeyValue<K1, K2>> extends HashMap<K1, V>
    {
        public static interface TwoKeyValue<K1, K2>
        {
            K1 getKey1();
            K2 getKey2();
        }
    }

    public static class Value implements TwoKeyHashMap.TwoKeyValue<String, Integer>
    {
        @Override
        public String getKey1()
        {
            return "1";
        }

        @Override
        public Integer getKey2()
        {
            return 2;
        }
    }
}
