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
package org.apache.webbeans.web.context;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.spi.ContextsService;
import org.apache.webbeans.spi.ConversationService;

/**
 * Conversation propagation in pure Servlets happens via cid servlet parameter.
 */
public class WebConversationService implements ConversationService
{
    public static final String REQUEST_PARAM_CONVERSATION_ID = "cid";
    public static final String REQUEST_PARAM_SUPPRESS_CONVERSATION_PROPAGATION = "conversationPropagation";
    public static final String SESSION_CONVERSATION_ID_PARAM_NAME = "openWebBeansConversationIdCounter";

    private final WebBeansContext webBeansContext;

    /**
     * For non-servlet requests we simply generate the numbers internally
     */
    private AtomicInteger nonServletRequestConversationIdCounter = new AtomicInteger(0);


    public WebConversationService(WebBeansContext webBeansContext)
    {
        this.webBeansContext = webBeansContext;
    }

    /**
     * Try to get the current conversationId from the servlet.
     */
    @Override
    public String getConversationId()
    {
        ServletRequest servletRequest = getCurrentServletRequest();
        if (servletRequest != null)
        {
            // get the cid parameter from the servlet request
            String cidParamValue = servletRequest.getParameter(REQUEST_PARAM_CONVERSATION_ID);
            if (cidParamValue != null && cidParamValue.length() > 0)
            {
                // also check the suppress conversation propagation parameter
                String suppressConversationPropagation = servletRequest.getParameter(REQUEST_PARAM_SUPPRESS_CONVERSATION_PROPAGATION);
                if (!"none".equals(suppressConversationPropagation))
                {
                    // wohu, we found our cid parameter
                    return cidParamValue;
                }
            }
        }

        // seems we cannot find any cid parameter
        return null;
    }

    @Override
    public String generateConversationId()
    {
        return Long.toString(getConversationIdCounter().incrementAndGet());
    }

    protected AtomicInteger getConversationIdCounter()
    {
        AtomicInteger counter = getSessionConversationIdCounter();
        return counter != null ? counter : nonServletRequestConversationIdCounter;
    }

    /**
     * @return conversationId counter AtomicInteger if this is a real http request, {@code null} otherwise
     */
    protected AtomicInteger getSessionConversationIdCounter()
    {
        ServletRequest servletRequest = getCurrentServletRequest();
        if (servletRequest instanceof HttpServletRequest)
        {
            HttpSession session = ((HttpServletRequest) servletRequest).getSession(true);
            AtomicInteger sessionCounter = (AtomicInteger) session.getAttribute(SESSION_CONVERSATION_ID_PARAM_NAME);
            if (sessionCounter == null)
            {
                synchronized (session)
                {
                    sessionCounter = (AtomicInteger) session.getAttribute(SESSION_CONVERSATION_ID_PARAM_NAME);
                    if (sessionCounter == null)
                    {
                        sessionCounter = new AtomicInteger(0);
                        session.setAttribute(SESSION_CONVERSATION_ID_PARAM_NAME, sessionCounter);
                    }
                }
            }
            return sessionCounter;
        }


        return null;
    }

    /**
     * @return the current ServletRequest or {@code null} if this thread is not attached to a servlet request
     */
    protected ServletRequest getCurrentServletRequest()
    {
        ContextsService contextsService = webBeansContext.getContextsService();
        if (contextsService instanceof WebContextsService)
        {
            return ((WebContextsService) contextsService).getRequestContext(false).getServletRequest();
        }

        return null;
    }
}
