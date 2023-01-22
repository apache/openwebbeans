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
package org.apache.webbeans.test.spi.configuration;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.BeforeBeanDiscovery;
import jakarta.enterprise.inject.spi.Extension;
import java.util.HashSet;
import java.util.Set;


import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.spi.ContainerLifecycle;
import org.junit.Test;
import org.junit.Assert;

/**
 * Test the feature to exclude extensions which are not needed.
 *
 * @see org.apache.webbeans.config.OpenWebBeansConfiguration#IGNORED_EXTENSIONS
 */
public class ExcludeExtensionsTest
{
    public static Set<String> EXT_CALLS = new HashSet<>();


    @Test
    public void testExtensionExclusion() throws Exception
    {
        EXT_CALLS.clear();

        ContainerLifecycle containerLifecycle = WebBeansContext.getInstance().getService(ContainerLifecycle.class);
        containerLifecycle.startApplication(null);

        try
        {
            Assert.assertEquals(1, EXT_CALLS.size());
            Assert.assertTrue(EXT_CALLS.contains("B"));
        }
        finally
        {
            containerLifecycle.stopApplication(null);
        }

    }


    public static class ExtensionA implements Extension
    {
        public void bbd(@Observes BeforeBeanDiscovery bbd)
        {
            EXT_CALLS.add("A");
        }
    }

    public static class ExtensionB implements Extension
    {
        public void bbd(@Observes BeforeBeanDiscovery bbd)
        {
            EXT_CALLS.add("B");
        }
    }

    public static class ExtensionC implements Extension
    {
        public void bbd(@Observes BeforeBeanDiscovery bbd)
        {
            EXT_CALLS.add("C");
        }
    }
}
