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
package org.apache.webbeans.web.failover;

import java.io.Serializable;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.context.Conversation;
import javax.enterprise.context.SessionScoped;
import javax.servlet.http.HttpSession;

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.context.ConversationContext;
import org.apache.webbeans.context.SessionContext;
import org.apache.webbeans.conversation.ConversationManager;
import org.apache.webbeans.logger.WebBeansLoggerFacade;
import org.apache.webbeans.spi.FailOverService;
import org.apache.webbeans.web.context.SessionContextManager;
import org.apache.webbeans.web.context.WebContextsService;

/**
 * 
 * The bag that collects all conversation, session owb bean instances.
 * 
 */
public class FailOverBag implements Serializable 
{
    /**
     * 
     */
    private static final long serialVersionUID = -6314819837009653189L;
    
    /**Logger instance*/
    protected static final Logger logger =
            WebBeansLoggerFacade.getLogger(FailOverBag.class);
    
    private String sessionId;

    private String owbFailoverJVMId;
    
    private SessionContext sessionContext;
    
    private Map<Conversation, ConversationContext> conversationContextMap;

    private transient WebBeansContext webBeansContext;
    
    public FailOverBag()
    {
        webBeansContext = WebBeansContext.getInstance();
    }
    
    public FailOverBag(HttpSession session, FailOverService service) 
    {
        webBeansContext = WebBeansContext.getInstance();
        
        sessionId = session.getId();
        owbFailoverJVMId = service.getJVMId();
        updateOwbFailOverBag(session, service);
    }
    
    public void updateOwbFailOverBag(HttpSession session, FailOverService service) 
    {
        // get the session context
        sessionContext = (SessionContext) webBeansContext.getBeanManagerImpl().getContext(SessionScoped.class);

        // get all conversation contexts 
        ConversationManager conversationManager = webBeansContext.getConversationManager();
        conversationContextMap = conversationManager.getConversationMapWithSessionId(session.getId());
    }
    
    public void restore()
    {
        try 
        {
            //Transient, so we need to look this up again during restore.
            webBeansContext = WebBeansContext.getInstance();
            
            if (sessionContext != null) 
            {
                SessionContextManager sessionManager = ((WebContextsService)webBeansContext.getContextsService()).getSessionContextManager();
                sessionManager.addNewSessionContext(sessionId, sessionContext);
                sessionContext.setActive(true);
            }
            if (conversationContextMap != null && !conversationContextMap.isEmpty())
            {
                ConversationManager conversationManager = webBeansContext.getConversationManager();
                java.util.Iterator<Conversation> it = conversationContextMap.keySet().iterator();
                while(it.hasNext()) 
                {
                    Conversation c = it.next();
                    ConversationContext cc = conversationContextMap.get(c);
                    conversationManager.addConversationContext(c, cc);
                }
            }
        } 
        catch (Exception e)
        {
            logger.log(Level.SEVERE, "FailOverBag", e);
        }
    }

    public String getSessionId() 
    {
        return this.sessionId;
    }
    
    public String getJVMId() 
    {
        return this.owbFailoverJVMId;
    }

}
