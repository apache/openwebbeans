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

import javax.enterprise.context.Conversation;
import javax.faces.component.UIViewRoot;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;

import org.apache.webbeans.config.OWBLogConst;
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
    private static final ThreadLocal<ConversationImpl> conversation = new ThreadLocal<ConversationImpl>();
        
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
                    logger.info(OWBLogConst.INFO_0034, new Object[]{JSFUtil.getViewId()});
                    
                    setConversation((ConversationImpl) conversationManager.createNewConversationInstance());                    

                    ContextFactory.initConversationContext(null);

                }
                else
                {
                    logger.info(OWBLogConst.INFO_0035, new Object[]{cid, JSFUtil.getViewId()});
                    
                    setConversation((ConversationImpl) conversationManager.getConversation(cid, JSFUtil.getSession().getId()));

                    // can not restore conversation, create new transitional
                    if (getConversation() == null)
                    {
                        logger.info(OWBLogConst.INFO_0036, new Object[]{JSFUtil.getViewId()});
                        setConversation((ConversationImpl) conversationManager.createNewConversationInstance());

                        ContextFactory.initConversationContext(null);                        
                    }
                    else
                    {
                        logger.info(OWBLogConst.INFO_0038, new Object[]{cid, JSFUtil.getViewId()});
                        
                        ContextFactory.initConversationContext(conversationManager.getConversationContext(getConversation()));
                    }
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
                    setConversation((ConversationImpl) conversationManager.getConversation(conversationId, JSFUtil.getSession().getId()));
                    
                    if(getConversation() != null)
                    {
                        logger.info(OWBLogConst.INFO_0039, new Object[]{conversationId, JSFUtil.getViewId()});
                        
                        ContextFactory.initConversationContext(conversationManager.getConversationContext(getConversation()));   
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
                    logger.info(OWBLogConst.INFO_0040, new Object[]{JSFUtil.getViewId()});
                    
                    setConversation((ConversationImpl) conversationManager.createNewConversationInstance());

                    ContextFactory.initConversationContext(null);
                }
            }
        }

        else if (phaseEvent.getPhaseId().equals(PhaseId.RENDER_RESPONSE))
        {
            if (getConversation().isTransient())
            {
                logger.info(OWBLogConst.INFO_0041, new Object[]{getConversation().getId(), JSFUtil.getViewId()});
                
                getConversation().end();
                
                ContextFactory.destroyConversationContext();                                                    
            }
            else
            {
                getConversation().updateTimeOut();
            }
            
            conversation.remove();
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
            if (!getConversation().isTransient())
            {
                logger.info(OWBLogConst.INFO_0042, new Object[]{getConversation().getId(), JSFUtil.getViewId()});
                
                UIViewRoot viewRoot = JSFUtil.getViewRoot();
                
                viewRoot.getAttributes().put(CONVERSATION_ATTR_ID, getConversation().getId());
                
                JSFUtil.getExternalContext().getRequestMap().put("cid",getConversation().getId());
            }
            else
            {
                UIViewRoot viewRoot = JSFUtil.getViewRoot();
                
                viewRoot.getAttributes().remove(CONVERSATION_ATTR_ID);                
            }
        }

    }
    
    public ConversationImpl getConversation()
    {
        return conversation.get();
    }
    
    public void setConversation(ConversationImpl conversationImpl)
    {
        conversation.set(conversationImpl);
    }

    public PhaseId getPhaseId()
    {
        return PhaseId.ANY_PHASE;
    }

}
