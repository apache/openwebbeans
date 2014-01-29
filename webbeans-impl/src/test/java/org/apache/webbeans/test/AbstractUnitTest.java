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
package org.apache.webbeans.test;

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.config.WebBeansFinder;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.inject.OWBInjector;
import org.apache.webbeans.lifecycle.test.OpenWebBeansTestLifeCycle;
import org.apache.webbeans.lifecycle.test.OpenWebBeansTestMetaDataDiscoveryService;
import org.apache.webbeans.spi.ContainerLifecycle;
import org.apache.webbeans.util.WebBeansUtil;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;

import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;


public abstract class AbstractUnitTest
{
    private OpenWebBeansTestLifeCycle testLifecycle;
    private List<Extension>  extensions = new ArrayList<Extension>();
    private List<Class<?>> interceptors = new ArrayList<Class<?>>();
    private List<Class<?>> decorators = new ArrayList<Class<?>>();
    private WebBeansContext webBeansContext;

    protected AbstractUnitTest()
    {

    }

    @Before
    public void cleanup()
    {
        extensions.clear();
        interceptors.clear();
        decorators.clear();
    }

    /**
     * always shut down the container after each test.
     */
    @After
    public void shutdown()
    {
        shutDownContainer();
    }

    protected void startContainer(Class<?>... beanClasses)
    {
        startContainer(new ArrayList<Class<?>>(Arrays.asList(beanClasses)), null);
    }

    protected void startContainer(String beansXml, Class<?>... beanClasses)
    {
        List<String> beansXmls = new ArrayList<String>(1);
        beansXmls.add(getXmlUrl(beansXml));
        startContainer(new ArrayList<Class<?>>(Arrays.asList(beanClasses)), beansXmls);
    }

    protected void startContainer(Collection<Class<?>> beanClasses)
    {
        startContainer(beanClasses, null);
    }
    
    protected void startContainer(Collection<Class<?>> beanClasses, Collection<String> beanXmls)
    {
        startContainer(beanClasses, beanXmls, false);
    }

    protected void startContainer(Collection<Class<?>> rawBeanClasses, Collection<String> beanXmls, boolean inject)
    {
        final Collection<Class<?>> beanClasses = new ArrayList<Class<?>>(); // ensure it is updatable
        beanClasses.addAll(rawBeanClasses);

        WebBeansFinder.clearInstances(WebBeansUtil.getCurrentClassLoader());
        //Creates a new container
        testLifecycle = new OpenWebBeansTestLifeCycle();
        
        webBeansContext = WebBeansContext.getInstance();
        for (Extension ext : extensions)
        {
            webBeansContext.getExtensionLoader().addExtension(ext);
        }
        
        for (Class interceptor : interceptors)
        {
            // add it as enabled interceptor class, like it would be listed in beans.xml
            webBeansContext.getInterceptorsManager().addEnabledInterceptorClass(interceptor);

            // but also add it for scanning
            beanClasses.add(interceptor);
        }

        for (Class decorator : decorators)
        {
            // add it as enabled decorator class, like it would be listed in beans.xml
            webBeansContext.getDecoratorsManager().addEnabledDecorator(decorator);

            // but also add it for scanning
            beanClasses.add(decorator);
        }

        //Deploy bean classes
        OpenWebBeansTestMetaDataDiscoveryService discoveryService = (OpenWebBeansTestMetaDataDiscoveryService)webBeansContext.getScannerService();
        discoveryService.deployClasses(beanClasses);
        if (beanXmls != null)
        {
            discoveryService.deployXMLs(beanXmls);
        }

        //Start application
        try
        {
            testLifecycle.startApplication(null);
        }
        catch (Exception e)
        {
            throw new WebBeansConfigurationException(e);
        }

        if (inject)
        {
            try
            {
                OWBInjector.inject(getBeanManager(), this, null);
            }
            catch (Exception e)
            {
                throw new WebBeansConfigurationException(e);
            }
        }
    }

    protected ContainerLifecycle getLifecycle()
    {
        return testLifecycle;
    }
    
    protected void shutDownContainer()
    {
        //Shutdown application
        if(this.testLifecycle != null)
        {
            this.testLifecycle.stopApplication(null);
            this.testLifecycle = null;
            cleanup();
        }        
    }
        
    protected WebBeansContext getWebBeansContext()
    {
        return this.webBeansContext;
    }
    
    protected BeanManager getBeanManager()
    {
        return this.webBeansContext.getBeanManagerImpl();
    }

    @SuppressWarnings("unchecked")
    protected <T> Bean<T> getBean(Class<T> type, Annotation... qualifiers)
    {
        Set beans = getBeanManager().getBeans(type, qualifiers);
        return (Bean<T>) getBeanManager().resolve(beans);
    }

    protected <T> T getInstance(Class<T> type, Annotation... qualifiers)
    {
        return getInstance((Type) type, qualifiers);
    }

    protected <T> T getInstance(Type type, Annotation... qualifiers)
    {
        Set<Bean<?>> beans = getBeanManager().getBeans(type, qualifiers);
        Assert.assertNotNull(beans);

        Bean<?> bean = getBeanManager().resolve(beans);
        Assert.assertNotNull("Bean with type " + type + " could not be found!", bean);

        return (T) getBeanManager().getReference(bean, type, getBeanManager().createCreationalContext(bean));
    }

    @SuppressWarnings("unchecked")
    protected <T> T getInstance(String elName)
    {
        Set<Bean<?>> beans = getBeanManager().getBeans(elName);
        Assert.assertNotNull(beans);

        Bean<?> bean = getBeanManager().resolve(beans);
        Assert.assertNotNull("Bean with name " + elName + " could not be found!", bean);

        return (T) getBeanManager().getReference(bean, Object.class, getBeanManager().createCreationalContext(bean));
    }


    /**
     * @param packageName package of the beans.xml file
     * @param fileName name of the beans xml file, without '.xml'
     * @return the absolute beans.xml URL
     */
    protected String getXmlPath(String packageName, String fileName)
    {
        StringBuilder prefix = new StringBuilder(packageName.replace('.', '/'));
        prefix.append("/");
        prefix.append(fileName);
        prefix.append(".xml");

        return getXmlUrl(prefix.toString());
    }

    /**
     * @param fileName the resource path of the beans.xml to parse
     * @return the URL of the beans.xml.
     */
    protected String getXmlUrl(String fileName)
    {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        return loader.getResource(fileName).toExternalForm();
    }

    /**
     * Add a CDI Extension which should get used in the test case.
     * Use this function instead of defining test Extensions via the usual
     * META-INF/services/javax.enterprise.inject.spi.Extension file!
     * 
     * @param ext the {@link Extension} which should get loaded
     */
    public void addExtension(Extension ext) {
        this.extensions.add(ext);
    }

    /**
     * Add the given interceptor class to get picked up
     * by startContainer.
     * This has the same effect as adding it to the
     * &lt;interceptors&gt; section in beans.xml.
     */
    public void addInterceptor(Class interceptorClass)
    {
        interceptors.add(interceptorClass);
    }

    /**
     * Add the given interceptor class to get picked up
     * by startContainer.
     * This has the same effect as adding it to the
     * &lt;interceptors&gt; section in beans.xml.
     */
    public void addDecorator(Class decoratorClass)
    {
        decorators.add(decoratorClass);
    }
}
