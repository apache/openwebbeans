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
package org.apache.webbeans.spi.ee.deployer;

import java.net.URL;
import java.util.HashSet;

import java.util.Set;

import javax.servlet.ServletContext;

import org.apache.webbeans.logger.WebBeansLogger;
import org.apache.webbeans.spi.deployer.AbstractMetaDataDiscovery;
import org.apache.webbeans.util.WebBeansUtil;
import org.scannotation.ClasspathUrlFinder;
import org.scannotation.WarUrlFinder;

/**
 * Configures the web application to find beans.
 */
public class WarMetaDataDiscoveryImpl extends AbstractMetaDataDiscovery
{
    private WebBeansLogger logger = WebBeansLogger.getLogger(WarMetaDataDiscoveryImpl.class);

    private boolean configure = false;

    private ServletContext servletContext = null;

    public WarMetaDataDiscoveryImpl()
    {
        
    }

    public void init(Object context)
    {
        this.servletContext = (ServletContext) context;        
    }
    
    protected void configure() throws Exception
    {
        try
        {
            if (!configure)
            {
                Set<URL> arcs = getArchieves();
                URL[] urls = new URL[arcs.size()];
                arcs.toArray(urls);

                ANNOTATION_DB.scanArchives(urls);
                configure = true;
            }

        }
        catch (Exception e)
        {
            logger.error("Initializing of the WebBeans container is failed.", e);
            throw e;
        }

    }

    /* Collects all URLs */
    private Set<URL> getArchieves() throws Exception
    {
        Set<URL> lists = createURLFromMarkerFile();
        URL warUrl = createURLFromWARFile();

        if (warUrl != null)
        {
            lists.add(warUrl);
        }

        return lists;
    }

    /* Creates URLs from the marker file */
    private Set<URL> createURLFromMarkerFile() throws Exception
    {
        Set<URL> listURL = new HashSet<URL>();
        URL[] urls = null;

        // Root with beans.xml marker.
        urls = ClasspathUrlFinder.findResourceBases("META-INF/beans.xml", WebBeansUtil.getCurrentClassLoader());

        if (urls != null)
        {
            for (URL url : urls)
            {

                URL addPath = null;

                String fileDir = url.getFile();
                if (fileDir.endsWith(".jar!/"))
                {
                    fileDir = fileDir.substring(0, fileDir.lastIndexOf("/")) + "/META-INF/beans.xml";
                    addPath = new URL("jar:" + fileDir);
                }
                else
                {
                    addPath = new URL("file:" + url.getFile() + "META-INF/beans.xml");
                }

                listURL.add(url);

                WEBBEANS_XML_LOCATIONS.put(addPath.getFile(), addPath.openStream());
            }
        }

        return listURL;
    }

    private URL createURLFromWARFile() throws Exception
    {
        URL url = this.servletContext.getResource("/WEB-INF/beans.xml");

        if (url != null)
        {
            WEBBEANS_XML_LOCATIONS.put(url.getFile(), url.openStream());

            return WarUrlFinder.findWebInfClassesPath(this.servletContext);
        }

        return null;
    }

}
