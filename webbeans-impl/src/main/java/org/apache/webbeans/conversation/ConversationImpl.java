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
package org.apache.webbeans.conversation;

import javax.enterprise.context.Conversation;
import javax.enterprise.context.ConversationScoped;

import org.apache.webbeans.container.BeanManagerImpl;
import org.apache.webbeans.context.ConversationContext;
import org.apache.webbeans.logger.WebBeansLogger;
import org.apache.webbeans.util.Asserts;
import org.apache.webbeans.util.StringUtil;

public class ConversationImpl implements Conversation
{
    private static final WebBeansLogger logger = WebBeansLogger.getLogger(ConversationImpl.class);
    
    private String id;

    private boolean isTransient = true;

    private long timeout;

    private String sessionId;

    private long activeTime = 0L;

    public ConversationImpl()
    {

    }

    public ConversationImpl(String sessionId)
    {
        Asserts.assertNotNull(sessionId);
        this.sessionId = sessionId;

    }

    public void begin()
    {
        if(this.isTransient)
        {
            this.isTransient = false;
            this.id = StringUtil.generateUUIDStringWithoutDash();
            
            ConversationManager manager = ConversationManager.getInstance();
            
            try
            {
                manager.addConversationContext(this, (ConversationContext) BeanManagerImpl.getManager().getContext(ConversationScoped.class));
                
            }catch(Exception e)
            {
                //TCK tests
                manager.addConversationContext(this, new ConversationContext());
            }            
        }
        else
        {
            logger.warn("Conversation with cid=" + id + " is already began!");
            throw new IllegalStateException();
        }
    }

    public void begin(String id)
    {
        if(this.isTransient)
        {
            this.isTransient = false;
            this.id = id;

            ConversationManager.getInstance().addConversationContext(this, (ConversationContext) BeanManagerImpl.getManager().getContext(ConversationScoped.class));            
        }
    }

    public void end()
    {
        if(!this.isTransient)
        {
            this.isTransient = true;
            
            ConversationManager.getInstance().removeConversation(this);            
        }
    }
    
    public void setTransient(boolean value)
    {
        this.isTransient = value;
    }

    public String getId()
    {
        return this.id;
    }

    public long getTimeout()
    {
        return this.timeout;
    }

    public boolean isTransient()
    {
        return isTransient;
    }

    public void setTimeout(long milliseconds)
    {
        this.timeout = milliseconds;
    }

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