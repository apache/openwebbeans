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
package org.apache.openwebbeans.maven.shade;

import org.apache.maven.plugins.shade.relocation.Relocator;
import org.apache.maven.plugins.shade.resource.ResourceTransformer;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

public class OpenWebBeansPropertiesTransformer implements ResourceTransformer
{
    private static final String CONFIGURATION_ORDINAL_PROPERTY_NAME = "configuration.ordinal";
    private static final int CONFIGURATION_ORDINAL_DEFAULT_VALUE = 100;

    private final List<Properties> configurations = new ArrayList<Properties>();

    @Override
    public boolean canTransformResource(final String s)
    {
        return "META-INF/openwebbeans/openwebbeans.properties".equals(s) || "/META-INF/openwebbeans/openwebbeans.properties".equals(s);
    }

    @Override
    public void processResource(final String s, final InputStream inputStream, final List<Relocator> list) throws IOException
    {
        final Properties p = new Properties();
        p.load(inputStream);
        configurations.add(p);
    }

    @Override
    public boolean hasTransformedResource()
    {
        return !configurations.isEmpty();
    }

    @Override
    public void modifyOutputStream(final JarOutputStream jarOutputStream) throws IOException
    {
        final Properties out = mergeProperties(sortProperties(configurations));
        jarOutputStream.putNextEntry(new ZipEntry("META-INF/openwebbeans/openwebbeans.properties"));
        out.store(jarOutputStream, "# gradle openwebbeans.properties merge");
        jarOutputStream.closeEntry();
    }

    private static List<Properties> sortProperties(List<Properties> allProperties)
    {
        final List<Properties> sortedProperties = new ArrayList<Properties>();
        for (final Properties p : allProperties)
        {
            final int configOrder = getConfigurationOrdinal(p);

            int i;
            for (i = 0; i < sortedProperties.size(); i++)
            {
                final int listConfigOrder = getConfigurationOrdinal(sortedProperties.get(i));
                if (listConfigOrder > configOrder)
                {
                    break;
                }
            }
            sortedProperties.add(i, p);
        }
        return sortedProperties;
    }

    private static int getConfigurationOrdinal(final Properties p)
    {
        final String configOrderString = p.getProperty(CONFIGURATION_ORDINAL_PROPERTY_NAME);
        if (configOrderString != null && configOrderString.length() > 0)
        {
            return Integer.parseInt(configOrderString);
        }
        return CONFIGURATION_ORDINAL_DEFAULT_VALUE;
    }

    private static Properties mergeProperties(final List<Properties> sortedProperties)
    {
        final Properties mergedProperties = new Properties();
        for (final Properties p : sortedProperties)
        {
            mergedProperties.putAll(p);
        }

        return mergedProperties;
    }
}
