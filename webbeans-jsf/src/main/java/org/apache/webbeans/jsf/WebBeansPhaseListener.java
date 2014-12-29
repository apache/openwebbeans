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

import javax.enterprise.context.BusyConversationException;
import javax.enterprise.context.Conversation;
import javax.enterprise.context.NonexistentConversationException;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.context.ContextFactory;
import org.apache.webbeans.context.ConversationContext;
import org.apache.webbeans.conversation.ConversationImpl;
import org.apache.webbeans.conversation.ConversationManager;
import org.apache.webbeans.logger.WebBeansLoggerFacade;

import java.util.logging.Level;
import java.util.logging.Logger;

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
    private final static Logger logger = WebBeansLoggerFacade.getLogger(WebBeansPhaseListener.class);

    private Boolean owbApplication = null;

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterPhase(PhaseEvent phaseEvent)
    {
        if (phaseEvent.getPhaseId().equals(PhaseId.RENDER_RESPONSE) ||
                phaseEvent.getFacesContext().getResponseComplete())
        {
            if(!isOwbApplication())
            {
                return;
            }

            WebBeansContext webBeansContext = WebBeansContext.getInstance();
            ConversationManager conversationManager = webBeansContext.getConversationManager();
            Conversation conversation = conversationManager.getConversationBeanReference();

            if (conversation.isTransient())
            {
                if (logger.isLoggable(Level.FINE))
                {
                    logger.log(Level.FINE, "Destroying the conversation context with cid : [{0}]", conversation.getId());
                }
                ContextFactory contextFactory = webBeansContext.getContextFactory();
                contextFactory.destroyConversationContext();
            }
            else
            {
                //Conversation must be used by one thread at a time
                ConversationImpl owbConversation = (ConversationImpl)conversation;
                owbConversation.updateTimeOut();
                //Other threads can now access propogated conversation.
                owbConversation.iDontUseItAnymore();
            }            
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void beforePhase(PhaseEvent phaseEvent)
    {
        if (phaseEvent.getPhaseId().equals(PhaseId.RESTORE_VIEW) && isOwbApplication())
        {
            //It looks for cid parameter in the JSF request.
            //If request contains cid, then it must restore conversation
            //Otherwise create NonexistentException
            WebBeansContext webBeansContext = WebBeansContext.getInstance();
            ConversationManager conversationManager = webBeansContext.getConversationManager();
            Conversation conversation = conversationManager.getConversationBeanReference();
            ContextFactory contextFactory = webBeansContext.getContextFactory();

            String propagation = JSFUtil.getConversationPropagation();
            boolean createNew = "none".equals(propagation);

            if (createNew)
            {
                contextFactory.initConversationContext(getRequest(phaseEvent));
            }
            else if (conversation.isTransient())
            {
                if (logger.isLoggable(Level.FINE))
                {
                    logger.log(Level.FINE, "Creating a new transitional conversation with cid : [{0}]", conversation.getId());
                }
                contextFactory.initConversationContext(getRequest(phaseEvent));

                //Not restore, throw exception
                String cid = JSFUtil.getConversationId();
                if(cid != null && !cid.equals(""))
                {
                    throw new NonexistentConversationException("Propogated conversation with cid=" + cid + " is not restored. It creates a new transient conversation.");
                }
            }
            else
            {
                if (logger.isLoggable(Level.FINE))
                {
                    logger.log(Level.FINE, "Restoring conversation with cid : [{0}]", conversation.getId());
                }

                //Conversation must be used by one thread at a time
                ConversationImpl owbConversation = (ConversationImpl)conversation;
                if(owbConversation.iUseIt() > 1)
                {
                    contextFactory.initConversationContext(getRequest(phaseEvent));
                    //Throw Busy exception
                    throw new BusyConversationException(
                            "Propogated conversation with cid=" + JSFUtil.getConversationId() + " is used by other request. It creates a new transient conversation");
                }
                else
                {
                    ConversationContext conversationContext = conversationManager.getConversationContext(conversation);
                    contextFactory.initConversationContext(conversationContext);
                }
            }
        }
    }

    // depend the lifecycle impl so give the request if we have it
    private Object getRequest(final PhaseEvent phaseEvent)
    {
        final FacesContext facesContext = phaseEvent.getFacesContext();
        if (facesContext != null)
        {
            final ExternalContext externalContext = facesContext.getExternalContext();
            if (externalContext != null)
            {
                return externalContext.getRequest();
            }
        }
        return null;
    }

    @Override
    public PhaseId getPhaseId()
    {
        return PhaseId.ANY_PHASE;
    }

    private boolean isOwbApplication()
    {
        if (owbApplication == null)
        {
            owbApplication = Boolean.valueOf(WebBeansContext.getInstance().getBeanManagerImpl().isInUse());
        }

        return owbApplication.booleanValue();
    }

}
