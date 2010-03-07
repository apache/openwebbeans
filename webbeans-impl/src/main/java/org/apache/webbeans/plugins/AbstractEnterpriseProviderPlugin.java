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
package org.apache.webbeans.plugins;

import javax.enterprise.inject.spi.Bean;

import org.apache.webbeans.spi.SecurityService;
import org.apache.webbeans.spi.TransactionService;
import org.apache.webbeans.spi.ValidatorService;

public abstract class AbstractEnterpriseProviderPlugin extends AbstractOwbPlugin implements OpenWebBeansJavaEEPlugin
{

    @Override
    public Bean<?> getPrincipalBean()
    {
        
        return null;
    }

    @Override
    public SecurityService getSecurityService()
    {
        
        return null;
    }

    @Override
    public TransactionService getTransactionService()
    {
        
        return null;
    }

    @Override
    public Bean<?> getUserTransactionBean()
    {
        
        return null;
    }

    @Override
    public Bean<?> getValidatorBean()
    {
        
        return null;
    }

    @Override
    public Bean<?> getValidatorFactoryBean()
    {
        
        return null;
    }

    @Override
    public ValidatorService getValidatorService()
    {
        
        return null;
    }

}
