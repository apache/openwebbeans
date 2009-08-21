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
package org.apache.webbeans.spi.ee.openejb.jpa;

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

public class JPAServiceOpenEJBImpl
{
    private Context context;
    
    public JPAServiceOpenEJBImpl(Context context)
    {
        this.context = context;
        
    }
    
    public  EntityManager getPersistenceContext( String unitName) {
        // get JtaEntityManagerRegistry
        JtaEntityManagerRegistry jtaEntityManagerRegistry = SystemInstance.get().getComponent(JtaEntityManagerRegistry.class);

        EntityManagerFactory factory = getPersistenceUnit( unitName );

        JtaEntityManager jtaEntityManager = new JtaEntityManager(jtaEntityManagerRegistry, factory, null, false);

        return jtaEntityManager;
    }

    public  EntityManagerFactory getPersistenceUnit(String unitName) {
        EntityManagerFactory factory;
        try {

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
