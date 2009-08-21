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
package org.apache.webbeans.resource.spi.se;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;


/**
 * SPI Implementation which accesses the PersistenceManagerFactory via the Persistence class
 */
public class JPAServicePersistenceImpl
{
    
    public static final String SINGLETON_WEBBEANS_ENTITYMANAGERS_MAP = EntityManagersManager.class.getName();
    
    private Map<String, EntityManagerFactory> factoryCache = new ConcurrentHashMap<String, EntityManagerFactory>();

    /**
     * {@inheritDoc}
     * 
     */
    public EntityManagerFactory getPersistenceUnit(String unitName)
    {
        if(factoryCache.get(unitName) != null)
        {
            return factoryCache.get(unitName);
        }
        
        //X TODO this currently ignores JNDI
        EntityManagerFactory emf = Persistence.createEntityManagerFactory(unitName);
        
        factoryCache.put(unitName, emf);
            
        return emf;
    }

    /** 
     * {@inheritDoc}
     * TODO: currently this returns an extended EntityManager, so we have to wrap it
     * We have to create a Proxy for injecting entity managers. So, whenever method is called
     * on the entity managers, look at current Transaction, if exist call joinTransaction();
     */
    public EntityManager getPersistenceContext(String unitName)
    {
        EntityManagerFactory emf = getPersistenceUnit(unitName);        
        EntityManager em = emf.createEntityManager();
        
        return em;
    }
}
