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

import static java.util.stream.Collectors.toMap;

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.config.WebBeansFinder;
import org.apache.webbeans.corespi.DefaultSingletonService;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.inject.OWBInjector;
import org.apache.webbeans.lifecycle.StandaloneLifeCycle;
import org.apache.webbeans.lifecycle.test.OpenWebBeansTestMetaDataDiscoveryService;
import org.apache.webbeans.spi.ContainerLifecycle;
import org.apache.webbeans.spi.ContextsService;
import org.apache.webbeans.spi.ScannerService;
import org.apache.webbeans.spi.SingletonService;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collector;


public abstract class AbstractUnitTest
{
    private StandaloneLifeCycle testLifecycle;
    private Map<Class<?>, Object> services = new HashMap<>();
    private Properties configuration = new Properties();
    private List<Extension>  extensions = new ArrayList<>();
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
        services.clear();
        configuration.clear();
    }

    /**
     * always shut down the container after each test.
     */
    @After
    public void shutdown()
    {
        shutDownContainer();
    }

    /**
     * Take all the inner classes of this very test class
     * and boot OWB with them.
     * Also inject all the fields of the Testclass afterwards.
     */
    protected void startContainerInnerClasses()
    {
        startContainer(Arrays.asList(getClass().getClasses()), null, true);
    }

    /**
     * Start OWB with the given Classes but with just an empty beans.xml
     */
    protected void startContainer(Class<?>... beanClasses)
    {
        startContainer(new ArrayList<>(Arrays.asList(beanClasses)), null);
    }

    /**
     * Start up OWB with a beansXml file name and the given classes
     */
    protected void startContainer(String beansXmlResourceName, Class<?>... beanClasses)
    {
        List<String> beansXmls = new ArrayList<String>(1);
        beansXmls.add(getXmlUrl(beansXmlResourceName));
        startContainer(new ArrayList<Class<?>>(Arrays.asList(beanClasses)), beansXmls);
    }

    /**
     * Start OWB with the given Classes but with just an empty beans.xml
     */
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

        final ClassLoader currentClassLoader = WebBeansUtil.getCurrentClassLoader();
        WebBeansFinder.clearInstances(currentClassLoader);
        //Creates a new container
        final SingletonService<WebBeansContext> singletonInstance = WebBeansFinder.getSingletonService();
        final boolean lateServiceRegistration = !DefaultSingletonService.class.isInstance(singletonInstance);
        if (!lateServiceRegistration)
        {
            final Map<Class<?>, Object> servicesInstances = services.entrySet().stream()
                    .filter(it -> !Class.class.isInstance(it.getValue()))
                    .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));

            final Map<Class<?>, Object> wbcAwareServices = new HashMap<>(services);
            servicesInstances.keySet().forEach(wbcAwareServices::remove);

            final Properties properties = wbcAwareServices.entrySet().stream()
                    .collect(Collector.of(
                            () -> configuration == null ? new Properties() : configuration,
                        (p, e) -> p.setProperty(e.getKey().getName(), Class.class.cast(e.getValue()).getName()),
                        (properties1, properties2) -> {
                            properties1.putAll(properties2);
                            return properties1;
                        }));

            final WebBeansContext context = new WebBeansContext(servicesInstances, properties);
            DefaultSingletonService.class.cast(singletonInstance).register(currentClassLoader, context);
        }
        testLifecycle = new StandaloneLifeCycle()
        {
            @Override
            public void beforeInitApplication(final Properties properties)
            {
                final WebBeansContext instance = WebBeansContext.getInstance();
                if (lateServiceRegistration)
                {
                    services.forEach((k, v) ->
                    {
                        final Object impl = k.cast(getServiceInstance(instance, v));
                        instance.registerService(Class.class.cast(k), impl);
                    });
                }
                if (!services.containsKey(ScannerService.class))
                {
                    instance.registerService(ScannerService.class, new OpenWebBeansTestMetaDataDiscoveryService());
                }

                super.beforeInitApplication(properties);
            }
        };
        
        webBeansContext = WebBeansContext.getInstance();
        webBeansContext.getExtensionLoader().addExtensions(extensions);
        
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
        if (!beanClasses.isEmpty() || beanXmls != null)
        {
            OpenWebBeansTestMetaDataDiscoveryService discoveryService = (OpenWebBeansTestMetaDataDiscoveryService)webBeansContext.getScannerService();
            discoveryService.deployClasses(beanClasses);
            if (beanXmls != null)
            {
                discoveryService.deployXMLs(beanXmls);
            }
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
            inject(this);
        }
    }

    private Object getServiceInstance(final WebBeansContext instance, final Object v)
    {
        try
        {
            return Class.class.isInstance(v) ?
                    Class.class.cast(v).getConstructor(WebBeansContext.class).newInstance(instance) :
                    v;
        }
        catch (final Exception e)
        {
            throw new IllegalArgumentException(e);
        }
    }

    public void inject(final Object bean)
    {
        OWBInjector.inject(getBeanManager(), bean, null);
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

    protected <T> void addService(final Class<T> type, final Class<? extends T> instance)
    {
        this.services.put(type, instance);
    }

    protected <T> void addService(final Class<T> type, final T instance)
    {
        this.services.put(type, instance);
    }

    protected void addConfiguration(final String key, final String value)
    {
        if (configuration == null)
        {
            configuration = new Properties();
        }
        configuration.setProperty(key, value);
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
    protected void addExtension(Extension ext) {
        this.extensions.add(ext);
    }

    /**
     * Add the given interceptor class to get picked up
     * by startContainer.
     * This has the same effect as adding it to the
     * &lt;interceptors&gt; section in beans.xml.
     */
    protected void addInterceptor(Class interceptorClass)
    {
        interceptors.add(interceptorClass);
    }

    /**
     * Add the given interceptor class to get picked up
     * by startContainer.
     * This has the same effect as adding it to the
     * &lt;interceptors&gt; section in beans.xml.
     */
    protected void addDecorator(Class decoratorClass)
    {
        decorators.add(decoratorClass);
    }

    protected void restartContext(Class<? extends Annotation> scopeType)
    {
        ContextsService contextsService = webBeansContext.getContextsService();
        contextsService.endContext(scopeType, null);
        contextsService.startContext(scopeType, null);
    }

    protected void startContext(Class<? extends Annotation> scopeType)
    {
        webBeansContext.getContextsService().startContext(scopeType, null);
    }

    protected void endContext(Class<? extends Annotation> scopeType)
    {
        webBeansContext.getContextsService().endContext(scopeType, null);
    }
}
