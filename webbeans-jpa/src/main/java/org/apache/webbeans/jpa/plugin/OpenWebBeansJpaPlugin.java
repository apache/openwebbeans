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
package org.apache.webbeans.jpa.plugin;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.inject.BindingType;

import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.jpa.spi.JPAService;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;
import javax.persistence.PersistenceUnit;

import org.apache.webbeans.plugins.AbstractOpenWebBeansPlugin;
import org.apache.webbeans.spi.ServiceLoader;
import org.apache.webbeans.util.AnnotationUtil;
import org.apache.webbeans.util.Asserts;
import org.apache.webbeans.xml.XMLAnnotationTypeManager;

public class OpenWebBeansJpaPlugin extends AbstractOpenWebBeansPlugin 
{

    /** {@inheritDoc} */
    public void startUp() throws WebBeansConfigurationException 
    {
        // nothing to do 
    }

    /** {@inheritDoc} */
    public void shutDown() throws WebBeansConfigurationException 
    {
        // nothing to do
    }

    /** {@inheritDoc} */
    public void isSimpleBeanClass( Class<?> clazz ) throws WebBeansConfigurationException 
    {
        if (AnnotationUtil.isAnnotationExistOnClass(clazz, Entity.class))
            throw new WebBeansConfigurationException("Web Beans component implementation class : " + clazz.getName() + " can not be JPA Entity class");

        if (AnnotationUtil.isAnnotationExistOnClass(clazz, PersistenceContext.class))
            throw new WebBeansConfigurationException("Web Beans component implementation class : " + clazz.getName() + " can not be JPA Entity class");

        if (AnnotationUtil.isAnnotationExistOnClass(clazz, PersistenceUnit.class))
            throw new WebBeansConfigurationException("Web Beans component implementation class : " + clazz.getName() + " can not be JPA Entity class");
        
    }

    /** {@inheritDoc} */
    public Object injectResource(Type type, Annotation[] annotations)
    {
        if (!isResource(annotations))
        {
            return null;
        }
        
        Annotation annot = AnnotationUtil.getAnnotation(annotations, PersistenceUnit.class);
        if (annot != null)
        {
            PersistenceUnit pu = (PersistenceUnit) annot;
            String unitName = pu.unitName();
            
            //X TODO what if the EntityManagerFactory is null?
            return getJPAService().getPersistenceUnit(unitName);
        }
        
        annot = AnnotationUtil.getAnnotation(annotations, PersistenceContext.class);
        if (annot != null)
        {
            PersistenceContext pc = (PersistenceContext) annot;
            String unitName = pc.unitName();
            String name = pc.name();
            
            //X TODO what if the EntityManager is null?
            return getJPAService().getPersistenceContext(unitName, name);
        }
        return null;
    }

    /**
     * {@inheritDoc}
     * Returns true if the annotation is a valid JPA Resource handled
     * by this very Plugin.
     * The following annotations indicate such resources:
     * <ol>
     * <li>&#x0040;CustomerDataservice</li>
     * <li>&#x0040;Resource</li>
     * <li>&#x0040;PersistenceContext</li>
     * <li>&#x0040;PersistenceUnit</li>
     * <li>&#x0040;</li>
     * </ol>
     * 
     * Please note that &#x0040;PersistenceContext(type=EXTENDED) 
     * is not supported for simple beans.
     * 
     * @param clazz type of the annotation
     * @return true if the annotation is defined in xml or annotated with
     *         {@link BindingType} false otherwise
     */
    public boolean isResourceAnnotation(Class<? extends Annotation> clazz)
    {
        Asserts.assertNotNull(clazz, "clazz parameter can not be null");
        XMLAnnotationTypeManager manager = XMLAnnotationTypeManager.getInstance();
        if (manager.isResourceExist(clazz))
        {
            return true;
        }
        else if (clazz.equals(PersistenceContext.class) ||
                 clazz.equals(PersistenceUnit.class) )
        {
            return true;
        }

        return false;
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
                    throw new WebBeansConfigurationException("@PersistenceUnit must only be injected into field/method with type EntityManagerFactory! class : "  
                                                             + clazz.getName() + " in field/method : " + name);
                }
            }
            
            if (a.annotationType().equals(PersistenceContext.class))
            {
                PersistenceContext pc = (PersistenceContext) a;
                
                if (!type.equals(EntityManager.class))
                {
                    throw new WebBeansConfigurationException("@PersistenceContext must only be injected into field/method with type EntityManager! class : "  
                                                             + clazz.getName() + " in field/method : " + name);
                }
             
                if (pc.type().equals(PersistenceContextType.EXTENDED))
                {
                    throw new WebBeansConfigurationException("type of @PersistenceContext must not be 'EXTENDED'! class : "  
                            + clazz.getName() + " in field/method : " + name);
                    
                }
            }
        }
    }

    private JPAService getJPAService()
    {
        return ServiceLoader.getService(JPAService.class);
    }

    private boolean isResource(Annotation[] annotations)
    {
        for (Annotation anno : annotations)
        {
            if (isResourceAnnotation(anno.annotationType()))
            {
                return true;
            }
        }
        
        return false;
    }

}
