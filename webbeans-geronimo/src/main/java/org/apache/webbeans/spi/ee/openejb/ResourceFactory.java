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
package org.apache.webbeans.spi.ee.openejb;

import java.lang.reflect.Field;

import javax.naming.Context;

import org.apache.openejb.loader.SystemInstance;
import org.apache.openejb.spi.ContainerSystem;

public class ResourceFactory
{
    private static ResourceFactory factory = null;
    
    private ResourceInjectionProcessor processor = null;
    
    private static Context context = null;
    
    static
    {
        try
        {
            context = SystemInstance.get().getComponent(ContainerSystem.class).getJNDIContext();
            
        } catch(Exception e) {
           
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
    
    public Object getResourceObject(Field field) throws RuntimeException
    {
        try
        {
            return this.processor.getResourceObject(field);   
            
        }catch(Exception e)
        {
            throw new RuntimeException(e);
        }
    }

}
