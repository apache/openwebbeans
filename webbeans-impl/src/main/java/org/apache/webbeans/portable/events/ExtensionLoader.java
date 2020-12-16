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
package org.apache.webbeans.portable.events;

import static java.util.stream.Collectors.toSet;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.inject.spi.DefinitionException;
import javax.enterprise.inject.spi.DeploymentException;
import javax.enterprise.inject.spi.Extension;

import org.apache.webbeans.component.ExtensionBean;
import org.apache.webbeans.component.creation.ExtensionBeanBuilder;
import org.apache.webbeans.component.creation.ObserverMethodsBuilder;
import org.apache.webbeans.config.OpenWebBeansConfiguration;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.container.BeanManagerImpl;
import org.apache.webbeans.exception.WebBeansException;
import org.apache.webbeans.logger.WebBeansLoggerFacade;
import org.apache.webbeans.util.Asserts;
import org.apache.webbeans.util.ExceptionUtil;
import org.apache.webbeans.util.WebBeansUtil;
import org.apache.xbean.finder.archive.FileArchive;

/**
 * Loads META-INF/services/javax.enterprise.inject.spi.Extension
 * services.
 * 
 * @version $Rev$ $Date$
 *
 */
public class ExtensionLoader
{
    /**Map of extensions*/
    private final  Map<Class<?>, Object> extensions = new ConcurrentHashMap<>();
    private final Set<Class<? extends Extension>> extensionClasses = new HashSet<>();
    private final BeanManagerImpl manager;

    private final WebBeansContext webBeansContext;
    private final Set<URL> extensionJars = new HashSet<>();
    private boolean loaded;

    /**
     * Creates a new loader instance.
     * @param webBeansContext
     */
    public ExtensionLoader(WebBeansContext webBeansContext)
    {

        this.webBeansContext = webBeansContext;
        manager = this.webBeansContext.getBeanManagerImpl();
    }

    /**
     * Load extension services.
     */
    public synchronized void loadExtensionServices()
    {
        if (!loaded)
        {
            loadExtensionServices(WebBeansUtil.getCurrentClassLoader());
            loaded = true;
        }
    }

    /**
     * Load extension services.
     * @param classLoader
     */
    public void loadExtensionServices(ClassLoader classLoader)
    {
        Set<String> ignoredExtensions = webBeansContext.getOpenWebBeansConfiguration().getIgnoredExtensions();
        if (!ignoredExtensions.isEmpty())
        {
            WebBeansLoggerFacade.getLogger(ExtensionLoader.class)
                    .info("Ignoring the following CDI Extensions. " +
                            "See " + OpenWebBeansConfiguration.IGNORED_EXTENSIONS +
                            " " + ignoredExtensions.toString());
        }

        List<Extension> loader = webBeansContext.getLoaderService().load(Extension.class, classLoader);
        for (Extension extension : loader)
        {
            if (ignoredExtensions.contains(extension.getClass().getName()))
            {
                WebBeansLoggerFacade.getLogger(ExtensionLoader.class)
                        .info("Skipping CDI Extension due to exclusion: " + extension.getClass().getName());
                continue;
            }

            if (!extensionClasses.contains(extension.getClass()))
            {
                extensionClasses.add(extension.getClass());
                try
                {
                    addExtension(extension);
                }
                catch (Exception e)
                {
                    if (e instanceof DefinitionException || e instanceof DeploymentException)
                    {
                        ExceptionUtil.throwAsRuntimeException(e);
                    }

                    throw new WebBeansException("Error occurred while reading Extension service list", e);
                }
            }
        }

        if (!webBeansContext.getOpenWebBeansConfiguration().getScanExtensionJars())
        {
            extensionJars.addAll(extensionClasses.stream()
                    .map(clazz -> toJar(classLoader, clazz))
                    .filter(Objects::nonNull)
                    .collect(toSet()));
        }
    }

    private URL toJar(final ClassLoader loader, final Class<?> clazz)
    {
        try
        {
            final String resource = clazz.getName().replace('.', '/') + ".class";
            Enumeration<URL> urls = loader.getResources(resource);
            while (urls.hasMoreElements())
            {
                URL url = urls.nextElement();
                switch (url.getProtocol())
                {
                    case "jar":
                    {
                        final String spec = url.getFile();
                        int separator = spec.indexOf('!');
                        if (separator == -1)
                        {
                            break;
                        }
                        url = new URL(spec.substring(0, separator));
                        return new File(FileArchive.decode(url.getFile())).toURI().toURL();
                    }
                    case "file":
                    {
                        String path = url.getFile();
                        path = path.substring(0, path.length() - resource.length());
                        return new File(FileArchive.decode(path)).toURI().toURL();
                    }
                    default:
                }
            }
        }
        catch (final IOException ioe)
        {
            WebBeansLoggerFacade.getLogger(ExtensionLoader.class).warning(ioe.getMessage());
        }
        return null;
    }

    public Set<URL> getExtensionJars()
    {
        return extensionJars;
    }

    /**
     * Returns service bean instance.
     * 
     * @param extensionClass class of the extension
     * @return service bean instance
     */
    @SuppressWarnings("unchecked")
    public <T> T getExtension(Class<T> extensionClass)
    {
        return (T) extensions.get(extensionClass);
    }


    /**
     * Add a CDI Extension to our internal list.
     * @param ext Extension to add
     */
    public void addExtension(final Extension ext)
    {
        final Class<Extension> extensionClass = (Class<Extension>) ext.getClass();
        Asserts.nullCheckForClass(extensionClass);
        extensions.put(extensionClass, ext);

        final ExtensionBeanBuilder<Extension> extensionBeanBuilder =
                new ExtensionBeanBuilder<>(webBeansContext, extensionClass);
        final ExtensionBean<Extension> bean = extensionBeanBuilder.getBean();
        manager.addBean(bean);

        // since an extension can fire a ProcessInjectionPoint event when observing something else than a lifecycle event
        // and at the same time observe it, we must ensure to build the observers only once the bean is available
        new ObserverMethodsBuilder<>(webBeansContext, extensionBeanBuilder.getAnnotatedType())
                .defineObserverMethods(bean);
    }

    /**
     * Clear service list.
     */
    public void clear()
    {
        extensions.clear();
        extensionClasses.clear();
    }
}
