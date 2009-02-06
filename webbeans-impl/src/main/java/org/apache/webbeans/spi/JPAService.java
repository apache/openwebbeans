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
package org.apache.webbeans.spi;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

/**
 * 
 * TODO there is currently no Implementation for OpenEJB integration
 */
public interface JPAService
{

    /**
     * get the EntityManagerFactory with the given name.
     * @param unitName JPA persistence unit name
     * @return EntityManagerFactory or <code>null</code> if not found
     */
    public abstract EntityManagerFactory getPersistenceUnit(String unitName);

    /**
     * Get a transactional EntityManager for the current thread using a 
     * ThreadLocal.
     * TODO: from the SPEC: the EntityManger must have dependent scope, but this 
     * does not make sense for e.g. &#x0040;ApplicationScoped.
     * @param unitName the name of the persistence unit. Can be empty or <code>null</code>
     * @param name the name of the EntityManager. Can be empty or <code>null</code>
     * @return a transactional EntityManager
     */
    public abstract EntityManager getPersistenceContext(String unitName, String name);

}