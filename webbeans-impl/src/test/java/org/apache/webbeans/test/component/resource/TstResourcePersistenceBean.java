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
package org.apache.webbeans.test.component.resource;

import javax.annotation.Named;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;

/**
 * This simple web bean gets a &#x0040;PersistenceContext injected 
 *
 */
@Named
public class TstResourcePersistenceBean
{

    @PersistenceUnit(unitName="openwebbeanstest")
    private EntityManagerFactory emf;

    @PersistenceContext(unitName="openwebbeanstest")
    private EntityManager em;
    
    public EntityManagerFactory getEntityManagerFactory()
    {
        return emf;
    }

    public EntityManager getEntityManager()
    {
        return em;
    }
    
}
