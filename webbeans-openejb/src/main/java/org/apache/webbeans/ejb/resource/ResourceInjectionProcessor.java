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
package org.apache.webbeans.ejb.resource;

import java.lang.annotation.Annotation;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.enterprise.inject.CreationException;
import javax.naming.Context;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
import javax.xml.ws.WebServiceRef;

import org.apache.openejb.loader.SystemInstance;
import org.apache.openejb.persistence.JtaEntityManager;
import org.apache.openejb.persistence.JtaEntityManagerRegistry;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.ejb.EjbPlugin;
import org.apache.webbeans.logger.WebBeansLogger;
import org.apache.webbeans.spi.api.ResourceReference;


class ResourceInjectionProcessor
{
    private Context context = null;
    
    private static final WebBeansLogger logger = WebBeansLogger.getLogger(ResourceInjectionProcessor.class);

    ResourceInjectionProcessor(Context context)
    {
        this.context = context;
    }
    
    public <X, T extends Annotation> X getResourceReference(ResourceReference<X, T> resourceReference) throws Exception
    {
        X resource = null;
        
        if (context == null)
        {
            // No resource injection
            return null;
        }
        
        if(resourceReference.supports(Resource.class))
        {
            Resource annotation = resourceReference.getAnnotation(Resource.class);
            String name = annotation.name();
            if(name == null || name.equals(""))
            {
                name = resourceReference.getOwnerClass() + "/" + resourceReference.getName();
            }
            
            resource= lookupFieldResource(context, "openejb/Resource/"+name, resourceReference.getResourceType());
        }
        else if(resourceReference.supports(EJB.class))
        {
            EJB annotation = resourceReference.getAnnotation(EJB.class);
                        
            String intf = null;
            if(annotation.beanInterface() != null 
                    && annotation.beanInterface() != Object.class
                    && !annotation.beanInterface().equals(""))
            {
                intf = annotation.beanInterface().getName();
            }
            
            if(intf == null)
            {
                intf = resourceReference.getResourceType().getName();
            }
            
            if(intf == null &&  annotation.beanName() == null)
            {
                if(logger.wblWillLogWarn())
                {
                    logger.warn("To inject @EJBs into the managed beans you have to give @EJB(beanName), " +
                            "trying to get inject with @EJB(name) where name is the internal JNDI name of the OpenEJB Bean");
                }
                
                String name = annotation.name();
                if(name == null || name.equals(""))
                {
                    name = resourceReference.getOwnerClass() + "/" + resourceReference.getName();
                }                
             
                resource = lookupFieldResource(context, "openejb/Deployment/"+ name, resourceReference.getResourceType());                
            }            
            else
            {
                EjbPlugin plugin =(EjbPlugin) WebBeansContext.getInstance().getPluginLoader().getEjbPlugin();
                return plugin.getEjbInstance(intf, resourceReference.getResourceType());                
            }
        }
        else if(resourceReference.supports(WebServiceRef.class))
        {
            WebServiceRef annotation = resourceReference.getAnnotation(WebServiceRef.class);
            String name = annotation.name();
            if(name == null || name.equals(""))
            {
                name = resourceReference.getOwnerClass() + "/" + resourceReference.getName();
            }            
            resource = lookupFieldResource(context, name, resourceReference.getResourceType());
        }
        else if(resourceReference.supports(PersistenceUnit.class))
        {
            PersistenceUnit annotation = resourceReference.getAnnotation(PersistenceUnit.class);
            resource = getPersistenceUnit(context, annotation.unitName(), resourceReference.getResourceType());
        }
        else if(resourceReference.supports(PersistenceContext.class))
        {
            PersistenceContext annotation = resourceReference.getAnnotation(PersistenceContext.class);
            resource = getPersistenceContext(context, annotation.unitName(), resourceReference.getResourceType());
        }
        
        return resource;
    }

    /**
     * Inject resources in specified field.
     */
    protected <X> X lookupFieldResource(javax.naming.Context context, String name, Class<X> clazz) throws NamingException, IllegalAccessException
    {
        Object lookedupResource = null;

        if ((name != null) && (name.length() > 0))
        {
            lookedupResource = context.lookup("java:/" + name);                        
        }
        
        return clazz.cast(lookedupResource);
    }    
    
    private  <X> X getPersistenceContext(Context context, String unitName, Class<X> clazz)
    {
        // get JtaEntityManagerRegistry
        JtaEntityManagerRegistry jtaEntityManagerRegistry = SystemInstance.get().getComponent(JtaEntityManagerRegistry.class);

        EntityManagerFactory factory = getPersistenceUnit(context, unitName, EntityManagerFactory.class);

        JtaEntityManager jtaEntityManager = new JtaEntityManager(unitName, jtaEntityManagerRegistry, factory, null, false);

        return clazz.cast(jtaEntityManager);
    }

    private  <X> X getPersistenceUnit(Context context, String unitName, Class<X> clazz)
    {
        EntityManagerFactory factory;
        try
        {

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
            
            
        }
        catch (NamingException e)
        {
            throw new CreationException("PersistenceUnit '" + unitName + "' not found", e );
        }
        return clazz.cast(factory);
    }



}
