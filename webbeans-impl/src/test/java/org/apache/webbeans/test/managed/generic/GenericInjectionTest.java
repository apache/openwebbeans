/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.webbeans.test.managed.generic;

import org.apache.webbeans.test.AbstractUnitTest;
import org.junit.Test;

import jakarta.enterprise.inject.spi.Bean;
import jakarta.inject.Inject;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.assertNotNull;

public class GenericInjectionTest extends AbstractUnitTest
{
    @Test
    public void testGenericInjection()
    {
        final Collection<Class<?>> beanClasses = new ArrayList<Class<?>>();
        beanClasses.add(StringIntAddress.class);
        beanClasses.add(AddressStringInt.class);
        beanClasses.add(IntStringAddress.class);
        beanClasses.add(Helper.class);

        startContainer(beanClasses);

        // will fail if generics are not handled properly
        final Bean<Helper> bean = getBean(Helper.class);
        final Helper helper = Helper.class.cast(getBeanManager().getReference(bean, Helper.class, null));
        helper.checkAll();

        shutDownContainer();
    }

    public static interface InterfaceWithMultipleGenerics<A, B, C>
    {
    }

    public static class StringIntAddress implements InterfaceWithMultipleGenerics<String, Integer, InetAddress>
    {
    }

    public static class AddressStringInt implements InterfaceWithMultipleGenerics<Integer, InetAddress, String>
    {
    }

    public static class IntStringAddress implements InterfaceWithMultipleGenerics<Integer, String, InetAddress>
    {
    }

    public static class Helper
    {
        @Inject
        private InterfaceWithMultipleGenerics<String, Integer, InetAddress> sia;

        @Inject
        private InterfaceWithMultipleGenerics<Integer, InetAddress, String> ias;

        @Inject
        private InterfaceWithMultipleGenerics<Integer, String, InetAddress> isa;

        public void checkAll()
        {
            assertNotNull(sia);
            assertNotNull(ias);
            assertNotNull(isa);
        }
    }
}
