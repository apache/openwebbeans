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

import javax.context.ConversationScoped;
import javax.faces.component.UIViewRoot;
import javax.faces.component.html.HtmlInputHidden;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;

import org.apache.webbeans.container.ManagerImpl;
import org.apache.webbeans.context.ContextFactory;
import org.apache.webbeans.context.ConversationContext;
import org.apache.webbeans.conversation.ConversationImpl;
import org.apache.webbeans.conversation.ConversationManager;
import org.apache.webbeans.logger.WebBeansLogger;
import org.apache.webbeans.util.JSFUtil;

public class WebBeansPhaseListener implements PhaseListener
{
    private static final long serialVersionUID = -8131516076829979596L;

    private static WebBeansLogger logger = WebBeansLogger.getLogger(WebBeansPhaseListener.class);

    private static ConversationManager conversationManager = ConversationManager.getInstance();

    private ConversationImpl conversation = null;

    public void afterPhase(PhaseEvent phaseEvent)
    {
        if (phaseEvent.getPhaseId().equals(PhaseId.RESTORE_VIEW))
        {
            if (!JSFUtil.isPostBack())
            {
                String cid = JSFUtil.getExternalContext().getRequestParameterMap().get("cid");

                // non-faces get request
                if (cid == null)
                {
                    logger.info("Create new transitional conversation for non-faces request with view id : " + JSFUtil.getViewId());
                    conversation = (ConversationImpl) conversationManager.createNewConversation();

                    ContextFactory.initConversationContext(null);

                }
                else
                {
                    logger.info("Propogation of the conversation with id : " + cid + " for view : " + JSFUtil.getViewId());
                    conversation = (ConversationImpl) conversationManager.getConversation(cid);

                    // can not restore conversation, create new transitional
                    if (conversation == null)
                    {
                        logger.info("Propogated conversation can not be restored for view id : " + JSFUtil.getViewId() + ". Creates new transitional conversation");
                        conversation = (ConversationImpl) conversationManager.createNewConversation();

                        ContextFactory.initConversationContext(null);
                    }
                    else
                    {
                        ContextFactory.initConversationContext(conversationManager.getConversationContext(conversation));
                    }
                }
            }
            else
            {
                logger.info("Postback JSF Request for view id : " + JSFUtil.getViewId());

                UIViewRoot viewRoot = JSFUtil.getViewRoot();
                HtmlInputHidden conversationId = (HtmlInputHidden) viewRoot.findComponent("javax_webbeans_ConversationId");

                if (conversationId != null)
                {
                    // look long running conversation if exist
                    conversation = (ConversationImpl) conversationManager.getConversation(conversationId.getValue().toString());
                    ContextFactory.initConversationContext(conversationManager.getConversationContext(conversation));

                }

                // no long running conversation, create one transitional
                else
                {
                    logger.info("Create new transient conversation for JSF postback view id : " + JSFUtil.getViewId());
                    conversation = (ConversationImpl) conversationManager.createNewConversation();

                    ContextFactory.initConversationContext(null);
                }
            }
        }

        else if (phaseEvent.getPhaseId().equals(PhaseId.RENDER_RESPONSE))
        {
            ConversationContext context = (ConversationContext) ManagerImpl.getManager().getContext(ConversationScoped.class);

            // if long running, saves it
            if (conversation.isLongRunning())
            {
                logger.info("Conversation with id : " + conversation.getId() + " is marked as long running conversation");
                context.setActive(false);
            }

            // else destroy conversation context
            else
            {
                logger.info("Destroying the conversation context for view id : " + JSFUtil.getViewId());
                context.destroy();                                    
            }

        }

    }

    public void beforePhase(PhaseEvent phaseEvent)
    {
        if (phaseEvent.getPhaseId().equals(PhaseId.APPLY_REQUEST_VALUES))
        {
            ConversationContext context = (ConversationContext) ManagerImpl.getManager().getContext(ConversationScoped.class);

            if (JSFUtil.isPostBack())
            {
                logger.info("Activating the conversation context for view id : " + JSFUtil.getViewId());
                context.setActive(true);

                conversation.updateTimeOut();
            }
        }

        else if (phaseEvent.getPhaseId().equals(PhaseId.RENDER_RESPONSE))
        {
            ConversationContext context = (ConversationContext) ManagerImpl.getManager().getContext(ConversationScoped.class);

            if (!JSFUtil.isPostBack())
            {
                logger.info("Activating the conversation context for view id : " + JSFUtil.getViewId());
                context.setActive(true);

                conversation.updateTimeOut();
            }

            if (conversation.isLongRunning())
            {
                UIViewRoot viewRoot = JSFUtil.getViewRoot();

                HtmlInputHidden hidden = (HtmlInputHidden) viewRoot.findComponent("javax_webbeans_ConversationId");

                if (hidden != null)
                {
                    viewRoot.getChildren().remove(hidden);
                }

                hidden = (HtmlInputHidden) JSFUtil.getApplication().createComponent(HtmlInputHidden.COMPONENT_TYPE);
                hidden.setValue(conversation.getId());
                hidden.setId("javax_webbeans_ConversationId");

                viewRoot.getChildren().add(hidden);
            }
            else
            {
                //Remove the hidden component
                UIViewRoot viewRoot = JSFUtil.getViewRoot();

                HtmlInputHidden hidden = (HtmlInputHidden) viewRoot.findComponent("javax_webbeans_ConversationId");

                if (hidden != null)
                {
                    viewRoot.getChildren().remove(hidden);
                }
                
            }
        }

    }

    public PhaseId getPhaseId()
    {
        return PhaseId.ANY_PHASE;
    }

}
