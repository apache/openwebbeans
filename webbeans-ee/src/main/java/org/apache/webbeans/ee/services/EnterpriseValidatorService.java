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
package org.apache.webbeans.ee.services;

import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.apache.webbeans.plugins.OpenWebBeansJavaEEPlugin;
import org.apache.webbeans.plugins.PluginLoader;
import org.apache.webbeans.spi.ValidatorService;

public class EnterpriseValidatorService implements ValidatorService
{
    private ValidatorService service = null;
    
    public EnterpriseValidatorService()
    {
        OpenWebBeansJavaEEPlugin provider = PluginLoader.getInstance().getJavaEEPlugin();
        if(provider != null)
        {
            service = provider.getValidatorService();
        }
        
    }

    @Override
    public Validator getDefaultValidator()
    {
        if(service != null)
        {
            return service.getDefaultValidator();
        }
        
        return null;
    }

    @Override
    public ValidatorFactory getDefaultValidatorFactory()
    {
        if(service != null)
        {
            return service.getDefaultValidatorFactory();
        }
        
        return null;        
    }

}
