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
package org.apache.webbeans.portable.events.discovery;

import javax.enterprise.inject.spi.AfterTypeDiscovery;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.configurator.AnnotatedTypeConfigurator;
import java.util.List;

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.portable.events.EventBase;

/**
 * OWB fires this event after all AnnotatedTypes from scanned classes
 * got picked up.
 */
public class AfterTypeDiscoveryImpl extends EventBase implements AfterTypeDiscovery, ExtensionAware
{
    private final WebBeansContext webBeansContext;
    private final List<Class<?>> sortedAlternatives;
    private final List<Class<?>> sortedInterceptors;
    private final List<Class<?>> sortedDecorators;
    private final List<AnnotatedType<?>> newAt;
    private Extension extension;

    public AfterTypeDiscoveryImpl(WebBeansContext webBeansContext,
                                  List<AnnotatedType<?>> newAt,
                                  List<Class<?>> sortedInterceptors,
                                  List<Class<?>> sortedDecorators,
                                  List<Class<?>> sortedAlternatives)
    {
        this.webBeansContext = webBeansContext;
        this.newAt = newAt;
        this.sortedAlternatives = sortedAlternatives;
        this.sortedInterceptors = sortedInterceptors;
        this.sortedDecorators = sortedDecorators;
    }

    @Override
    public List<Class<?>> getAlternatives()
    {
        checkState();
        return sortedAlternatives;
    }

    @Override
    public List<Class<?>> getInterceptors()
    {
        checkState();
        return sortedInterceptors;
    }

    @Override
    public List<Class<?>> getDecorators()
    {
        checkState();
        return sortedDecorators;
    }

    @Override
    public void addAnnotatedType(AnnotatedType<?> type, String id)
    {
        checkState();
        webBeansContext.getBeanManagerImpl().addAdditionalAnnotatedType(extension, type, id);
        newAt.add(type);
    }

    //X TODO OWB-1182 CDI 2.0
    @Override
    public <T> AnnotatedTypeConfigurator<T> addAnnotatedType(Class<T> aClass, String s)
    {
        throw new UnsupportedOperationException("CDI 2.0 not yet imlemented");
    }

    @Override
    public void setExtension(Extension instance)
    {
        this.extension = instance;
    }
}
