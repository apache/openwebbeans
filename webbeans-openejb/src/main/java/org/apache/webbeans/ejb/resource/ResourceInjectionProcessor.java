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

import java.lang.annotation.Annotation;
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
import org.apache.webbeans.util.AnnotationUtil;


class ResourceInjectionProcessor
{
    private Context context = null;

    ResourceInjectionProcessor(Context context)
    {
        this.context = context;
    }

    public <T> T getResourceObject(Class<T> resourceType, Annotation[] resourceAnns) throws IllegalAccessException, InvocationTargetException, NamingException
    {
        Object resource = null;
        
        if (context == null)
        {
            // No resource injection
            return null;
        }

        if (AnnotationUtil.hasAnnotation(resourceAnns, Resource.class))
        {
            Resource annotation = AnnotationUtil.getAnnotation(resourceAnns, Resource.class);            
            resource= lookupFieldResource(context, "openejb/Resource/"+annotation.name());
            
        }
        
        if (AnnotationUtil.hasAnnotation(resourceAnns, EJB.class))
        {
            EJB annotation = AnnotationUtil.getAnnotation(resourceAnns, EJB.class);            
            resource = lookupFieldResource(context, annotation.name());
        }
        
        if (AnnotationUtil.hasAnnotation(resourceAnns, WebServiceRef.class))
        {
            WebServiceRef annotation = AnnotationUtil.getAnnotation(resourceAnns, WebServiceRef.class);            
            resource = lookupFieldResource(context, annotation.name());
        }
        
        if (AnnotationUtil.hasAnnotation(resourceAnns, PersistenceContext.class))
        {
            PersistenceContext annotation = AnnotationUtil.getAnnotation(resourceAnns, PersistenceContext.class);            
            resource = this.getPersistenceContext(context, annotation.unitName());
            
        }
        
        if (AnnotationUtil.hasAnnotation(resourceAnns, PersistenceUnit.class))
        {
            PersistenceUnit annotation = AnnotationUtil.getAnnotation(resourceAnns, PersistenceUnit.class);            
            resource = this.getPersistenceUnit(context, annotation.unitName());
        }
        
        return resourceType.cast(resource);
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
