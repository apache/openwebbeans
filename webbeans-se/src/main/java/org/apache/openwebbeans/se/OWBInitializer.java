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
package org.apache.openwebbeans.se;

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.config.WebBeansFinder;
import org.apache.webbeans.corespi.DefaultSingletonService;
import org.apache.webbeans.spi.BeanArchiveService;
import org.apache.webbeans.spi.ContainerLifecycle;
import org.apache.webbeans.spi.LoaderService;
import org.apache.webbeans.spi.ScannerService;
import org.apache.webbeans.spi.SingletonService;
import org.apache.webbeans.xml.DefaultBeanArchiveInformation;

import javax.enterprise.inject.se.SeContainer;
import javax.enterprise.inject.se.SeContainerInitializer;
import javax.enterprise.inject.spi.Extension;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

public class OWBInitializer extends SeContainerInitializer
{
    private final CDISeScannerService scannerService = new CDISeScannerService();
    private final Properties properties = new Properties();
    private final Map<String, Object> services = new HashMap<>();
    private final Collection<Extension> extensions = new ArrayList<>();
    private final DefaultBeanArchiveInformation bai = new DefaultBeanArchiveInformation(CDISeBeanArchiveService.EMBEDDED_URL);
    private ClassLoader loader = Thread.currentThread().getContextClassLoader();

    public OWBInitializer()
    {
        scannerService.loader(loader);
    }

    @Override
    public SeContainer initialize()
    {
        final Thread thread = Thread.currentThread();
        final ClassLoader old = thread.getContextClassLoader();
        thread.setContextClassLoader(loader);
        try
        {
            services.putIfAbsent(ScannerService.class.getName(), scannerService);
            services.putIfAbsent(LoaderService.class.getName(), new CDISeLoaderService(extensions, loader));
            services.putIfAbsent(BeanArchiveService.class.getName(), new CDISeBeanArchiveService(bai));
            final Map<Class<?>, Object> preparedServices = services.entrySet().stream()
                    .collect(toMap(e ->
                    {
                        try
                        {
                            return loader.loadClass(e.getKey());
                        }
                        catch (final ClassNotFoundException e1)
                        {
                            throw new IllegalArgumentException(e1);
                        }
                    }, Map.Entry::getValue));

            final WebBeansContext context = new WebBeansContext(preparedServices, properties);

            final SingletonService<WebBeansContext> singletonInstance = WebBeansFinder.getSingletonService();
            DefaultSingletonService.class.cast(singletonInstance).register(loader, context);

            final Object startObj = new Object();
            context.getService(ContainerLifecycle.class).startApplication(startObj);
            return new OWBContainer(context, startObj);
        }
        finally
        {
            thread.setContextClassLoader(old);
        }
    }

    @Override
    public SeContainerInitializer addBeanClasses(final Class<?>... classes)
    {
        scannerService.classes(classes);
        return this;
    }

    @Override
    public SeContainerInitializer addPackages(final Package... packages)
    {
        return addPackages(false, packages);
    }

    @Override
    public SeContainerInitializer addPackages(final boolean scanRecursively, final Package... packages)
    {
        scannerService.packages(scanRecursively, packages);
        return this;
    }

    @Override
    public SeContainerInitializer addPackages(final Class<?>... packageClasses)
    {
        return addPackages(false, packageClasses);
    }

    @Override
    public SeContainerInitializer addPackages(final boolean scanRecursively, final Class<?>... packageClasses)
    {
        scannerService.packages(scanRecursively, packageClasses);
        return this;
    }

    @Override
    public SeContainerInitializer enableInterceptors(final Class<?>... interceptorClasses)
    {
        bai.getInterceptors().addAll(Stream.of(interceptorClasses).map(Class::getName).collect(toList()));
        return this;
    }

    @Override
    public SeContainerInitializer enableDecorators(final Class<?>... decoratorClasses)
    {
        bai.getDecorators().addAll(Stream.of(decoratorClasses).map(Class::getName).collect(toList()));
        return this;
    }

    @Override
    public SeContainerInitializer selectAlternatives(final Class<?>... alternativeClasses)
    {
        bai.getAlternativeClasses().addAll(Stream.of(alternativeClasses).map(Class::getName).collect(toList()));
        return this;
    }

    @Override
    public SeContainerInitializer selectAlternativeStereotypes(final Class<? extends Annotation>... alternativeStereotypeClasses)
    {
        bai.getAlternativeStereotypes().addAll(Stream.of(alternativeStereotypeClasses).map(Class::getName).collect(toList()));
        return this;
    }

    @Override
    public SeContainerInitializer addExtensions(final Extension... extensions)
    {
        this.extensions.addAll(asList(extensions));
        return this;
    }

    @Override
    public SeContainerInitializer addExtensions(final Class<? extends Extension>... extensions)
    {
        this.extensions.addAll(Stream.of(extensions).map(e ->
        {
            try
            {
                return e.getConstructor().newInstance();
            }
            catch (final InstantiationException | IllegalAccessException | NoSuchMethodException e1)
            {
                throw new IllegalArgumentException(e1);
            }
            catch (final InvocationTargetException e1)
            {
                throw new IllegalArgumentException(e1.getCause());
            }
        }).collect(toList()));
        return this;
    }

    @Override
    public SeContainerInitializer addProperty(final String key, final Object value)
    {
        if (String.class.isInstance(value))
        {
            properties.put(key, value);
        }
        else
        {
            services.put(key, value);
        }
        return this;
    }

    @Override
    public SeContainerInitializer setProperties(final Map<String, Object> properties)
    {
        properties.forEach(this::addProperty);
        return this;
    }

    @Override
    public SeContainerInitializer disableDiscovery()
    {
        scannerService.disableAutoScanning();
        return this;
    }

    @Override
    public SeContainerInitializer setClassLoader(final ClassLoader classLoader)
    {
        loader = classLoader;
        scannerService.loader(loader);
        return this;
    }
}
