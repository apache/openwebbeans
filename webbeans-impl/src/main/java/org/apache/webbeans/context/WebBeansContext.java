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
package org.apache.webbeans.context;

import java.util.Map;

import javax.enterprise.context.spi.Contextual;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;

import org.apache.webbeans.context.type.ContextTypes;

/**
 * Defines spi for contexts.
 * 
 * @version $Rev$ $Date$
 */
public interface WebBeansContext extends javax.enterprise.context.spi.Context
{
    /**
     * Removes the given web beans component from the context.
     * 
     * @param <T> generic type
     * @param component web beans component
     */
    public <T> void remove(Contextual<T> component);
        
    /**
     * Destroys the context.
     */
    public void destroy();
    
    /**
     * Return context type.
     * 
     * @return context type
     */
    public ContextTypes getType();
    
    /**
     * Returns instance map.
     * 
     * @return instance map
     */
    public Map<Contextual<?>, Object> getComponentInstanceMap();
    
    /**
     * Remove given bean from context.
     * 
     * @param <T> type of bean
     * @param container beans container
     * @param component bean
     */
    public <T> void remove(BeanManager container, Bean<T> component);
    
}