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

import java.lang.annotation.Annotation;
import java.util.Iterator;
import java.util.Set;

import javax.enterprise.context.ContextException;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.context.spi.Context;

import org.apache.webbeans.annotation.BeforeDestroyedLiteral;
import org.apache.webbeans.annotation.DestroyedLiteral;
import org.apache.webbeans.annotation.InitializedLiteral;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.conversation.ConversationImpl;
import org.apache.webbeans.conversation.ConversationManager;
import org.apache.webbeans.event.NotificationManager;
import org.apache.webbeans.spi.ContextsService;

public abstract class AbstractContextsService implements ContextsService
{
    protected final WebBeansContext webBeansContext;

    protected boolean supportsConversation;

    protected Boolean fireRequestLifecycleEvents;

    protected AbstractContextsService(WebBeansContext webBeansContext)
    {
        this.webBeansContext = webBeansContext;
        supportsConversation = webBeansContext.getOpenWebBeansConfiguration().supportsConversation();

    }

    @Override
    public void destroy(Object destroyObject)
    {
        //Default no-op
    }

    @Override
    public void endContext(Class<? extends Annotation> scopeType, Object endParameters)
    {
        //Default no-op
    }

    @Override
    public Context getCurrentContext(Class<? extends Annotation> scopeType)
    {
        return null;
    }

    @Override
    public Context getCurrentContext(Class<? extends Annotation> scopeType, boolean createIfNotExists)
    {
        // by default behaves the same
        return getCurrentContext(scopeType);
    }

    @Override
    public void init(Object initializeObject)
    {
        //Default no-op        
    }

    @Override
    public void startContext(Class<? extends Annotation> scopeType, Object startParameter) throws ContextException
    {
        //Default no-op        
    }

    @Override
    public void removeThreadLocals()
    {
        // no ThreadLocals to clean up by default
    }

    @Override
    public void setSupportConversations(boolean supportConversations)
    {
        this.supportsConversation = supportConversations;
    }

    /**
     * Destroy inactive (timed out) conversations.
     */
    public void destroyOutdatedConversations(ConversationContext currentConversationContext)
    {
        ConversationManager conversationManager = null;
        Context sessionContext = getCurrentContext(SessionScoped.class, false);
        if (sessionContext != null && sessionContext.isActive())
        {
            conversationManager = webBeansContext.getConversationManager();
            Set<ConversationContext> conversationContexts = conversationManager.getSessionConversations(sessionContext, false);
            if (conversationContexts != null)
            {
                Iterator<ConversationContext> convIt = conversationContexts.iterator();
                while (convIt.hasNext())
                {
                    ConversationContext conversationContext = convIt.next();

                    ConversationImpl conv = conversationContext.getConversation();
                    if (conversationManager.conversationTimedOut(conv))
                    {
                        conversationManager.destroyConversationContext(conversationContext);
                        convIt.remove();
                    }
                }
            }
        }

        if (currentConversationContext != null)
        {
            currentConversationContext.getConversation().iDontUseItAnymore();
            if (currentConversationContext.getConversation().isTransient())
            {
                conversationManager = conversationManager != null ? conversationManager : webBeansContext.getConversationManager();
                conversationManager.destroyConversationContext(currentConversationContext);
            }
        }
    }

    public boolean isSupportsConversation()
    {
        return supportsConversation;
    }

    protected boolean shouldFireRequestLifecycleEvents()
    {
        if (fireRequestLifecycleEvents == null)
        {
            NotificationManager notificationManager = webBeansContext.getNotificationManager();
            fireRequestLifecycleEvents
                    = notificationManager.hasContextLifecycleObserver(InitializedLiteral.INSTANCE_REQUEST_SCOPED) ||
                    notificationManager.hasContextLifecycleObserver(BeforeDestroyedLiteral.INSTANCE_REQUEST_SCOPED) ||
                    notificationManager.hasContextLifecycleObserver(DestroyedLiteral.INSTANCE_REQUEST_SCOPED) ;
        }

        return fireRequestLifecycleEvents;
    }
}
