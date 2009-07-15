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

import javax.enterprise.inject.spi.Bean;

public interface OpenWebBeansEjbPlugin extends OpenWebBeansPlugin
{
    /**
     * Returns true if class is an ejb class false otherwise
     * 
     * @param clazz class definition
     * @return true if class is an ejb class false otherwise
     */
    public boolean isEjbClass(Class<?> clazz);
    
    /**
     * Configures ejb component and adds it into the container.
     * @param clazz ejb class
     */
    public <T> Bean<T> defineEjbComponent(Class<T> clazz);
    
    public boolean isSingletonBean(Class<?> clazz);
    
    public boolean isStatelessBean(Class<?> clazz);
    
    public boolean isStatefulBean(Class<?> clazz);

    public Object getProxy(Bean<?> bean);
}
