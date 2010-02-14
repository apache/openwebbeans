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

import javax.ejb.EJB;

import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.logger.WebBeansLogger;
import org.apache.webbeans.plugins.AbstractOwbPlugin;
import org.apache.webbeans.plugins.OpenWebBeansResourcePlugin;

public class OpenEjbResourcePlugin extends AbstractOwbPlugin implements OpenWebBeansResourcePlugin
{
    private static final WebBeansLogger logger = WebBeansLogger.getLogger(OpenEjbResourcePlugin.class);

    @Override
    public void clear()
    {
        ResourceFactory.getInstance().close();
    }

    @Override
    public boolean isResourceAnnotation(Class<? extends Annotation> annotationClass)
    {
        if(annotationClass.equals(EJB.class))
        {
            return true;
        }
        
        return false;
    }


    @Override
    public <T> T getResource(Class<?> owner, String name, Class<T> resourceType, Annotation[] resoAnnotations)
    {
        try
        {
            return ResourceFactory.getInstance().getResource(resourceType, resoAnnotations);
            
        }catch(Exception e)
        {
            logger.error("Unable to get resource with class " + resourceType + " in " + owner + " with name " + name,e);
            throw new WebBeansConfigurationException("Unable to get resource with class " + resourceType + " in " + owner + " with name " + name,e);
        }
    }


    @Override
    public <T> void validateResource(Class<?> owner, String name, Class<T> resourceType, Annotation[] resoAnnotations)
    {
        getResource(owner, name, resourceType, resoAnnotations);
    }
     
}
