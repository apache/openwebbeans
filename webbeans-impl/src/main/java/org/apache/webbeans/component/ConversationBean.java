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
package org.apache.webbeans.component;

import javax.enterprise.context.Conversation;
import javax.enterprise.context.spi.CreationalContext;

import org.apache.webbeans.conversation.ConversationImpl;
import org.apache.webbeans.conversation.ConversationManager;
import org.apache.webbeans.spi.ConversationService;
import org.apache.webbeans.spi.ServiceLoader;

/**
 * Conversation bean implementation.
 * @version $Rev$ $Date$
 *
 */
public class ConversationBean extends AbstractBean<Conversation>
{
    /**
     * Default constructor.
     */
    public ConversationBean()
    {
        super(WebBeansType.CONVERSATION, Conversation.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Conversation createInstance(CreationalContext<Conversation> creationalContext)
    {
        Conversation conversation = null;
        //Gets conversation service
        ConversationService conversationService = ServiceLoader.getService(ConversationService.class);        
        //Gets conversation id
        String conversationId = conversationService.getConversationId();       
        //Gets session id that conversation is created
        String sessionId = conversationService.getConversationSessionId();

        //If conversation id is not null, this means that
        //conversation is propogated
        if (conversationId != null)
        {
            //Gets propogated conversation
            conversation = ConversationManager.getInstance().getPropogatedConversation(conversationId,sessionId);
        }
        
        if (conversation == null)
        {
            if(sessionId != null)
            {
                conversation = new ConversationImpl(conversationService.getConversationSessionId());    
            }
            else
            {
                //Used in Tests
                conversation = new ConversationImpl();
            }
            
        }

        return conversation;
    }

    @Override
    protected void destroyInstance(Conversation instance, CreationalContext<Conversation> creationalContext)
    {
        if (instance.isTransient())
        {
            instance = null;
        }
    }
}
