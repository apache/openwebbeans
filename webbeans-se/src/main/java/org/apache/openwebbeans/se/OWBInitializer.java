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

import java.io.IOException;
import java.io.StringReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

public class OWBInitializer extends SeContainerInitializer
{
    protected final CDISeScannerService scannerService = createDefaultScannerService();
    protected final Properties properties = new Properties();
    protected final Map<String, Object> services = new HashMap<>();
    protected final Collection<Extension> extensions = new ArrayList<>();
    protected final DefaultBeanArchiveInformation bai = new DefaultBeanArchiveInformation(CDISeBeanArchiveService.EMBEDDED_URL);
    protected ClassLoader loader = Thread.currentThread().getContextClassLoader();

    public OWBInitializer()
    {
        scannerService.loader(loader);
    }

    protected CDISeScannerService createDefaultScannerService()
    {
        return new CDISeScannerService();
    }

    @Override
    public SeContainer initialize()
    {
        Thread thread = Thread.currentThread();
        ClassLoader old = thread.getContextClassLoader();
        thread.setContextClassLoader(loader);
        try
        {
            if (!properties.containsKey(ScannerService.class.getName()))
            {
                services.putIfAbsent(ScannerService.class.getName(), getScannerService());
            }
            if (!properties.containsKey(LoaderService.class.getName()))
            {
                services.putIfAbsent(LoaderService.class.getName(), new CDISeLoaderService(extensions, loader));
            }
            if (!properties.containsKey(BeanArchiveService.class.getName()))
            {
                services.putIfAbsent(BeanArchiveService.class.getName(), new CDISeBeanArchiveService(bai));
            }
            addCustomServices(services);
            Map<Class<?>, Object> preparedServices = services.entrySet().stream()
                    .collect(toMap(e ->
                    {
                        try
                        {
                            return loader.loadClass(e.getKey());
                        }
                        catch (ClassNotFoundException e1)
                        {
                            throw new IllegalArgumentException(e1);
                        }
                    }, Map.Entry::getValue));

            WebBeansContext context = new WebBeansContext(preparedServices, properties);

            SingletonService<WebBeansContext> singletonInstance = WebBeansFinder.getSingletonService();
            DefaultSingletonService.class.cast(singletonInstance).register(loader, context);

            return newContainer(context);
        }
        finally
        {
            thread.setContextClassLoader(old);
        }
    }
    protected void addCustomServices(final Map<String, Object> services)
    {
        // for children classes
    }

    protected SeContainer newContainer(final WebBeansContext context)
    {
        Object startObj = new Object();
        context.getService(ContainerLifecycle.class).startApplication(startObj);
        return new OWBContainer(context, startObj);
    }

    protected ScannerService getScannerService()
    {
        return scannerService;
    }

    @Override
    public SeContainerInitializer addBeanClasses(Class<?>... classes)
    {
        scannerService.classes(classes);
        return this;
    }

    @Override
    public SeContainerInitializer addPackages(Package... packages)
    {
        return addPackages(false, packages);
    }

    @Override
    public SeContainerInitializer addPackages(boolean scanRecursively, Package... packages)
    {
        scannerService.packages(scanRecursively, packages);
        return this;
    }

    @Override
    public SeContainerInitializer addPackages(Class<?>... packageClasses)
    {
        return addPackages(false, packageClasses);
    }

    @Override
    public SeContainerInitializer addPackages(boolean scanRecursively, Class<?>... packageClasses)
    {
        scannerService.packages(scanRecursively, packageClasses);
        return this;
    }

    @Override
    public SeContainerInitializer enableInterceptors(Class<?>... interceptorClasses)
    {
        bai.getInterceptors().addAll(Stream.of(interceptorClasses).map(Class::getName).collect(toList()));
        return this;
    }

    @Override
    public SeContainerInitializer enableDecorators(Class<?>... decoratorClasses)
    {
        bai.getDecorators().addAll(Stream.of(decoratorClasses).map(Class::getName).collect(toList()));
        return this;
    }

    @Override
    public SeContainerInitializer selectAlternatives(Class<?>... alternativeClasses)
    {
        bai.getAlternativeClasses().addAll(Stream.of(alternativeClasses).map(Class::getName).collect(toList()));
        return this;
    }

    @Override
    public SeContainerInitializer selectAlternativeStereotypes(Class<? extends Annotation>... alternativeStereotypeClasses)
    {
        bai.getAlternativeStereotypes().addAll(Stream.of(alternativeStereotypeClasses).map(Class::getName).collect(toList()));
        return this;
    }

    @Override
    public SeContainerInitializer addExtensions(Extension... extensions)
    {
        this.extensions.addAll(asList(extensions));
        return this;
    }

    @Override
    public SeContainerInitializer addExtensions(Class<? extends Extension>... extensions)
    {
        this.extensions.addAll(Stream.of(extensions).map(e ->
        {
            try
            {
                return e.getConstructor().newInstance();
            }
            catch (InstantiationException | IllegalAccessException | NoSuchMethodException e1)
            {
                throw new IllegalArgumentException(e1);
            }
            catch (InvocationTargetException e1)
            {
                throw new IllegalArgumentException(e1.getCause());
            }
        }).collect(toList()));
        return this;
    }

    @Override
    public SeContainerInitializer addProperty(final String key, final Object value)
    {
        switch (key)
        {
            case "openwebbeans.disableDiscovery":
                if ("true".equalsIgnoreCase(String.valueOf(value)))
                {
                    disableDiscovery();
                }
                break;
            case "openwebbeans.classes":
                addBeanClasses(list(value, this::loadClass).toArray(Class[]::new));
                break;
            case "openwebbeans.interceptors":
                enableInterceptors(list(value, this::loadClass).toArray(Class[]::new));
                break;
            case "openwebbeans.decorators":
                enableDecorators(list(value, this::loadClass).toArray(Class[]::new));
                break;
            case "openwebbeans.alternatives":
                selectAlternatives(list(value, this::loadClass).toArray(Class[]::new));
                break;
            case "openwebbeans.stereotypes":
                selectAlternativeStereotypes(list(value, this::loadClass).toArray(Class[]::new));
                break;
            case "openwebbeans.extensions":
                final Class<? extends Extension>[] extensions = list(value, this::loadClass).toArray(Class[]::new);
                addExtensions(extensions);
                break;
            case "openwebbeans.packages":
                addPackages(list(value, this::loadPackage).toArray(Package[]::new));
                break;
            case "openwebbeans.packages.recursive":
                addPackages(true, list(value, this::loadPackage).toArray(Package[]::new));
                break;
            case "openwebbeans.properties":
            {
                final Properties properties = new Properties();
                try (final StringReader reader = new StringReader(String.valueOf(value)))
                {
                    properties.load(reader);
                }
                catch (final IOException e)
                {
                    throw new IllegalArgumentException(e);
                }
                properties.stringPropertyNames().forEach(k -> addProperty(k, properties.getProperty(k)));
                break;
            }
            case "openwebbeans.property.":
            {
                addProperty(key.substring("openwebbeans.property.".length()), value);
                break;
            }
            default:
                if (String.class.isInstance(value))
                {
                    properties.put(key, value);
                }
                else
                {
                    services.put(key, value);
                }
        }
        return this;
    }

    @Override
    public SeContainerInitializer setProperties(Map<String, Object> properties)
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
    public SeContainerInitializer setClassLoader(ClassLoader classLoader)
    {
        loader = classLoader;
        scannerService.loader(loader);
        return this;
    }

    private <T> Stream<T> list(final Object list, final Function<Object, T> mapper)
    {
        if (Collection.class.isInstance(list))
        {
            return Collection.class.cast(list).stream().map(mapper);
        }
        return Stream.of(String.valueOf(list).split(","))
                .map(String::trim)
                .filter(it -> !it.isEmpty())
                .map(mapper);
    }

    private Package loadPackage(final Object obj)
    {
        if (Package.class.isInstance(obj))
        {
            return Package.class.cast(obj);
        }
        final String name = String.valueOf(obj);
        try
        {
            return loader.loadClass(name + ".package-info").getPackage();
        }
        catch (final ClassNotFoundException e)
        {
            try
            {
                return loader.loadClass(name).getPackage();
            }
            catch (final ClassNotFoundException e1)
            {
                throw new IllegalArgumentException(e);
            }
        }
    }

    private Class loadClass(final Object it)
    {
        if (Class.class.isInstance(it))
        {
            return Class.class.cast(it);
        }
        try
        {
            return loader.loadClass(String.valueOf(it));
        }
        catch (final ClassNotFoundException e)
        {
            throw new IllegalArgumentException(e);
        }
    }
}
