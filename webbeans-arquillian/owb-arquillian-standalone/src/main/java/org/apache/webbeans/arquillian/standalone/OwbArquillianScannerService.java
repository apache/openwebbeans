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
package org.apache.webbeans.arquillian.standalone;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.webbeans.spi.BDABeansXmlScanner;
import org.apache.webbeans.spi.ScannerService;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ArchivePath;
import org.jboss.shrinkwrap.api.Filters;
import org.jboss.shrinkwrap.api.Node;
import org.jboss.shrinkwrap.api.asset.ArchiveAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

/**
 *
 */
public class OwbArquillianScannerService implements ScannerService
{

    private final static String WEB_INF_CLASS_FOLDER = "/WEB-INF/classes/";

    private boolean beansXmlBdaScanningEnabled;
    private Archive archive;

    private Set<URL> beansXmls = new HashSet<URL>();
    private Set<Class<?>> beanClasses = new HashSet<Class<?>>();


    public OwbArquillianScannerService()
    {
        this.beansXmlBdaScanningEnabled = false;
    }

    @Override
    public BDABeansXmlScanner getBDABeansXmlScanner()
    {
        return null;
    }

    @Override
    public void init(Object object)
    {
        // nothing to do
    }

    public void setArchive(Archive archive)
    {
        this.archive = archive;
    }

    @Deprecated
    public Set<String> getAllAnnotations(String className)
    {
        return Collections.EMPTY_SET;
    }

    @Override
    public void scan()
    {
        if (archive == null)
        {
            return;
        }

        if (archive instanceof JavaArchive)
        {
            scanJarArchive((JavaArchive) archive);
        }
        else if (archive instanceof WebArchive)
        {
            scanWebArchive((WebArchive) archive);
        }
        else
        {
            throw new IllegalStateException("Scanning of Archive " + archive.getClass().getName() + "Not yet implemented");
        }
    }

    @Override
    public Set<URL> getBeanXmls()
    {
        return beansXmls;
    }

    @Override
    public Set<Class<?>> getBeanClasses()
    {
        return beanClasses;
    }

    @Override
    public void release()
    {
        beansXmls.clear();
        beanClasses.clear();
    }

    @Override
    public boolean isBDABeansXmlScanningEnabled()
    {
        return beansXmlBdaScanningEnabled;
    }

    // --------- private implementation -----------

    private void scanWebArchive(WebArchive archive)
    {
        URL webBeansXmlUrl = getBeanXmlUrl(archive, "WEB-INF/beans.xml");
        if (webBeansXmlUrl != null)
        {
            beansXmls.add(webBeansXmlUrl);
        }

        URL metainfBeansXmlUrl = getBeanXmlUrl(archive, WEB_INF_CLASS_FOLDER + "META-INF/beans.xml");
        if (metainfBeansXmlUrl != null)
        {
            beansXmls.add(webBeansXmlUrl);
        }

        if (metainfBeansXmlUrl != null || webBeansXmlUrl != null)
        {
            // in this case we need to scan the WEB-INF/classses folder for .class files
            Map<ArchivePath, Node> classes = archive.getContent(Filters.include(WEB_INF_CLASS_FOLDER + ".*\\.class"));
            scanClasses(classes, WEB_INF_CLASS_FOLDER);
        }


        // and now scan all the jars in the WAR
        Map<ArchivePath, Node> jars = archive.getContent(Filters.include("/WEB-INF/lib/.*\\.jar"));
        for (Map.Entry<ArchivePath, Node> jarEntry : jars.entrySet())
        {
            ArchiveAsset archiveAsset = (ArchiveAsset) jarEntry.getValue().getAsset();
            JavaArchive jarArchive = (JavaArchive) archiveAsset.getArchive();
            scanJarArchive(jarArchive);
        }

    }

    private void scanJarArchive(JavaArchive archive)
    {
        URL beansXmlUrl = getBeanXmlUrl(archive, "META-INF/beans.xml");

        if (beansXmlUrl == null)
        {
            // this is not a CDI archive
            return;
        }

        // otherwise we store it for later use
        beansXmls.add(beansXmlUrl);

        // and now add all classes
        Map<ArchivePath, Node> classes = archive.getContent(Filters.include(".*\\.class"));
        scanClasses(classes, null);
    }

    /**
     *
     * @param classes the scanned classes
     * @param classBasePath the base class in which the classes are, or null if they are directly in the root
     */
    private void scanClasses(Map<ArchivePath, Node> classes, String classBasePath)
    {
        for (Map.Entry<ArchivePath, Node> classEntry : classes.entrySet())
        {
            String className = classEntry.getKey().get();

            if (classBasePath != null && className.startsWith(WEB_INF_CLASS_FOLDER))
            {
                className = className.substring(WEB_INF_CLASS_FOLDER.length());
            }

            // cut off leading slashes
            if (className.startsWith("/"))
            {
                className = className.substring(1);
            }

            // cut off ".class"
            className = className.substring(0, className.length() - ".class".length());

            className = className.replace('/', '.');


            try
            {
                Class<?> beanClass = Class.forName(className);
                beanClasses.add(beanClass);
            }
            catch (ClassNotFoundException cnfe)
            {
                throw new RuntimeException("Could not scan class", cnfe);
            }
        }
    }

    private URL getBeanXmlUrl(Archive archive, String beansXmlPath)
    {
        final Node beansXml = archive.get(beansXmlPath);

        if (beansXml == null)
        {
            return null;
        }

        try
        {
            String urlLocation = "archive://" + archive.getName() + "/" + beansXmlPath;

            return  new URL(null, urlLocation, new URLStreamHandler()
                        {
                            @Override
                            protected URLConnection openConnection(URL u) throws IOException
                            {
                                return new URLConnection(u)
                                {
                                    @Override
                                    public void connect() throws IOException
                                    {}

                                    @Override
                                    public InputStream getInputStream() throws IOException
                                    {
                                        return beansXml.getAsset().openStream();
                                    }
                                };
                            };
                        });
        }
        catch (Exception e)
        {
            RuntimeException runtimeException;
            if (e instanceof RuntimeException)
            {
                runtimeException = (RuntimeException) e;
            }
            else
            {
                runtimeException = new RuntimeException("Error while parsing beans.xml location", e);
            }

            throw runtimeException;
        }
    }


    public void clear()
    {
        archive = null;

        beansXmls = new HashSet<URL>();
        beanClasses = new HashSet<Class<?>>();
    }
}
