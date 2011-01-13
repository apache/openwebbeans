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
package org.apache.webbeans.web.scanner;

import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.ServletContext;

import org.apache.webbeans.config.OWBLogConst;
import org.apache.webbeans.corespi.scanner.AbstractMetaDataDiscovery;
import org.apache.webbeans.logger.WebBeansLogger;
import org.apache.webbeans.util.WebBeansUtil;
import org.scannotation.ClasspathUrlFinder;
import org.scannotation.WarUrlFinder;

/**
 * Configures the web application to find beans.
 */
public class WebScannerService extends AbstractMetaDataDiscovery
{
    private final WebBeansLogger logger = WebBeansLogger.getLogger(WebScannerService.class);

    private boolean configure = false;

    protected ServletContext servletContext = null;

    public WebScannerService()
    {
        
    }

    public void init(Object context)
    {
        super.init(context);
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

                getAnnotationDB().scanArchives(urls);
                
                configure = true;
            }

        }
        catch (Exception e)
        {
            logger.error(OWBLogConst.ERROR_0002, e);
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
    protected Set<URL> createURLFromMarkerFile() throws Exception
    {
        Set<URL> listURL = new HashSet<URL>();

        // Root with beans.xml marker.
        URL[] urls = ClasspathUrlFinder.findResourceBases("META-INF/beans.xml", WebBeansUtil.getCurrentClassLoader());

        if (urls != null)
        {
            URL addPath;
            for (URL url : urls)
            {
                String fileDir = url.getFile();
                if (fileDir.endsWith(".jar!/"))
                {
                    fileDir = fileDir.substring(0, fileDir.lastIndexOf("/")) + "/META-INF/beans.xml";

                    //fix for weblogic
                    if (!fileDir.startsWith("file:/"))
                    {
                        fileDir = "file:/" + fileDir;
                    }

                    addPath = new URL("jar:" + fileDir);
                    url = new URL("jar:" + fileDir);
                }
                else
                {
                    addPath = new URL("file:" + url.getFile() + "META-INF/beans.xml");
                }

                listURL.add(url);

                addWebBeansXmlLocation(addPath);
            }
        }

        return listURL;
    }

    /**
     * Returns <code>URL</code> of the web application class path.
     * 
     * @return <code>URL</code> of the web application class path
     * @throws Exception if any exception occurs
     */
    protected URL createURLFromWARFile() throws Exception
    {
        if (servletContext == null)
        {
            // this may happen if we are running in a test container, in IDE development, etc
            return null;
        }
        
        URL url = servletContext.getResource("/WEB-INF/beans.xml");

        if (url != null)
        {
            addWebBeansXmlLocation(url);
            URL resourceuUrl = WarUrlFinder.findWebInfClassesPath(this.servletContext);
            //set resource to beans.xml mapping
            getAnnotationDB().setResourceBeansXml(resourceuUrl, url);
            return resourceuUrl;
        }

        return null;
    }

}
