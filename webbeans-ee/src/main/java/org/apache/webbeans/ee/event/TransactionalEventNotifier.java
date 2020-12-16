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
package org.apache.webbeans.ee.event;

import javax.enterprise.event.TransactionPhase;
import javax.enterprise.inject.spi.EventMetadata;
import javax.enterprise.inject.spi.ObserverMethod;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;

import org.apache.webbeans.config.OWBLogConst;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.event.EventContextImpl;
import org.apache.webbeans.logger.WebBeansLoggerFacade;
import org.apache.webbeans.spi.TransactionService;

import java.util.logging.Level;

@SuppressWarnings("unchecked")
public final class TransactionalEventNotifier
{
    private TransactionalEventNotifier()
    {
        // utility class ct
    }

    /**
     * This will get called by the EJB integration code
     *
     * Since registration of the event can blow up if the tx is not active we have a matrix of assumed behaviour
     *
     * There are 3 different error cases when registering a TX Synchronization:
     * RollbackException - Thrown to indicate that the transaction has been marked for rollback only.
     * IllegalStateException - Thrown if the transaction in the target object is in the prepared state or the transaction is inactive.
     * SystemException - Thrown if the transaction manager encounters an unexpected error condition.
     *
     * In case of the SystemException we simply let it blow up. This is usually the case if there
     * is some setup problem.
     *
     * In case of a RollbackException or IllegalStateException we will perform different actions based on the
     * desired TransactionPhase:
     * For AFTER_COMPLETION, BEFORE_COMPLETION and AFTER_FAILURE we will deliver the event immediately.
     * For AFTER_SUCCESS we copmletely skip the event. It will not get invoked at all because the transaction
     * will not succeed.
     */
    public static void registerTransactionSynchronization(TransactionPhase phase, ObserverMethod<? super Object> observer, Object event, EventMetadata metadata) throws Exception
    {
        TransactionService transactionService = WebBeansContext.currentInstance().getService(TransactionService.class);
        
        Transaction transaction = null;
        if(transactionService != null)
        {
            transaction = transactionService.getTransaction();
        }
        
        if(transaction != null)
        {
            if (phase == TransactionPhase.AFTER_COMPLETION)
            {
                registerEvent(transaction, new AfterCompletion(observer, event, metadata), true);
            }
            else if (phase == TransactionPhase.AFTER_SUCCESS)
            {
                if (transaction.getStatus() == Status.STATUS_NO_TRANSACTION)
                {
                    // the AFTER_SUCCESS observers only get invoked if the TX succeeds or if there is no transaction
                    new AfterCompletionSuccess(observer, event, metadata).notifyObserver();
                }
                else
                {
                    registerEvent(transaction, new AfterCompletionSuccess(observer, event, metadata), false);
                }
            }
            else if (phase == TransactionPhase.AFTER_FAILURE)
            {
                registerEvent(transaction, new AfterCompletionFailure(observer, event, metadata), true);
            }
            else if (phase == TransactionPhase.BEFORE_COMPLETION)
            {
                registerEvent(transaction, new BeforeCompletion(observer, event, metadata), true);
            }
            else
            {
                throw new IllegalStateException(WebBeansLoggerFacade.getTokenString(OWBLogConst.EXCEPT_0007) + phase);
            }            
        }
        else
        {
            observer.notify(new EventContextImpl(event, metadata));
        }
    }

    private static void registerEvent(Transaction transaction, AbstractSynchronization synchronization, boolean immediateOnError)
        throws SystemException
    {
        try
        {
            transaction.registerSynchronization(synchronization);
        }
        catch (RollbackException | IllegalStateException re)
        {
            if (immediateOnError)
            {
                synchronization.notifyObserver();
            }
        }
    }

    private static class AbstractSynchronization<T> implements Synchronization
    {

        private final ObserverMethod<T> observer;
        private final T event;
        private final EventMetadata metadata;

        public AbstractSynchronization(ObserverMethod<T> observer, T event, EventMetadata metadata)
        {
            this.observer = observer;
            this.event = event;
            this.metadata = metadata;
        }

        @Override
        public void beforeCompletion()
        {
            // Do nothing
        }

        @Override
        public void afterCompletion(int i)
        {
            //Do nothing
        }

        public void notifyObserver()
        {
            try
            {
                observer.notify(new EventContextImpl(event, metadata));
            }
            catch (Exception e)
            {
                WebBeansLoggerFacade.getLogger(TransactionalEventNotifier.class)
                        .log(Level.SEVERE, OWBLogConst.ERROR_0003, e);
            }
        }
    }

    private static final class BeforeCompletion extends AbstractSynchronization
    {
        private BeforeCompletion(ObserverMethod observer, Object event, EventMetadata metadata)
        {
            super(observer, event, metadata);
        }

        @Override
        public void beforeCompletion()
        {
            notifyObserver();
        }
    }

    private static final class AfterCompletion extends AbstractSynchronization
    {
        private AfterCompletion(ObserverMethod observer, Object event, EventMetadata metadata)
        {
            super(observer, event, metadata);
        }

        @Override
        public void afterCompletion(int i)
        {
            notifyObserver();
        }
    }

    private static final class AfterCompletionSuccess extends AbstractSynchronization
    {
        private AfterCompletionSuccess(ObserverMethod observer, Object event, EventMetadata metadata)
        {
            super(observer, event, metadata);
        }

        @Override
        public void afterCompletion(int i)
        {
            if (i == Status.STATUS_COMMITTED)
            {
                notifyObserver();
            }
        }
    }

    private static final class AfterCompletionFailure extends AbstractSynchronization
    {
        private AfterCompletionFailure(ObserverMethod observer, Object event, EventMetadata metadata)
        {
            super(observer, event, metadata);
        }

        @Override
        public void afterCompletion(int i)
        {
            if (i != Status.STATUS_COMMITTED)
            {
                notifyObserver();
            }
        }
    }
    
}
