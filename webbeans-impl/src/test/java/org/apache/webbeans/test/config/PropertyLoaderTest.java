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
package org.apache.webbeans.test.config;

import static java.util.Arrays.asList;
import static java.util.Collections.enumeration;
import static java.util.Comparator.comparing;

import org.apache.webbeans.config.PropertyLoader;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

public class PropertyLoaderTest
{
    private static final String PROPERTY_FILE = "org/apache/webbeans/test/config/propertyloadertest.properties";
    private static final String PROPERTY_FILE2 = "org/apache/webbeans/test/config/propertyloadertest2.properties";
    private static final String PROPERTY_FILE3 = "org/apache/webbeans/test/config/propertyloadertest3.properties";
    private static final String PROPERTY_FILE4 = "org/apache/webbeans/test/config/propertyloadertest3.properties";

    @Test
    public void testPropertyLoaderCustomMerger() {
        final Thread thread = Thread.currentThread();
        final ClassLoader loader = thread.getContextClassLoader();
        thread.setContextClassLoader(new ClassLoader(loader)
        {
            @Override
            public Enumeration<URL> getResources(String name) throws IOException
            {
                return enumeration(asList(
                        new URL("memory", null, -1, PROPERTY_FILE4,
                                new MemoryHandler("order = 2\ntestConfig=second\nconfiguration.ordinal=2")),
                        new URL("memory", null, -1, PROPERTY_FILE4,
                                new MemoryHandler("order = 1\ntestConfig=first\nconfiguration.ordinal=1"))
                ));
            }
        });
        try
        {
            final Properties p = PropertyLoader.getProperties(PROPERTY_FILE4, props ->
                    props.stream().sorted(comparing(it -> Integer.parseInt(it.getProperty("order"))))
                            .findFirst().orElseThrow(IllegalStateException::new));
            Assert.assertNotNull(p);

            String testValue = p.getProperty("testConfig");
            Assert.assertNotNull(testValue);
            Assert.assertEquals("first", testValue);
        }
        finally
        {
            thread.setContextClassLoader(loader);
        }
    }

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
    
    @Test
    public void testOrdinal()
    {
        Properties p15 = PropertyLoader.getProperties(PROPERTY_FILE);
        Properties p16 = PropertyLoader.getProperties(PROPERTY_FILE2);
        Properties p20 = PropertyLoader.getProperties(PROPERTY_FILE3);
        
        List<Properties> properties = new ArrayList<Properties>();
        properties.add(p15);
        properties.add(p16);
        properties.add(p20);
        
        Properties prop = MockPropertyLoader.mergeProperties(MockPropertyLoader.sortProperties(properties));
        Assert.assertEquals("testValue16", prop.get("testConfig"));
        Assert.assertEquals("16", prop.get("test16"));
        Assert.assertEquals("15", prop.get("test15"));
        Assert.assertEquals("20", prop.get("configuration.ordinal"));
        Assert.assertEquals("z", prop.get("override_y"));
        Assert.assertEquals("20", prop.get("override_all"));
        Assert.assertEquals("15", prop.get("unique_1"));
        Assert.assertEquals("16", prop.get("unique_2"));
        Assert.assertEquals("20", prop.get("unique_3"));
        
    }

    private static class MemoryHandler extends URLStreamHandler
    {
        private final String content;

        private MemoryHandler(final String content)
        {
            this.content = content;
        }

        @Override
        protected URLConnection openConnection(final URL u) {
            return new URLConnection(u)
            {
                @Override
                public void connect()
                {
                    // no-op
                }

                @Override
                public InputStream getInputStream() {
                    return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
                }
            };
        }
    }
}
