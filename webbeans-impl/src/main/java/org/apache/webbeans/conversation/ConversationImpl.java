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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.enterprise.context.Conversation;
import javax.enterprise.context.ConversationScoped;

import org.apache.webbeans.config.OWBLogConst;
import org.apache.webbeans.container.BeanManagerImpl;
import org.apache.webbeans.context.ConversationContext;
import org.apache.webbeans.logger.WebBeansLogger;
import org.apache.webbeans.util.Asserts;

/**
 * Implementation of the {@link Conversation} interface.
 * @version $Rev$ $Date$
 *
 */
public class ConversationImpl implements Conversation
{
    /**Logger instance*/
    private static final WebBeansLogger logger = WebBeansLogger.getLogger(ConversationImpl.class);
    
    /**Conversation id*/
    private String id;

    /**Transient or not. Transient conversations are destroyed at the end of JSF request*/
    private boolean isTransient = true;

    /**Default timeout is 30mins*/
    private long timeout = 30 * 60 * 1000 ;

    /**Id of the session that this conversation is created*/
    private String sessionId;

    /**Active duration of the conversation*/
    private long activeTime = 0L;
    
    /**Generating ids*/
    private static AtomicInteger conversationIdGenerator = new AtomicInteger(0);
    
    /**This instance is under used*/
    private AtomicBoolean inUsed = new AtomicBoolean(false);

    /**
     * Default constructor. Used in tests.
     */
    public ConversationImpl()
    {
        
    }

    /**
     * Creates a new conversation instance. Id is not
     * set until conversation is begin.
     * @param sessionId
     */
    public ConversationImpl(String sessionId)
    {
        Asserts.assertNotNull(sessionId);
        this.sessionId = sessionId;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void begin()
    {
        //Transient state
        if(this.isTransient)
        {
            this.isTransient = false;
            this.id = Integer.toString(conversationIdGenerator.incrementAndGet());
            
            //Conversation manager
            ConversationManager manager = ConversationManager.getInstance();            
            try
            {
                //Gets current converation context instance.
                //Each conversation has its own conversation context instance.
                //Sets at the beginning of each JSF request.
                manager.addConversationContext(this, (ConversationContext) BeanManagerImpl.getManager().getContext(ConversationScoped.class));
                
            }catch(Exception e)
            {
                //TCK tests
                manager.addConversationContext(this, new ConversationContext());
            }            
        }
        //Already started conversation.
        else
        {
            logger.warn(OWBLogConst.WARN_0003, id);
            throw new IllegalStateException();
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void begin(String id)
    {   
        //Look at other conversation, that may collate with this is
        if(ConversationManager.getInstance().isConversationExistWithGivenId(id))
        {
            throw new IllegalArgumentException("Conversation with id=" + id + " is already exist!");
        }
        
        //Transient state
        if(this.isTransient)
        {
            this.isTransient = false;
            this.id = id;
            this.updateTimeOut();
            ConversationManager.getInstance().addConversationContext(this, (ConversationContext) BeanManagerImpl.getManager().getContext(ConversationScoped.class));            
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void end()
    {
        if(!this.isTransient)
        {
            this.isTransient = true;
            
            ConversationManager.getInstance().removeConversation(this);            
        }
        else
        {
            logger.warn(OWBLogConst.WARN_0004, id);
            throw new IllegalStateException();
        }
    }
    
    
    /**
     * @return the inUsed
     */
    public AtomicBoolean getInUsed()
    {
        return inUsed;
    }

    /**
     * @param inUsed the inUsed to set
     */
    public void setInUsed(boolean inUsed)
    {
        this.inUsed.set(inUsed);
    }
    
    /**
     * Sets transient.
     * @param value transient value
     */
    public void setTransient(boolean value)
    {
        this.isTransient = value;
    }
    
    /**
     * {@inheritDoc}
     */    
    @Override
    public String getId()
    {
        return this.id;
    }

    /**
     * {@inheritDoc}
     */    
    @Override
    public long getTimeout()
    {
        return this.timeout;
    }

    /**
     * {@inheritDoc}
     */    
    @Override
    public boolean isTransient()
    {
        return isTransient;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void setTimeout(long milliseconds)
    {
        this.timeout = milliseconds;
    }

    /**
     * Gets session id.
     * @return conversation session id
     */
    public String getSessionId()
    {
        return this.sessionId;
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
        this.activeTime = System.currentTimeMillis();
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
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final ConversationImpl other = (ConversationImpl) obj;
        if (id == null)
        {
            if (other.id != null)
                return false;
        }
        else if (!id.equals(other.id))
            return false;
        if (sessionId == null)
        {
            if (other.sessionId != null)
                return false;
        }
        else if (!sessionId.equals(other.sessionId))
            return false;
        return true;
    }

}
