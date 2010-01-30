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
package org.apache.webbeans.resource;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Type;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;
import javax.persistence.PersistenceUnit;
import javax.xml.ws.WebServiceRef;

import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.plugins.AbstractOpenWebBeansPlugin;
import org.apache.webbeans.spi.ResourceService;
import org.apache.webbeans.spi.ServiceLoader;

public class OpenWebBeansResourcePlugin extends AbstractOpenWebBeansPlugin implements org.apache.webbeans.plugins.OpenWebBeansResourcePlugin
{

    @Override
    public boolean isResourceAnnotation(Class<? extends Annotation> annotationClass)
    {
        if (annotationClass.equals(Resource.class) || 
                annotationClass.equals(WebServiceRef.class) || 
                annotationClass.equals(PersistenceUnit.class) || 
                annotationClass.equals(PersistenceContext.class) ||
                annotationClass.equals(EJB.class))
        {
            return true;
        }

        return false;
    }

    @Override
    public void injectResources(Object instance) throws RuntimeException
    {
        Field[] fields = instance.getClass().getDeclaredFields();
        ResourceService service = ServiceLoader.getService(ResourceService.class);
        for(Field field : fields)
        {
            boolean access = field.isAccessible();
            try
            {                
                field.setAccessible(true);
                Object resource = service.getResource(field);
                
                if(resource != null)
                {
                    field.set(instance, resource);   
                }                
            }
            catch (IllegalAccessException e)
            {
                throw new RuntimeException(e);
            }finally
            {
                field.setAccessible(access);
            }
        }
    }

    /** {@inheritDoc} */
    public void checkForValidResources(Type type, Class<?> clazz, String name, Annotation[] annotations)
    {
        for (Annotation a : annotations)
        {
            if (a.annotationType().equals(PersistenceUnit.class))
            {
                if (!type.equals(EntityManagerFactory.class))
                {
                    throw new WebBeansConfigurationException("@PersistenceUnit must only be injected into field/method with type EntityManagerFactory! class : " + clazz.getName() + " in field/method : " + name);
                }
            }

            if (a.annotationType().equals(PersistenceContext.class))
            {
                PersistenceContext pc = (PersistenceContext) a;

                if (!type.equals(EntityManager.class))
                {
                    throw new WebBeansConfigurationException("@PersistenceContext must only be injected into field/method with type EntityManager! class : " + clazz.getName() + " in field/method : " + name);
                }

                if (pc.type().equals(PersistenceContextType.EXTENDED))
                {
                    throw new WebBeansConfigurationException("type of @PersistenceContext must not be 'EXTENDED'! class : " + clazz.getName() + " in field/method : " + name);

                }
            }
        }
    }

}
