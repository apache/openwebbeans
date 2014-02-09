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
import org.apache.webbeans.corespi.scanner.xbean.OwbAnnotationFinder;
import org.apache.webbeans.exception.WebBeansDeploymentException;
import org.apache.webbeans.logger.WebBeansLoggerFacade;
import org.apache.webbeans.spi.BDABeansXmlScanner;
import org.apache.webbeans.spi.BeanArchiveService;
import org.apache.webbeans.spi.BeanArchiveService.BeanDiscoveryMode;
import org.apache.webbeans.spi.ScannerService;
import org.apache.webbeans.util.ClassUtil;
import org.apache.webbeans.util.UrlSet;
import org.apache.webbeans.util.WebBeansUtil;
import org.apache.xbean.finder.AnnotationFinder;
import org.apache.xbean.finder.ClassLoaders;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;


public abstract class AbstractMetaDataDiscovery implements ScannerService
{
    protected static final Logger logger = WebBeansLoggerFacade.getLogger(AbstractMetaDataDiscovery.class);

    public static final String META_INF_BEANS_XML = "META-INF/beans.xml";

    private BeanArchiveService beanArchiveService;

    /**
     * Location of the beans.xml files.
     * Since CDI-1.1 (OWB-2.0) this also includes 'implicit bean archives.
     * Means URLs of JARs which do not have a beans.xml marker file.
     */
    private final UrlSet beanArchiveLocations = new UrlSet();

    /**
     * This Map contains the corresponding deployment URL for each beans.xml locations.
     *
     * key: the beans.xml externalForm
     * value: the corresponding base URL
     *
     * We store this information since not all containers and storages do support
     * new URL(...).
     */
    private final Map<String, URL> beanDeploymentUrls = new HashMap<String, URL>();

    protected ClassLoader loader;
    protected CdiArchive archive;
    protected OwbAnnotationFinder finder;
    protected boolean isBDAScannerEnabled = false;
    protected BDABeansXmlScanner bdaBeansXmlScanner;
    protected final WebBeansContext webBeansContext;


    protected AbstractMetaDataDiscovery()
    {
        webBeansContext = WebBeansContext.getInstance();
    }

    protected AnnotationFinder initFinder()
    {
        if (finder != null)
        {
            return finder;
        }

        if (beanArchiveService == null)
        {
            beanArchiveService = webBeansContext.getBeanArchiveService();
        }

        archive = new CdiArchive(beanArchiveService, WebBeansUtil.getCurrentClassLoader(), getBeanDeploymentUrls());
        finder = new OwbAnnotationFinder(archive);

        return finder;
    }

    /**
     * @return list of beans.xml locations or implicit bean archives
     */
    protected Iterable<URL> getBeanArchiveUrls()
    {
        return beanArchiveLocations;
    }

    /**
     * @return URLs of all classpath entries which
     */
    public Map<String, URL> getBeanDeploymentUrls()
    {
        return beanDeploymentUrls;
    }

    /**
     * Configure the Web Beans Container with deployment information and fills
     * annotation database and beans.xml stream database.
     *
     * @throws org.apache.webbeans.exception.WebBeansConfigurationException if any run time exception occurs
     */
    @Override
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

    abstract protected void configure();

    /**
     * Since CDI-1.1 this is actually more a 'findBdaBases' as it also
     * picks up jars without marker file.
     * This will register all 'explicit' Bean Archives, aka all
     * META-INF/beans.xml resources on the classpath. Those will
     * be added including the META-INF/beans.xml in the URL.
     *
     * We will also add all other classpath locations which do not
     * have the beans.xml marker file, the 'implicit bean archives'.
     * In this case the URL will point to the root of the classpath entry.
     *
     * @param loader the ClassLoader which should be used
     *
     * @see #getBeanArchiveUrls()
     * @see #getBeanDeploymentUrls()
     */
    protected void registerBeanArchives(ClassLoader loader)
    {
        this.loader = loader;

        try
        {
            Set<URL> classPathUrls = ClassLoaders.findUrls(loader);

            // first step: get all META-INF/beans.xml marker files
            Enumeration<URL> beansXmlUrls = loader.getResources(META_INF_BEANS_XML);
            while (beansXmlUrls.hasMoreElements())
            {
                URL beansXmlUrl = beansXmlUrls.nextElement();
                addWebBeansXmlLocation(beansXmlUrl);

                // second step: remove the corresponding classpath entry if we found an explicit beans.xml
                String beansXml = beansXmlUrl.toExternalForm();
                beansXml = stripProtocol(beansXml);

                Iterator<URL> cpIt = classPathUrls.iterator(); // do not use Set<URL> remove as this would trigger hashCode -> DNS
                while (cpIt.hasNext())
                {
                    URL cpUrl = cpIt.next();
                    if (beansXml.startsWith(stripProtocol(cpUrl.toExternalForm())))
                    {
                        cpIt.remove();
                        addDeploymentUrl(beansXml, cpUrl);
                        break;
                    }
                }
            }

            // third step: remove all jars we know they do not contain any CDI beans
            filterExcludedJars(classPathUrls);

            // forth step: add all 'implicit bean archives'
            for (URL url : classPathUrls)
            {
                if (isBdaUrlEnabled(url))
                {
                    addWebBeansXmlLocation(url);
                    addDeploymentUrl(url.toExternalForm(), url);
                }
            }
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get rid of any protocol header from the url externalForm
     * @param urlPath
     */
    protected String stripProtocol(String urlPath)
    {
        int pos = urlPath.lastIndexOf(":/");
        if (pos > 0)
        {
            return urlPath.substring(pos+1);
        }

        return urlPath;
    }

    protected void filterExcludedJars(Set<URL> classPathUrls)
    {
        Iterator<URL> it = classPathUrls.iterator();
        while (it.hasNext())
        {
            URL url = it.next();
            String path = url.toExternalForm();
            if (path.contains("/jre/lib") ||
                path.contains("/Contents/Home/") ||
                path.contains("/dt.jar") ||
                path.contains("/tools.jar") ||
                path.contains("/asm") ||
                path.contains("/javassist") ||
                path.contains("/xbean-") ||
                path.contains("/jconsole.jar") ||
                path.contains("/geronimo-") ||
                path.contains("/commons-") ||
                path.contains("/arquillian-") ||
                path.contains("/bsh-") ||
                path.contains("/shrinkwrap-") ||
                path.contains("/junit-") ||
                path.contains("/testng-") ||
                path.contains("/openjpa-") ||
                path.contains("/bcel") ||
                path.contains("/hamcrest") ||
                path.contains("/mysql-connector") ||
                path.contains("/testng") ||
                path.contains("/idea_rt") ||
                path.contains("/eclipse") ||
                path.contains("/jcommander") ||
                path.contains("/tomcat") ||
                path.contains("/catalina") ||
                path.contains("/jasper") ||
                path.contains("/jsp-api") ||
                path.contains("/myfaces-") ||
                path.contains("/servlet-api") ||
                path.contains("/javax") ||
                path.contains("/annotation-api") ||
                path.contains("/el-api") ||
                path.contains("/mojarra") ||
                path.contains("/openwebbeans-"))
            {
                //X TODO this should be much more actually
                //X TODO we might need to configure it via files
                it.remove();
            }
        }
    }


    @Override
    public void release()
    {
        finder = null;
        archive = null;
        loader = null;
    }


    /**
     * Add an URL for a deployment later on
     * @param beansXml
     * @param cpUrl
     */
    protected void addDeploymentUrl(String beansXml, URL cpUrl)
    {
        beanDeploymentUrls.put(beansXml, cpUrl);
    }

    /**
     * This method could filter out known JARs or even JVM classpaths which
     * shall not be considered bean archives.
     *
     * @return whether the URL is a bean archive or not
     */
    protected boolean isBdaUrlEnabled(URL bdaUrl)
    {
        return true;
    }


    @Override
    public void init(Object object)
    {
        // set per BDA beans.xml flag here because setting it in constructor
        // occurs before
        // properties are loaded.
        String usage = WebBeansContext.currentInstance().getOpenWebBeansConfiguration().getProperty(OpenWebBeansConfiguration.USE_BDA_BEANSXML_SCANNER);
        isBDAScannerEnabled = Boolean.parseBoolean(usage);
    }

    /**
     * add the given beans.xml path to the locations list
     * @param beanArchiveUrl location path
     */
    protected void addWebBeansXmlLocation(URL beanArchiveUrl)
    {
        if(logger.isLoggable(Level.INFO))
        {
            logger.info("added beans archive URL: " + beanArchiveUrl.toExternalForm());
        }
        beanArchiveLocations.add(beanArchiveUrl);

        // and also scan the bean archive!
        if (beanArchiveService == null)
        {

            beanArchiveService = webBeansContext.getBeanArchiveService();
        }

        beanArchiveService.getBeanArchiveInformation(beanArchiveUrl);
    }

    /* (non-Javadoc)
     * @see org.apache.webbeans.corespi.ScannerService#getBeanClasses()
     */
    @Override
    public Set<Class<?>> getBeanClasses()
    {
        final Set<Class<?>> classSet = new HashSet<Class<?>>();
        for (CdiArchive.FoundClasses foundClasses : archive.classesByUrl().values())
        {
            boolean scanModeAnnotated = BeanDiscoveryMode.ANNOTATED.equals(foundClasses.getBeanArchiveInfo().getBeanDiscoveryMode());
            for(String className : foundClasses.getClassNames())
            {
                try
                {
                    if (scanModeAnnotated)
                    {
                        // in this case we need to find out whether we should keep this class in the Archive
                        AnnotationFinder.ClassInfo classInfo = finder.getClassInfo(className);
                        if (classInfo == null || !isBeanAnnotatedClass(classInfo))
                        {
                            continue;
                        }
                    }

                    Class<?> clazz = ClassUtil.getClassFromName(className);
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
                        logger.log(Level.WARNING, OWBLogConst.WARN_0018, new Object[] { className, e.toString() });
                    }
                }
            }

        }

        return classSet;
    }

    /**
     * This method is called for classes from bean archives with
     * bean-discovery-mode 'annotated'.
     *
     * This method is intended to be overwritten in integration scenarios and e.g.
     * allows to add other criterias for keeping the class.
     *
     * @param classInfo
     * @return true if this class should be kept and further get picked up as CDI Bean
     */
    protected boolean isBeanAnnotatedClass(AnnotationFinder.ClassInfo classInfo)
    {
        // check whether this class has 'scope' annotations or a stereotype
        for (AnnotationFinder.AnnotationInfo annotationInfo : classInfo.getAnnotations())
        {
            if (isBeanAnnotation(annotationInfo))
            {
                return true;
            }
        }
        return false;
    }

    protected boolean isBeanAnnotation(AnnotationFinder.AnnotationInfo annotationInfo)
    {
        String annotationName = annotationInfo.getName();

        // TODO add caches

        try
        {
            Class<? extends Annotation> annotationType = (Class<? extends Annotation>) WebBeansUtil.getCurrentClassLoader().loadClass(annotationName);
            boolean isBeanAnnotation = webBeansContext.getBeanManagerImpl().isScope(annotationType);
            isBeanAnnotation = isBeanAnnotation || webBeansContext.getBeanManagerImpl().isStereotype(annotationType);

            return isBeanAnnotation;
        }
        catch (ClassNotFoundException e)
        {
            return false;
        }
    }


    @Override
    public Set<URL> getBeanXmls()
    {
        return Collections.unmodifiableSet(beanArchiveLocations);
    }

    @Override
    public BDABeansXmlScanner getBDABeansXmlScanner()
    {
        return bdaBeansXmlScanner;
    }

    @Override
    public boolean isBDABeansXmlScanningEnabled()
    {
        return isBDAScannerEnabled;
    }
}
