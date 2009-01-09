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
package org.apache.webbeans.transaction;

import javax.naming.NamingException;
import javax.transaction.TransactionManager;

import org.apache.webbeans.exception.WebBeansException;
import org.apache.webbeans.logger.WebBeansLogger;
import org.apache.webbeans.util.JNDIUtil;

public final class TransactionUtil
{
    private static WebBeansLogger logger = WebBeansLogger.getLogger(TransactionUtil.class);

    private static TransactionManager transactionManager = null;

    private TransactionUtil()
    {

    }

    public static TransactionManager getCurrentTransactionManager()
    {
        if (transactionManager == null)
        {
            try
            {
                transactionManager = (TransactionManager) JNDIUtil.getInitialContext().lookup("java:/TransactionManager");

            }
            catch (NamingException e)
            {
                logger.error("Unable to get TransactionManager", e);
                throw new WebBeansException(e);
            }
        }

        return transactionManager;
    }

}
