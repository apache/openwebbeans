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
package org.apache.webbeans.jpa;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

/**
 * Helper class which contains various static functions for accessing JPA functionality.
 */
public class JPAUtil
{

    /**
     * get the EntityManagerFactory with the given name.
     * TODO: this should later be implemented via a SPI and either look at 
     *  JNDI if running in a J2EE or Persistence factory if running in a SE environment.
     * @param unitName JPA persistence unit name
     * @return EntityManagerFactory or <code>null</code> if not found
     */
    public static EntityManagerFactory getPersistenceUnit(String unitName)
    {
        //X TODO this currently ignores JNDI
        EntityManagerFactory emf = Persistence.createEntityManagerFactory(unitName);
            
        return emf;
    }

    /**
     * Get a transactional EntityManager for the current thread using a 
     * ThreadLocal.
     * TODO: from the SPEC: the EntityManger must have dependent scope, but this 
     * does not make sense for e.g. &#x0040;ApplicationScoped.
     * TODO: currently this returns an extended EntityManager, so we have to wrap it
     * @param unitName
     * @return a transactional EntityManager
     */
    public static EntityManager getPersistenceContext(String unitName)
    {
        EntityManagerFactory emf = getPersistenceUnit(unitName);
        ThreadLocal<EntityManager> tl = new ThreadLocal<EntityManager>();
        
        EntityManager em = tl.get();
        if (em == null)
        {
            em = emf.createEntityManager();
            tl.set(em);
        }
        
        return em;
    }
}
