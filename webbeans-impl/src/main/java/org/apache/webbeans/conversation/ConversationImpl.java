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

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.context.BusyConversationException;
import javax.enterprise.context.ContextNotActiveException;
import javax.enterprise.context.Conversation;
import javax.enterprise.context.ConversationScoped;

import org.apache.webbeans.config.OWBLogConst;
import org.apache.webbeans.config.OpenWebBeansConfiguration;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.context.ConversationContext;
import org.apache.webbeans.logger.WebBeansLoggerFacade;
import org.apache.webbeans.spi.plugins.OpenWebBeansWebPlugin;
import org.apache.webbeans.util.Asserts;

/**
 * Implementation of the {@link Conversation} interface.
 * @version $Rev$ $Date$
 *
 */
public class ConversationImpl implements Conversation, Serializable
{
    /**
     * 
     */
    private static final long serialVersionUID = 8511063860333431722L;

    /**Logger instance*/
    private final static Logger logger = WebBeansLoggerFacade.getLogger(ConversationImpl.class);
    
    /**Conversation id*/
    private String id;

    /**Transient or not. Transient conversations are destroyed at the end of JSF request*/
    private boolean isTransient = true;

    /**Default timeout is 30mins*/
    private long timeout;

    /**Id of the session that this conversation is created*/
    private String sessionId;

    /**Active duration of the conversation*/
    private long activeTime = 0L;
    
    /**Generating ids*/
    private static AtomicInteger conversationIdGenerator = new AtomicInteger(0);
    
    /**
     This instance is under used and by which threads, Atomicinteger would be great but then contract of ContextsService but be enhanced to
     be compatible wih WBPhaseListeners. Using thread allow to call iUseIt() multiple times.
     String to be serializable.
     TODO: serialization should be done manually to use the manager otherwise all is broken
     */
    private Set<Long> threadsUsingIt = new HashSet<Long>();

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
        catch(NumberFormatException e)
        {
            timeout = 30 * 60 * 1000;
        }
    }

    /**
     * Creates a new conversation instance. Id is not
     * set until conversation is begin.
     * @param sessionId
     * @param webBeansContext
     */
    public ConversationImpl(String sessionId, WebBeansContext webBeansContext)
    {
        Asserts.assertNotNull(sessionId);

        this.webBeansContext = webBeansContext;

        try
        {
            timeout = Long.parseLong(this.webBeansContext.getOpenWebBeansConfiguration().
                    getProperty(OpenWebBeansConfiguration.CONVERSATION_TIMEOUT_INTERVAL, "1800000"));   
        }
        catch(NumberFormatException e)
        {
            timeout = 30 * 60 * 1000;
        }
        
        this.sessionId = sessionId;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void begin()
    {
        //Transient state
        if(isTransient)
        {
            isTransient = false;
            id = Integer.toString(conversationIdGenerator.incrementAndGet());
            iUseIt();
            updateTimeOut();

            //Conversation manager
            ConversationManager manager = webBeansContext.getConversationManager();
            try
            {
                //Gets current conversation context instance.
                //Each conversation has its own conversation context instance.
                //Sets at the beginning of each JSF request.
                manager.addConversationContext(this, getOrStartConversationScope());
                
            }
            catch (ContextNotActiveException cnae)
            {
                throw cnae; // expected by TCKs so throwing it before next catch recreates it
            }
            catch (Exception e)
            {
                //TCK tests, TODO: old ones? remove it?
                manager.addConversationContext(this, new ConversationContext());
            }            
        }
        //Already started conversation.
        else
        {
            logger.log(Level.WARNING, OWBLogConst.WARN_0003, id);
            throw new IllegalStateException();
        }
    }

    private ConversationContext getOrStartConversationScope()
    {
        ConversationContext context = (ConversationContext) webBeansContext.getContextsService().getCurrentContext(ConversationScoped.class);
        if (context == null)
        {
            webBeansContext.getContextsService().startContext(ConversationScoped.class, null);
            context = (ConversationContext) webBeansContext.getContextsService().getCurrentContext(ConversationScoped.class);
        }
        if (context == null)
        {
            throw new ContextNotActiveException(ConversationScoped.class.getName());
        }
        if (!context.isActive())
        {
            context.setActive(true);
        }
        return context;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void begin(String id)
    {
        //Look at other conversation, that may collate with this is
        final ConversationManager conversationManager = webBeansContext.getConversationManager();
        if(conversationManager.isConversationExistWithGivenId(id))
        {
            throw new IllegalArgumentException("Conversation with id=" + id + " is already exist!");
        }
        
        //Transient state
        if(isTransient)
        {
            isTransient = false;
            this.id = id;
            iUseIt();
            if (this.sessionId == null)
            {
                OpenWebBeansWebPlugin web = webBeansContext.getPluginLoader().getWebPlugin();
                if (web != null)
                {
                    this.sessionId = web.currentSessionId();
                }
            }
            updateTimeOut();

            conversationManager.addConversationContext(this, getOrStartConversationScope());
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void end()
    {
        check();
        if(!isTransient)
        {
            webBeansContext.getConversationManager().removeConversation(this);
            iDontUseItAnymore();
            id = null;
            isTransient = true;
        }
        else
        {
            logger.log(Level.WARNING, OWBLogConst.WARN_0004, id);
            throw new IllegalStateException(toString() + " has already ended");
        }
    }

    public int iUseIt()
    {
        long thread = Thread.currentThread().getId();
        synchronized (this)
        {
            threadsUsingIt.add(thread);
            return threadsUsingIt.size();
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
        check();
        return timeout;
    }

    /**
     * {@inheritDoc}
     */    
    @Override
    public boolean isTransient()
    {
        check();
        return isTransient;
    }

    private synchronized void check()
    {
        if (threadsUsingIt.size() > 1)
        {
            throw new BusyConversationException(
                    "Propogated conversation with sessionid/cid=" + sessionId + "/" + id + " is used by other request.");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setTimeout(long milliseconds)
    {
        check();
        timeout = milliseconds;
    }

    /**
     * Gets session id.
     * @return conversation session id
     */
    public String getSessionId()
    {
        return sessionId;
    }

    /**
     * @return the creationTime
     */
    public long getActiveTime()
    {
        return activeTime;
    }


    /**
     * Update conversation timeout value.
     */
    public void updateTimeOut()
    {
        activeTime = System.currentTimeMillis();
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((sessionId == null) ? 0 : sessionId.hashCode());
        return result;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }

        if (obj == null)
        {
            return false;
        }

        if (getClass() != obj.getClass())
        {
            return false;
        }

        final ConversationImpl other = (ConversationImpl) obj;
        if (id == null)
        {
            if (other.id != null)
            {
                return false;
            }
        }
        else if (!id.equals(other.id))
        {
            return false;
        }
        if (sessionId == null)
        {
            if (other.sessionId != null)
            {
                return false;
            }
        }
        else if (!sessionId.equals(other.sessionId))
        {
            return false;
        }
        
        return true;
    }
    
    @Override
    public String toString()
    {
        return "Conversation with id [ " + id + " ]";
    }

    private Object writeReplace() throws ObjectStreamException
    {
        Serialization serialization = new Serialization();
        serialization.setId(id);
        serialization.setSessionId(sessionId);
        return serialization;
    }

    public static class Serialization implements Serializable
    {
        private String sessionId;
        private String id;

        public void setSessionId(String sessionId)
        {
            this.sessionId = sessionId;
        }

        public void setId(String id)
        {
            this.id = id;
        }

        Object  readResolve() throws ObjectStreamException
        {
            return WebBeansContext.currentInstance().getConversationManager().getPropogatedConversation(id, sessionId);
        }
    }
}

