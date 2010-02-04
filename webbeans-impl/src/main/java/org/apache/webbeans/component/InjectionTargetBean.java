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
package org.apache.webbeans.component;

import java.lang.reflect.Method;
import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;

/**
 * Defines contract for beans that coud have observable
 * method.
 * 
 * @version $Rev$ $Date$
 *
 * @param <T> bean type
 */
public interface InjectionTargetBean<T>
{
    /**
     * Returns set of observable methods.
     * 
     * @return set of observable methods
     */
    public Set<Method> getObservableMethods();

    /**
     * Adds new observer method.
     * 
     * @param observerMethod observer method
     */
    public void addObservableMethod(Method observerMethod);
    
    /**
     * Inject JavaEE resources.
     * 
     * @param instance bean instance
     * @param creationalContext creational context
     */
    public void injectResources(T instance, CreationalContext<T> creationalContext);
    
    /**
     * Inject fields of the bean instance.
     * 
     * @param instance bean instance
     * @param creationalContext creational context
     */
    public void injectFields(T instance, CreationalContext<T> creationalContext);
    
    /**
     * Inject initializer methods of the bean instance.
     * 
     * @param instance bean instance
     * @param creationalContext creational context
     */
    public void injectMethods(T instance, CreationalContext<T> creationalContext);
    
    /**
     * Inject fields of the bean instance.
     * 
     * @param instance bean instance
     * @param creationalContext creational context
     */
    public void injectSuperFields(T instance, CreationalContext<T> creationalContext);
    
    /**
     * Inject initializer methods of the bean instance.
     * 
     * @param instance bean instance
     * @param creationalContext creational context
     */
    public void injectSuperMethods(T instance, CreationalContext<T> creationalContext);

    
    /**
     * Calls post constrcut method.
     * 
     * @param instance bean instance
     */
    public void postConstruct(T instance);
    
    /**
     * Calls predestroy method.
     * 
     * @param instance bean instance
     */
    public void preDestroy(T instance);    
}
