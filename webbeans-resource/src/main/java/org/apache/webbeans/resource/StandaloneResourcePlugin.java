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

import javax.annotation.Resource;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
import javax.xml.ws.WebServiceRef;

import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.logger.WebBeansLogger;
import org.apache.webbeans.plugins.AbstractOwbPlugin;
import org.apache.webbeans.resource.spi.se.StandaloneResourceProcessor;

public class StandaloneResourcePlugin extends AbstractOwbPlugin implements org.apache.webbeans.plugins.OpenWebBeansResourcePlugin
{

    private static final WebBeansLogger logger = WebBeansLogger.getLogger(StandaloneResourcePlugin.class);
    
    @Override
    public boolean isResourceAnnotation(Class<? extends Annotation> annotationClass)
    {
        if (annotationClass.equals(Resource.class) || 
                annotationClass.equals(WebServiceRef.class) || 
                annotationClass.equals(PersistenceUnit.class) || 
                annotationClass.equals(PersistenceContext.class))
        {
            return true;
        }

        return false;
    }

    @Override
    public void clear()
    {
        StandaloneResourceProcessor.getProcessor().clear();
    }

    @Override
    public <T> T getResource(Class<?> owner, String name, Class<T> resourceType, Annotation[] resourceAnns)
    {
        try
        {
            return StandaloneResourceProcessor.getProcessor().getResource(owner, resourceType, resourceAnns);
            
        }catch(Exception e)
        {
            logger.error("Unable to get resource with class " + resourceType + " in " + owner + " with name " + name,e);
            throw new WebBeansConfigurationException("Unable to get resource with class " + resourceType + " in " + owner + " with name " + name,e);
        }
        
    }

    @Override
    public <T> void validateResource(Class<?> owner, String name, Class<T> resourceType, Annotation[] resourceAnns)
    {
        getResource(owner, name, resourceType, resourceAnns);
    }

}
