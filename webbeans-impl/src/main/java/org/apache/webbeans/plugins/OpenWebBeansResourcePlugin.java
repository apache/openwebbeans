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

public interface OpenWebBeansResourcePlugin extends OpenWebBeansPlugin 
{
    /**
     * Given plugin supports injection of resource. 
     * @param annotationClass annotation class
     * @return true if supports
     */
    public boolean isResourceAnnotation(Class<? extends Annotation> annotationClass);

    /**
     * Gets resource instance.
     * @param <T> resource type info
     * @param owner reource owner
     * @param name resource member name
     * @param resourceType resource type
     * @param resourceAnnoations resource anns
     * @return resource instance
     * @throws Exception for exception
     */
    <T> T getResource(Class<?> owner, String name, Class<T> resourceType, Annotation[] resourceAnnoations);    
    
    /**
     * Any clear functionality.
     */
    void clear();
    
    /**
     * Validate resource.
     * @param <T> resource type info
     * @param owner reource owner
     * @param name resource member name
     * @param resourceType resource type
     * @param resourceAnnoations resource anns
     * @return resource instance
     * @throws Exception for exception
     */    
    <T> void validateResource(Class<?> owner, String name, Class<T> resourceType, Annotation[] resourceAnnoations);
    
}
