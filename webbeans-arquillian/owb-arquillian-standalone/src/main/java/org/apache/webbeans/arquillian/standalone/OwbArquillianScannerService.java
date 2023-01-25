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

import jakarta.enterprise.inject.spi.BeanManager;

import jakarta.decorator.Decorator;
import jakarta.interceptor.Interceptor;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.spi.BDABeansXmlScanner;
import org.apache.webbeans.spi.BdaScannerService;
import org.apache.webbeans.spi.BeanArchiveService;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ArchivePath;
import org.jboss.shrinkwrap.api.Filters;
import org.jboss.shrinkwrap.api.Node;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.ArchiveAsset;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.FileAsset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 *
 */
public class OwbArquillianScannerService implements BdaScannerService
{
    private static final Logger logger = Logger.getLogger(OwbArquillianScannerService.class.getName());


    private static final String WEB_INF_CLASS_FOLDER = "/WEB-INF/classes/";

    private final boolean beansXmlBdaScanningEnabled;
    private final WebBeansContext webBeansContext;
    private final BeanManager beanManager;
    private final BeanArchiveService beanArchiveService;


    private Archive archive;

    private UrlSet beansXmls = new UrlSet();
    private Map<BeanArchiveService.BeanArchiveInformation, Set<Class<?>>> beanClassesPerBda = new HashMap<>();


    public OwbArquillianScannerService()
    {
        this.beansXmlBdaScanningEnabled = false;
        webBeansContext = WebBeansContext.getInstance();
        this.beanManager = webBeansContext.getBeanManagerImpl();
        beanArchiveService = webBeansContext.getBeanArchiveService();
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

    @Override
    public void scan()
    {
        if (archive == null)
        {
            this.archive = null;
            return;
        }

        String archiveName = archive.getName();
        if (archiveName.endsWith(".jar"))
        {
            scanJarArchive(archive);
        }
        else if (archiveName.endsWith(".war"))
        {
            scanWebArchive(archive);
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
        return Collections.EMPTY_SET;
    }

    @Override
    public Map<BeanArchiveService.BeanArchiveInformation, Set<Class<?>>> getBeanClassesPerBda()
    {
        return beanClassesPerBda;
    }

    @Override
    public void release()
    {
        beansXmls.clear();
        beanClassesPerBda.clear();
    }

    @Override
    public boolean isBDABeansXmlScanningEnabled()
    {
        return beansXmlBdaScanningEnabled;
    }

    // --------- private implementation -----------

    private void scanWebArchive(Archive<?> archive)
    {
        URL webBeansXmlUrl = getBeanXmlUrl(archive, "WEB-INF/beans.xml");
        if (webBeansXmlUrl != null)
        {
            beansXmls.add(webBeansXmlUrl);
        }

        URL metainfBeansXmlUrl = getBeanXmlUrl(archive, WEB_INF_CLASS_FOLDER + "META-INF/beans.xml");
        if (metainfBeansXmlUrl != null)
        {
            beansXmls.add(metainfBeansXmlUrl);
        }

        if (metainfBeansXmlUrl != null || webBeansXmlUrl != null)
        {
            BeanArchiveService.BeanArchiveInformation info = beanArchiveService.getBeanArchiveInformation(webBeansXmlUrl != null ? webBeansXmlUrl : metainfBeansXmlUrl);

            // in this case we need to scan the WEB-INF/classses folder for .class files
            Map<ArchivePath, Node> classes = archive.getContent(Filters.include(WEB_INF_CLASS_FOLDER + ".*\\.class"));
            scanClasses(info, classes, WEB_INF_CLASS_FOLDER);
        }


        // and now scan all the jars in the WAR
        Map<ArchivePath, Node> jars = archive.getContent(Filters.include("/WEB-INF/lib/.*\\.jar"));
        for (Map.Entry<ArchivePath, Node> jarEntry : jars.entrySet())
        {
            Asset asset = jarEntry.getValue().getAsset();
            if (asset instanceof FileAsset)
            {
                FileAsset fileAsset = (FileAsset) asset;
                if (fileAsset.getSource().getName().endsWith(".jar"))
                {
                    Archive<?> fileArchive = ShrinkWrap.createFromZipFile(JavaArchive.class, fileAsset.getSource());
                    asset = new ArchiveAsset(fileArchive, ZipExporter.class);
                }
            }
            if (asset instanceof ArchiveAsset)
            {
                ArchiveAsset archiveAsset = (ArchiveAsset) asset;
                JavaArchive jarArchive = (JavaArchive) archiveAsset.getArchive();
                scanJarArchive(jarArchive);
            }
        }
    }

    private void scanJarArchive(Archive<?> archive)
    {
        URL beansXmlUrl = getBeanXmlUrl(archive, "META-INF/beans.xml");

        if (beansXmlUrl == null)
        {
            // that means we switch to 'Implicit BDA' mode
            beansXmlUrl = getBeanXmlUrl(archive, "/");
        }

        // otherwise we store it for later use
        beansXmls.add(beansXmlUrl);

        // and now scan all classes those classes acording to their BDA info
        Map<ArchivePath, Node> classes = archive.getContent(Filters.include(".*\\.class"));
        scanClasses(beanArchiveService.getBeanArchiveInformation(beansXmlUrl), classes, null);
    }

    /**
     *
     * @param classes the scanned classes
     * @param classBasePath the base class in which the classes are, or null if they are directly in the root
     */
    private void scanClasses(BeanArchiveService.BeanArchiveInformation info,
                             Map<ArchivePath, Node> classes, String classBasePath)
    {
        Set<Class<?>> bdaClasses = beanClassesPerBda.computeIfAbsent(info, k -> new HashSet<>());
        if (info != null && info.getBeanDiscoveryMode() == BeanArchiveService.BeanDiscoveryMode.NONE)
        {
            // this jar should not get scanned at all.
            return;
        }

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

            Class<?> beanClass = null;
            try
            {
                beanClass = Class.forName(className);
            }
            catch (ClassNotFoundException cnfe)
            {
                throw new RuntimeException("Could not scan class", cnfe);
            }
            catch (NoClassDefFoundError | UnsatisfiedLinkError ce)
            {
                logger.info("Error while loading class - ignoring class " + className + " " + ce.getMessage());
                continue;
            }

            if (info != null && info.isClassExcluded(className))
            {
                continue;
            }
            if (info != null && info.getBeanDiscoveryMode() == BeanArchiveService.BeanDiscoveryMode.ANNOTATED)
            {
                // only classes with a 'Bean Defining Annotation should get included
                boolean hasBeanDefiningAnnotation = false;
                for (Annotation annotation : beanClass.getAnnotations())
                {
                    if (isBeanDefiningAnnotation(annotation))
                    {
                        hasBeanDefiningAnnotation = true;
                        break;
                    }
                }
                if (!hasBeanDefiningAnnotation)
                {
                    continue;
                }
            }

            bdaClasses.add(beanClass);
        }
    }

    protected boolean isBeanDefiningAnnotation(Annotation annotation)
    {
        Class<? extends Annotation> annotationType = annotation.annotationType();
        boolean isBeanAnnotation = beanManager.isScope(annotationType);
        isBeanAnnotation |= annotationType.equals(Decorator.class) || annotationType.equals(Interceptor.class);
        isBeanAnnotation = isBeanAnnotation || beanManager.isStereotype(annotationType);

        return isBeanAnnotation;
    }

    private URL getBeanXmlUrl(Archive archive, String beansXmlPath)
    {
        Node beansXml = archive.get(beansXmlPath);

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
                            }
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

        beansXmls = new UrlSet();
        beanClassesPerBda = new HashMap<>();
    }
}
