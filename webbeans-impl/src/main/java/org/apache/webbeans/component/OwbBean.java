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
package org.apache.webbeans.component;

import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.Producer;

import org.apache.webbeans.config.WebBeansContext;

/**
 * OWB specific extension of the {@link Bean} interface.
 * It is used internally. Do not use it. Instead use {@link AbstractOwbBean}
 * for extension.
 * 
 * @version $Rev$ $Date$
 * <T> bean class
 */
public interface OwbBean<T> extends Bean<T>
{
    /**
     * @return the producer for this bean;
     */
    Producer<T> getProducer();

    /**
     * Returns bean type.
     * 
     * @return webbeans type
     * @see WebBeansType
     */
    WebBeansType getWebBeansType();
    
    /**
     * Returns bean class type
     * @return bean class type
     */
    Class<T> getReturnType();

    /**
     * Set specialized flag.
     * @param specialized flag
     */
    void setSpecializedBean(boolean specialized);
    
    /**
     * Returns true if bean is a specialized bean, false otherwise.
     * @return true if bean is a specialized bean
     */
    boolean isSpecializedBean();
    
    /**
     * Set enableed flag.
     * @param enabled flag
     */
    void setEnabled(boolean enabled);
    
    /**
     * Bean is enabled or not.
     * @return true if enabled
     */
    boolean isEnabled();
    
    /**
     * Gets id of the bean.
     * @return id of the bean
     */
    String getId();
    
    /**
     * True if passivation capable false otherwise.
     * @return true if this bean is passivation capable
     */
    boolean isPassivationCapable();
    
    /**
     * This determines if this bean is really a dependent bean,
     * and as such always creats a freshl instance for each
     * InjectionPoint. A BeanManagerBean is e.g. not a dependent bean.
     * @return <code>true</code> if this is a dependent bean
     */
    boolean isDependent();
    
    
    /**
     * Gets the context instance in which this bean belongs to.
     * @return the {@link WebBeansContext} instance
     */
    WebBeansContext getWebBeansContext();
}
