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
package org.apache.webbeans.service;

import javax.annotation.Priority;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.Annotated;
import javax.inject.Inject;

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.container.BeanManagerImpl;
import org.apache.webbeans.spi.InjectionPointService;
import org.apache.webbeans.util.ClassUtil;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static java.util.Comparator.comparing;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

/**
 * Enables to elect an annotated (field, constructor, method) as having (virtually) {@code @Inject}.
 * {@code implicitSupport} enables to use qualifiers as implicit markers for {@code @Inject}.
 * It also supports a delegation chain through {@code delegateClasses} configuration
 * which will look up services (from their class names) and they will be sorted by {@code @Priority}.
 */
public class DefaultInjectionPointService implements InjectionPointService
{
    private final BeanManagerImpl manager;
    private final boolean implicitSupport;
    private final List<InjectionPointService> delegates;

    public DefaultInjectionPointService(final WebBeansContext context)
    {
        this.manager = context.getBeanManagerImpl();
        this.implicitSupport = Boolean.parseBoolean(context.getOpenWebBeansConfiguration().getProperty(
                DefaultInjectionPointService.class.getName() + ".implicitSupport"));
        this.delegates = ofNullable(context.getOpenWebBeansConfiguration().getProperty(
                    DefaultInjectionPointService.class.getName() + ".delegateClasses"))
                .map(Stream::of)
                .orElseGet(Stream::empty)
                .flatMap(it -> Stream.of(it.split(",")))
                .map(String::trim)
                .filter(it -> !it.isEmpty())
                .map(ClassUtil::getClassFromName)
                .filter(Objects::nonNull)
                .map(context::getService)
                .map(InjectionPointService.class::cast)
                .sorted(comparing(it -> ofNullable(it.getClass().getAnnotation(Priority.class))
                        .map(Priority::value)
                        .orElse(Integer.MAX_VALUE)))
                .collect(toList());
    }

    @Override
    public boolean hasInjection(final Annotated annotated)
    {
        if (annotated.isAnnotationPresent(Inject.class))
        {
            return true;
        }
        if (!implicitSupport)
        {
            return false;
        }
        if (annotated.getAnnotations().stream().anyMatch(a -> manager.isQualifier(a.annotationType()))
                && annotated.getAnnotations().stream().noneMatch(it -> it.annotationType() == Produces.class))
        {
            return true;
        }
        if (delegates.isEmpty())
        {
            return false;
        }
        return delegates.stream().anyMatch(d -> d.hasInjection(annotated));
    }
}
