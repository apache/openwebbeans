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

import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.container.BeanManagerImpl;
import org.apache.webbeans.container.ExternalScope;
import org.apache.webbeans.deployment.StereoTypeModel;

/**
 * Events that is fired before container starts to discover beans.
 * 
 * @version $Rev$ $Date$
 *
 */
public class BeforeBeanDiscoveryImpl implements BeforeBeanDiscovery, ExtensionAware
{
    
    private BeanManagerImpl beanManager = null;
    private final WebBeansContext webBeansContext;
    private Object extension;
    private boolean started;

    public BeforeBeanDiscoveryImpl(WebBeansContext webBeansContext)
    {
        this.webBeansContext = webBeansContext;
        beanManager = this.webBeansContext.getBeanManagerImpl();
    }

    public void setStarted()
    {
        started = true;
    }
    /**
     * {@inheritDoc}
     */
    @Override
    public void addAnnotatedType(AnnotatedType<?> type)
    {
        if (started)
        {
            throw new IllegalStateException("Only call container eevnt methods in their lifecycle");
        }
        beanManager.addAdditionalAnnotatedType(extension, type);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addQualifier(Class<? extends Annotation> qualifier)
    {
        if (started)
        {
            throw new IllegalStateException("Only call container eevnt methods in their lifecycle");
        }
        beanManager.addAdditionalQualifier(qualifier);
        
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addInterceptorBinding(Class<? extends Annotation> binding, Annotation... bindingDef)
    {
        if (started)
        {
            throw new IllegalStateException("Only call container eevnt methods in their lifecycle");
        }
        webBeansContext.getInterceptorsManager().addInterceptorBindingType(binding, bindingDef);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addScope(Class<? extends Annotation> scope, boolean normal, boolean passivating)
    {
        if (started)
        {
            throw new IllegalStateException("Only call container eevnt methods in their lifecycle");
        }
        ExternalScope additionalScope = new ExternalScope(scope, normal, passivating); 
        beanManager.addAdditionalScope(additionalScope);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addStereotype(Class<? extends Annotation> stereotype, Annotation... stereotypeDef)
    {
        if (started)
        {
            throw new IllegalStateException("Only call container eevnt methods in their lifecycle");
        }
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
        if (started)
        {
            throw new IllegalStateException("Only call container eevnt methods in their lifecycle");
        }
        beanManager.addAdditionalAnnotatedType(extension, annotatedType, id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addInterceptorBinding(AnnotatedType<? extends Annotation> annotatedType)
    {
        if (started)
        {
            throw new IllegalStateException("Only call container eevnt methods in their lifecycle");
        }
        // TODO extract inherited types
        webBeansContext.getInterceptorsManager().addInterceptorBindingType(annotatedType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addQualifier(AnnotatedType<? extends Annotation> annotatedType)
    {
        if (started)
        {
            throw new IllegalStateException("Only call container eevnt methods in their lifecycle");
        }
        beanManager.addAdditionalQualifier(annotatedType.getJavaClass());
    }

    public void setExtension(final Object extension)
    {
        this.extension = extension;
    }
}
