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
package org.apache.webbeans.ejb.service;

import javax.enterprise.event.TransactionPhase;
import javax.enterprise.inject.spi.ObserverMethod;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import org.apache.openejb.core.CoreUserTransaction;
import org.apache.openejb.loader.SystemInstance;
import org.apache.webbeans.ee.event.TransactionalEventNotifier;
import org.apache.webbeans.logger.WebBeansLogger;
import org.apache.webbeans.spi.TransactionService;

public class OpenEJBTransactionService implements TransactionService
{
    private static final WebBeansLogger logger = WebBeansLogger.getLogger(OpenEJBTransactionService.class);
    
    public OpenEJBTransactionService()
    {
        
    }

    @Override
    public Transaction getTransaction()
    {
        TransactionManager manager = getTransactionManager();
        if(manager != null)
        {
            try
            {
                return manager.getTransaction();
            }
            catch (SystemException e)
            {
                logger.error(e);
            }
        }        
        
        return null; 
    }

    @Override
    public TransactionManager getTransactionManager()
    {
        return SystemInstance.get().getComponent(TransactionManager.class);
    }

    @Override
    public UserTransaction getUserTransaction()
    {
        CoreUserTransaction ut = new CoreUserTransaction(getTransactionManager());
        return ut;
    }

    @Override
    public void registerTransactionSynchronization(TransactionPhase phase, ObserverMethod<? super Object> observer, Object event) throws Exception
    {
        TransactionalEventNotifier.registerTransactionSynchronization(phase, observer, event);
    }

}
