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
package org.apache.webbeans.spi;

/**
 * 
 * Responsible for providing singleton instances
 * for OWB runtime. Provider can implement
 * their own logic to provide singleton instances.
 * 
 * @version $Rev$ $Date$
 */
public interface SingletonService
{
    /**
     * Get instance for given key and class name.
     * @param key instance key
     * @param singletonClassName instance class name
     * @return instance
     */
    public Object get(Object key, String singletonClassName);
    
    /**
     * Any clean activity.
     * @param key instance key
     */
    public void clear(Object key);
    
    /**
     * Returns true if singleton exist,
     * false otherwise.
     * @param key instance key
     * @param singletonClassName class name
     * @return true if singleton exist
     */
    public boolean isExist(Object key, String singletonClassName);
    
    /**
     * Returns exist instance or null.
     * @param key instance key
     * @param singletonClassName class name
     * @return exist instance
     */
    public Object getExist(Object key, String singletonClassName);
    
    /**
     * Returns key for the given singleton
     * instance.
     * @param singleton instance
     * @return key for given instance
     */
    public Object getKey(Object singleton);
}
