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

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.BeforeBeanDiscovery;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.configurator.AnnotatedTypeConfigurator;

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.configurator.AnnotatedTypeConfiguratorImpl;
import org.apache.webbeans.container.BeanManagerImpl;
import org.apache.webbeans.container.ExternalScope;
import org.apache.webbeans.deployment.StereoTypeModel;
import org.apache.webbeans.portable.events.EventBase;

/**
 * Events that is fired before container starts to discover beans.
 * 
 * @version $Rev$ $Date$
 *
 */
public class BeforeBeanDiscoveryImpl extends EventBase implements BeforeBeanDiscovery, ExtensionAware
{
    
    private BeanManagerImpl beanManager;
    private final WebBeansContext webBeansContext;
    private Extension extension;
    private Map<String, AnnotatedTypeConfiguratorHolder> annotatedTypeConfigurators = new HashMap<>();
    private Set<AnnotatedTypeConfiguratorImpl<?>> interceptorBindingConfigurators = new HashSet<>();
    private Set<AnnotatedTypeConfiguratorImpl<?>> qualifierConfigurators = new HashSet<>();

    public BeforeBeanDiscoveryImpl(WebBeansContext webBeansContext)
    {
        this.webBeansContext = webBeansContext;
        beanManager = this.webBeansContext.getBeanManagerImpl();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addQualifier(Class<? extends Annotation> qualifier)
    {
        checkState();
        beanManager.addAdditionalQualifier(qualifier);
        
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addInterceptorBinding(Class<? extends Annotation> binding, Annotation... bindingDef)
    {
        checkState();
        webBeansContext.getInterceptorsManager().addInterceptorBindingType(binding, bindingDef);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addScope(Class<? extends Annotation> scope, boolean normal, boolean passivating)
    {
        checkState();
        ExternalScope additionalScope = new ExternalScope(scope, normal, passivating); 
        beanManager.addAdditionalScope(additionalScope);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addStereotype(Class<? extends Annotation> stereotype, Annotation... stereotypeDef)
    {
        checkState();
        webBeansContext.getAnnotationManager().checkStereoTypeClass(stereotype, stereotypeDef);
        StereoTypeModel model = new StereoTypeModel(webBeansContext, stereotype, stereotypeDef);
        webBeansContext.getStereoTypeManager().addStereoTypeModel(model);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addAnnotatedType(AnnotatedType<?> annotatedType, String id)
    {
        checkState();
        beanManager.addAdditionalAnnotatedType(extension, annotatedType, id);

        // and also remove any AnnotatedTypeConfigurator for that combination
        String key = annotatedType.getJavaClass() + id;
        annotatedTypeConfigurators.remove(key);
    }

    @Override
    public <T> AnnotatedTypeConfigurator<T> addAnnotatedType(Class<T> clazz, String id)
    {
        checkState();
        String key = clazz.getName() + id;
        AnnotatedTypeConfiguratorHolder configuratorHolder = annotatedTypeConfigurators.get(key);
        if (configuratorHolder == null)
        {
            AnnotatedTypeConfiguratorImpl<T> configurator = getAnnotatedTypeConfigurator(clazz);
            configuratorHolder = new AnnotatedTypeConfiguratorHolder(extension, id, configurator);
            annotatedTypeConfigurators.put(key, configuratorHolder);
        }

        return configuratorHolder.getAnnotatedTypeConfigurator();
    }

    @Override
    public <T extends Annotation> AnnotatedTypeConfigurator<T> configureInterceptorBinding(Class<T> clazz)
    {
        checkState();

        AnnotatedTypeConfiguratorImpl<T> configurator = getAnnotatedTypeConfigurator(clazz);
        interceptorBindingConfigurators.add(configurator);

        return configurator;
    }

    @Override
    public <T extends Annotation> AnnotatedTypeConfigurator<T> configureQualifier(Class<T> clazz)
    {
        checkState();

        AnnotatedTypeConfiguratorImpl<T> configurator = getAnnotatedTypeConfigurator(clazz);
        qualifierConfigurators.add(configurator);

        return configurator;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addInterceptorBinding(AnnotatedType<? extends Annotation> annotatedType)
    {
        checkState();
        // TODO extract inherited types
        webBeansContext.getInterceptorsManager().addInterceptorBindingType(annotatedType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addQualifier(AnnotatedType<? extends Annotation> annotatedType)
    {
        checkState();
        beanManager.addAdditionalQualifier(annotatedType);
    }

    public void setExtension(Extension extension)
    {
        this.extension = extension;
    }

    public Collection<AnnotatedTypeConfiguratorHolder> getAnnotatedTypeConfigurators()
    {
        return annotatedTypeConfigurators.values();
    }

    public Collection<AnnotatedTypeConfiguratorImpl<?>> getInterceptorBindingConfigurators()
    {
        return interceptorBindingConfigurators;
    }

    public Collection<AnnotatedTypeConfiguratorImpl<?>> getQualifierConfigurators()
    {
        return qualifierConfigurators;
    }

    private <T> AnnotatedTypeConfiguratorImpl<T> getAnnotatedTypeConfigurator(Class<T> clazz)
    {
        AnnotatedType<T> initialAnnotatedType = webBeansContext.getAnnotatedElementFactory().newAnnotatedType(clazz);
        return (AnnotatedTypeConfiguratorImpl<T>) new AnnotatedTypeConfiguratorImpl(webBeansContext, initialAnnotatedType);
    }
}
