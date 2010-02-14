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

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Resource;
import javax.naming.InitialContext;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;

import org.apache.webbeans.config.OWBLogConst;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.logger.WebBeansLogger;
import org.apache.webbeans.util.AnnotationUtil;

public class StandaloneResourceProcessor
{ 
    private static InitialContext context = null;
    
    private static WebBeansLogger logger = WebBeansLogger.getLogger(StandaloneResourceProcessor.class);
    
    private static final StandaloneResourceProcessor processor = new StandaloneResourceProcessor();
    
    /**
     *  A cache for EntityManagerFactories.
     */
    private Map<String, EntityManagerFactory> factoryCache = new ConcurrentHashMap<String, EntityManagerFactory>();    
    
    static
    {
        try
        {
            context = new InitialContext();
            
        }catch(Exception e)
        {
            throw new ExceptionInInitializerError(e);
        }
    }

    public static StandaloneResourceProcessor getProcessor()
    {
        return processor;
    }
    
    public <T> T getResource(Class<?> owner, Class<T> resourceClass, Annotation[] resourceAnnotations)
    {
        Object obj = null;
        
        if (AnnotationUtil.hasAnnotation(resourceAnnotations, PersistenceContext.class))
        {
            PersistenceContext context = AnnotationUtil.getAnnotation(resourceAnnotations, PersistenceContext.class);            
            obj = getPersistenceContext(context.unitName());
            if (obj == null) 
            {
            	logger.info("Could not find @PersistenceContext with unit name " + context.unitName());
            }
        }
        
        else if (AnnotationUtil.hasAnnotation(resourceAnnotations, PersistenceUnit.class))
        {
            PersistenceUnit annotation = AnnotationUtil.getAnnotation(resourceAnnotations, PersistenceUnit.class);
            
            obj = getPersistenceUnit(annotation.unitName());
            if (obj == null) 
            {
            	logger.info("Could not find @PersistenceUnit with unit name " + annotation.unitName());
            }
        }
        else if (AnnotationUtil.hasAnnotation(resourceAnnotations, Resource.class))
        {
            Resource resource = AnnotationUtil.getAnnotation(resourceAnnotations, Resource.class);     
            try
            {
                obj = context.lookup("java:/comp/env/"+ resource.name()); 
                if (obj == null) 
                {
                	logger.info("Could not find @Resource with name " + resource.name());
                }

            }
            catch(Exception e)
            {
                logger.error(OWBLogConst.ERROR_0001, new Object[] {resource.toString()});
            }             
        }        
        
        return resourceClass.cast(obj);
    }
    
    public <T> void validateResource(Class<?> owner, Class<T> resourceClass, Annotation[] resourceAnnotations)
    {
        try
        {
            getResource(owner, resourceClass, resourceAnnotations);
            
        }catch(Exception e)
        {
            logger.error("Unable to get resource with class " + resourceClass + " in " + owner,e);
            throw new WebBeansConfigurationException("Unable to get resource with class " + resourceClass + " in " + owner,e);
        }    
    }

    /**
     * {@inheritDoc}
     * 
     */
    private EntityManagerFactory getPersistenceUnit(String unitName)
    {
        if(factoryCache.get(unitName) != null)
        {
            return factoryCache.get(unitName);
        }
        
        EntityManagerFactory emf = Persistence.createEntityManagerFactory(unitName);        
        factoryCache.put(unitName, emf);
            
        return emf;
    }

    /** 
     * TODO: currently this returns an extended EntityManager, so we have to wrap it
     * We have to create a Proxy for injecting entity managers. So, whenever method is called
     * on the entity managers, look at current Transaction, if exist call joinTransaction();
     */
    private EntityManager getPersistenceContext(String unitName)
    {
        EntityManagerFactory emf = getPersistenceUnit(unitName);        
        EntityManager em = emf.createEntityManager();
        
        return em;
    }
    
    public void clear()
    {
        Set<String> keys = this.factoryCache.keySet();
        for(String key : keys)
        {
            EntityManagerFactory factory = this.factoryCache.get(key);
            try
            {
                factory.close();
                
            }catch(Exception e)
            {
                logger.warn("Unable to close entity manager factory with name : " + key,e);
            }
        }
    }

}
