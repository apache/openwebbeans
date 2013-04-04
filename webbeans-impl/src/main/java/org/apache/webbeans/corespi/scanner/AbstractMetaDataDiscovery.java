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
package org.apache.webbeans.corespi.scanner;


import org.apache.webbeans.config.OWBLogConst;
import org.apache.webbeans.config.OpenWebBeansConfiguration;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.corespi.scanner.xbean.CdiArchive;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.exception.WebBeansDeploymentException;
import org.apache.webbeans.logger.WebBeansLoggerFacade;
import org.apache.webbeans.spi.BDABeansXmlScanner;
import org.apache.webbeans.spi.ScannerService;
import org.apache.webbeans.util.ClassUtil;
import org.apache.webbeans.util.WebBeansUtil;
import org.apache.xbean.finder.AnnotationFinder;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class AbstractMetaDataDiscovery implements ScannerService
{
    protected static final Logger logger = WebBeansLoggerFacade.getLogger(AbstractMetaDataDiscovery.class);

    public static final String META_INF_BEANS_XML = "META-INF/beans.xml";

    /** Location of the beans.xml files. */
    private final Set<URL> webBeansXmlLocations = new HashSet<URL>();

    //private Map<String, InputStream> EJB_XML_LOCATIONS = new HashMap<String, InputStream>();

    protected ClassLoader loader;
    protected CdiArchive archive;
    protected AnnotationFinder finder;
    protected boolean isBDAScannerEnabled = false;
    protected BDABeansXmlScanner bdaBeansXmlScanner;

    protected AnnotationFinder initFinder()
    {
        if (finder != null)
        {
            return finder;
        }

        final Collection<URL> trimmedUrls = new ArrayList<URL>();
        for (final URL url : getUrls())
        {
            try
            {
                String file = url.getFile();
                if (file.endsWith(META_INF_BEANS_XML))
                {
                    file = file.substring(0, file.length() - META_INF_BEANS_XML.length());
                }
                else if (file.endsWith("WEB-INF/beans.xml"))
                {
                    file = file.substring(0, file.length() - "WEB-INF/beans.xml".length());
                }
                trimmedUrls.add(new URL(url.getProtocol(), url.getHost(), url.getPort(), file));
            }
            catch (MalformedURLException e)
            {
                throw new WebBeansConfigurationException("Can't trim url " + url.toExternalForm());
            }
        }

        archive = new CdiArchive(WebBeansUtil.getCurrentClassLoader(), trimmedUrls);
        finder = new AnnotationFinder(archive);

        return finder;
    }

    protected Iterable<URL> getUrls()
    {
        return webBeansXmlLocations;
    }

    /**
     * Configure the Web Beans Container with deployment information and fills
     * annotation database and beans.xml stream database.
     *
     * @throws org.apache.webbeans.exception.WebBeansConfigurationException if any run time exception occurs
     */
    public void scan() throws WebBeansDeploymentException
    {
        try
        {
            configure();
            initFinder();
        }
        catch (Exception e)
        {
            throw new WebBeansDeploymentException(e);
        }
    }

    public void release()
    {
        finder = null;
        archive = null;
        loader = null;
    }

    abstract protected void configure();

    /**
     * Find the base paths of all available resources with the given
     * resourceName in the classpath.
     * The returned Strings will <i>NOT</i> contain the resourceName itself!
     *
     * @param resourceName the name of the resource, e.g. 'META-INF/beans.xml'
     * @param loader the ClassLoader which should be used
     * @return array of Strings with the URL path to the resources.
     */
    protected String[] findBeansXmlBases(String resourceName, ClassLoader loader)
    {
        this.loader = loader;

        ArrayList<String> list = new ArrayList<String>();
        try
        {
            Enumeration<URL> urls = loader.getResources(resourceName);

            while (urls.hasMoreElements())
            {
                URL url = urls.nextElement();

                addWebBeansXmlLocation(url);

                String urlString = url.toString();
                int idx = urlString.lastIndexOf(resourceName);
                urlString = urlString.substring(0, idx);

                list.add(urlString);
            }
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        return list.toArray(new String[list.size()]);

    }



    public void init(Object object)
    {
        // set per BDA beans.xml flag here because setting it in constructor
        // occurs before
        // properties are loaded.
        String usage = WebBeansContext.currentInstance().getOpenWebBeansConfiguration().getProperty(OpenWebBeansConfiguration.USE_BDA_BEANSXML_SCANNER);
        isBDAScannerEnabled = Boolean.parseBoolean(usage);
    }

    public Set<String> getAllAnnotations(String className)
    {
        throw new UnsupportedOperationException();
    }


    /**
     * add the given beans.xml path to the locations list
     * @param beansXmlUrl location path
     */
    protected void addWebBeansXmlLocation(URL beansXmlUrl)
    {
        if(logger.isLoggable(Level.INFO))
        {
            logger.info("added beans.xml marker: " + beansXmlUrl.toExternalForm());
        }
        webBeansXmlLocations.add(beansXmlUrl);
    }

    /* (non-Javadoc)
     * @see org.apache.webbeans.corespi.ScannerService#getBeanClasses()
     */
    public Set<Class<?>> getBeanClasses()
    {
        final Set<Class<?>> classSet = new HashSet<Class<?>>();
        for(String str : archive.getClasses())
        {
            try
            {
                Class<?> clazz = ClassUtil.getClassFromName(str);
                if (clazz != null)
                {

                    // try to provoke a NoClassDefFoundError exception which is thrown
                    // if some dependencies of the class are missing
                    clazz.getDeclaredFields();
                    clazz.getDeclaredMethods();

                    // we can add this class cause it has been loaded completely
                    classSet.add(clazz);

                }
            }
            catch (NoClassDefFoundError e)
            {
                if (logger.isLoggable(Level.WARNING))
                {
                    logger.log(Level.WARNING, OWBLogConst.WARN_0018, new Object[] { str, e.toString() });
                }
            }
        }

        return classSet;
    }


    /* (non-Javadoc)
    * @see org.apache.webbeans.corespi.ScannerService#getBeanXmls()
    */
    public Set<URL> getBeanXmls()
    {
        return Collections.unmodifiableSet(webBeansXmlLocations);
    }

    public BDABeansXmlScanner getBDABeansXmlScanner()
    {
        return bdaBeansXmlScanner;
    }

    public boolean isBDABeansXmlScanningEnabled()
    {
        return isBDAScannerEnabled;
    }
}
