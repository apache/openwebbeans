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
package org.apache.webbeans.context.creational;

import jakarta.enterprise.context.spi.Contextual;
import jakarta.enterprise.context.spi.CreationalContext;

import org.apache.webbeans.config.WebBeansContext;

/**
 * Factory for {@link CreationalContext} instances.
 * 
 * @version $Rev$ $Date$
 *
 * @param <T> contextual type info
 */
public final class CreationalContextFactory<T>
{
    private WebBeansContext webBeansContext;

    /**
     * Creates a new <code>CreationalContextFactory</code> instance.
     * @param webBeansContext
     */
    public CreationalContextFactory(WebBeansContext webBeansContext)
    {
        this.webBeansContext = webBeansContext;
    }

    /**
     * Returns a new creational context for given contextual.
     * 
     * @param contextual contextual instance
     * @return new creational context for given contextual
     */
    public CreationalContextImpl<T> getCreationalContext(Contextual<T> contextual)
    {        
        return new CreationalContextImpl<>(contextual, webBeansContext);
    }        
    
    public CreationalContextImpl<T> wrappedCreationalContext(CreationalContext<T> creationalContext, Contextual<T> contextual)
    {
        return new WrappedCreationalContext<>(contextual, creationalContext, webBeansContext);
    }
    
}
