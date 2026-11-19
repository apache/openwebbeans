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

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.se.SeContainer;
import org.apache.webbeans.spi.BeanArchiveService.BeanDiscoveryMode;
import org.junit.Test;

import java.io.InputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;

import static java.util.Collections.emptyEnumeration;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class PreScannedCDISeScannerServiceTest
{
    @Test
    public void configuredClassesOnly()
    {
        checkDeployment(BeanDiscoveryMode.ALL, false);
    }

    @Test
    public void annotatedDiscovery()
    {
        checkDeployment(BeanDiscoveryMode.ANNOTATED, false);
    }

    @Test
    public void reflectionOnlyAnnotatedDiscovery()
    {
        checkDeployment(BeanDiscoveryMode.ANNOTATED, true);
    }

    private void checkDeployment(final BeanDiscoveryMode mode, final boolean hideBytecode)
    {
        final ClassLoader loader = new ClassLoader(getClass().getClassLoader())
        {
            @Override
            public URL getResource(final String name)
            {
                return hideBytecode && name.endsWith(".class") ? null : super.getResource(name);
            }

            @Override
            public Enumeration<URL> getResources(final String name) throws IOException
            {
                return hideBytecode && name.endsWith(".class") ? emptyEnumeration() : super.getResources(name);
            }

            @Override
            public InputStream getResourceAsStream(final String name)
            {
                return hideBytecode && name.endsWith(".class") ? null : super.getResourceAsStream(name);
            }
        };
        if (hideBytecode)
        {
            final String resource = ConfiguredBean.class.getName().replace('.', '/') + ".class";
            assertNull(loader.getResource(resource));
            assertNull(loader.getResourceAsStream(resource));
        }
        final OWBInitializer initializer = new OWBInitializer()
        {
            @Override
            protected CDISeScannerService createDefaultScannerService()
            {
                return new PreScannedCDISeScannerService();
            }
        };
        initializer.bai.setBeanDiscoveryMode(mode);
        try (final SeContainer container = initializer
                .setClassLoader(loader)
                .disableDiscovery()
                .addProperty(PreScannedCDISeScannerService.class.getName() + ".classes",
                        ConfiguredBean.class.getName() + "," + PlainBean.class.getName())
                .initialize())
        {
            assertTrue(container.isRunning());
            assertEquals("configured", container.select(ConfiguredBean.class).get().value());
            assertEquals(mode == BeanDiscoveryMode.ALL, container.select(PlainBean.class).isResolvable());
            assertTrue(container.select(UnlistedBean.class).isUnsatisfied());
        }
    }

    @Dependent
    public static class ConfiguredBean
    {
        public String value()
        {
            return "configured";
        }
    }

    public static class PlainBean
    {
    }

    @Dependent
    public static class UnlistedBean
    {
    }
}
