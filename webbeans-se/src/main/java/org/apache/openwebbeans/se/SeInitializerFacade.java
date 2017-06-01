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

import javax.enterprise.inject.se.SeContainer;
import javax.enterprise.inject.se.SeContainerInitializer;
import javax.enterprise.inject.spi.Extension;
import java.lang.annotation.Annotation;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;

// will allow to plug other impl but reusing most of our logic
public class SeInitializerFacade extends SeContainerInitializer
{
    private final SeContainerInitializer delegate;

    public SeInitializerFacade()
    {
        delegate = Optional.of(ServiceLoader.load(SeContainerSelector.class).iterator())
                .filter(Iterator::hasNext)
                .map(Iterator::next)
                .map(SeContainerSelector::find)
                .orElseGet(OWBInitializer::new);
    }

    @Override
    public SeContainerInitializer addBeanClasses(final Class<?>... classes)
    {
        return delegate.addBeanClasses(classes);
    }

    @Override
    public SeContainerInitializer addPackages(final Class<?>... packageClasses)
    {
        return delegate.addPackages(packageClasses);
    }

    @Override
    public SeContainerInitializer addPackages(final boolean scanRecursively, final Class<?>... packageClasses)
    {
        return delegate.addPackages(scanRecursively, packageClasses);
    }

    @Override
    public SeContainerInitializer addPackages(final Package... packages)
    {
        return delegate.addPackages(packages);
    }

    @Override
    public SeContainerInitializer addPackages(final boolean scanRecursively, final Package... packages)
    {
        return delegate.addPackages(scanRecursively, packages);
    }

    @Override
    public SeContainerInitializer addExtensions(final Extension... extensions)
    {
        return delegate.addExtensions(extensions);
    }

    @Override
    public SeContainerInitializer addExtensions(final Class<? extends Extension>... extensions)
    {
        return delegate.addExtensions(extensions);
    }

    @Override
    public SeContainerInitializer enableInterceptors(final Class<?>... interceptorClasses)
    {
        return delegate.enableInterceptors(interceptorClasses);
    }

    @Override
    public SeContainerInitializer enableDecorators(final Class<?>... decoratorClasses)
    {
        return delegate.enableDecorators(decoratorClasses);
    }

    @Override
    public SeContainerInitializer selectAlternatives(final Class<?>... alternativeClasses)
    {
        return delegate.selectAlternatives(alternativeClasses);
    }

    @Override
    public SeContainerInitializer selectAlternativeStereotypes(final Class<? extends Annotation>... alternativeStereotypeClasses)
    {
        return delegate.selectAlternativeStereotypes(alternativeStereotypeClasses);
    }

    @Override
    public SeContainerInitializer addProperty(final String key, final Object value)
    {
        return delegate.addProperty(key, value);
    }

    @Override
    public SeContainerInitializer setProperties(final Map<String, Object> properties)
    {
        return delegate.setProperties(properties);
    }

    @Override
    public SeContainerInitializer disableDiscovery()
    {
        return delegate.disableDiscovery();
    }

    @Override
    public SeContainerInitializer setClassLoader(final ClassLoader classLoader)
    {
        return delegate.setClassLoader(classLoader);
    }

    @Override
    public SeContainer initialize()
    {
        return delegate.initialize();
    }
}
