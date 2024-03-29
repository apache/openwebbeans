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
package org.apache.webbeans.jsf;

import java.util.List;
import java.util.Map;
import jakarta.enterprise.context.ContextNotActiveException;

import jakarta.enterprise.context.Conversation;
import jakarta.faces.application.ViewHandler;
import jakarta.faces.application.ViewHandlerWrapper;
import jakarta.faces.context.FacesContext;

import org.apache.webbeans.config.WebBeansContext;

public class ConversationAwareViewHandler extends ViewHandlerWrapper
{
    private final ViewHandler delegate;

    private Boolean owbApplication;

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
        if(!isOwbApplication())
        {
            return delegate.getActionURL(context, viewId);
        }
        
        String url = delegate.getActionURL(context, viewId);

        try
        {
            Object session = context.getExternalContext().getSession(false);
            if (session != null)
            {
                WebBeansContext webBeansContext = WebBeansContext.currentInstance();
                Conversation conversation = webBeansContext.getConversationManager().getConversationBeanReference();
                if (conversation != null && !conversation.isTransient())
                {
                    url = JSFUtil.getRedirectViewIdWithCid(url, conversation.getId());
                }
            }
        }
        catch (ContextNotActiveException e)
        {
            // ignore
        }

        return url;
    }

    private boolean isOwbApplication()
    {
        if (owbApplication == null)
        {
            owbApplication = WebBeansContext.getInstance().getBeanManagerImpl().isInUse();
        }

        return owbApplication;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getRedirectURL(FacesContext context, String viewId,
            Map<String, List<String>> parameters, boolean includeViewParams)
    {
        
        if(!isOwbApplication())
        {
            return delegate.getRedirectURL(context, viewId, parameters, includeViewParams);
        }
        
        String url = delegate.getRedirectURL(context, viewId, parameters, includeViewParams);
        int indexOfQuery = url.indexOf('?');
        if (indexOfQuery > 0) 
        {
            String queryString = url.substring(indexOfQuery);
            // If the query string already has a cid parameter, return url directly.
            if (queryString.contains("?cid=") || queryString.contains("&cid="))
            {
                return url;
            }
        }
        
        try
        {
            Object session = context.getExternalContext().getSession(false);
            if (session != null)
            {
                WebBeansContext webBeansContext = WebBeansContext.currentInstance();
                Conversation conversation = webBeansContext.getConversationManager().getConversationBeanReference();
                if (conversation != null && !conversation.isTransient())
                {
                    url = JSFUtil.getRedirectViewIdWithCid(url, conversation.getId());
                }
            }
        }
        catch (ContextNotActiveException e)
        {
            // ignore
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
}
