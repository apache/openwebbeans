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
    public void startUp() throws Exception;

    /**
     * At shutdown, the plugin must release all locked resources.
     * This is called once before the very plugin gets destroyed.
     * This is usually the case when the WebApplication gets stopped.
     * @throws WebBeansConfigurationException
     */
    public void shutDown() throws Exception;
    

    /**
     * Make sure that the given class is ok for simple web bean conditions, 
     * otherwise throw a {@code WebBeansConfigurationException}
     * @param clazz the class to check
     * @throws WebBeansConfigurationException if the given clazz cannot be used as simple web bean.
     */
    public void isManagedBean(Class<?> clazz) throws Exception;
}
