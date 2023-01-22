/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.webbeans.test.portable.scopeextension;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

import jakarta.enterprise.context.spi.Context;
import jakarta.enterprise.context.spi.Contextual;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.BeforeShutdown;
import jakarta.enterprise.event.Observes;

public class ExternalTestScopeContext implements Context 
{
    private static final Map<Contextual<?>, Object> instances = new HashMap<Contextual<?>, Object>();
    
    private boolean active;
    
    public ExternalTestScopeContext(boolean active)
    {
        this.active = active;
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public <T> T get(Contextual<T> component) 
    {
        
        return (T) instances.get(component);
    }

    @Override
    public <T> T get(Contextual<T> component, CreationalContext<T> creationalContext) 
    {
        @SuppressWarnings("unchecked")
        T instance = (T) instances.get(component);
        if (instance == null)
        {
            component.create(creationalContext);
        }
        return null;
    }

    @Override
    public Class<? extends Annotation> getScope() 
    {
        return ExternalTestScoped.class;
    }

    @Override
    public boolean isActive()
    {
        return active;
    }
    
    public void endContext(@Observes BeforeShutdown beforeShutdown)
    {
        // a real world extension would destroy all contextual instances here
    }

}
