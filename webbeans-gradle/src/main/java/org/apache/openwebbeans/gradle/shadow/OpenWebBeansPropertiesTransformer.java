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
package org.apache.openwebbeans.gradle.shadow;

import com.github.jengelman.gradle.plugins.shadow.relocation.Relocator;
import com.github.jengelman.gradle.plugins.shadow.transformers.Transformer;
import org.apache.tools.zip.ZipOutputStream;
import org.gradle.api.file.FileTreeElement;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

// note: it is very important to not bring webbeans-impl in the classpath there cause of gradle dep mecanism
public class OpenWebBeansPropertiesTransformer implements Transformer
{
    private final List<Properties> configurations = new ArrayList<Properties>();

    private String resource = "META-INF/openwebbeans/openwebbeans.properties";
    private String ordinalKey = "configuration.ordinal";
    private int defaultOrdinal = 100;
    private boolean reverseOrder;

    @Override
    public boolean canTransformResource(final FileTreeElement s)
    {
        return resource.equals(s.getPath());
    }

    @Override
    public void transform(final String s, final InputStream inputStream, final List<Relocator> list)
    {
        final Properties p = new Properties();
        try
        {
            p.load(inputStream);
        }
        catch (final IOException e)
        {
            throw new IllegalStateException(e);
        }
        configurations.add(p);
    }

    @Override
    public boolean hasTransformedResource()
    {
        return !configurations.isEmpty();
    }

    @Override
    public void modifyOutputStream(final ZipOutputStream zipOutputStream)
    {
        final Properties out = mergeProperties(sortProperties(configurations));
        try
        {
            zipOutputStream.putNextEntry(new org.apache.tools.zip.ZipEntry(resource));
            out.store(zipOutputStream, "# gradle " + resource + " merge");
            zipOutputStream.closeEntry();
        }
        catch (final IOException ioe)
        {
            throw new IllegalStateException(ioe);
        }
    }

    public void setReverseOrder(final boolean reverseOrder)
    {
        this.reverseOrder = reverseOrder;
    }

    public void setResource(final String resource)
    {
        this.resource = resource;
    }

    public void setOrdinalKey(final String ordinalKey)
    {
        this.ordinalKey = ordinalKey;
    }

    public void setDefaultOrdinal(final int defaultOrdinal)
    {
        this.defaultOrdinal = defaultOrdinal;
    }

    private List<Properties> sortProperties(List<Properties> allProperties)
    {
        final List<Properties> sortedProperties = new ArrayList<Properties>();
        for (final Properties p : allProperties)
        {
            final int configOrder = getConfigurationOrdinal(p);

            int i;
            for (i = 0; i < sortedProperties.size(); i++)
            {
                final int listConfigOrder = getConfigurationOrdinal(sortedProperties.get(i));
                if ((!reverseOrder && listConfigOrder > configOrder) || (reverseOrder && listConfigOrder < configOrder))
                {
                    break;
                }
            }
            sortedProperties.add(i, p);
        }
        return sortedProperties;
    }

    private int getConfigurationOrdinal(final Properties p)
    {
        final String configOrderString = p.getProperty(ordinalKey);
        if (configOrderString != null && configOrderString.length() > 0)
        {
            return Integer.parseInt(configOrderString);
        }
        return defaultOrdinal;
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
