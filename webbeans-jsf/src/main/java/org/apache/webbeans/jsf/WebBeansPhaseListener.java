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

import javax.enterprise.context.BusyConversationException;
import javax.enterprise.context.Conversation;
import javax.enterprise.context.NonexistentConversationException;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;
import javax.servlet.http.HttpServletRequest;

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
    private static final long serialVersionUID = 1L;

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
        	Conversation conversation = conversationManager.getConversationBeanReference();
        	
            if (conversation.isTransient())
            {
                logger.info(OWBLogConst.INFO_0041, new Object[]{conversation.getId(), JSFUtil.getViewId()});
                ContextFactory.destroyConversationContext();                                                    
            }
            else
            {
                //Conversation must be used by one thread at a time
                ConversationImpl owbConversation = (ConversationImpl)conversation;
                owbConversation.updateTimeOut();
                //Other threads can now access propogated conversation.
                owbConversation.setInUsed(false);                
            }
            
            HttpServletRequest request = (HttpServletRequest)phaseEvent.getFacesContext().getExternalContext().getRequest();
            if(request.getMethod().equals("POST"))
            {
                JSFUtil.getSession().removeAttribute("POST_CONVERSATION");
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void beforePhase(PhaseEvent phaseEvent)
    {
        HttpServletRequest request = (HttpServletRequest)phaseEvent.getFacesContext().getExternalContext().getRequest();
        
        if (phaseEvent.getPhaseId().equals(PhaseId.RESTORE_VIEW))
        {
            if(request.getMethod().equals("POST"))
            {
                JSFUtil.getSession().setAttribute("POST_CONVERSATION", true);
            }
            
            //It looks for cid parameter in the JSF request.
            //If request contains cid, then it must restore conversation
            //Otherwise create NonexistentException
        	Conversation conversation = conversationManager.getConversationBeanReference();
        	String cid = JSFUtil.getConversationId();
        	
			if (conversation.isTransient())
			{
				logger.info(OWBLogConst.INFO_0043, new Object[]{conversation.getId(), JSFUtil.getViewId()});
				ContextFactory.initConversationContext(null);
				
	            //Not restore, throw exception
				if(cid != null && !cid.equals(""))
				{
				    throw new NonexistentConversationException("Propogated conversation with cid=" + cid + " is not restored. It creates a new transient conversation.");
				}
			}
			else
			{
				logger.info(OWBLogConst.INFO_0042, new Object[]{conversation.getId(), JSFUtil.getViewId()});
				
				//Conversation must be used by one thread at a time
				ConversationImpl owbConversation = (ConversationImpl)conversation;
				if(!owbConversation.getInUsed().compareAndSet(false, true))
				{
				    if(request.getMethod().equals("GET"))
				    {
				        //POST-Redirect-GET
				        if(JSFUtil.getSession().getAttribute("POST_CONVERSATION") != null)
				        {
			                   ConversationContext conversationContext = conversationManager.getConversationContext(conversation);
			                   ContextFactory.initConversationContext(conversationContext);
			                   
			                   JSFUtil.getSession().removeAttribute("POST_CONVERSATION");
			                   
			                   return;
				        }
				    }
				    
				    ContextFactory.initConversationContext(null);
				    //Throw Busy exception
				    throw new BusyConversationException("Propogated conversation with cid=" + cid + " is used by other request. It creates a new transient conversation");
				}
				else
				{
	               ConversationContext conversationContext = conversationManager.getConversationContext(conversation);
	               ContextFactory.initConversationContext(conversationContext);
				}				
			}
        }
    }

    public PhaseId getPhaseId()
    {
        return PhaseId.ANY_PHASE;
    }
}
