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
package org.apache.webbeans.component.creation;

import java.util.Set;

import javax.enterprise.inject.spi.InjectionTarget;
import javax.enterprise.inject.spi.ObserverMethod;

import org.apache.webbeans.component.InjectionTargetBean;
import org.apache.webbeans.component.ProducerFieldBean;
import org.apache.webbeans.component.ProducerMethodBean;

/**
 * Contract for {@link InjectionTargetBean} creation.
 * 
 * <p>
 * Common operations for ManagedBean.
 * </p>
 * 
 * @version $Rev$ $Date$
 *
 * @param <T> bean class info
 */
public interface InjectedTargetBeanCreator<T> extends InjectionTarget<T>
{
    /**
     * Defines bean's producer methods.
     * 
     * @return set of producer methods
     */
    public Set<ProducerMethodBean<?>> defineProducerMethods();

    /**
     * Defines bean's producer fields.
     * 
     * @return set of producer fields
     */
    public Set<ProducerFieldBean<?>> defineProducerFields();
    
    /**
     * Defines disposal methods.
     */
    public void defineDisposalMethods();
    
    /**
     * Define injected fields.
     */
    public void defineInjectedFields();
    
    /**
     * Define injected methods, a.k.a <code>@Initializer</code>
     */
    public void defineInjectedMethods();
    
    /**
     * Define observer methods, a.k.a method parameter with <code>@Observes</code>
     */
    public Set<ObserverMethod<?>> defineObserverMethods();    
    
    /**
     * Gets injection target.
     * 
     * @return injection target
     */
    public InjectionTarget<T> getInjectedTarget();
    
    /**
     * Sets injection target.
     * 
     * @param injectionTarget injection target
     */
    public void setInjectedTarget(InjectionTarget<T> injectionTarget);
    
    /**
     * Returns whether injection target set or not.
     * 
     * <p>
     * If {@link InjectionTarget} instance is set, it is used
     * for creating bean instance, calling post construct and pre-destroy
     * methods.
     * </p>
     * 
     * @return injection target set or not
     */
    public boolean isInjectionTargetSet();
}