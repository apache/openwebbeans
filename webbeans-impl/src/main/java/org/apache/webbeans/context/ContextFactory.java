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
package org.apache.webbeans.context;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.context.ApplicationScoped;
import javax.context.Context;
import javax.context.ContextNotActiveException;
import javax.context.ConversationScoped;
import javax.context.Dependent;
import javax.context.RequestScoped;
import javax.context.SessionScoped;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequestEvent;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.webbeans.context.type.ContextTypes;
import org.apache.webbeans.conversation.ConversationManager;

/**
 * Gets current {@link WebBeansContext} instances on the current thread context.
 * 
 * @see WebBeansContext
 * @see AbstractContext
 */
public final class ContextFactory
{
    private static ThreadLocal<RequestContext> requestContext = null;

    private static ThreadLocal<SessionContext> sessionContext = null;

    private static ThreadLocal<ApplicationContext> applicationContext = null;

    private static ThreadLocal<ConversationContext> conversationContext = null;

    private static ThreadLocal<DependentContext> dependentContext = null;

    private static Map<ServletContext, ApplicationContext> currentApplicationContexts = new ConcurrentHashMap<ServletContext, ApplicationContext>();

    private static SessionContextManager sessionCtxManager = SessionContextManager.getInstance();

    private static ConversationManager conversationManager = ConversationManager.getInstance();

    static
    {
        requestContext = new ThreadLocal<RequestContext>();
        sessionContext = new ThreadLocal<SessionContext>();
        applicationContext = new ThreadLocal<ApplicationContext>();
        conversationContext = new ThreadLocal<ConversationContext>();
        dependentContext = new ThreadLocal<DependentContext>();
    }

    private ContextFactory()
    {
        throw new UnsupportedOperationException();
    }

    /**
     * Initialize requext context with the given request object.
     * 
     * @param request http servlet request object
     */
    public static void initRequestContext(ServletRequestEvent event)
    {
        requestContext.set(new RequestContext());// set thread local
        requestContext.get().setActive(true);
        
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
                            
                //Re-initialize thread local for application
                initApplicationContext(event.getServletContext());
            }            
        }
    }

    public static Context getCustomContext(Context context)
    {
        return new CustomContextImpl(context);
    }
    
    /**
     * Destroys the request context and all of its components.
     * 
     * @param request http servlet request object
     */
    public static void destroyRequestContext(HttpServletRequest request)
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
     * 
     * @param session http session object
     */
    public static void initSessionContext(HttpSession session)
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
     * 
     * @param session http session object
     */
    public static void destroySessionContext(HttpSession session)
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
     * 
     * @param servletContext servlet context object
     */
    public static void initApplicationContext(ServletContext servletContext)
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
     * 
     * @param servletContext servlet context object
     */
    public static void destroyApplicationContext(ServletContext servletContext)
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

    public static void initConversationContext(ConversationContext context)
    {
        if (context == null)
        {
            ConversationContext newContext = new ConversationContext();
            newContext.setActive(true);
            
            conversationContext.set(newContext);
        }
        else
        {
            context.setActive(true);
            conversationContext.set(context);
        }
    }

    public static void destroyConversationContext()
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
     * Gets the current context with given type.
     * 
     * @return the current context
     * @throws ContextNotActiveException if context is not active
     * @throws IllegalArgumentException if the type is not a standard context
     */
    public static WebBeansContext getStandartContext(ContextTypes type) throws ContextNotActiveException
    {
        WebBeansContext context = null;

        switch (type.getName())
        {
            case 0:
                context = getRequestContext();
                break;
    
            case 1:
                context = getSessionContext();
                break;
    
            case 2:
                context = getApplicationContext();
                break;
    
            case 3:
                context = getConversationContext();
                break;
                
            case 4:
                context = getDependentContext();
                break;
            
            default:
                throw new IllegalArgumentException("There is no such a standard context with name id=" + type.getName());
        }

        return context;
    }

    /**
     * Gets the standard context with given scope type.
     * 
     * @return the current context, or <code>null</code> if no standard context exists for the given scopeType
     */
    public static WebBeansContext getStandardContext(Class<? extends Annotation> scopeType)
    {
        WebBeansContext context = null;

        if (scopeType.equals(RequestScoped.class))
        {
            context = getRequestContext();
        }
        else if (scopeType.equals(SessionScoped.class))
        {
            context = getSessionContext();
        }
        else if (scopeType.equals(ApplicationScoped.class))
        {
            context = getApplicationContext();
        }
        else if (scopeType.equals(ConversationScoped.class))
        {
            context = getConversationContext();

        }
        else if (scopeType.equals(Dependent.class))
        {
            context = getDependentContext();
        }
        
        return context;
    }

    /*
     * Get current request ctx.
     */
    private static RequestContext getRequestContext()
    {
        return requestContext.get();
    }

    /*
     * Get current session ctx.
     */
    private static SessionContext getSessionContext()
    {
        return sessionContext.get();
    }

     private static ApplicationContext getApplicationContext()
    {
        return applicationContext.get();
    }

    /*
     * Get current conversation ctx.
     */
    private static ConversationContext getConversationContext()
    {
        return conversationContext.get();
    }

    public static DependentContext getDependentContext()
    {
        DependentContext dependentCtx = dependentContext.get();

        if (dependentCtx == null)
        {
            dependentCtx = new DependentContext();
            dependentContext.set(dependentCtx);
        }

        return dependentCtx;
    }
     
    public static boolean checkDependentContextActive()
    {
        return getDependentContext().isActive();
    }

    public static void activateDependentContext()
    {
        getDependentContext().setActive(true);
    }
    
    public static void passivateDependentContext()
    {
        getDependentContext().setActive(false);
    }
    
}