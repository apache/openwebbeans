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
import org.apache.webbeans.spi.ServiceLoader;
import org.apache.webbeans.spi.conversation.ConversationService;

public class ConversationComponent extends AbstractComponent<Conversation>
{

    public ConversationComponent()
    {
        super(WebBeansType.CONVERSATION, Conversation.class);
    }

    @Override
    protected Conversation createInstance(CreationalContext<Conversation> creationalContext)
    {
        ConversationService conversationService = ServiceLoader.getService(ConversationService.class);
        
        String conversationId = conversationService.getConversationId();
        
        Conversation conversation = null;

        if (conversationId != null)
        {
            conversation = ConversationManager.getInstance().getConversation(conversationId);
        }
        else
        {
            conversation = new ConversationImpl(conversationService.getConversationSessionId());
        }

        return conversation;
    }

    @Override
    protected void destroyInstance(Conversation instance)
    {
        if (!instance.isLongRunning())
        {
            instance = null;
        }
    }
}
