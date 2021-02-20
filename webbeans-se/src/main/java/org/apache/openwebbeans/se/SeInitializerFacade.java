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

import org.apache.webbeans.conversation.DefaultConversationService;
import org.apache.webbeans.corespi.se.StandaloneContextsService;
import org.apache.webbeans.lifecycle.StandaloneLifeCycle;
import org.apache.webbeans.spi.ContainerLifecycle;
import org.apache.webbeans.spi.ContextsService;
import org.apache.webbeans.spi.ConversationService;

import javax.annotation.Priority;
import javax.enterprise.inject.se.SeContainer;
import javax.enterprise.inject.se.SeContainerInitializer;
import javax.enterprise.inject.spi.Extension;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.function.Consumer;
import java.util.stream.StreamSupport;

import static java.util.Comparator.comparing;
import static java.util.Optional.ofNullable;

// will allow to plug other impl but reusing most of our logic
public class SeInitializerFacade extends SeContainerInitializer
{
    public final static String PROVIDER = SeContainerInitializer.class.getName() + ".provider";

    private final Collection<Consumer<SeContainerInitializer>> initializers = new ArrayList<>();
    private SeContainerInitializer delegate;

    @Override
    public SeContainerInitializer addBeanClasses(final Class<?>... classes)
    {
        initializers.add(i -> i.addBeanClasses(classes));
        return this;
    }

    @Override
    public SeContainerInitializer addPackages(final Class<?>... packageClasses)
    {
        initializers.add(i -> i.addPackages(packageClasses));
        return this;
    }

    @Override
    public SeContainerInitializer addPackages(final boolean scanRecursively, final Class<?>... packageClasses)
    {
        initializers.add(i -> i.addPackages(scanRecursively, packageClasses));
        return this;
    }

    @Override
    public SeContainerInitializer addPackages(final Package... packages)
    {
        initializers.add(i -> i.addPackages(packages));
        return this;
    }

    @Override
    public SeContainerInitializer addPackages(final boolean scanRecursively, final Package... packages)
    {
        initializers.add(i -> i.addPackages(scanRecursively, packages));
        return this;
    }

    @Override
    public SeContainerInitializer addExtensions(final Extension... extensions)
    {
        initializers.add(i -> i.addExtensions(extensions));
        return this;
    }

    @Override
    public SeContainerInitializer addExtensions(final Class<? extends Extension>... extensions)
    {
        initializers.add(i -> i.addExtensions(extensions));
        return this;
    }

    @Override
    public SeContainerInitializer enableInterceptors(final Class<?>... interceptorClasses)
    {
        initializers.add(i -> i.enableInterceptors(interceptorClasses));
        return this;
    }

    @Override
    public SeContainerInitializer enableDecorators(final Class<?>... decoratorClasses)
    {
        initializers.add(i -> i.enableDecorators(decoratorClasses));
        return this;
    }

    @Override
    public SeContainerInitializer selectAlternatives(final Class<?>... alternativeClasses)
    {
        initializers.add(i -> i.selectAlternatives(alternativeClasses));
        return this;
    }

    @Override
    public SeContainerInitializer selectAlternativeStereotypes(final Class<? extends Annotation>... alternativeStereotypeClasses)
    {
        initializers.add(i -> i.selectAlternativeStereotypes(alternativeStereotypeClasses));
        return this;
    }

    @Override
    public SeContainerInitializer addProperty(final String key, final Object value)
    {
        if (PROVIDER.endsWith(key))
        {
            delegate = loadProvider(value);
        }
        initializers.add(i -> i.addProperty(key, value));
        return this;
    }

    @Override
    public SeContainerInitializer setProperties(final Map<String, Object> properties)
    {
        if (properties != null && properties.containsKey(PROVIDER))
        {
            addProperty(PROVIDER, properties.get(PROVIDER));
        }
        initializers.add(i -> i.setProperties(properties));
        return this;
    }

    @Override
    public SeContainerInitializer disableDiscovery()
    {
        initializers.add(SeContainerInitializer::disableDiscovery);
        return this;
    }

    @Override
    public SeContainerInitializer setClassLoader(final ClassLoader classLoader)
    {
        initializers.add(i -> i.setClassLoader(classLoader));
        return this;
    }

    @Override
    public SeContainer initialize()
    {
        final SeContainerInitializer initializer = delegate != null ?
                delegate :
                StreamSupport.stream(ServiceLoader.load(SeContainerSelector.class).spliterator(), false)
                        .min(comparing(it -> ofNullable(it.getClass().getAnnotation(Priority.class))
                                .map(Priority::value)
                                .orElse(0)))
                        .map(SeContainerSelector::find)
                        .orElseGet(OWBInitializer::new);
        initializers.forEach(i -> i.accept(initializer));
        return initializer.initialize();
    }

    private SeContainerInitializer loadProvider(final Object value)
    {
        if (SeContainerInitializer.class.isInstance(value))
        {
            return SeContainerInitializer.class.cast(value);
        }
        if (Class.class.isInstance(value))
        {
            return newInitializerInstance(Class.class.cast(value).asSubclass(SeContainerInitializer.class));
        }
        return newInitializerInstance(findClass(String.valueOf(value)));
    }

    private Class<? extends SeContainerInitializer> findClass(final String name)
    {
        switch (name)
        {
            case "owb":
            case "openwebbeans":
            case "org.apache.openwebbeans.se.OWBInitializer":
                return OWBInitializer.class;
            default:
                try
                {
                    return ofNullable(SeInitializerFacade.class.getClassLoader())
                            .orElseGet(ClassLoader::getSystemClassLoader)
                            .loadClass(name.trim())
                            .asSubclass(SeContainerInitializer.class);
                }
                catch (final ClassNotFoundException e)
                {
                    throw new IllegalArgumentException(e);
                }
        }
    }

    private SeContainerInitializer newInitializerInstance(final Class<? extends SeContainerInitializer> type)
    {
        if (type == OWBInitializer.class)
        {
            final OWBInitializer initializer = new OWBInitializer();
            // in this mode force some SPI impl since when you force a provider you want to fix some ambiguity
            // and these services can easily conflict with meecrowave for example
            initializer.addProperty(ContainerLifecycle.class.getName(), StandaloneLifeCycle.class.getName());
            initializer.addProperty(ContextsService.class.getName(), StandaloneContextsService.class.getName());
            initializer.addProperty(ConversationService.class.getName(), DefaultConversationService.class.getName());
            return initializer;
        }
        try
        {
            return type.getDeclaredConstructor().newInstance();
        }
        catch (final NoSuchMethodException | InstantiationException | IllegalAccessException e)
        {
            throw new IllegalArgumentException(e);
        }
        catch (final InvocationTargetException e)
        {
            throw new IllegalArgumentException(e.getTargetException());
        }
    }
}
