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
package org.apache.webbeans.ee.services;

import javax.enterprise.event.TransactionPhase;
import javax.enterprise.inject.spi.ObserverMethod;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import org.apache.webbeans.ee.event.TransactionalEventNotifier;
import org.apache.webbeans.plugins.OpenWebBeansJavaEEPlugin;
import org.apache.webbeans.plugins.PluginLoader;
import org.apache.webbeans.spi.TransactionService;

public final class EnterpriseTransactionService implements TransactionService
{
    private TransactionService service = null;
    
    public EnterpriseTransactionService()
    {
        OpenWebBeansJavaEEPlugin provider = PluginLoader.getInstance().getJavaEEPlugin();
        if(provider != null)
        {
            service = provider.getTransactionService();
        }
    }

    /**
     * {@inheritDoc}
     */
    public TransactionManager getTransactionManager()
    {
        if(service != null)
        {
            return service.getTransactionManager();
        }
        
        return null;
    }


    /**
     * {@inheritDoc}
     */
    public Transaction getTransaction()
    {
        if(service != null)
        {
            return service.getTransaction();
        }
        
        return null;
        
    }


    public UserTransaction getUserTransaction()
    {
        if(service != null)
        {
            return service.getUserTransaction();
        }
        
        return null;
        
    }


    @Override
    public void registerTransactionSynchronization(TransactionPhase phase, ObserverMethod<? super Object> observer, Object event) throws Exception
    {
        TransactionalEventNotifier.registerTransactionSynchronization(phase, observer, event);
    }
}
