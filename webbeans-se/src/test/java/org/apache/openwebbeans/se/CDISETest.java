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
package org.apache.openwebbeans.se;

import org.junit.Test;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.se.SeContainer;
import jakarta.enterprise.inject.se.SeContainerInitializer;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class CDISETest
{
    @Test
    public void scanning()
    {
        // no scanning
        try (final SeContainer container = SeContainerInitializer.newInstance().disableDiscovery().initialize())
        {
            assertTrue(container.isRunning());
            assertTrue(container.select(ImNotScanned.class).isUnsatisfied());
        }
        // class
        try (final SeContainer container = SeContainerInitializer.newInstance()
                .disableDiscovery()
                .addBeanClasses(ImNotScanned.class)
                .initialize())
        {
            assertNotNull(container.select(ImNotScanned.class).get());
        }
        // from package
        try (final SeContainer container = SeContainerInitializer.newInstance()
                .disableDiscovery()
                .addPackages(ImNotScanned.class.getPackage())
                .initialize())
        {
            assertNotNull(container.select(ImNotScanned.class).get());
        }
        // from package based on a class
        try (final SeContainer container = SeContainerInitializer.newInstance()
                .disableDiscovery()
                .addPackages(ImNotScanned.class)
                .initialize())
        {
            assertNotNull(container.select(ImNotScanned.class).get());
        }
    }

    @Test
    public void discovery() {
        // regular discovery
        try (final SeContainer container = SeContainerInitializer.newInstance()
                .initialize()) {
            assertTrue(container.isRunning());
            assertTrue(container.select(Scanned.class).isResolvable());
        }
    }

    @Test // main for OWB-1260 and enable java 11 testing
    public void fire() {
        try (final SeContainer container = SeContainerInitializer.newInstance()
                .disableDiscovery()
                .initialize()) {
            container.getBeanManager().fireEvent(new Object());
        }
    }

    public static class ImNotScanned {
    }

    @ApplicationScoped
    public static class Scanned {
    }
}
