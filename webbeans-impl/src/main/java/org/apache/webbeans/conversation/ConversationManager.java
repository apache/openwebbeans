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

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.context.Conversation;
import javax.inject.manager.Bean;

import org.apache.webbeans.annotation.CurrentLiteral;
import org.apache.webbeans.config.WebBeansFinder;
import org.apache.webbeans.container.ManagerImpl;
import org.apache.webbeans.context.ConversationContext;
import org.apache.webbeans.util.Asserts;

public class ConversationManager
{
    private Map<Conversation, ConversationContext> conversations = null;

    public ConversationManager()
    {

    }

    public static ConversationManager getInstance()
    {
        ConversationManager manager = (ConversationManager) WebBeansFinder.getSingletonInstance(WebBeansFinder.SINGLETON_CONVERSATION_MANAGER);
        
        if(manager.conversations == null)
        {
            manager.conversations = new ConcurrentHashMap<Conversation, ConversationContext>();   
        }

        return manager;
    }

    public void addConversationContext(Conversation conversation, ConversationContext context)
    {
        conversations.put(conversation, context);
    }

    public ConversationContext removeConversation(Conversation conversation)
    {
        Asserts.assertNotNull(conversation, "conversation can not be null");

        return conversations.remove(conversation);
    }

    public ConversationContext getConversationContext(Conversation conversation)
    {
        Asserts.assertNotNull(conversation, "conversation can not be null");

        return conversations.get(conversation);
    }

    public Conversation getConversation(String conversationId)
    {
        Asserts.assertNotNull(conversationId, "conversationId parameter can not be null");

        ConversationImpl conv = null;
        Set<Conversation> set = conversations.keySet();
        Iterator<Conversation> it = set.iterator();

        while (it.hasNext())
        {
            conv = (ConversationImpl) it.next();
            if (conv.getId().equals(conversationId))
            {
                return conv;
            }
        }

        return null;

    }

    public void destroyConversationContextWithSessionId(String sessionId)
    {
        Asserts.assertNotNull(sessionId, "sessionId parameter can not be null");

        ConversationImpl conv = null;
        Set<Conversation> set = conversations.keySet();
        Iterator<Conversation> it = set.iterator();

        while (it.hasNext())
        {
            conv = (ConversationImpl) it.next();
            if (conv.getSessionId().equals(sessionId))
            {
                it.remove();
            }
        }
    }

    public Conversation createNewConversation()
    {
        Conversation conversation = getCurrentConversation();

        return conversation;

    }

    public Conversation getCurrentConversation()
    {

        Bean<Conversation> bean = ManagerImpl.getManager().resolveByType(Conversation.class, new CurrentLiteral()).iterator().next();
        Conversation conversation = ManagerImpl.getManager().getInstance(bean);

        return conversation;
    }

    public void destroyWithRespectToTimout()
    {
        ConversationImpl conv = null;
        Set<Conversation> set = conversations.keySet();
        Iterator<Conversation> it = set.iterator();

        while (it.hasNext())
        {
            conv = (ConversationImpl) it.next();
            long timeout = conv.getTimeout();

            if (timeout != 0L)
            {
                if ((System.currentTimeMillis() - conv.getActiveTime()) > timeout)
                {
                    it.remove();
                }
            }
        }
    }

    public void destroyAllConversations()
    {
        if (conversations != null)
        {
            conversations.clear();
            conversations = null;
        }
    }
}
