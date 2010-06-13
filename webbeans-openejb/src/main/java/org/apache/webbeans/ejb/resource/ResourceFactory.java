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

import javax.naming.Context;
import javax.naming.NamingException;

import org.apache.webbeans.config.OWBLogConst;
import org.apache.openejb.loader.SystemInstance;
import org.apache.openejb.spi.ContainerSystem;
import org.apache.webbeans.logger.WebBeansLogger;
import org.apache.webbeans.spi.api.ResourceReference;

public class ResourceFactory
{
    private static final WebBeansLogger logger = WebBeansLogger.getLogger(ResourceFactory.class);
    
    private static ResourceFactory factory = null;
    
    private ResourceInjectionProcessor processor = null;
    
    private static Context context = null;
    
    static
    {
        try
        {
            context = SystemInstance.get().getComponent(ContainerSystem.class).getJNDIContext();
        }
        catch(Exception e)
        {
            context = null;
        }

    }
    
    public static ResourceFactory getInstance()
    {
        if(factory ==  null)
        {
            factory = new ResourceFactory();
            factory.processor = new ResourceInjectionProcessor(context);
        }
        
        return factory;
    }
    
    public <X, T extends Annotation> X getResourceReference(ResourceReference<X, T> resourceReference) throws Exception
    {
        try
        {
            return this.processor.getResourceReference(resourceReference);   
        }
        catch(Exception e)
        {
           throw e;
        }
    }
    
    public void close()
    {
        try
        {
            context.close();
            factory = null;
            processor = null;
        }
        catch (NamingException e)
        {
            logger.warn(OWBLogConst.WARN_0013, e);
        }
    }

}
