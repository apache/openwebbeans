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
package org.apache.webbeans.jsf;

import java.util.Set;

import javax.enterprise.context.Conversation;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.util.AnnotationLiteral;
import javax.faces.application.ViewHandler;
import javax.faces.application.ViewHandlerWrapper;
import javax.faces.context.FacesContext;

import org.apache.webbeans.container.BeanManagerImpl;
import org.apache.webbeans.util.JSFUtil;

public class ConversationAwareViewHandler extends ViewHandlerWrapper
{
    private final ViewHandler delegate;

    public ConversationAwareViewHandler(ViewHandler delegate)
    {
        this.delegate = delegate;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getActionURL(FacesContext context, String viewId)
    {
        String url = delegate.getActionURL(context, viewId);

        Conversation conversation = lookupConversation();
        if (conversation != null && !conversation.isTransient())
        {
            url = JSFUtil.getRedirectViewIdWithCid(url, conversation.getId());
        }

        return url;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ViewHandler getWrapped()
    {
        return delegate;
    }

    private Conversation lookupConversation()
    {
        BeanManager beanManager = BeanManagerImpl.getManager();
        if (beanManager == null)
        {
            return null;
        }

        Set<Bean<?>> beans = beanManager.getBeans(Conversation.class, new AnnotationLiteral<Default>() {});
        if (beans.isEmpty())
        {
            return null;
        }

        @SuppressWarnings("unchecked")
        Bean<Conversation> conversationBean = (Bean<Conversation>) beans.iterator().next();
        CreationalContext<Conversation> creationalContext = beanManager.createCreationalContext(conversationBean);
        Conversation conversation =
            (Conversation) beanManager.getReference(conversationBean, Conversation.class, creationalContext);

        return conversation;
    }
}
