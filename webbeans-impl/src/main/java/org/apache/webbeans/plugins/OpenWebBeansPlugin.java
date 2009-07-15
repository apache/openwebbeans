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
package org.apache.webbeans.plugins;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.enterprise.inject.spi.InjectionPoint;

import org.apache.webbeans.exception.WebBeansConfigurationException;

/**
 * <p>Interface which all OpenWebBeans plugins has to implement to 
 * extend the webbeans-core with additional IOC functionality.</p>
 * 
 * <p>There are 4 different types of functions for this interface:
 * <ol>
 *  <li>
 *    plugin lifecycle like {@code #startUp()} and {@code #shutDown()}
 *  </li>
 *  <li>
 *    check functions which will be called when a class is scanned
 *    like {@code #isSimpleBeanClass(Class)}
 *  </li>
 *  <li>
 *    injection preparation functions will be called once when
 *    the bean is being scanned like TODO
 *  </li>
 *  <li>
 *    injection execution will be called every time a been get's
 *    injected like {@code #injectResource(Type, Annotation[])}
 *  </li>
 * </ol> 
 * @see PluginLoader for documentation of the whole mechanism
 */
public interface OpenWebBeansPlugin
{
    /**
     * initialise the plugin.
     * This is called once after the very plugin has been loaded.
     * @throws WebBeansConfigurationException
     */
    public void startUp() throws WebBeansConfigurationException;

    /**
     * At shutdown, the plugin must release all locked resources.
     * This is called once before the very plugin gets destroyed.
     * This is usually the case when the WebApplication gets stopped.
     * @throws WebBeansConfigurationException
     */
    public void shutDown() throws WebBeansConfigurationException;
    

    /**
     * Make sure that the given class is ok for simple web bean conditions, 
     * otherwise throw a {@code WebBeansConfigurationException}
     * @param clazz the class to check
     * @throws WebBeansConfigurationException if the given clazz cannot be used as simple web bean.
     */
    public void isManagedBean(Class<?> clazz) throws WebBeansConfigurationException;
    
    /**
     * If plugins applicable, it adds the component for the given {@code InjectionPoint}
     * jms related bean into the manager.
     *
     * @param injectionPoint injection point for the jms bean
     * @return <code>true</code> if plugin is capable for handling the injection point, <code>false</code> otherwise
     */
    public <T> boolean addImplicitComponentForInjectionPoint(InjectionPoint injectionPoint);

    /**
     * Check whether the given annotation class represents a resource which
     * can be injected by this plugin.
     * @param annotationClass which should be ckecked
     * @return <code>true</code> if this plugin handles the resource represented by this annotation
     */
    public boolean isResourceAnnotation(Class<? extends Annotation> annotationClass);

    /**
     * Check conditions for the resources.
     * 
     * @param type
     * @param clazz
     * @param name
     * @param annotations annotations
     * @throws WebBeansConfigurationException if resource annotations exists and do not fit to the fields type, etc.
     * @see #isResourceAnnotation(Class)
     */
    
    public void checkForValidResources(Type type, Class<?> clazz, String name, Annotation[] annotations);
    
    /**
     * Get a resource to inject.
     * A resource is not a usual bean but one predefined one like 
     * javax.persistence.PersistenceUnit, javax.persistence.PersistenceContext
     * etc.
     * @param type the bean type
     * @param annotations all the annotations which are defined on the member. 
     * @return the bean to inject or <code>null</code> if non found.  
     */
    public Object injectResource(Type type, Annotation[] annotations);


}
