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
    private final List<Properties> configurations = new ArrayList<Properties>();

    private String resource = "META-INF/openwebbeans/openwebbeans.properties";
    private String ordinalKey = "configuration.ordinal";
    private int defaultOrdinal = 100;
    private boolean reverseOrder;

    @Override
    public boolean canTransformResource(String s)
    {
        return resource.equals(s);
    }

    @Override
    public void processResource(String s, InputStream inputStream, List<Relocator> list) throws IOException
    {
        Properties p = new Properties();
        p.load(inputStream);
        configurations.add(p);
    }

    @Override
    public boolean hasTransformedResource()
    {
        return !configurations.isEmpty();
    }

    @Override
    public void modifyOutputStream(JarOutputStream jarOutputStream) throws IOException
    {
        Properties out = mergeProperties(sortProperties(configurations));
        jarOutputStream.putNextEntry(new ZipEntry(resource));
        out.store(jarOutputStream, "# maven " + resource + " merge");
        jarOutputStream.closeEntry();
    }

    public void setReverseOrder(boolean reverseOrder)
    {
        this.reverseOrder = reverseOrder;
    }

    public void setResource(String resource)
    {
        this.resource = resource;
    }

    public void setOrdinalKey(String ordinalKey)
    {
        this.ordinalKey = ordinalKey;
    }

    public void setDefaultOrdinal(int defaultOrdinal)
    {
        this.defaultOrdinal = defaultOrdinal;
    }

    private List<Properties> sortProperties(List<Properties> allProperties)
    {
        List<Properties> sortedProperties = new ArrayList<Properties>();
        for (Properties p : allProperties)
        {
            int configOrder = getConfigurationOrdinal(p);

            int i;
            for (i = 0; i < sortedProperties.size(); i++)
            {
                int listConfigOrder = getConfigurationOrdinal(sortedProperties.get(i));
                if ((!reverseOrder && listConfigOrder > configOrder) || (reverseOrder && listConfigOrder < configOrder))
                {
                    break;
                }
            }
            sortedProperties.add(i, p);
        }
        return sortedProperties;
    }

    private int getConfigurationOrdinal(Properties p)
    {
        String configOrderString = p.getProperty(ordinalKey);
        if (configOrderString != null && configOrderString.length() > 0)
        {
            return Integer.parseInt(configOrderString);
        }
        return defaultOrdinal;
    }

    private static Properties mergeProperties(List<Properties> sortedProperties)
    {
        Properties mergedProperties = new Properties();
        for (Properties p : sortedProperties)
        {
            mergedProperties.putAll(p);
        }

        return mergedProperties;
    }
}
