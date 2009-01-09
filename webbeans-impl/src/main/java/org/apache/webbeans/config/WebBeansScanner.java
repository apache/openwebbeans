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
package org.apache.webbeans.config;

import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import java.util.Set;

import javax.servlet.ServletContext;

import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.logger.WebBeansLogger;
import org.apache.webbeans.util.WebBeansUtil;
import org.scannotation.AnnotationDB;
import org.scannotation.ClasspathUrlFinder;
import org.scannotation.WarUrlFinder;

/**
 * Configures the <b>Web Beans Container</b> at the enterprise application
 * deployment time.
 * <p>
 * See the web beans specification section-10 for further details of how to
 * configure web beans container.
 * </p>
 * 
 * @author <a href="mailto:gurkanerdogdu@yahoo.com">Gurkan Erdogdu</a>
 * @since 1.0
 */
public final class WebBeansScanner
{
    private WebBeansLogger logger = WebBeansLogger.getLogger(WebBeansScanner.class);

    /** Location of the web-beans.xml files. */
    private Map<String, InputStream> WEBBEANS_XML_LOCATIONS = new HashMap<String, InputStream>();

    /** Location of the ejb-jar.xml files */
    private Map<String, InputStream> EJB_XML_LOCATIONS = new HashMap<String, InputStream>();

    /** Annotation Database */
    private AnnotationDB ANNOTATION_DB = null;

    /** It is configured or not */
    private boolean configure = false;

    private ServletContext servletContext = null;

    public WebBeansScanner()
    {

    }

    /**
     * Configure the Web Beans Container with deployment information and fills
     * annotation database and web-beans.xml stream database.
     * 
     * @throws WebBeansConfigurationException if any run time exception occurs
     */
    public void scan(ServletContext servletContext) throws WebBeansConfigurationException
    {
        if (!configure)
        {
            this.servletContext = servletContext;

            configureAnnotationDB();
            configure = true;

        }

    }

    /* Configures annotation database */
    private void configureAnnotationDB() throws WebBeansConfigurationException
    {
        try
        {
            Set<URL> arcs = getArchieves();
            URL[] urls = new URL[arcs.size()];
            arcs.toArray(urls);

            if (ANNOTATION_DB == null)
            {
                ANNOTATION_DB = new AnnotationDB();

                ANNOTATION_DB.setScanClassAnnotations(true);
                ANNOTATION_DB.scanArchives(urls);
                ANNOTATION_DB.crossReferenceMetaAnnotations();

            }

        }
        catch (Throwable e)
        {
            logger.error("Initializing of the WebBeans container is failed.", e);
            throw new WebBeansConfigurationException(e);
        }

    }

    /* Collects all URLs */
    private Set<URL> getArchieves() throws Throwable
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
    private Set<URL> createURLFromMarkerFile() throws Throwable
    {
        Set<URL> listURL = new HashSet<URL>();
        URL[] urls = null;

        // Root with web-beans.xml marker.
        urls = ClasspathUrlFinder.findResourceBases("META-INF/web-beans.xml", WebBeansUtil.getCurrentClassLoader());

        if (urls != null)
        {
            for (URL url : urls)
            {

                URL addPath = null;

                String fileDir = url.getFile();
                if (fileDir.endsWith(".jar!/"))
                {
                    fileDir = fileDir.substring(0, fileDir.lastIndexOf("/")) + "/META-INF/web-beans.xml";
                    addPath = new URL("jar:" + fileDir);
                }
                else
                {
                    addPath = new URL("file:" + url.getFile() + "META-INF/web-beans.xml");
                }

                listURL.add(url);
                WEBBEANS_XML_LOCATIONS.put(addPath.getFile(), addPath.openStream());
            }
        }

        // Scan for ejb-jar.xml
        URL[] ejbUrls = ClasspathUrlFinder.findResourceBases("META-INF/ejb-jar.xml", WebBeansUtil.getCurrentClassLoader());

        if (ejbUrls != null && ejbUrls.length > 0)
        {
            for (URL ejbUrl : ejbUrls)
            {
                // ok, web-beans.xml and ejb-jar.xml is in the same root
                if (listURL.contains(ejbUrl))
                {
                    URL addPath = null;

                    String fileDir = ejbUrl.getFile();

                    if (fileDir.endsWith(".jar!/"))
                    {
                        fileDir = fileDir.substring(0, fileDir.lastIndexOf("/")) + "/META-INF/ejb-jar.xml";
                        addPath = new URL("jar:" + fileDir);
                    }
                    else
                    {
                        addPath = new URL("file:" + ejbUrl.getFile() + "META-INF/ejb-jar.xml");
                    }

                    EJB_XML_LOCATIONS.put(addPath.getFile(), addPath.openStream());
                }
            }
        }

        return listURL;
    }

    private URL createURLFromWARFile() throws Throwable
    {
        URL url = this.servletContext.getResource("/WEB-INF/web-beans.xml");

        if (url != null)
        {
            WEBBEANS_XML_LOCATIONS.put(url.getFile(), url.openStream());

            return WarUrlFinder.findWebInfClassesPath(this.servletContext);
        }

        return null;
    }

    /**
     * Gets list of stream that points to the web-beans.xml file in the specific
     * locations.
     * 
     * @return list of stream
     */
    public Map<String, InputStream> getWEBBEANS_XML_LOCATIONS()
    {
        return WEBBEANS_XML_LOCATIONS;
    }

    /**
     * Gets list of stream that points to the ejb-jar.xml file in the specific
     * locations.
     * 
     * @return list of stream
     */
    public Map<String, InputStream> getEJB_XML_LOCATIONS()
    {
        return EJB_XML_LOCATIONS;
    }

    /**
     * Gets annotated classes.
     * 
     * @return annotation database
     */
    public AnnotationDB getANNOTATION_DB()
    {
        return ANNOTATION_DB;
    }
}
