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

import org.apache.webbeans.config.OWBLogConst;
import org.apache.webbeans.corespi.scanner.AbstractMetaDataDiscovery;
import org.apache.webbeans.corespi.scanner.xbean.CdiArchive;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.logger.WebBeansLoggerFacade;
import org.apache.webbeans.util.WebBeansUtil;
import org.apache.xbean.finder.AnnotationFinder;

import javax.servlet.ServletContext;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Configures the web application to find beans.
 */
public class WebScannerService extends AbstractMetaDataDiscovery
{
    private final static Logger logger = WebBeansLoggerFacade.getLogger(WebScannerService.class);

    protected ServletContext servletContext = null;

    public WebScannerService()
    {
        
    }

    @Override
    protected AnnotationFinder initFinder()
    {
        final Collection<URL> trimmedUrls = new ArrayList<URL>();
        try
        {
            for (final String trimmed : getArchives())
            {
                try
                {
                    String file = trimmed;
                    if (file.endsWith(META_INF_BEANS_XML))
                    {
                        file = file.substring(0, file.length() - META_INF_BEANS_XML.length());
                    }
                    trimmedUrls.add(new URL(file));
                }
                catch (MalformedURLException e)
                {
                    throw new WebBeansConfigurationException("Can't trim url " + trimmed);
                }
            }
        }
        catch (Exception e)
        {
            throw new WebBeansConfigurationException(WebBeansLoggerFacade.getTokenString(OWBLogConst.ERROR_0002), e);
        }

        archive = new CdiArchive(WebBeansUtil.getCurrentClassLoader(), trimmedUrls);
        finder = new AnnotationFinder(archive);

        return finder;
    }

    @Override
    public void init(Object context)
    {
        super.init(context);
        this.servletContext = (ServletContext) context;        
    }
    
    @Override
    protected void configure()
    {
    }

    /**
     *  @return all beans.xml paths
     */
    private Set<String> getArchives() throws Exception
    {
        Set<String> lists = createURLFromMarkerFile();
        String warUrlPath = createURLFromWARFile();

        if (warUrlPath != null)
        {
            lists.add(warUrlPath);
        }

        return lists;
    }

    /* Creates URLs from the marker file */
    protected Set<String> createURLFromMarkerFile() throws Exception
    {
        Set<String> listURL = new HashSet<String>();

        // Root with beans.xml marker.
        String[] urls = findBeansXmlBases(META_INF_BEANS_XML, WebBeansUtil.getCurrentClassLoader());

        if (urls != null)
        {
            String addPath;
            for (String url : urls)
            {
                String fileDir = new URL(url).getFile();
                if (fileDir.endsWith(".jar!/"))
                {
                    fileDir = fileDir.substring(0, fileDir.lastIndexOf("/") + 1) + META_INF_BEANS_XML;

                    //fix for weblogic
                    if (!fileDir.startsWith("file:/"))
                    {
                        fileDir = "file:/" + fileDir;
                    }

                    if (logger.isLoggable(Level.FINE))
                    {
                        logger.log(Level.FINE, "OpenWebBeans found the following url while doing web scanning: " + fileDir);
                    }

                    addPath = "jar:" + fileDir;

                    if (logger.isLoggable(Level.FINE))
                    {
                        logger.log(Level.FINE, "OpenWebBeans added the following jar based path while doing web scanning: " +
                                addPath);
                    }
                }
                else
                {
                    //X TODO check!
                    addPath = "file:" + url + "META-INF/beans.xml";

                    if (logger.isLoggable(Level.FINE))
                    {
                        logger.log(Level.FINE, "OpenWebBeans added the following file based path while doing web scanning: " +
                                addPath);
                    }

                }

                listURL.add(addPath);
            }
        }

        return listURL;
    }

    /**
     * Returns the web application class path if it contains
     * a beans.xml marker file.
     * 
     * @return the web application class path
     * @throws Exception if any exception occurs
     */
    protected String createURLFromWARFile() throws Exception
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

            URL resourceUrl = null;
            final String path = servletContext.getRealPath("/WEB-INF/classes");
            if (path != null)
            {
                final File fp = new File(path);
                if (fp.exists())
                {
                    resourceUrl = fp.toURI().toURL();
                }
            }

            if (resourceUrl == null)
            {
                return null;
            }

            return resourceUrl.toExternalForm();
        }

        return null;
    }

}
