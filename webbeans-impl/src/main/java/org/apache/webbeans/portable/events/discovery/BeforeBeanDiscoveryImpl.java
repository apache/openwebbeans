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

/**
 * Events that is fired before container starts to discover beans.
 * 
 * @version $Rev$ $Date$
 *
 */
public class BeforeBeanDiscoveryImpl implements BeforeBeanDiscovery
{
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void addAnnotatedType(AnnotatedType<?> type)
    {
        // TODO Auto-generated method stub
        
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addBindingType(Class<? extends Annotation> bindingType)
    {
        // TODO Auto-generated method stub
        
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addInterceptorBindingType(Class<? extends Annotation> bindingType, Annotation... bindingTypeDef)
    {
        // TODO Auto-generated method stub
        
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addScopeType(Class<? extends Annotation> scopeType, boolean normal, boolean passivating)
    {
        // TODO Auto-generated method stub
        
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addStereotype(Class<? extends Annotation> stereotype, Annotation... stereotypeDef)
    {
        // TODO Auto-generated method stub
        
    }

}