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
    private static ThreadLocal<DependentContext> dependentContext = null;

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
        dependentContext = new ThreadLocal<DependentContext>();
        singletonContext = new ThreadLocal<SingletonContext>();
    }

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
        initializeThreadLocals();
        startContext(ApplicationScoped.class, initializeObject);
        startContext(Singleton.class, initializeObject);
    }    
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void destroy(Object destroyObject)
    {
        endContext(ApplicationScoped.class, destroyObject);
        endContext(Singleton.class, destroyObject);
        
        requestContext.remove();
        requestContext = null;        
        dependentContext.remove();
        dependentContext = null;
        sessionContext.remove();
        sessionContext = null;
        conversationContext.remove();
        conversationContext = null;
        applicationContext.remove();
        applicationContext = null;
        singletonContext.remove();
        singletonContext = null;
        
        currentApplicationContexts.clear();
        currentApplicationContexts = null;
        currentSingletonContexts.clear();
        currentSingletonContexts = null;
        sessionCtxManager = null;
        conversationManager = null;
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
                return getDependentContext();
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
     * Initialize thread locals.
     */
    private void initializeThreadLocals()
    {
        requestContext.remove();
        sessionContext.remove();
        applicationContext.remove();
        conversationContext.remove();
        dependentContext.remove();
        singletonContext.remove();
    }

    /**
     * Initialize requext context with the given request object.
     * @param event http servlet request event
     */
    private void initRequestContext(ServletRequestEvent event)
    {
        initializeThreadLocals();
        
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
                            
                initApplicationContext(event.getServletContext());                
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
        if (requestContext != null)
        {
            RequestContext context = getRequestContext();

            if (context != null)
            {
                context.destroy();
            }

            requestContext.remove();
            
        }
    }

    /**
     * Creates the session context at the session start. 
     * @param session http session object
     */
    private void initSessionContext(HttpSession session)
    {
        String sessionId = session.getId();
        SessionContext currentSessionContext = sessionCtxManager.getSessionContextWithSessionId(sessionId);

        if (currentSessionContext == null)
        {
            currentSessionContext = new SessionContext();
            sessionCtxManager.addNewSessionContext(sessionId, currentSessionContext);
        }

        currentSessionContext.setActive(true);

        sessionContext.set(currentSessionContext);
    }

    /**
     * Destroys the session context and all of its components at the end of the
     * session. 
     * @param session http session object
     */
    private void destroySessionContext(HttpSession session)
    {
        if (sessionContext != null)
        {
            SessionContext context = getSessionContext();

            if (context != null)
            {
                context.destroy();
            }

            sessionContext.remove();

        }

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
        if (applicationContext != null)
        {
            ApplicationContext context = getApplicationContext();

            if (context != null)
            {
                context.destroy();
            }

            applicationContext.remove();

        }
        
        if(servletContext != null)
        {
            currentApplicationContexts.remove(servletContext);   
        }
        
        sessionCtxManager.destroyAllSessions();
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
        if (singletonContext != null)
        {
            SingletonContext context = getSingletonContext();

            if (context != null)
            {
                context.destroy();
            }
            
            singletonContext.remove();            

        }
        
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
        if (conversationContext != null)
        {
            ConversationContext context = getConversationContext();

            if (context != null)
            {
                context.destroy();
            }

            conversationContext.remove();
        }
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

    /**
     * Gets dependent context.
     * @return dependent context
     */
    private DependentContext getDependentContext()
    {
        DependentContext dependentCtx = dependentContext.get();

        if (dependentCtx == null)
        {
            dependentCtx = new DependentContext();
            dependentContext.set(dependentCtx);
        }

        return dependentCtx;
    }
    
}