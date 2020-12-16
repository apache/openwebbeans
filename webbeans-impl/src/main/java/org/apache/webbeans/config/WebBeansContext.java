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
package org.apache.webbeans.config;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;

import org.apache.webbeans.annotation.AnnotationManager;
import org.apache.webbeans.container.BeanManagerImpl;
import org.apache.webbeans.container.SerializableBeanVault;
import org.apache.webbeans.context.creational.CreationalContextFactory;
import org.apache.webbeans.conversation.ConversationManager;
import org.apache.webbeans.conversation.DefaultConversationService;
import org.apache.webbeans.corespi.se.DefaultApplicationBoundaryService;
import org.apache.webbeans.corespi.se.DefaultContextsService;
import org.apache.webbeans.corespi.se.DefaultJndiService;
import org.apache.webbeans.corespi.security.SimpleSecurityService;
import org.apache.webbeans.decorator.DecoratorsManager;
import org.apache.webbeans.deployment.StereoTypeManager;
import org.apache.webbeans.event.NotificationManager;
import org.apache.webbeans.exception.WebBeansException;
import org.apache.webbeans.inject.AlternativesManager;
import org.apache.webbeans.inject.impl.InjectionPointFactory;
import org.apache.webbeans.intercept.InterceptorResolutionService;
import org.apache.webbeans.intercept.InterceptorUtil;
import org.apache.webbeans.intercept.InterceptorsManager;
import org.apache.webbeans.logger.WebBeansLoggerFacade;
import org.apache.webbeans.plugins.PluginLoader;
import org.apache.webbeans.portable.AnnotatedElementFactory;
import org.apache.webbeans.portable.events.ExtensionLoader;
import org.apache.webbeans.proxy.SubclassProxyFactory;
import org.apache.webbeans.proxy.InterceptorDecoratorProxyFactory;
import org.apache.webbeans.proxy.NormalScopeProxyFactory;
import org.apache.webbeans.service.DefaultInjectionPointService;
import org.apache.webbeans.service.DefaultLoaderService;
import org.apache.webbeans.spi.BeanArchiveService;
import org.apache.webbeans.spi.ApplicationBoundaryService;
import org.apache.webbeans.spi.ContextsService;
import org.apache.webbeans.spi.ConversationService;
import org.apache.webbeans.spi.LoaderService;
import org.apache.webbeans.spi.ScannerService;
import org.apache.webbeans.spi.SecurityService;
import org.apache.webbeans.spi.TransactionService;
import org.apache.webbeans.spi.plugins.OpenWebBeansPlugin;
import org.apache.webbeans.util.ClassUtil;
import org.apache.webbeans.util.WebBeansUtil;
import org.apache.webbeans.xml.DefaultBeanArchiveService;

/**
 * This is the central point to manage the whole CDI container
 * for a single application There is one WebBeansContext per BeanManagerImpl.
 *
 * @version $Rev$ $Date$
 */
public class WebBeansContext
{
    private final Map<Class<?>, Object> managerMap = new HashMap<>();

    private final Map<Class<?>, Object> serviceMap = new HashMap<>();

    private final WebBeansUtil webBeansUtil = new WebBeansUtil(this);
    private final AlternativesManager alternativesManager = new AlternativesManager(this);
    private final AnnotatedElementFactory annotatedElementFactory = new AnnotatedElementFactory(this);
    private final BeanManagerImpl beanManagerImpl = new BeanManagerImpl(this);
    private final CreationalContextFactory creationalContextFactory = new CreationalContextFactory(this);
    private final DecoratorsManager decoratorsManager = new DecoratorsManager(this);
    private final ExtensionLoader extensionLoader = new ExtensionLoader(this);
    private final InterceptorsManager interceptorsManager = new InterceptorsManager(this);
    private final InterceptorDecoratorProxyFactory interceptorDecoratorProxyFactory;
    private final NormalScopeProxyFactory normalScopeProxyFactory;
    private final SubclassProxyFactory subclassProxyFactory;
    private final OpenWebBeansConfiguration openWebBeansConfiguration;
    private final PluginLoader pluginLoader = new PluginLoader();
    private final SerializableBeanVault serializableBeanVault = new SerializableBeanVault();
    private final StereoTypeManager stereoTypeManager = new StereoTypeManager();
    private final AnnotationManager annotationManager;
    private final InjectionPointFactory injectionPointFactory;
    private final InterceptorUtil interceptorUtil = new InterceptorUtil(this);
    private final SecurityService securityService;
    private final LoaderService loaderService;
    private BeanArchiveService beanArchiveService;
    private final InterceptorResolutionService interceptorResolutionService = new InterceptorResolutionService(this);
    private final DeploymentValidationService deploymentValidationService = new DeploymentValidationService(this);
    private ScannerService scannerService;
    private ContextsService contextsService;
    private final ConversationManager conversationManager;
    private ConversationService conversationService;
    private final ApplicationBoundaryService applicationBoundaryService;
    private final NotificationManager notificationManager;
    private TransactionService transactionService;


    public WebBeansContext()
    {
        this(null, new OpenWebBeansConfiguration());
    }

    public WebBeansContext(Map<Class<?>, Object> initialServices, Properties properties)
    {
        this(initialServices, new OpenWebBeansConfiguration(properties));
    }

    private WebBeansContext(Map<Class<?>, Object> initialServices, OpenWebBeansConfiguration openWebBeansConfiguration)
    {
        this.openWebBeansConfiguration = openWebBeansConfiguration != null ? openWebBeansConfiguration : new OpenWebBeansConfiguration();
        annotationManager = new AnnotationManager(this);

        //pluggable service-loader
        if (initialServices == null || !initialServices.containsKey(LoaderService.class))
        {
            String implementationLoaderServiceName =
                    this.openWebBeansConfiguration.getProperty(LoaderService.class.getName());
            if (implementationLoaderServiceName == null)
            {
                serviceMap.put(LoaderService.class, new DefaultLoaderService());
            }
            else
            {
                serviceMap.put(LoaderService.class, LoaderService.class.cast(get(implementationLoaderServiceName)));
            }
        }

        if (initialServices != null)
        {
            for (Map.Entry<Class<?>, Object> entry: initialServices.entrySet())
            {
                if (!entry.getKey().isAssignableFrom(entry.getValue().getClass()))
                {
                    throw new IllegalArgumentException("Initial service claiming to be of type " + entry.getKey() + " is a " + entry.getValue().getClass());
                }
                serviceMap.put(entry.getKey(), entry.getValue());
            }
        }
        injectionPointFactory = new InjectionPointFactory(this);
        loaderService = getService(LoaderService.class);
        securityService = getService(SecurityService.class);
        applicationBoundaryService = getService(ApplicationBoundaryService.class);

        interceptorDecoratorProxyFactory = new InterceptorDecoratorProxyFactory(this);
        normalScopeProxyFactory = new NormalScopeProxyFactory(this);
        subclassProxyFactory = new SubclassProxyFactory(this);

        beanArchiveService = getService(BeanArchiveService.class);
        conversationManager = new ConversationManager(this);

        notificationManager = new NotificationManager(this);

        beanManagerImpl.getInjectionResolver().setFastMatching(!"false".equalsIgnoreCase(getOpenWebBeansConfiguration()
                .getProperty(OpenWebBeansConfiguration.FAST_MATCHING)));
    }

    public static WebBeansContext getInstance()
    {
        WebBeansContext webBeansContext = WebBeansFinder.getSingletonInstance();

        return webBeansContext;
    }

    /**
     * Method to be used when static use is truely unavoidable, such as serialization
     *
     * Ideally this method would never lazily create a WebBeansContext and as we don't
     * want to do any deployment of new apps during deserialization, we want to rehydrate
     * objects from an existing WebBeansContext which should be the active context.
     *
     * This method could throw a runtime exception if no instance currently exists. 
     *
     * @return
     */
    public static WebBeansContext currentInstance()
    {
        return getInstance();
    }

    public <T> T getService(Class<T> clazz)
    {
        T t = clazz.cast(serviceMap.get(clazz));
        if (t == null)
        {
            t = doServiceLoader(clazz);
            registerService(clazz, t);
        }
        return t;
    }

    public <T> void registerService(Class<T> clazz, T t)
    {
        if (t != null)
        {
            serviceMap.put(clazz, t);
        }
    }

    private <T> T doServiceLoader(Class<T> serviceInterface)
    {
        String implName = getOpenWebBeansConfiguration().getProperty(serviceInterface.getName());

        if (implName == null)
        {
            //Look for plugins
            List<OpenWebBeansPlugin> plugins = getPluginLoader().getPlugins();
            if(plugins != null && plugins.size() > 0)
            {
                for(OpenWebBeansPlugin plugin : plugins)
                {
                    if(plugin.supportService(serviceInterface))
                    {
                        return plugin.getSupportedService(serviceInterface);
                    }
                }
            }

            return null;
        }
        return serviceInterface.cast(get(implName));
    }

    public InterceptorUtil getInterceptorUtil()
    {
        return interceptorUtil;
    }

    public InjectionPointFactory getInjectionPointFactory()
    {
        return injectionPointFactory;
    }

    public WebBeansUtil getWebBeansUtil()
    {
        return webBeansUtil;
    }

    public AnnotationManager getAnnotationManager()
    {
        return annotationManager;
    }

    public ConversationManager getConversationManager()
    {
        return conversationManager;
    }

    public OpenWebBeansConfiguration getOpenWebBeansConfiguration()
    {
        return openWebBeansConfiguration;
    }

    public AnnotatedElementFactory getAnnotatedElementFactory()
    {
        return annotatedElementFactory;
    }

    public BeanManagerImpl getBeanManagerImpl()
    {
        return beanManagerImpl;
    }

    public SerializableBeanVault getSerializableBeanVault()
    {
        return serializableBeanVault;
    }

    public CreationalContextFactory getCreationalContextFactory()
    {
        return creationalContextFactory;
    }

    public DecoratorsManager getDecoratorsManager()
    {
        return decoratorsManager;
    }

    public StereoTypeManager getStereoTypeManager()
    {
        return stereoTypeManager;
    }

    public AlternativesManager getAlternativesManager()
    {
        return alternativesManager;
    }

    public InterceptorsManager getInterceptorsManager()
    {
        return interceptorsManager;
    }

    public InterceptorResolutionService getInterceptorResolutionService()
    {
        return interceptorResolutionService;
    }

    public PluginLoader getPluginLoader()
    {
        return pluginLoader;
    }

    public ExtensionLoader getExtensionLoader()
    {
        return extensionLoader;
    }


    public InterceptorDecoratorProxyFactory getInterceptorDecoratorProxyFactory()
    {
        return interceptorDecoratorProxyFactory;
    }

    public NormalScopeProxyFactory getNormalScopeProxyFactory()
    {
        return normalScopeProxyFactory;
    }

    public SubclassProxyFactory getSubclassProxyFactory()
    {
        return subclassProxyFactory;
    }

    public TransactionService getTransactionService() // used in event bus so ensure it is a plain getter at runtime
    {
        if (transactionService == null)
        {
            // lazy init
            transactionService = getService(TransactionService.class);
        }
        return transactionService;
    }

    public ScannerService getScannerService()
    {
        if (scannerService == null)
        {
            // lazy init
            scannerService = getService(ScannerService.class);
        }
        return scannerService;
    }

    public ContextsService getContextsService()
    {
        if (contextsService == null)
        {
            contextsService = getService(ContextsService.class);
        }
        return contextsService;
    }

    public SecurityService getSecurityService()
    {
        return securityService;
    }

    public BeanArchiveService getBeanArchiveService()
    {
        return beanArchiveService;
    }

    public NotificationManager getNotificationManager()
    {
        return notificationManager;
    }

    public ConversationService getConversationService()
    {
        if (conversationService == null)
        {
            conversationService = getService(ConversationService.class);
        }

        return conversationService;
    }


    private Object get(String singletonName)
    {
        // skip reflection for these services
        if (DefaultInjectionPointService.class.getName().equals(singletonName))
        {
            return new DefaultInjectionPointService(this);
        }
        if (SimpleSecurityService.class.getName().equals(singletonName))
        {
            return new SimpleSecurityService();
        }
        if (DefaultApplicationBoundaryService.class.getName().equals(singletonName))
        {
            return new DefaultApplicationBoundaryService();
        }
        if (DefaultBeanArchiveService.class.getName().equals(singletonName))
        {
            return new DefaultBeanArchiveService();
        }
        if (DefaultJndiService.class.getName().equals(singletonName))
        {
            return new DefaultJndiService();
        }
        if (DefaultContextsService.class.getName().equals(singletonName))
        {
            return new DefaultContextsService(this);
        }
        if (DefaultConversationService.class.getName().equals(singletonName))
        {
            return new DefaultConversationService();
        }

        // Load class by reflection
        Class<?> clazz = ClassUtil.getClassFromName(singletonName);
        if (clazz == null)
        {
            throw new WebBeansException("Class not found exception in creating instance with class : " + singletonName,
                    new ClassNotFoundException("Class with name: " + singletonName + " is not found in the system"));
        }
        return get(clazz);

    }

    public <T> T get(Class<T> clazz)
    {
        //util.Track.get(clazz);
        T object = clazz.cast(managerMap.get(clazz));

        /* No singleton for this application, create one */
        if (object == null)
        {
            object = backwardCompatibilityLookup(clazz);
            if (object == null)
            {
                object = createInstance(clazz);
            }

            // Save it for future usages
            managerMap.put(clazz, object);
        }

        return object;
    }

    private <T> T backwardCompatibilityLookup(final Class<T> clazz)
    {
        if (clazz.isInstance(this)) // Allow the WebBeansContext itself to be looked up
        {
            return clazz.cast(this);
        }

        // Add them all into the map for backwards compatibility

        if (clazz == AlternativesManager.class)
        {
            return clazz.cast(alternativesManager);
        }
        if (clazz == AnnotatedElementFactory.class)
        {
            return clazz.cast(annotatedElementFactory);
        }
        if (clazz == BeanManagerImpl.class)
        {
            return clazz.cast(beanManagerImpl);
        }
        if (clazz == ConversationManager.class)
        {
            return clazz.cast(conversationManager);
        }
        if (clazz == CreationalContextFactory.class)
        {
            return clazz.cast(creationalContextFactory);
        }
        if (clazz == DecoratorsManager.class)
        {
            return clazz.cast(decoratorsManager);
        }
        if (clazz == ExtensionLoader.class)
        {
            return clazz.cast(extensionLoader);
        }
        if (clazz == InterceptorsManager.class)
        {
            return clazz.cast(interceptorsManager);
        }
        if (clazz == InterceptorDecoratorProxyFactory.class)
        {
            return clazz.cast(interceptorDecoratorProxyFactory);
        }
        if (clazz == NormalScopeProxyFactory.class)
        {
            return clazz.cast(normalScopeProxyFactory);
        }
        if (clazz == SubclassProxyFactory.class)
        {
            return clazz.cast(subclassProxyFactory);
        }
        if (clazz == OpenWebBeansConfiguration.class)
        {
            return clazz.cast(openWebBeansConfiguration);
        }
        if (clazz == PluginLoader.class)
        {
            return clazz.cast(pluginLoader);
        }
        if (clazz == SerializableBeanVault.class)
        {
            return clazz.cast(serializableBeanVault);
        }
        if (clazz == StereoTypeManager.class)
        {
            return clazz.cast(stereoTypeManager);
        }
        if (clazz == InterceptorResolutionService.class)
        {
            return clazz.cast(interceptorResolutionService);
        }
        if (clazz == NotificationManager.class)
        {
            return clazz.cast(notificationManager);
        }
        return null;
    }

    private <T> T createInstance(Class<T> clazz)
    {
        // skip the reflection for know classes
        if (DefaultLoaderService.class == clazz)
        {
            return clazz.cast(new DefaultLoaderService());
        }
        if (SimpleSecurityService.class == clazz)
        {
            return clazz.cast(new SimpleSecurityService());
        }
        if (DefaultApplicationBoundaryService.class == clazz)
        {
            return clazz.cast(new DefaultApplicationBoundaryService());
        }
        if (DefaultBeanArchiveService.class == clazz)
        {
            return clazz.cast(new DefaultBeanArchiveService());
        }
        if (DefaultJndiService.class == clazz)
        {
            return clazz.cast(new DefaultJndiService());
        }
        if (DefaultContextsService.class == clazz)
        {
            return clazz.cast(new DefaultContextsService(this));
        }
        if (DefaultConversationService.class == clazz)
        {
            return clazz.cast(new DefaultConversationService());
        }

        // try by reflection for extensions
        try
        {

            // first try constructor that takes this object as an argument
            try
            {
                Constructor<T> constructor = clazz.getConstructor(WebBeansContext.class);
                return constructor.newInstance(this);
            }
            catch (NoSuchMethodException e)
            {
            }

            // then try a no-arg constructor
            try
            {
                Constructor<T> constructor = clazz.getConstructor();
                return constructor.newInstance();
            }
            catch (NoSuchMethodException e)
            {
                throw new WebBeansException("No suitable constructor : " + clazz.getName(), e.getCause());
            }
        }
        catch (InstantiationException | InvocationTargetException e)
        {
            throw new WebBeansException("Unable to instantiate class : " + clazz.getName(), e.getCause());
        }
        catch (IllegalAccessException e)
        {
            throw new WebBeansException("Illegal access exception in creating instance with class : " + clazz.getName(), e);
        }
    }

    /**
     * Clear and destroy the whole WebBeansContext.
     * This will also properly destroy all SPI services
     */
    public void clear()
    {
        destroyServices(managerMap.values());
        destroyServices(serviceMap.values());

        managerMap.clear();
        serviceMap.clear();
    }

    private void destroyServices(Collection<Object> services)
    {
        for (Object spiService : services)
        {
            if (spiService instanceof Closeable)
            {
                try
                {
                    ((Closeable) spiService).close();
                }
                catch (IOException e)
                {
                    WebBeansLoggerFacade.getLogger(WebBeansContext.class)
                            .log(Level.SEVERE, "Error while destroying SPI service " + spiService.getClass().getName(), e);
                }
            }
            else if (ExecutorService.class.isInstance(spiService))
            {
                ExecutorService es = ExecutorService.class.cast(spiService);
                es.shutdownNow().forEach(r -> {
                    try
                    {
                        r.run();
                    }
                    catch (RuntimeException re)
                    {
                        WebBeansLoggerFacade.getLogger(WebBeansContext.class).warning(re.getMessage());
                    }
                });
            }
        }
    }

    public LoaderService getLoaderService()
    {
        return loaderService;
    }

    public DeploymentValidationService getDeploymentValidationService()
    {
        return deploymentValidationService;
    }

    public ApplicationBoundaryService getApplicationBoundaryService()
    {
        return applicationBoundaryService;
    }

    public boolean findMissingAnnotatedType(Class<?> missing)
    {
        return false; // used in hierarchical WBC
    }
}
