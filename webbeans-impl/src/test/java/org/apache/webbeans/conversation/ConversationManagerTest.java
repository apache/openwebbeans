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

import static java.lang.Thread.sleep;
import static org.apache.webbeans.util.Asserts.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.util.concurrent.atomic.AtomicReference;

import jakarta.enterprise.context.ConversationScoped;

import org.apache.webbeans.context.ConversationContext;
import org.apache.webbeans.spi.ContextsService;
import org.apache.webbeans.spi.ConversationService;
import org.apache.webbeans.test.AbstractUnitTest;
import org.junit.Test;

public class ConversationManagerTest extends AbstractUnitTest
{
    @Test
    public void ensureLastUpdateIsCorrect()
    {
        final AtomicReference<String> conversationId = new AtomicReference<>();
        addService(ConversationService.class, new DefaultConversationService() {
            @Override
            public String getConversationId() {
                return conversationId.get();
            }
        });
        addConfiguration("org.apache.webbeans.application.supportsConversation", "true");
        startContainer();
        final ContextsService contextsService = getWebBeansContext().getContextsService();
        contextsService.startContext(ConversationScoped.class, null);
        final ConversationContext conversationContext1 = ConversationContext.class.cast(
                contextsService.getCurrentContext(ConversationScoped.class));
        final ConversationImpl conversation1 = conversationContext1.getConversation();
        assertNotNull(conversation1);
        assertEquals(0, conversation1.getLastAccessTime());
        conversation1.begin("foo");
        conversationId.set(conversation1.getId());
        final long beginTime = conversation1.getLastAccessTime();
        assertNotEquals(0, beginTime);
        try {
            sleep(100); // avoid cached value (win)
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // another request, will trigger the update
        contextsService.endContext(ConversationScoped.class, null);

        contextsService.startContext(ConversationScoped.class, null);
        final ConversationContext conversationContext2 = ConversationContext.class.cast(
                contextsService.getCurrentContext(ConversationScoped.class));
        final ConversationImpl conversation2 = conversationContext2.getConversation();
        assertEquals(conversation1, conversation2);

        contextsService.getCurrentContext(ConversationScoped.class); // trigger update
        assertNotEquals(beginTime, conversation2.getLastAccessTime());
    }
}
