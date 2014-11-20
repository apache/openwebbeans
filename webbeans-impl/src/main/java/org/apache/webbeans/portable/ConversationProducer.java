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
package org.apache.webbeans.portable;

import java.util.Collections;

import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.InjectionPoint;

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.context.creational.CreationalContextImpl;
import org.apache.webbeans.conversation.ConversationImpl;
import org.apache.webbeans.spi.ConversationService;

public class ConversationProducer extends InjectionTargetImpl<ConversationImpl>
{

    private WebBeansContext webBeansContext;
    
    public ConversationProducer(AnnotatedType<ConversationImpl> annotatedType, WebBeansContext webBeansContext)
    {
        super(annotatedType, Collections.<InjectionPoint>emptySet(), webBeansContext, null, null);
        this.webBeansContext = webBeansContext;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected ConversationImpl newInstance(CreationalContextImpl<ConversationImpl> creationalContext)
    {
        ConversationImpl conversation = null;
        //Gets conversation service
        ConversationService conversationService = webBeansContext.getService(ConversationService.class);

        if (conversationService == null)
        {
            // in case where we do not support a 'real' Conversation handling, the user will just get a temporarily one.
            return new ConversationImpl(webBeansContext);

        }

        //Gets conversation id
        String conversationId = conversationService.getConversationId();       
        //Gets session id that conversation is created
        String sessionId = conversationService.getConversationSessionId();

        //If conversation id is not null, this means that
        //conversation is propogated
        if (conversationId != null)
        {
            //Gets propogated conversation
            conversation = webBeansContext.getConversationManager().getPropogatedConversation(conversationId, sessionId);
        }
        
        if (conversation == null)
        {
            if(sessionId != null)
            {
                conversation = new ConversationImpl(conversationService.getConversationSessionId(), webBeansContext);
            }
            else
            {
                //Used in Tests
                conversation = new ConversationImpl(webBeansContext);
            }
            
        }

        return conversation;
    }
}
