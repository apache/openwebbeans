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

import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.context.BusyConversationException;
import javax.enterprise.context.Conversation;
import javax.enterprise.context.NonexistentConversationException;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.context.spi.Context;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;

import org.apache.webbeans.annotation.BeforeDestroyedLiteral;
import org.apache.webbeans.annotation.DefaultLiteral;
import org.apache.webbeans.annotation.DestroyedLiteral;
import org.apache.webbeans.config.OWBLogConst;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.container.BeanManagerImpl;
import org.apache.webbeans.context.ConversationContext;
import org.apache.webbeans.context.RequestContext;
import org.apache.webbeans.context.creational.CreationalContextImpl;
import org.apache.webbeans.logger.WebBeansLoggerFacade;
import org.apache.webbeans.spi.ConversationService;
import org.apache.webbeans.util.Asserts;

/**
 * Manager for the conversations.
 * Each conversation is related with conversation id and session id.
 *
 *
 * @version $Rev$ $Date$
 *
 */
public class ConversationManager
{
    private static final Logger logger = WebBeansLoggerFacade.getLogger(ConversationManager.class);


    private final WebBeansContext webBeansContext;
    private final Bean<Set<ConversationContext>> conversationStorageBean;

    /**
     * Creates new conversation manager
     */
    public ConversationManager(WebBeansContext webBeansContext)
    {
        this.webBeansContext = webBeansContext;

        // We cannot use this directly since it will change after passivation
        ConversationStorageBean convBean = new ConversationStorageBean(webBeansContext);

        BeanManagerImpl bm = webBeansContext.getBeanManagerImpl();
        bm.addInternalBean(convBean);

        // this will return the internally wrapped ThirdPartyBean.
        conversationStorageBean = (Bean<Set<ConversationContext>>)
                bm.resolve(bm.getBeans(ConversationStorageBean.OWB_INTERNAL_CONVERSATION_STORAGE_BEAN_PASSIVATION_ID));
    }


    /**
     * This method shall only get called from the ContextsService.
     * It will create a new ConversationContext if there is no long running Conversation already running.
     * @return the ConversationContext which is valid for the whole request.
     */
    public ConversationContext getConversationContext(Context sessionContext)
    {
        ConversationService conversationService = webBeansContext.getConversationService();

        Set<ConversationContext> conversationContexts = getSessionConversations(sessionContext, false);

        RuntimeException problem = null;
        String conversationId = conversationService.getConversationId();
        if (conversationId != null && conversationId.length() > 0)
        {
            if (conversationContexts != null)
            {
                for (ConversationContext conversationContext : conversationContexts)
                {
                    if (conversationId.equals(conversationContext.getConversation().getId()))
                    {
                        if (conversationContext.getConversation().iUseIt() > 1)
                        {
                            problem =  new BusyConversationException("Propogated conversation with cid=" +
                                    conversationContext.getConversation().getId() +
                                    " is used by other request. It creates a new transient conversation");
                            conversationContext.getConversation().setProblemDuringCreation(problem);
                        }
                        else
                        {
                            conversationContext.getConversation().updateLastAccessTime();
                        }

                        conversationContext.setActive(true);
                        return conversationContext;
                    }
                }
            }

            problem = new NonexistentConversationException("Propogated conversation with cid=" + conversationId +
                " cannot be restored. Will create a new transient conversation.");
        }

        ConversationContext conversationContext = new ConversationContext(webBeansContext);
        conversationContext.setActive(true);
        conversationContext.getConversation().setProblemDuringCreation(problem);

        return conversationContext;
    }

    /**
     * Add the given ConversationContext to the SessionContext.
     * This method usually will get called at {@link Conversation#begin()}.
     */
    public void addToConversationStorage(ConversationContext conversationContext, String conversationId)
    {
        Asserts.assertNotNull(conversationId, "conversationId");
        Context sessionContext = webBeansContext.getContextsService().getCurrentContext(SessionScoped.class, true);
        Set<ConversationContext> sessionConversations = getSessionConversations(sessionContext, true);

        // check whether this conversation already exists
        for (ConversationContext sessionConversation : sessionConversations)
        {
            if (conversationId.equals(sessionConversation.getConversation().getId()))
            {
                throw new IllegalArgumentException("Conversation with id=" + conversationId + " already exists!");
            }
        }

        // if not, then simply add this conversation
        sessionConversations.add(conversationContext);
    }

    /**
     * Remove the given ConversationContext from the SessionContext storage.
     * This method usually will get called at {@link Conversation#end()} or during cleanup.
     * Not that this does <b>not</b> destroy the ConversationContext!
     * @return {@code true} if the conversationContext got removed
     */
    public boolean removeConversationFromStorage(ConversationContext conversationContext)
    {
        Context sessionContext = webBeansContext.getContextsService().getCurrentContext(SessionScoped.class);
        if (sessionContext != null)
        {
            Set<ConversationContext> sessionConversations = getSessionConversations(sessionContext, true);
            return sessionConversations.remove(conversationContext);
        }

        return false;
    }


    /**
     * Gets conversation instance from conversation bean.
     * @return conversation instance
     */
    public Conversation getConversationBeanReference()
    {
        BeanManager beanManager = webBeansContext.getBeanManagerImpl();
        Bean<Conversation> bean = (Bean<Conversation>)beanManager.getBeans(Conversation.class, DefaultLiteral.INSTANCE).iterator().next();
        Conversation conversation =(Conversation) beanManager.getReference(bean, Conversation.class, beanManager.createCreationalContext(bean));

        return conversation;
    }


    public boolean conversationTimedOut(ConversationImpl conv)
    {
        long timeout = 0L;
        try
        {
            timeout = conv.getTimeout();
        }
        catch (BusyConversationException bce)
        {
            // if the Conversation is concurrently used by another thread then it is surely not timed out
            return false;
        }

        if (timeout != 0L && (System.currentTimeMillis() - conv.getLastAccessTime()) > timeout)
        {
            logger.log(Level.FINE, OWBLogConst.INFO_0011, conv.getId());
            return true;
        }

        return false;
    }

    /**
     * Destroy the given ConversationContext and fire the proper
     * &#064;Destroyed event with the correct payload.
     */
    public void destroyConversationContext(ConversationContext ctx)
    {
        webBeansContext.getBeanManagerImpl().fireEvent(
                getLifecycleEventPayload(ctx), BeforeDestroyedLiteral.INSTANCE_CONVERSATION_SCOPED);

        ctx.destroy();

        webBeansContext.getBeanManagerImpl().fireEvent(
            getLifecycleEventPayload(ctx), DestroyedLiteral.INSTANCE_CONVERSATION_SCOPED);
    }

    public Object getLifecycleEventPayload(ConversationContext ctx)
    {
        Object payLoad = null;
        if (ctx.getConversation().getId() != null)
        {
            payLoad = ctx.getConversation().getId();
        }

        if (payLoad == null)
        {
            RequestContext requestContext
                = (RequestContext) webBeansContext.getContextsService().getCurrentContext(RequestScoped.class, false);
            if (requestContext != null)
            {
                payLoad = requestContext.getRequestObject();
            }
        }

        if (payLoad == null)
        {
            payLoad = new Object();
        }
        return payLoad;
    }


    /**
     * @param create whether a session and the map in there shall get created or not
     * @return the conversation Map from the current session
     */
    public Set<ConversationContext> getSessionConversations(Context sessionContext, boolean create)
    {
        Set<ConversationContext> conversationContexts = null;
        if (sessionContext != null)
        {
            if (!create)
            {
                conversationContexts = sessionContext.get(conversationStorageBean);
            }
            else
            {
                CreationalContextImpl<Set<ConversationContext>> creationalContext
                        = webBeansContext.getBeanManagerImpl().createCreationalContext(conversationStorageBean);

                conversationContexts = sessionContext.get(conversationStorageBean, creationalContext);
            }
        }

        return conversationContexts;
    }

}
