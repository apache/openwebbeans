/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.apache.webbeans.spi.se.deployer;

import java.io.IOException;

import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Enumeration;

import org.apache.webbeans.spi.deployer.AbstractMetaDataDiscovery;

import org.scannotation.ClasspathUrlFinder;

public class MetaDataDiscoveryStandard extends AbstractMetaDataDiscovery
{
    public MetaDataDiscoveryStandard()
    {
        super();
    }

    protected void configure() throws Exception
    {
        configureAnnotationDB();
    }

    private void configureAnnotationDB() throws Exception
    {
        ClassLoader loader = null;

        loader = AccessController.doPrivileged(new PrivilegedAction<ClassLoader>()
        {

            public ClassLoader run()
            {
                try
                {
                    return Thread.currentThread().getContextClassLoader();

                }
                catch (Exception e)
                {
                    return null;
                }
            }

        });

        if (loader == null)
        {
            loader = this.getClass().getClassLoader();
        }

        URL[] urls = ClasspathUrlFinder.findResourceBases("META-INF/beans.xml", loader);
        this.ANNOTATION_DB.scanArchives(urls);

        configureXML();

    }

    private void configureXML() throws Exception
    {
        try
        {
            Enumeration<URL> resources = getClass().getClassLoader().getResources("META-INF/beans.xml");

            while (resources.hasMoreElements())
            {
                URL resource = resources.nextElement();
                this.WEBBEANS_XML_LOCATIONS.put(resource.getFile(), resource.openStream());
            }

        }
        catch (IOException e)
        {
            throw e;
        }
    }

}
