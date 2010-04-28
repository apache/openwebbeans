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
package org.apache.webbeans.web.context;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.ContextException;
import javax.enterprise.context.ConversationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.context.spi.Context;
import javax.inject.Singleton;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequestEvent;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.webbeans.config.OpenWebBeansConfiguration;
import org.apache.webbeans.context.AbstractContextsService;
import org.apache.webbeans.context.ApplicationContext;
import org.apache.webbeans.context.ConversationContext;
import org.apache.webbeans.context.DependentContext;
import org.apache.webbeans.context.RequestContext;
import org.apache.webbeans.context.SessionContext;
import org.apache.webbeans.context.SingletonContext;
import org.apache.webbeans.conversation.ConversationManager;
import org.apache.webbeans.spi.ContextsService;

/**
 * Web container {@link ContextsService}
 * implementation.
 */
public class WebContextsService extends AbstractContextsService
{
    /**Current request context*/
    private static ThreadLocal<RequestContext> requestContext = null;

    /**Current session context*/
    private static ThreadLocal<SessionContext> sessionContext = null;

    /**Current application context*/
    private static ThreadLocal<ApplicationContext> applicationContext = null;

    /**Current conversation context*/
    private static ThreadLocal<ConversationContext> conversationContext = null;
    
    /**Current singleton context*/
    private static ThreadLocal<SingletonContext> singletonContext = null;

    /**Current dependent context*/
    private static DependentContext dependentContext;

    /**Current application contexts*/
    private static Map<ServletContext, ApplicationContext> currentApplicationContexts = new ConcurrentHashMap<ServletContext, ApplicationContext>();
    
    /**Current singleton contexts*/
    private static Map<ServletContext, SingletonContext> currentSingletonContexts = new ConcurrentHashMap<ServletContext, SingletonContext>();

    /**Session context manager*/
    private static SessionContextManager sessionCtxManager = SessionContextManager.getInstance();

    /**Conversation context manager*/
    private static ConversationManager conversationManager = ConversationManager.getInstance();
    
    private boolean supportsConversation = false;

    /**Initialize thread locals*/
    static
    {
        requestContext = new ThreadLocal<RequestContext>();
        sessionContext = new ThreadLocal<SessionContext>();
        applicationContext = new ThreadLocal<ApplicationContext>();
        conversationContext = new ThreadLocal<ConversationContext>();
        singletonContext = new ThreadLocal<SingletonContext>();
        
        //Dependent context is always active
        dependentContext = new DependentContext();
        dependentContext.setActive(true);

    }
    
    /**
     * Creates a new instance.
     */
    public WebContextsService()
    {
        supportsConversation =  !OpenWebBeansConfiguration.getInstance().isJspApplication();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void init(Object initializeObject)
    {        
        //Start application context
        startContext(ApplicationScoped.class, initializeObject);
        
        //Start signelton context
        startContext(Singleton.class, initializeObject);
    }    
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void destroy(Object destroyObject)
    {
        //Destroy application context
        endContext(ApplicationScoped.class, destroyObject);
        
        //Destroy singleton context
        endContext(Singleton.class, destroyObject);
     
        //Clear saved contexts related with 
        //this servlet context
        currentApplicationContexts.clear();
        currentSingletonContexts.clear();
        
        //Thread local values to null
        requestContext.set(null);
        sessionContext.set(null);
        conversationContext.set(null);
        applicationContext.set(null);
        singletonContext.set(null);
        
        //Remove thread locals
        //for preventing memory leaks
        requestContext.remove();
        sessionContext.remove();
        conversationContext.remove();
        applicationContext.remove();
        singletonContext.remove();        
                
    }    
    
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void endContext(Class<? extends Annotation> scopeType, Object endParameters)
    {        
        if(supportsContext(scopeType))
        {
            if(scopeType.equals(RequestScoped.class))
            {
                destroyRequestContext((ServletRequestEvent)endParameters);
            }
            else if(scopeType.equals(SessionScoped.class))
            {
                destroySessionContext((HttpSession)endParameters);
            }
            else if(scopeType.equals(ApplicationScoped.class))
            {
                destroyApplicationContext((ServletContext)endParameters);
            }
            else if(scopeType.equals(ConversationScoped.class))
            {
                destroyConversationContext();
            }
            else if(scopeType.equals(Dependent.class))
            {
                //Do nothing
            }
            else
            {
                destroySingletonContext((ServletContext)endParameters);
            }
        }
        
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Context getCurrentContext(Class<? extends Annotation> scopeType)
    {
        if(supportsContext(scopeType))
        {
            if(scopeType.equals(RequestScoped.class))
            {
                return getRequestContext();
            }
            else if(scopeType.equals(SessionScoped.class))
            {
                return getSessionContext();
            }
            else if(scopeType.equals(ApplicationScoped.class))
            {
                return getApplicationContext();
            }
            else if(scopeType.equals(ConversationScoped.class))
            {
                return getConversationContext();
            }
            else if(scopeType.equals(Dependent.class))
            {
                return dependentContext;
            }
            else
            {
                return getSingletonContext();
            }
        }
        
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void startContext(Class<? extends Annotation> scopeType, Object startParameter) throws ContextException
    {
        if(supportsContext(scopeType))
        {
            if(scopeType.equals(RequestScoped.class))
            {
                initRequestContext((ServletRequestEvent)startParameter);
            }
            else if(scopeType.equals(SessionScoped.class))
            {
                initSessionContext((HttpSession)startParameter);
            }
            else if(scopeType.equals(ApplicationScoped.class))
            {
                initApplicationContext((ServletContext)startParameter);
            }
            else if(scopeType.equals(ConversationScoped.class))
            {
                initConversationContext((ConversationContext)startParameter);
            }
            else if(scopeType.equals(Dependent.class))
            {
                //Do nothing
            }
            else
            {
                initSingletonContext((ServletContext)startParameter);
            }
        }
        
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean supportsContext(Class<? extends Annotation> scopeType)
    {
        if(scopeType.equals(RequestScoped.class) ||
                scopeType.equals(SessionScoped.class) ||
                scopeType.equals(ApplicationScoped.class) ||
                scopeType.equals(Dependent.class) ||
                scopeType.equals(Singleton.class) ||
                (scopeType.equals(ConversationScoped.class) && supportsConversation))
        {
            return true;
        }
        
        return false;
    }
    
    /**
     * Initialize requext context with the given request object.
     * @param event http servlet request event
     */
    private void initRequestContext(ServletRequestEvent event)
    {
        
        RequestContext rq = new RequestContext();
        rq.setActive(true);

        requestContext.set(rq);// set thread local
        if(event != null)
        {
            HttpServletRequest request = (HttpServletRequest) event.getServletRequest();
            
            if (request != null)
            {
                //Re-initialize thread local for session
                HttpSession session = request.getSession(false);
                
                if(session != null)
                {
                    initSessionContext(session);    
                }
                            
                //Init thread local application context
                initApplicationContext(event.getServletContext());
                
                //Init thread local sigleton context
                initSingletonContext(event.getServletContext());
            }            
        }
    }
    
    /**
     * Destroys the request context and all of its components. 
     * @param request http servlet request object
     */
    private void destroyRequestContext(ServletRequestEvent request)
    {
        //Get context
        RequestContext context = getRequestContext();

        //Destroy context
        if (context != null)
        {
            context.destroy();
        }
        
        //Clear thread locals
        requestContext.set(null);
        requestContext.remove();
        
        //Also clear application and singleton context
        applicationContext.set(null);
        applicationContext.remove();
        
        //Singleton context
        singletonContext.set(null);
        singletonContext.remove();
    }

    /**
     * Creates the session context at the session start. 
     * @param session http session object
     */
    private void initSessionContext(HttpSession session)
    {
        String sessionId = session.getId();
        //Current context
        SessionContext currentSessionContext = sessionCtxManager.getSessionContextWithSessionId(sessionId);
        
        //No current context
        if (currentSessionContext == null)
        {
            currentSessionContext = new SessionContext();
            sessionCtxManager.addNewSessionContext(sessionId, currentSessionContext);
        }

        //Activate
        currentSessionContext.setActive(true);
        
        //Set thread local
        sessionContext.set(currentSessionContext);
    }

    /**
     * Destroys the session context and all of its components at the end of the
     * session. 
     * @param session http session object
     */
    private void destroySessionContext(HttpSession session)
    {
        //Get current session context
        SessionContext context = getSessionContext();
        
        //Destroy context
        if (context != null)
        {
            context.destroy();
        }

        //Clear thread locals
        sessionContext.set(null);
        sessionContext.remove();
        
        //Remove session from manager
        sessionCtxManager.destroySessionContextWithSessionId(session.getId());

    }

    /**
     * Creates the application context at the application startup 
     * @param servletContext servlet context object
     */
    private void initApplicationContext(ServletContext servletContext)
    {
        
        if(servletContext != null && currentApplicationContexts.containsKey(servletContext))
        {
            applicationContext.set(currentApplicationContexts.get(servletContext));
        }
        
        else
        {
            ApplicationContext currentApplicationContext = new ApplicationContext();         
            currentApplicationContext.setActive(true);
            
            if(servletContext != null)
            {
                currentApplicationContexts.put(servletContext, currentApplicationContext);
                
            }
            
            applicationContext.set(currentApplicationContext);
        }
    }

    /**
     * Destroys the application context and all of its components at the end of
     * the application. 
     * @param servletContext servlet context object
     */
    private void destroyApplicationContext(ServletContext servletContext)
    {
        //look for thread local
        //this can be set by initRequestContext
        ApplicationContext context = null;
        
        //Looking the context from saved context
        if(servletContext != null)
        {
            context = currentApplicationContexts.get(servletContext);   
        }
        
        //Destroy context
        if(context != null)
        {
            context.destroy();
        }
        
        //Remove from saved contexts
        if(servletContext != null)
        {
            currentApplicationContexts.remove(servletContext);   
        }
        
        //destroy all sessions
        sessionCtxManager.destroyAllSessions();
        
        //destroy all conversations
        conversationManager.destroyAllConversations();
    }
    
    /**
     * Initialize singleton context.
     * @param servletContext servlet context
     */
    private void initSingletonContext(ServletContext servletContext)
    {
        if(servletContext != null && currentSingletonContexts.containsKey(servletContext))
        {
            singletonContext.set(currentSingletonContexts.get(servletContext));
        }
        
        else
        {
            SingletonContext context = new SingletonContext();
            context.setActive(true);
            
            if(servletContext != null)
            {
                currentSingletonContexts.put(servletContext, context);
                
            }
            
            singletonContext.set(context);   
        }
                        
    }
    
    /**
     * Destroy singleton context.
     * @param servletContext servlet context
     */
    private void destroySingletonContext(ServletContext servletContext)
    {
        SingletonContext context = null;

        //look for saved context
        if(servletContext != null)
        {
            context = currentSingletonContexts.get(servletContext);
        }
        
        //context is not null
        //destroy it
        if(context != null)
        {
            context.destroy();
        }

        //remove it from saved contexts
        if(servletContext != null)
        {
            currentSingletonContexts.remove(servletContext);   
        }                
    }

    /**
     * Initialize conversation context.
     * @param context context
     */
    private void initConversationContext(ConversationContext context)
    {
        if (context == null)
        {
            if(conversationContext.get() == null)
            {
                ConversationContext newContext = new ConversationContext();
                newContext.setActive(true);
                
                conversationContext.set(newContext);                
            }
            else
            {
                conversationContext.get().setActive(true);
            }
            
        }
        else
        {
            context.setActive(true);
            conversationContext.set(context);
        }
    }

    /**
     * Destroy conversation context.
     */
    private void destroyConversationContext()
    {
        ConversationContext context = getConversationContext();

        if (context != null)
        {
            context.destroy();
        }

        conversationContext.set(null);
        conversationContext.remove();
    }

    /**
     * Get current request ctx.
     * @return request context
     */
    private  RequestContext getRequestContext()
    {
        return requestContext.get();
    }

    /**
     * Get current session ctx.
     * @return session context
     */
    private  SessionContext getSessionContext()
    {
        return sessionContext.get();
    }

    /**
     * Gets application context.
     * @return application context
     */
    private  ApplicationContext getApplicationContext()
    {
        return applicationContext.get();
    }

    /**
     * Gets singleton context.
     * @return singleton context
     */
    private  SingletonContext getSingletonContext()
    {
        return singletonContext.get();
    }

    /**
     * Get current conversation ctx.
     * @return conversation context
     */
    private  ConversationContext getConversationContext()
    {
        return conversationContext.get();
    }
}