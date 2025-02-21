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


import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import org.apache.webbeans.config.OWBLogConst;
import org.apache.webbeans.config.OpenWebBeansConfiguration;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.corespi.scanner.xbean.CdiArchive;
import org.apache.webbeans.corespi.scanner.xbean.OwbAnnotationFinder;
import org.apache.webbeans.exception.WebBeansDeploymentException;
import org.apache.webbeans.logger.WebBeansLoggerFacade;
import org.apache.webbeans.spi.BDABeansXmlScanner;
import org.apache.webbeans.spi.BdaScannerService;
import org.apache.webbeans.spi.BeanArchiveService;
import org.apache.webbeans.spi.BeanArchiveService.BeanDiscoveryMode;
import org.apache.webbeans.util.ClassUtil;
import org.apache.webbeans.util.UrlSet;
import org.apache.webbeans.util.WebBeansUtil;
import org.apache.xbean.finder.AnnotationFinder;
import org.apache.xbean.finder.ClassLoaders;
import org.apache.xbean.finder.archive.Archive;
import org.apache.xbean.finder.filter.Filter;
import org.apache.xbean.finder.util.Files;

import jakarta.decorator.Decorator;
import jakarta.enterprise.context.Dependent;
import jakarta.interceptor.Interceptor;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class AbstractMetaDataDiscovery implements BdaScannerService
{
    protected static final Logger logger = WebBeansLoggerFacade.getLogger(AbstractMetaDataDiscovery.class);

    public static final String META_INF_BEANS_XML = "META-INF/beans.xml";

    // via constant to also adopt to shading.
    private static final String DEPENDENT_CLASS = Dependent.class.getName();

    private Map<String, Boolean> annotationCache = new HashMap<>();

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
    protected final Map<String, URL> beanDeploymentUrls = new HashMap<>();

    /**
     * for having proper scan mode 'SCOPED' support we need to know which bean class
     * has which beans.xml.
     */
    private Map<BeanArchiveService.BeanArchiveInformation, Set<Class<?>>> beanClassesPerBda;

    protected String[] scanningExcludes;

    protected ClassLoader loader;
    protected CdiArchive archive;
    protected OwbAnnotationFinder finder;
    protected boolean isBDAScannerEnabled;
    protected BDABeansXmlScanner bdaBeansXmlScanner;
    protected WebBeansContext webBeansContext;

    protected AnnotationFinder initFinder()
    {
        if (finder != null)
        {
            return finder;
        }

        final WebBeansContext webBeansContext = webBeansContext();
        if (beanArchiveService == null)
        {
            beanArchiveService = webBeansContext.getBeanArchiveService();
        }

        final Filter userFilter = webBeansContext.getService(Filter.class);
        Map<String, URL> beanDeploymentUrls = getBeanDeploymentUrls();
        if (!webBeansContext.getOpenWebBeansConfiguration().getScanExtensionJars())
        {
            webBeansContext.getExtensionLoader().loadExtensionServices();

            final Set<URL> extensionJars = webBeansContext.getExtensionLoader().getExtensionJars();
            beanDeploymentUrls = extensionJars.isEmpty() ? beanDeploymentUrls : beanDeploymentUrls.entrySet().stream()
                    .filter(it -> !extensionJars.contains(it.getValue()))
                    .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
            extensionJars.clear(); // no more needed
        }
        archive = new CdiArchive(
                beanArchiveService, WebBeansUtil.getCurrentClassLoader(),
                beanDeploymentUrls, userFilter, getAdditionalArchive());
        finder = new OwbAnnotationFinder(archive);

        return finder;
    }

    protected Archive getAdditionalArchive()
    {
        return null;
    }

    /**
     * @return list of beans.xml locations or implicit bean archives
     * @deprecated just here for backward compat reasons
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

    protected abstract void configure();

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
            final Set<URL> classPathUrls = ClassLoaders.findUrls(loader);
            Map<File, URL> classpathFiles = toFiles(classPathUrls);

            // first step: get all META-INF/beans.xml marker files
            Enumeration<URL> beansXmlUrls = loader.getResources(META_INF_BEANS_XML);
            while (beansXmlUrls.hasMoreElements())
            {
                URL beansXmlUrl = beansXmlUrls.nextElement();
                addWebBeansXmlLocation(beansXmlUrl);

                if (classpathFiles == null) // handle protocols out if [jar,file] set
                {
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
                else
                {
                    File key = Files.toFile(beansXmlUrl);
                    URL url = classpathFiles.remove(key);
                    if (url == null &&
                            "beans.xml".equals(key.getName()) &&
                            key.getParentFile() != null &&
                            "META-INF".equals(key.getParentFile().getName()))
                    {
                        key = key.getParentFile().getParentFile();
                        url = classpathFiles.remove(key);
                    }
                    if (url != null)
                    {
                        addDeploymentUrl(beansXmlUrl.toExternalForm(), url);
                    }
                }
            }

            boolean onlyBeansXmlJars = webBeansContext().getOpenWebBeansConfiguration().scanOnlyBeansXmlJars();
            if (!onlyBeansXmlJars)
            {
                // third step: remove all jars we know they do not contain any CDI beans
                filterExcludedJars(classPathUrls);
                if (classpathFiles != null)
                {
                    classpathFiles = classpathFiles.entrySet().stream()
                            .filter(e -> classPathUrls.contains(e.getValue()))
                            .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
                }

                // forth step: add all 'implicit bean archives'
                for (URL url : (classpathFiles == null ? classPathUrls : classpathFiles.values()))
                {
                    if (isBdaUrlEnabled(url))
                    {
                        addWebBeansXmlLocation(url);
                        addDeploymentUrl(url.toExternalForm(), url);
                    }
                }
            }
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    protected Map<File, URL> toFiles(final Set<URL> classPathUrls)
    {
        try
        {
            final Map<File, URL> collected = classPathUrls.stream()
                    .collect(toMap(Files::toFile, identity(), (a, b) -> a));
            if (collected.containsKey(null)) // not a known protocol
            {
                return null;
            }
            return collected;
        }
        catch (final RuntimeException re)
        {
            return null;
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
        classPathUrls.removeIf(i -> isExcludedJar(i));
    }
    
    protected boolean isExcludedJar(URL url)
    {
        String path = url.toExternalForm();   
        // TODO: should extract file path and test file.getName(), not the whole path
        // + should be configurable
        int knownJarIdx = isExcludedJar(path);
        // -Prun-its openwebbeans-tomcat7 in path but WEB-INF/classes
        if (knownJarIdx > 0 && knownJarIdx < path.indexOf(".jar"))
        {
            //X TODO this should be much more actually
            //X TODO we might need to configure it via files
            return true;
        }
        else
        {
            if (path.contains("geronimo-"))
            {
                // we could check for META-INF/maven/org.apache.geronimo.specs presence there but this is faster
                final File file = Files.toFile(url);
                if (file != null)
                {
                    final String filename = file.getName();
                    if (filename.startsWith("geronimo-") && filename.contains("_spec"))
                    {
                        return true;
                    }
                }
            }
        }
        
        return false;
    }

    protected int isExcludedJar(String path)
    {
        // lazy init - required when using DS CdiTestRunner
        initScanningExcludes();

        for (String p : scanningExcludes)
        {
            int i = path.indexOf(p);
            if (i > 0)
            {
                return i;
            }
        }
        return -1;
    }


    @Override
    public void release()
    {
        finder = null;
        archive = null;
        loader = null;
        annotationCache.clear();
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

        initScanningExcludes();
    }

    public void initScanningExcludes()
    {
        if (scanningExcludes == null)
        {
            OpenWebBeansConfiguration owbConfiguration = WebBeansContext.currentInstance().getOpenWebBeansConfiguration();
            String scanningExcludesProperty = owbConfiguration.getProperty(OpenWebBeansConfiguration.SCAN_EXCLUSION_PATHS);
            List<String> excludes = owbConfiguration.splitValues(scanningExcludesProperty);
            scanningExcludes = excludes.toArray(new String[excludes.size()]);
        }
    }

    /**
     * add the given beans.xml path to the locations list
     * @param beanArchiveUrl location path
     */
    protected void addWebBeansXmlLocation(URL beanArchiveUrl)
    {
        // just the logging there to let children customize the way it is printed out,
        // no logic there but in doAddWebBeansXmlLocation(URL) please
        if(logger.isLoggable(Level.INFO))
        {
            logger.info("added beans archive URL: " + beanArchiveUrl.toExternalForm());
        }
        doAddWebBeansXmlLocation(beanArchiveUrl);
    }

    protected void doAddWebBeansXmlLocation(URL beanArchiveUrl)
    {
        beanArchiveLocations.add(beanArchiveUrl);

        // and also scan the bean archive!
        if (beanArchiveService == null)
        {

            beanArchiveService = webBeansContext().getBeanArchiveService();
        }

        // just to trigger the creation
        beanArchiveService.getBeanArchiveInformation(beanArchiveUrl);
    }


    /**
     * This method only gets called if the initialisation is done already.
     * It will collect all the classes from all the BDAs it can find.
     */
    public Map<BeanArchiveService.BeanArchiveInformation, Set<Class<?>>> getBeanClassesPerBda()
    {
        if (beanClassesPerBda == null)
        {
            beanClassesPerBda = new HashMap<>();
            ClassLoader loader = WebBeansUtil.getCurrentClassLoader();
            boolean dontSkipNCDFT = !(webBeansContext != null &&
                    webBeansContext.getOpenWebBeansConfiguration().isSkipNoClassDefFoundErrorTriggers());

            for (CdiArchive.FoundClasses foundClasses : archive.classesByUrl().values())
            {
                Set<Class<?>> classSet = new HashSet<>();
                boolean scanModeAnnotated = BeanDiscoveryMode.ANNOTATED == foundClasses.getBeanArchiveInfo().getBeanDiscoveryMode();
                for (String className : foundClasses.getClassNames())
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

                        Class<?> clazz = ClassUtil.getClassFromName(className, loader, dontSkipNCDFT);
                        if (clazz != null)
                        {
                            if (dontSkipNCDFT)
                            {
                                // try to provoke a NoClassDefFoundError exception which is thrown
                                // if some dependencies of the class are missing
                                clazz.getDeclaredFields();
                            }

                            // we can add this class cause it has been loaded completely
                            classSet.add(clazz);
                        }
                    }
                    catch (NoClassDefFoundError e)
                    {
                        if (isAnonymous(className))
                        {
                            if (logger.isLoggable(Level.FINE))
                            {
                                logger.log(Level.FINE, OWBLogConst.WARN_0018, new Object[]{className, e.toString()});
                            }
                        }
                        else if (logger.isLoggable(Level.WARNING))
                        {
                            logger.log(Level.WARNING, OWBLogConst.WARN_0018, new Object[]{className, e.toString()});
                        }
                    }
                }

                beanClassesPerBda.put(foundClasses.getBeanArchiveInfo(), classSet);
            }

        }
        return beanClassesPerBda;
    }

    private boolean isAnonymous(final String className)
    {
        final int start = className.lastIndexOf('$');
        if (start <= 0)
        {
            return false;
        }
        try
        {
            Integer.parseInt(className.substring(start + 1));
            return true;
        }
        catch (final NumberFormatException nfe)
        {
            return false;
        }
    }

    /* (non-Javadoc)
     * @see org.apache.webbeans.corespi.ScannerService#getBeanClasses()
     */
    @Override
    public Set<Class<?>> getBeanClasses()
    {
        // do nothing, getBeanClasses() should not get invoked anymore
        return Collections.EMPTY_SET;
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
            if (Interceptor.class.getName().equals(annotationInfo.getName()) ||
                    Decorator.class.getName().equals(annotationInfo.getName()) ||
                    isBeanAnnotation(annotationInfo))
            {
                return true;
            }
        }
        return false;
    }

    protected boolean isBeanAnnotation(AnnotationFinder.AnnotationInfo annotationInfo)
    {
        String annotationName = annotationInfo.getName();

        Boolean isBeanAnnotation = annotationCache.get(annotationName);
        if (isBeanAnnotation != null)
        {
            return isBeanAnnotation;
        }

        try
        {
            Class<? extends Annotation> annotationType = (Class<? extends Annotation>) WebBeansUtil.getCurrentClassLoader().loadClass(annotationName);

            isBeanAnnotation = DEPENDENT_CLASS.equals(annotationName) || webBeansContext().getBeanManagerImpl().isNormalScope(annotationType);
            if (!isBeanAnnotation)
            {
                isBeanAnnotation = webBeansContext().getBeanManagerImpl().isStereotype(annotationType);
            }
            annotationCache.put(annotationName, isBeanAnnotation);

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

    protected WebBeansContext webBeansContext()
    {
        if (webBeansContext == null)
        {
            webBeansContext = WebBeansContext.getInstance();
        }
        return WebBeansContext.getInstance();
    }
}
