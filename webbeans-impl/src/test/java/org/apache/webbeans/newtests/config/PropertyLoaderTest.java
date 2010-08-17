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
package org.apache.webbeans.newtests.config;

import org.apache.webbeans.config.PropertyLoader;
import org.junit.Assert;
import org.junit.Test;

import java.util.Properties;

public class PropertyLoaderTest
{
    private static final String PROPERTY_FILE = "org/apache/webbeans/newtests/config/propertyloadertest.properties";

    @Test
    public void testPropertyLoader() throws Exception
    {
        Properties p = PropertyLoader.getProperties(PROPERTY_FILE);
        Assert.assertNotNull(p);

        String testValue = p.getProperty("testConfig");
        Assert.assertNotNull(testValue);
        Assert.assertEquals("testValue", testValue);
    }

    @Test
    public void testNonExistentProperties()
    {
        Properties p = PropertyLoader.getProperties("notexisting.properties");
        Assert.assertNull(p);
    }
}
