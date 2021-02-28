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
package org.apache.webbeans.conversation;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.context.BusyConversationException;
import javax.enterprise.context.Conversation;
import javax.enterprise.context.ConversationScoped;

import org.apache.webbeans.config.OWBLogConst;
import org.apache.webbeans.config.OpenWebBeansConfiguration;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.context.ConversationContext;
import org.apache.webbeans.logger.WebBeansLoggerFacade;

/**
 * Implementation of the {@link Conversation} interface.
 */
public class ConversationImpl implements Conversation, Serializable
{
    private static final long serialVersionUID = 8511063860333431722L;

    /**
     * Logger instance
     */
    private static final Logger logger = WebBeansLoggerFacade.getLogger(ConversationImpl.class);

    /**
     * Conversation id
     */
    private String id;

    /**
     * Transient or not. Transient conversations are destroyed at the end of the request
     */
    private boolean isTransient = true;

    /**
     * Default timeout is 30mins
     */
    private long timeout;

    /**
     * Active duration of the conversation
     */
    private long lastAccessTime;

    private transient RuntimeException problemDuringCreation;

    /**
     * This instance is in use and by which threads.
     * Atomicinteger would be great but then contract of ContextsService would need to be enhanced to
     * be compatible wih WBPhaseListeners. Using thread allow to call iUseIt() multiple times.
     * Long to be serializable.
     * TODO: serialization should be done manually to use the manager otherwise all is broken
     */
    private transient Set<Long> threadsUsingIt = new HashSet<>();

    private transient WebBeansContext webBeansContext;

    /**
     * Default constructor. Used for proxies.
     */
    public ConversationImpl()
    {
        super();
    }

    public ConversationImpl(WebBeansContext webBeansContext)
    {
        this.webBeansContext = webBeansContext;
        try
        {
            timeout = Long.parseLong(this.webBeansContext.getOpenWebBeansConfiguration().
                    getProperty(OpenWebBeansConfiguration.CONVERSATION_TIMEOUT_INTERVAL, "1800000"));
        }
        catch (NumberFormatException e)
        {
            timeout = 30 * 60 * 1000;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void begin()
    {
        begin(null);
    }

    /**
     * {@inheritDoc}
     *
     * This will also put the current ConversationContext into the SessionContext
     */
    @Override
    public void begin(String id)
    {
        if (id == null)
        {
            id = webBeansContext.getConversationService().generateConversationId();
        }

        //Transient state
        if (isTransient)
        {
            isTransient = false;
            this.id = id;
            iUseIt();
            updateLastAccessTime();
        }
        else
        {
            //Already started conversation.
            logger.log(Level.WARNING, OWBLogConst.WARN_0003, id);
            throw new IllegalStateException();
        }

        // now store this conversation in the SessionContext
        ConversationManager conversationManager = webBeansContext.getConversationManager();
        ConversationContext conversationContext = (ConversationContext) webBeansContext.getContextsService().getCurrentContext(ConversationScoped.class, true);
        conversationManager.addToConversationStorage(conversationContext, id);
    }

    /**
     * {@inheritDoc}
     *
     * This will also remove the current ConversationContext from the SessionContext
     */
    @Override
    public void end()
    {
        checkThreadUsage();
        if (!isTransient)
        {
            iDontUseItAnymore();
            id = null;
            isTransient = true;
        }
        else
        {
            logger.log(Level.WARNING, OWBLogConst.WARN_0004, id);
            throw new IllegalStateException(toString() + " has already ended");
        }

        // now store this conversation in the SessionContext
        ConversationManager conversationManager = webBeansContext.getConversationManager();
        ConversationContext conversationContext = (ConversationContext) webBeansContext.getContextsService().getCurrentContext(ConversationScoped.class);
        conversationManager.removeConversationFromStorage(conversationContext);
    }

    public int iUseIt()
    {
        long thread = Thread.currentThread().getId();
        synchronized (this)
        {
            threadsUsingIt.add(thread);
            final int size = threadsUsingIt.size();
            if (size == 1)
            {
                updateLastAccessTime();
            }
            return size;
        }
    }

    public void iDontUseItAnymore()
    {
        long thread = Thread.currentThread().getId();
        synchronized (this)
        {
            threadsUsingIt.remove(thread);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getId()
    {
        return id;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getTimeout()
    {
        checkThreadUsage();
        return timeout;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isTransient()
    {
        checkThreadUsage();
        return isTransient;
    }

    private synchronized void checkThreadUsage()
    {
        if (threadsUsingIt.size() > 1)
        {
            throw new BusyConversationException(
                    "Propogated conversation with sessionid/cid=" + id + " is used by other request.");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setTimeout(long milliseconds)
    {
        checkThreadUsage();
        timeout = milliseconds;
    }

    /**
     * @return the timestamp when this Conversation got accessed the last time
     */
    public long getLastAccessTime()
    {
        return lastAccessTime;
    }

    /**
     * Update conversation timeout value.
     * Basically a 'touch' for the access time
     */
    public void updateLastAccessTime()
    {
        lastAccessTime = System.currentTimeMillis();
    }

    @Override
    public String toString()
    {
        return "Conversation with id [ " + id + " ]";
    }

    public RuntimeException getProblemDuringCreation()
    {
        return problemDuringCreation;
    }

    public void setProblemDuringCreation(RuntimeException problemDuringCreation)
    {
        this.problemDuringCreation = problemDuringCreation;
    }

    /**
     * Initialize a few fields on deserialisation
     */
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException
    {
        in.defaultReadObject();
        webBeansContext = WebBeansContext.currentInstance();
        threadsUsingIt = new HashSet<>();
    }
}
