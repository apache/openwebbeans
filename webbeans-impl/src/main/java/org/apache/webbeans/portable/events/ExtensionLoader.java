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

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.DefinitionException;
import javax.enterprise.inject.spi.DeploymentException;
import javax.enterprise.inject.spi.Extension;

import org.apache.webbeans.config.OpenWebBeansConfiguration;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.container.BeanManagerImpl;
import org.apache.webbeans.exception.WebBeansException;
import org.apache.webbeans.logger.WebBeansLoggerFacade;
import org.apache.webbeans.util.ExceptionUtil;
import org.apache.webbeans.util.WebBeansUtil;

/**
 * Loads META-INF/services/javax.enterprise.inject.spi.Extension
 * services.
 * 
 * @version $Rev$ $Date$
 *
 */
public class ExtensionLoader
{
    /**Logger instance*/
    private static final Logger logger = WebBeansLoggerFacade.getLogger(ExtensionLoader.class);

    /**Map of extensions*/
    private final  Map<Class<?>, Object> extensions = new ConcurrentHashMap<>();
    private final Set<Class<? extends Extension>> extensionClasses = new HashSet<>();
    private final BeanManagerImpl manager;

    private final WebBeansContext webBeansContext;

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
    public void loadExtensionServices()
    {
        loadExtensionServices(WebBeansUtil.getCurrentClassLoader());
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
            logger.info("Ignoring the following CDI Extensions. See " + OpenWebBeansConfiguration.IGNORED_EXTENSIONS +
                " " + ignoredExtensions.toString());
        }

        List<Extension> loader = webBeansContext.getLoaderService().load(Extension.class, classLoader);
        for (Extension extension : loader)
        {
            if (ignoredExtensions.contains(extension.getClass().getName()))
            {
                logger.info("Skipping CDI Extension due to exclusion: " + extension.getClass().getName());
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
    public void addExtension(Extension ext)
    {
        Bean<?> bean = webBeansContext.getWebBeansUtil().createExtensionComponent(ext.getClass());
        Class<?> extensionClass = ext.getClass();
        extensions.put(extensionClass, ext);

        manager.addBean(bean);
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
