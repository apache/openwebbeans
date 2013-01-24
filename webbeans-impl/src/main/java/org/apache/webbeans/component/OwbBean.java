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

import java.lang.reflect.Member;
import java.util.List;

import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.Producer;

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
    public Producer<T> getProducer();

    /**
     * Sets the producer for this bean
     *
     * @param producer
     */
    public void setProducer(Producer<T> producer);

    /**
     * Returns bean type.
     * 
     * @return webbeans type
     * @see WebBeansType
     */
    public WebBeansType getWebBeansType();
    
    /**
     * Adds new injection point.
     * 
     * @param injectionPoint injection point
     */
    public void addInjectionPoint(InjectionPoint injectionPoint);
    
    /**
     * Gets injection points for given member.
     * <p>
     * For example, if member is field, it gets all
     * injected field's injection points of bean.
     * </p>
     * @param member java member
     * @return injection points for given member
     */
    public List<InjectionPoint> getInjectionPoint(Member member);

    /**
     * Returns bean class type
     * @return bean class type
     */
    public Class<T> getReturnType();

    /**
     * Set specialized flag.
     * @param specialized flag
     */
    public void setSpecializedBean(boolean specialized);
    
    /**
     * Returns true if bean is a specialized bean, false otherwise.
     * @return true if bean is a specialized bean
     */
    public boolean isSpecializedBean();
    
    /**
     * Set enableed flag.
     * @param enabled flag
     */
    public void setEnabled(boolean enabled);    
    
    /**
     * Bean is enabled or not.
     * @return true if enabled
     */    
    public boolean isEnabled();
    
    /**
     * Gets id of the bean.
     * @return id of the bean
     */
    public String getId();
    
    /**
     * True if passivation capable false otherwise.
     * @return true if this bean is passivation capable
     */
    public boolean isPassivationCapable();
    
    /**
     * This determines if this bean is really a dependent bean,
     * and as such always creats a freshl instance for each
     * InjectionPoint. A BeanManagerBean is e.g. not a dependent bean.
     * @return <code>true</code> if this is a dependent bean
     */
    public boolean isDependent();
    
    public WebBeansContext getWebBeansContext();
}
