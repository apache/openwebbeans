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

import javax.faces.component.UIViewRoot;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;

import org.apache.webbeans.context.ContextFactory;
import org.apache.webbeans.conversation.ConversationImpl;
import org.apache.webbeans.conversation.ConversationManager;
import org.apache.webbeans.logger.WebBeansLogger;
import org.apache.webbeans.util.JSFUtil;

/**
 * Conversation related phase listener.
 * 
 * @version $Rev$ $Date$
 *
 */
public class WebBeansPhaseListener implements PhaseListener
{
    /**Logger instance*/
    private static final WebBeansLogger logger = WebBeansLogger.getLogger(WebBeansPhaseListener.class);

    /**Conversation manager*/
    private static final ConversationManager conversationManager = ConversationManager.getInstance();
    
    /**Attribute id that conversation id is saved under*/
    public static final String CONVERSATION_ATTR_ID = "javax_webbeans_ConversationId";

    /**current conversation if exist*/
    private ConversationImpl conversation = null;
    
    public static ThreadLocal<Boolean> fromRedirect = new ThreadLocal<Boolean>();

    static
    {
        fromRedirect.set(Boolean.FALSE);
    }
    
    /**
     * {@inheritDoc}
     */
    public void afterPhase(PhaseEvent phaseEvent)
    {
        if (phaseEvent.getPhaseId().equals(PhaseId.RESTORE_VIEW))
        {
            //Get request
            if (!JSFUtil.isPostBack())
            {
                String cid = JSFUtil.getExternalContext().getRequestParameterMap().get("cid");

                if (cid == null || cid.equals(""))
                {
                    logger.info("Create new transitional conversation for non-faces request with view id : " + JSFUtil.getViewId());
                    
                    conversation = (ConversationImpl) conversationManager.createNewConversationInstance();                    

                    ContextFactory.initConversationContext(null);

                }
                else
                {
                    logger.info("Propogation of the conversation for non-faces request with cid=" + cid + " for view : " + JSFUtil.getViewId());
                    
                    conversation = (ConversationImpl) conversationManager.getConversation(cid, JSFUtil.getSession().getId());

                    // can not restore conversation, create new transitional
                    if (conversation == null)
                    {
                        logger.info("Propogated conversation for non-faces request can not be restored for view id : " + JSFUtil.getViewId() + ". Creates new transitional conversation");
                        conversation = (ConversationImpl) conversationManager.createNewConversationInstance();

                        ContextFactory.initConversationContext(null);                        
                    }
                    else
                    {
                        logger.info("Conversation is restored for non-faces request with cid=" + cid + " for view id : " + JSFUtil.getViewId());
                        
                        ContextFactory.initConversationContext(conversationManager.getConversationContext(conversation));
                    }
                }
                
                if(fromRedirect.get() != null && fromRedirect.get() && conversation.getId() != null)
                {
                    this.conversation.setTransient(false);  
                }                
            }
            else
            {
                UIViewRoot viewRoot = JSFUtil.getViewRoot();
                
                Object attr = viewRoot.getAttributes().get(CONVERSATION_ATTR_ID);

                String conversationId = null;
                
                if(attr != null)
                {
                    conversationId = attr.toString();
                }
                
                boolean createNew = false;
                
                if (conversationId != null)
                {
                    // look long running conversation if exist
                    conversation = (ConversationImpl) conversationManager.getConversation(conversationId, JSFUtil.getSession().getId());
                    
                    if(conversation != null)
                    {
                        logger.info("Conversation is restored for JSF postback with cid=" + conversationId + " for view id : " + JSFUtil.getViewId());
                        
                        ContextFactory.initConversationContext(conversationManager.getConversationContext(conversation));   
                    }
                    else
                    {
                        createNew = true;
                    }

                }
                else
                {
                    createNew = true;
                }

                if(createNew)
                {
                    logger.info("Create new transient conversation for JSF postback view id : " + JSFUtil.getViewId());
                    
                    conversation = (ConversationImpl) conversationManager.createNewConversationInstance();

                    ContextFactory.initConversationContext(null);
                }
            }
        }

        else if (phaseEvent.getPhaseId().equals(PhaseId.RENDER_RESPONSE))
        {
            if (conversation.isTransient())
            {
                logger.info("Destroying the conversation context with cid="+ conversation.getId() + " for view id : " + JSFUtil.getViewId());
                
                this.conversation.end();
                
                ContextFactory.destroyConversationContext();                                                    
            }
            else
            {
                conversation.updateTimeOut();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void beforePhase(PhaseEvent phaseEvent)
    {
        if (phaseEvent.getPhaseId().equals(PhaseId.RESTORE_VIEW))
        {            
            ContextFactory.initConversationContext(null);            
        }
        
        else if (phaseEvent.getPhaseId().equals(PhaseId.RENDER_RESPONSE))
        {
            if (!conversation.isTransient())
            {
                logger.info("Saving conversation with cid=" + this.conversation.getId() + " for view " + JSFUtil.getViewId());
                
                UIViewRoot viewRoot = JSFUtil.getViewRoot();
                
                viewRoot.getAttributes().put(CONVERSATION_ATTR_ID, conversation.getId());
                
                JSFUtil.getExternalContext().getRequestMap().put("cid",conversation.getId());
            }
            else
            {
                UIViewRoot viewRoot = JSFUtil.getViewRoot();
                
                viewRoot.getAttributes().remove(CONVERSATION_ATTR_ID);                
            }
        }

    }

    public PhaseId getPhaseId()
    {
        return PhaseId.ANY_PHASE;
    }

}
