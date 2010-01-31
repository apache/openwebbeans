/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.apache.webbeans.portable.events.discovery;

import java.lang.annotation.Annotation;

import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;

import org.apache.webbeans.container.BeanManagerImpl;
import org.apache.webbeans.container.ExternalScope;
import org.apache.webbeans.deployment.StereoTypeManager;
import org.apache.webbeans.deployment.StereoTypeModel;
import org.apache.webbeans.util.WebBeansUtil;
import org.apache.webbeans.xml.XMLAnnotationTypeManager;

/**
 * Events that is fired before container starts to discover beans.
 * 
 * @version $Rev$ $Date$
 *
 */
public class BeforeBeanDiscoveryImpl implements BeforeBeanDiscovery
{
    
    private BeanManagerImpl beanManager = null;

    public BeforeBeanDiscoveryImpl()
    {
        beanManager = BeanManagerImpl.getManager();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void addAnnotatedType(AnnotatedType<?> type)
    {
        beanManager.addAdditionalAnnotatedType(type);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addQualifier(Class<? extends Annotation> qualifier)
    {
        beanManager.addAdditionalQualifier(qualifier);
        
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addInterceptorBinding(Class<? extends Annotation> binding, Annotation... bindingDef)
    {
        XMLAnnotationTypeManager.getInstance().addInterceotorBindingTypeInheritAnnotation(binding, bindingDef);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addScope(Class<? extends Annotation> scope, boolean normal, boolean passivating)
    {
        ExternalScope additionalScope = new ExternalScope(scope, normal, passivating); 
        beanManager.addAdditionalScope(additionalScope);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addStereotype(Class<? extends Annotation> stereotype, Annotation... stereotypeDef)
    {
        if (!XMLAnnotationTypeManager.getInstance().hasStereoType(stereotype))
        {
            WebBeansUtil.checkStereoTypeClass(stereotype, stereotypeDef);
            StereoTypeModel model = new StereoTypeModel(stereotype, stereotypeDef);
            StereoTypeManager.getInstance().addStereoTypeModel(model);
        }
        
    }

}