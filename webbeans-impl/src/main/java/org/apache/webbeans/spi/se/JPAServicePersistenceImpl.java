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
package org.apache.webbeans.spi.se;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.apache.webbeans.config.WebBeansFinder;
import org.apache.webbeans.spi.JPAService;
import org.apache.webbeans.spi.se.jpa.EntityManagersManager;

/**
 * SPI Implementation which accesses the PersistenceManagerFactory via the Persistence class
 */
public class JPAServicePersistenceImpl implements JPAService
{
    
    public static final String SINGLETON_WEBBEANS_ENTITYMANAGERS_MAP = "org.apache.webbeans.spi.se.jpa.EntityManagersManager";

    /**
     * {@inheritDoc}
     * 
     */
    public EntityManagerFactory getPersistenceUnit(String unitName)
    {
        //X TODO this currently ignores JNDI
        EntityManagerFactory emf = Persistence.createEntityManagerFactory(unitName);
            
        return emf;
    }

    /** 
     * {@inheritDoc}
     * TODO: currently this returns an extended EntityManager, so we have to wrap it
     */
    public EntityManager getPersistenceContext(String unitName, String name)
    {
        EntityManagerFactory emf = getPersistenceUnit(unitName);
        
        EntityManagersManager entityManagersMgr = (EntityManagersManager) WebBeansFinder.getSingletonInstance(SINGLETON_WEBBEANS_ENTITYMANAGERS_MAP);
        
        EntityManager em = entityManagersMgr.get(unitName, name);
        if (em == null)
        {
            em = emf.createEntityManager();
            entityManagersMgr.set(unitName, name, em);
        }
        
        return em;
    }
}
