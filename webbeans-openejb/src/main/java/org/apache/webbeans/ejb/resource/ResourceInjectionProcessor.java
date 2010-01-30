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
package org.apache.webbeans.ejb.resource;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.enterprise.inject.CreationException;
import javax.naming.Context;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
import javax.xml.ws.WebServiceRef;

import org.apache.openejb.loader.SystemInstance;
import org.apache.openejb.persistence.JtaEntityManager;
import org.apache.openejb.persistence.JtaEntityManagerRegistry;


class ResourceInjectionProcessor
{
    private Context context = null;

    ResourceInjectionProcessor(Context context)
    {
        this.context = context;
    }

    public Object getResourceObject(Field field) throws IllegalAccessException, InvocationTargetException, NamingException
    {
        if (context == null)
        {
            // No resource injection
            return null;
        }

        if (field.isAnnotationPresent(Resource.class))
        {
            Resource annotation = field.getAnnotation(Resource.class);
            
            return lookupFieldResource(context, "openejb/Resource/"+annotation.name());
            
        }
        
        if (field.isAnnotationPresent(EJB.class))
        {
            EJB annotation = field.getAnnotation(EJB.class);
            
            return lookupFieldResource(context, annotation.name());
        }
        
        if (field.isAnnotationPresent(WebServiceRef.class))
        {
            WebServiceRef annotation = field.getAnnotation(WebServiceRef.class);
            
            return lookupFieldResource(context, annotation.name());
        }
        
        if (field.isAnnotationPresent(PersistenceContext.class))
        {
            PersistenceContext annotation = field.getAnnotation(PersistenceContext.class);
            
            return this.getPersistenceContext(context, annotation.unitName());
            
        }
        
        if (field.isAnnotationPresent(PersistenceUnit.class))
        {
            PersistenceUnit annotation = field.getAnnotation(PersistenceUnit.class);
            
            return this.getPersistenceUnit(context, annotation.unitName());
        }
        
        return null;
    }

    /**
     * Inject resources in specified field.
     */
    protected Object lookupFieldResource(javax.naming.Context context, String name) throws NamingException, IllegalAccessException
    {
        Object lookedupResource = null;

        if ((name != null) && (name.length() > 0))
        {
            lookedupResource = context.lookup("java:/" + name);                        
        }
        
        return lookedupResource;
    }    
    
    private  EntityManager getPersistenceContext(Context context, String unitName) {
        // get JtaEntityManagerRegistry
        JtaEntityManagerRegistry jtaEntityManagerRegistry = SystemInstance.get().getComponent(JtaEntityManagerRegistry.class);

        EntityManagerFactory factory = getPersistenceUnit(context, unitName);

        JtaEntityManager jtaEntityManager = new JtaEntityManager(jtaEntityManagerRegistry, factory, null, false);

        return jtaEntityManager;
    }

    private  EntityManagerFactory getPersistenceUnit(Context context, String unitName) {
        EntityManagerFactory factory;
        try {

            NamingEnumeration<NameClassPair> persUnits = context.list("java:openejb/PersistenceUnit");
            
            if (persUnits == null)
            {
                throw new CreationException("No PersistenceUnit found in java:openejb/PersistenceUnit!");
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
