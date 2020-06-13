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
package org.apache.openwebbeans.junit5.internal;

import org.apache.openwebbeans.junit5.Cdi;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.platform.commons.util.AnnotationUtils;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.se.SeContainer;
import javax.enterprise.inject.se.SeContainerInitializer;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionTarget;
import java.io.Closeable;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Stream;

// todo: enhance the setup to be thread safe? see Meecrowave ClassLoaderLock class and friends
public class CdiExtension implements BeforeAllCallback, AfterAllCallback, BeforeEachCallback, AfterEachCallback, ParameterResolver
{
    private static SeContainer reusableContainer;

    private SeContainer testInstanceContainer;
    private Collection<CreationalContext<Object>> creationalContexts = new ArrayList<>();
    private Closeable[] onStop;

    @Override
    public void beforeAll(final ExtensionContext extensionContext)
    {
        final Cdi config = getCdiConfig(extensionContext);
        if (config == null)
        {
            return;
        }

        final boolean reusable = config.reusable();
        if (reusable && reusableContainer != null)
        {
            return;
        }
        if (!reusable && reusableContainer != null)
        {
            throw new IllegalStateException(
                    "You can't mix @Cdi(reusable=true) and @Cdi(reusable=false) in the same suite");
        }

        final SeContainerInitializer initializer = SeContainerInitializer.newInstance();
        if (config.disableDiscovery())
        {
            initializer.disableDiscovery();
        }
        initializer.setClassLoader(Thread.currentThread().getContextClassLoader());
        initializer.addBeanClasses(config.classes());
        initializer.enableDecorators(config.decorators());
        initializer.enableInterceptors(config.interceptors());
        initializer.selectAlternatives(config.alternatives());
        initializer.selectAlternativeStereotypes(config.alternativeStereotypes());
        initializer.addPackages(
                Stream.of(config.packages()).map(Class::getPackage).toArray(Package[]::new));
        initializer.addPackages(true,
                Stream.of(config.recursivePackages()).map(Class::getPackage).toArray(Package[]::new));
        onStop = Stream.of(config.onStarts())
                .map(it ->
                {
                    try
                    {
                        return it.getConstructor().newInstance();
                    }
                    catch (final InstantiationException | IllegalAccessException | NoSuchMethodException e)
                    {
                        throw new IllegalStateException(e);
                    }
                    catch (final InvocationTargetException e)
                    {
                        throw new IllegalStateException(e.getTargetException());
                    }
                })
                .peek(Supplier::get)
                .filter(Objects::nonNull)
                .toArray(Closeable[]::new);
        SeContainer container = initializer.initialize();
        if (reusable)
        {
            reusableContainer = container;
            Runtime.getRuntime().addShutdownHook(new Thread(
                () -> doClose(reusableContainer), getClass().getName() + "-shutdown"));
        }
        else
        {
            testInstanceContainer = container;
        }
    }

    @Override
    public void afterAll(final ExtensionContext extensionContext)
    {
        if (testInstanceContainer != null)
        {
            doClose(testInstanceContainer);
            testInstanceContainer = null;
        }
    }

    @Override
    public void beforeEach(final ExtensionContext extensionContext)
    {
        final SeContainer container = getContainer();
        if (container == null)
        {
            return;
        }
        extensionContext.getTestInstances().ifPresent(testInstances ->
        {
            testInstances.getAllInstances().stream().distinct().forEach(instance ->
            {
                final BeanManager manager = container.getBeanManager();
                final AnnotatedType<?> annotatedType = manager.createAnnotatedType(instance.getClass());
                final InjectionTarget injectionTarget = manager.createInjectionTarget(annotatedType);
                final CreationalContext<Object> creationalContext = manager.createCreationalContext(null);
                creationalContexts.add(creationalContext);
                injectionTarget.inject(instance, creationalContext);
            });
        });
    }

    @Override
    public void afterEach(final ExtensionContext extensionContext)
    {
        if (!creationalContexts.isEmpty())
        {
            creationalContexts.forEach(CreationalContext::release);
            creationalContexts.clear();
        }
    }

    private void doClose(final SeContainer container)
    {
        container.close();
        Stream.of(onStop).forEach(it ->
        {
            try
            {
                it.close();
            }
            catch (final IOException e)
            {
                throw new IllegalStateException(e);
            }
        });
    }

    private Cdi getCdiConfig(ExtensionContext extensionContext)
    {
        return AnnotationUtils.findAnnotation(extensionContext.getElement(), Cdi.class).orElse(null);
    }

    private SeContainer getContainer()
    {
        if (testInstanceContainer != null)
        {
            return testInstanceContainer;
        }
        else
        {
            return reusableContainer;
        }
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException
    {
        final Cdi config = extensionContext.getParent().map(this::getCdiConfig).orElse(null);
        if (config == null || !config.injectParameters())
        {
            return false;
        }

        final SeContainer container = getContainer();
        if (container == null)
        {
            return false;
        }

        return container.select(
                parameterContext.getParameter().getType(),
                getQualifiers(parameterContext.getParameter())
        ).isResolvable();
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException
    {
        return getContainer().select(
                parameterContext.getParameter().getType(),
                getQualifiers(parameterContext.getParameter())
        ).get();
    }

    private Annotation[] getQualifiers(Parameter parameter)
    {
        final BeanManager beanManager = getContainer().getBeanManager();
        return Arrays.stream(parameter.getAnnotations())
                .filter(annotation -> beanManager.isQualifier(annotation.annotationType()))
                .toArray(Annotation[]::new);
    }

}
