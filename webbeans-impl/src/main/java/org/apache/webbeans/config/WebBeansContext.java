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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.webbeans.annotation.AnnotationManager;
import org.apache.webbeans.container.BeanManagerImpl;
import org.apache.webbeans.container.SerializableBeanVault;
import org.apache.webbeans.context.ContextFactory;
import org.apache.webbeans.context.creational.CreationalContextFactory;
import org.apache.webbeans.conversation.ConversationManager;
//import org.apache.webbeans.corespi.se.DefaultContextsService;
//import org.apache.webbeans.corespi.se.DefaultJndiService;
//import org.apache.webbeans.corespi.se.DefaultScannerService;
import org.apache.webbeans.decorator.DecoratorsManager;
import org.apache.webbeans.deployment.StereoTypeManager;
import org.apache.webbeans.exception.WebBeansException;
import org.apache.webbeans.inject.AlternativesManager;
import org.apache.webbeans.intercept.InterceptorsManager;
import org.apache.webbeans.jms.JMSManager;
import org.apache.webbeans.plugins.PluginLoader;
import org.apache.webbeans.portable.AnnotatedElementFactory;
import org.apache.webbeans.portable.events.ExtensionLoader;
import org.apache.webbeans.proxy.JavassistProxyFactory;
import org.apache.webbeans.spi.ContextsService;
import org.apache.webbeans.spi.ScannerService;
import org.apache.webbeans.spi.plugins.OpenWebBeansPlugin;
import org.apache.webbeans.util.ClassUtil;
import org.apache.webbeans.xml.WebBeansNameSpaceContainer;
import org.apache.webbeans.xml.XMLAnnotationTypeManager;
import org.apache.webbeans.xml.XMLSpecializesManager;

/**
 * @version $Rev$ $Date$
 */
public class WebBeansContext
{

    private ContextFactory contextFactory = new ContextFactory(this);
    private AlternativesManager alternativesManager = new AlternativesManager(this);
    private AnnotatedElementFactory annotatedElementFactory = new AnnotatedElementFactory();
    private BeanManagerImpl beanManagerImpl = new BeanManagerImpl(this);
    private ConversationManager conversationManager = new ConversationManager(this);
    private CreationalContextFactory creationalContextFactory = new CreationalContextFactory();
    private DecoratorsManager decoratorsManager = new DecoratorsManager(this);
    private ExtensionLoader extensionLoader = new ExtensionLoader(this);
    private InterceptorsManager interceptorsManager = new InterceptorsManager(this);
    private JMSManager jmsManager = new JMSManager();
    private JavassistProxyFactory javassistProxyFactory = new JavassistProxyFactory();
    private OpenWebBeansConfiguration openWebBeansConfiguration = new OpenWebBeansConfiguration();
    private PluginLoader pluginLoader = new PluginLoader();
    private SerializableBeanVault serializableBeanVault = new SerializableBeanVault();
    private StereoTypeManager stereoTypeManager = new StereoTypeManager();
    private XMLAnnotationTypeManager xmlAnnotationTypeManager = new XMLAnnotationTypeManager(this);
    private AnnotationManager annotationManager = new AnnotationManager(this);
    private WebBeansNameSpaceContainer webBeansNameSpaceContainer = new WebBeansNameSpaceContainer();
    private XMLSpecializesManager xmlSpecializesManager = new XMLSpecializesManager();

    private final Map<Class<?>, Object> managerMap = new HashMap<Class<?>, Object>();

    private final Map<Class<?>, Object> serviceMap = new HashMap<Class<?>, Object>();

    public WebBeansContext()
    {
        // Allow the WebBeansContext itself to be looked up
        managerMap.put(this.getClass(), this);

        // Add them all into the map for backwards compatibility
        managerMap.put(AlternativesManager.class, alternativesManager);
        managerMap.put(AnnotatedElementFactory.class, annotatedElementFactory);
        managerMap.put(BeanManagerImpl.class, beanManagerImpl);
        managerMap.put(ConversationManager.class, conversationManager);
        managerMap.put(CreationalContextFactory.class, creationalContextFactory);
        managerMap.put(DecoratorsManager.class, decoratorsManager);
        managerMap.put(ExtensionLoader.class, extensionLoader);
        managerMap.put(InterceptorsManager.class, interceptorsManager);
        managerMap.put(JMSManager.class, jmsManager);
        managerMap.put(JavassistProxyFactory.class, javassistProxyFactory);
        managerMap.put(OpenWebBeansConfiguration.class, openWebBeansConfiguration);
        managerMap.put(PluginLoader.class, pluginLoader);
        managerMap.put(SerializableBeanVault.class, serializableBeanVault);
        managerMap.put(StereoTypeManager.class, stereoTypeManager);
        managerMap.put(WebBeansNameSpaceContainer.class, webBeansNameSpaceContainer);
        managerMap.put(XMLAnnotationTypeManager.class, xmlAnnotationTypeManager);
        managerMap.put(XMLSpecializesManager.class, xmlSpecializesManager);
    }

    @Deprecated
    public static WebBeansContext getInstance()
    {
        WebBeansContext webBeansContext = WebBeansFinder.getSingletonInstance();

        return webBeansContext;
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
        serviceMap.put(clazz, t);
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

//            if (logger.wblWillLogWarn())
//            {
//                logger.warn(OWBLogConst.WARN_0009, serviceInterface.getName());
//            }
            return null;
        }
        return serviceInterface.cast(get(implName));
    }

    public ContextFactory getContextFactory()
    {
        return contextFactory;
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

    public JMSManager getjMSManager()
    {
        return jmsManager;
    }

    public PluginLoader getPluginLoader()
    {
        return pluginLoader;
    }

    public ExtensionLoader getExtensionLoader()
    {
        return extensionLoader;
    }

    public JavassistProxyFactory getJavassistProxyFactory()
    {
        return javassistProxyFactory;
    }

    public WebBeansNameSpaceContainer getWebBeansNameSpaceContainer()
    {
        return webBeansNameSpaceContainer;
    }

    public XMLAnnotationTypeManager getxMLAnnotationTypeManager()
    {
        return xmlAnnotationTypeManager;
    }

    public XMLSpecializesManager getxMLSpecializesManager()
    {
        return xmlSpecializesManager;
    }

    //candidates for fields
    public ScannerService getScannerService()
    {
        return getService(ScannerService.class);
    }

    public ContextsService getContextsService()
    {
        return getService(ContextsService.class);
    }

    private Object get(String singletonName)
    {
        //Load class
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
        //util.Track.get(singletonName);
        T object = clazz.cast(managerMap.get(clazz));

        /* No singleton for this application, create one */
        if (object == null)
        {
            object = createInstance(clazz);

            //Save it
            managerMap.put(clazz, object);
        }

        return object;
    }

    private <T> T createInstance(Class<T> clazz)
    {
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
        catch (InstantiationException e)
        {
            throw new WebBeansException("Unable to instantiate class : " + clazz.getName(), e.getCause());
        }
        catch (InvocationTargetException e)
        {
            throw new WebBeansException("Unable to instantiate class : " + clazz.getName(), e.getCause());
        }
        catch (IllegalAccessException e)
        {
            throw new WebBeansException("Illegal access exception in creating instance with class : " + clazz.getName(), e);
        }
    }

    public void clear()
    {
        managerMap.clear();
    }
}
