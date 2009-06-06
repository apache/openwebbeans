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
package org.apache.webbeans.spi.ee.openejb;

import javax.enterprise.inject.CreationException;
import javax.naming.Context;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.apache.openejb.loader.SystemInstance;
import org.apache.openejb.persistence.JtaEntityManager;
import org.apache.openejb.persistence.JtaEntityManagerRegistry;
import org.apache.openejb.spi.ContainerSystem;
import org.apache.webbeans.jpa.spi.JPAService;

/**
 * {@inheritDoc} 
 * Implementation for getting the EntityManager and EntityManagerFactory 
 * from the OpenEJB container.
 */
public class JPAServiceOpenEJBImpl implements JPAService 
{

    /**
     * {@inheritDoc}
     * This will create a JtaEntityManager instead of a simply JPA EntityManager.
     * So injecting this into a bean should be perfectly safe!
     */
    public EntityManager getPersistenceContext( String unitName, String name ) {
        // get JtaEntityManagerRegistry
        JtaEntityManagerRegistry jtaEntityManagerRegistry = SystemInstance.get().getComponent(JtaEntityManagerRegistry.class);

        EntityManagerFactory factory = getPersistenceUnit( unitName );

        JtaEntityManager jtaEntityManager = new JtaEntityManager(jtaEntityManagerRegistry, factory, null, false);

        return jtaEntityManager;
    }

    /**
     * {@inheritDoc}
     * 
     * We have to search for the shortest full match!
     * if our searched PersistenceUnit should be "movieDb" and the
     * persistence.xml contains "movieDbMirror and movieDb", then we must find the 2nd
     * otoh we cannot simply search for the PersistencUnits name, because in JNDI
     * it additionally contains a hash code at the end of the name string.
     * @param unitName the name of the PersistenceUnit to search for
     * @return EntitMa
     */
    public EntityManagerFactory getPersistenceUnit( String unitName ) {
        EntityManagerFactory factory;
        try {
            Context context = SystemInstance.get().getComponent(ContainerSystem.class).getJNDIContext();

            NamingEnumeration<NameClassPair> persUnits = context.list("java:openejb/PersistenceUnit");
            if (persUnits == null)
            {
                throw new CreationException( "No PersistenceUnit found in java:openejb/PersistenceUnit!" );
            }

            String shortestMatch = null;
            
            while (persUnits.hasMore())
            {
                NameClassPair puNc = persUnits.next();
                
                if (puNc.getName().startsWith(unitName))
                {
                    if (shortestMatch == null || shortestMatch.length() > puNc.getName().length())
                    {
                        shortestMatch = puNc.getName();
                    }
                }
                
            }
            
            if (shortestMatch == null)
            {
                throw new CreationException("PersistenceUnit '" + unitName + "' not found");
            }
            
            factory = (EntityManagerFactory) context.lookup("java:openejb/PersistenceUnit/" + shortestMatch);
        } catch (NamingException e) {
            throw new CreationException("PersistenceUnit '" + unitName + "' not found", e );
        }
        return factory;
    }

}
