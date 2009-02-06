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
package org.apache.webbeans.spi.ee;

import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.apache.webbeans.exception.WebBeansException;
import org.apache.webbeans.spi.TransactionService;
import org.apache.webbeans.util.JNDIUtil;

public final class TransactionServiceJndiImpl implements TransactionService
{
    private TransactionManager transactionManager = null;


    /**
     * {@inheritDoc}
     */
    public TransactionManager getTransactionManager()
    {
        if (transactionManager == null)
        {
            transactionManager = JNDIUtil.lookup("java:/TransactionManager", TransactionManager.class);
        }

        return transactionManager;
    }


    /**
     * {@inheritDoc}
     */
    public Transaction getTransaction()
    {
        TransactionManager tMgr = getTransactionManager();
        if (tMgr != null)
        {
            try
            {
                return tMgr.getTransaction();
            } 
            catch (SystemException e)
            {
                throw new WebBeansException("cannot get transaction context", e);
            }
        }
        
        return null;
    }
}
