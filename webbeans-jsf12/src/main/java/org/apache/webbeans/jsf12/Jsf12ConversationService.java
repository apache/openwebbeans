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
package org.apache.webbeans.jsf12;

import javax.servlet.http.HttpSession;

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.webbeans.spi.ConversationService;

public class Jsf12ConversationService implements ConversationService
{
    private static final String ATTRIBUTE_NAME_CONVERSATION_ID_COUNTER = "owb_coversationId_counter";

    private AtomicInteger conversationIdCounter = new AtomicInteger(0);


    @Override
    public String getConversationId()
    {
        String conversationId = JSFUtil.getConversationId();
        if (conversationId == null || conversationId.length() == 0)
        {
            return null;
        }
        return conversationId;
    }

    @Override
    public String generateConversationId()
    {
        HttpSession session = JSFUtil.getSession();
        if(session != null)
        {
            AtomicInteger convIdCounter = (AtomicInteger) session.getAttribute(ATTRIBUTE_NAME_CONVERSATION_ID_COUNTER);
            if (convIdCounter == null)
            {
                convIdCounter = new AtomicInteger(0);
                session.setAttribute(ATTRIBUTE_NAME_CONVERSATION_ID_COUNTER, convIdCounter);
            }

            return Long.toString(convIdCounter.incrementAndGet());
        }

        return "inMem_" + conversationIdCounter.incrementAndGet();
    }
}
