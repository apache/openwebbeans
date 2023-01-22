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
package org.apache.webbeans.context;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.enterprise.context.ConversationScoped;

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.conversation.ConversationImpl;

/**
 * Conversation context implementation.
 * This reflects THE current Conversation (there can only be one active at a time for a thread).
 * It should not be confused with the Map of conversationId -> Conversation
 * which we internally store in the SessionContext.
 */
public class ConversationContext extends PassivatingContext
{
    private static final long serialVersionUID = 2L;

    private ConversationImpl conversation;

    // for serialisation
    public ConversationContext()
    {
        this(WebBeansContext.currentInstance());
    }

    /**
     * Constructor
     */
    public ConversationContext(WebBeansContext webBeansContext)
    {
        super(ConversationScoped.class);
        this.conversation = new ConversationImpl(webBeansContext);
    }

    @Override
    public void setComponentInstanceMap()
    {
        componentInstanceMap = new ConcurrentHashMap<>();
    }

    public ConversationImpl getConversation()
    {
        return conversation;
    }


    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
    {
        this.conversation = (ConversationImpl) in.readObject();
        super.readExternal(in);
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException
    {
        out.writeObject(conversation);
        super.writeExternal(out);
    }
}
