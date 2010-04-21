/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.webbeans.resource.spi.se;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import javax.annotation.Resource;
import javax.enterprise.inject.Produces;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
import javax.xml.ws.WebServiceRef;

import org.apache.webbeans.api.ResourceReference;
import org.apache.webbeans.exception.WebBeansException;
import org.apache.webbeans.logger.WebBeansLogger;
import org.apache.webbeans.spi.ResourceInjectionService;
import org.apache.webbeans.util.AnnotationUtil;
import org.apache.webbeans.util.SecurityUtil;

public class StandaloneResourceInjectionService implements ResourceInjectionService
{
    private static final StandaloneResourceProcessor processor = StandaloneResourceProcessor.getProcessor();
    
    private static final WebBeansLogger logger = WebBeansLogger.getLogger(StandaloneResourceInjectionService.class);

    @Override
    public <X, T extends Annotation> X getResourceReference(ResourceReference<X, T> resourceReference)
    {        
        if(resourceReference.supports(Resource.class))
        {         
            Resource resource = resourceReference.getAnnotation(Resource.class);
            return processor.getResource(resource, resourceReference.getResourceType());
        }

        if(resourceReference.supports(WebServiceRef.class))
        {         
            WebServiceRef resource = resourceReference.getAnnotation(WebServiceRef.class);
            return processor.getWebServiceResource(resource, resourceReference.getResourceType());

        }
                
        if(resourceReference.supports(PersistenceContext.class))
        {
            PersistenceContext persistenceContext = resourceReference.getAnnotation(PersistenceContext.class);
            return processor.getEntityManager(persistenceContext, resourceReference.getResourceType());
        }
        
        if(resourceReference.supports(PersistenceUnit.class))
        {
            PersistenceUnit persistenceUnit = resourceReference.getAnnotation(PersistenceUnit.class);
            return processor.getEntityManagerFactory(persistenceUnit, resourceReference.getResourceType());
        }
        
        return null;
    }

    @Override
    public void injectJavaEEResources(Object managedBeanInstance) throws Exception
    {
        Field[] fields = SecurityUtil.doPrivilegedGetDeclaredFields(managedBeanInstance.getClass());
        for(Field field : fields)
        {
            if(!field.isAnnotationPresent(Produces.class))
            {
                if(!Modifier.isStatic(field.getModifiers()))
                {
                    Annotation ann = AnnotationUtil.hasOwbInjectableResource(field.getDeclaredAnnotations());                
                    if(ann != null)
                    {
                        @SuppressWarnings("unchecked")
                        ResourceReference<Object, ?> resourceRef = new ResourceReference(field.getDeclaringClass(), field.getName(), field.getType(), ann);
                        boolean acess = field.isAccessible();
                        try
                        {
                            SecurityUtil.doPrivilegedSetAccessible(field, true);
                            field.set(managedBeanInstance, getResourceReference(resourceRef));
                            
                        }catch(Exception e)
                        {
                            logger.error("Unable to inject field : " + field,e);
                            throw new WebBeansException("Unable to inject field : " + field,e);
                            
                        }finally
                        {
                            SecurityUtil.doPrivilegedSetAccessible(field, acess);
                        }                        
                    }                    
                }                
            }
        }    
     }

    @Override
    public void clear()
    {
        processor.clear();       
    }
    

}