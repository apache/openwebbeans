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
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;

import org.apache.webbeans.config.OWBLogConst;
import org.apache.webbeans.context.ContextFactory;
import org.apache.webbeans.context.ConversationContext;
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
    
    /**
     * {@inheritDoc}
     */
    public void afterPhase(PhaseEvent phaseEvent)
    {
        if (phaseEvent.getPhaseId().equals(PhaseId.RENDER_RESPONSE))
        {
        	Conversation conversation = conversationManager.getConversationInstance();
        	
            if (conversation.isTransient())
            {
                logger.info(OWBLogConst.INFO_0041, new Object[]{conversation.getId(), JSFUtil.getViewId()});
                ContextFactory.destroyConversationContext();                                                    
            }
            else
            {
            	((ConversationImpl) conversation).updateTimeOut();
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
        	Conversation conversation = conversationManager.getConversationInstance();

			if (conversation.isTransient())
			{
				logger.info(OWBLogConst.INFO_0043, new Object[]{conversation.getId(), JSFUtil.getViewId()});
				ContextFactory.initConversationContext(null);
			}
			else
			{
				logger.info(OWBLogConst.INFO_0042, new Object[]{conversation.getId(), JSFUtil.getViewId()});
				ConversationContext conversationContext = conversationManager.getConversationContext(conversation);
				ContextFactory.initConversationContext(conversationContext);
			}
        }
    }

    public PhaseId getPhaseId()
    {
        return PhaseId.ANY_PHASE;
    }
}
