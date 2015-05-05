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
package org.apache.webbeans.web.context;

import org.apache.webbeans.annotation.DestroyedLiteral;
import org.apache.webbeans.annotation.InitializedLiteral;
import org.apache.webbeans.config.OWBLogConst;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.context.AbstractContextsService;
import org.apache.webbeans.context.ApplicationContext;
import org.apache.webbeans.context.ConversationContext;
import org.apache.webbeans.context.DependentContext;
import org.apache.webbeans.context.RequestContext;
import org.apache.webbeans.context.SessionContext;
import org.apache.webbeans.context.SingletonContext;
import org.apache.webbeans.conversation.ConversationManager;
import org.apache.webbeans.el.ELContextStore;
import org.apache.webbeans.logger.WebBeansLoggerFacade;
import org.apache.webbeans.web.intercept.RequestScopedBeanInterceptorHandler;

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
import java.lang.annotation.Annotation;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Web container {@link org.apache.webbeans.spi.ContextsService}
 * implementation.
 */
public class WebContextsService extends AbstractContextsService
{
    /**Logger instance*/
    private static final Logger logger = WebBeansLoggerFacade.getLogger(WebContextsService.class);

    private static final String OWB_SESSION_CONTEXT_ATTRIBUTE_NAME = "openWebBeansSessionContext";

    /**Current request context*/
    protected static ThreadLocal<ServletRequestContext> requestContexts = null;

    /**Current session context*/
    protected static ThreadLocal<SessionContext> sessionContexts = null;

    /**
     * A single applicationContext
     */
    protected ApplicationContext applicationContext;

    protected SingletonContext singletonContext;

    /**Current conversation context*/
    protected static ThreadLocal<ConversationContext> conversationContexts = null;
    
    /**Current dependent context*/
    protected static DependentContext dependentContext;

    /**Conversation context manager*/
    protected final ConversationManager conversationManager;



    /**Initialize thread locals*/
    static
    {
        requestContexts = new ThreadLocal<ServletRequestContext>();
        sessionContexts = new ThreadLocal<SessionContext>();
        conversationContexts = new ThreadLocal<ConversationContext>();

        //Dependent context is always active
        dependentContext = new DependentContext();
        dependentContext.setActive(true);
    }

    /**
     * Creates a new instance.
     */
    public WebContextsService(WebBeansContext webBeansContext)
    {
        super(webBeansContext);
        conversationManager = webBeansContext.getConversationManager();

        applicationContext = new ApplicationContext();
        applicationContext.setActive(true);
    }

    /**
     * Removes the ThreadLocals from the ThreadMap to prevent memory leaks.
     */
    public void removeThreadLocals()
    {
        requestContexts.remove();
        sessionContexts.remove();
        conversationContexts.remove();
        RequestScopedBeanInterceptorHandler.removeThreadLocals();
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
        RequestContext requestCtx = requestContexts.get();
        if (requestCtx != null)
        {
            requestCtx.destroy();
            requestContexts.set(null);
            requestContexts.remove();
        }

        SessionContext sessionCtx = sessionContexts.get();
        if (sessionCtx != null)
        {
            sessionCtx.destroy();
            sessionContexts.set(null);
            sessionContexts.remove();
        }

        ConversationContext conversationCtx = conversationContexts.get();
        if (conversationCtx != null)
        {
            conversationCtx.destroy();
            conversationContexts.set(null);
            conversationContexts.remove();
        }

        if (singletonContext != null)
        {
            singletonContext.destroy();
            singletonContext = null;
        }

        if (applicationContext != null)
        {
            applicationContext.destroy();
            applicationContext.destroySystemBeans();
        }
    }
    
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void endContext(Class<? extends Annotation> scopeType, Object endParameters)
    {        
        if(scopeType.equals(RequestScoped.class))
        {
            destroyRequestContext(endParameters);
        }
        else if(scopeType.equals(SessionScoped.class))
        {
            destroySessionContext(endParameters);
        }
        else if(scopeType.equals(ApplicationScoped.class))
        {
            destroyApplicationContext(endParameters);
        }
        else if(supportsConversation && scopeType.equals(ConversationScoped.class))
        {
            destroyConversationContext();
        }
        else if(scopeType.equals(Dependent.class))
        {
            //Do nothing
        }
        else if (scopeType.equals(Singleton.class))
        {
            destroySingletonContext(endParameters);
        }
        else
        {
            logger.warning("CDI-OpenWebBeans container does not support context scope "
                    + scopeType.getSimpleName()
                    + ". Scopes @Dependent, @RequestScoped, @ApplicationScoped and @Singleton are supported scope types");
        }
    }

    @Override
    public Context getCurrentContext(Class<? extends Annotation> scopeType, boolean createIfNotExists)
    {
        if(scopeType.equals(SessionScoped.class))
        {
            return getSessionContext(createIfNotExists);
        }

        return super.getCurrentContext(scopeType, createIfNotExists);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Context getCurrentContext(Class<? extends Annotation> scopeType)
    {
        if(scopeType.equals(RequestScoped.class))
        {
            return getRequestContext(true);
        }
        else if(scopeType.equals(SessionScoped.class))
        {
            return getSessionContext(true);
        }
        else if(scopeType.equals(ApplicationScoped.class))
        {
            return applicationContext;
        }
        else if(scopeType.equals(ConversationScoped.class))
        {
            return getConversationContext(true, false);
        }
        else if(scopeType.equals(Dependent.class))
        {
            return dependentContext;
        }
        else if (scopeType.equals(Singleton.class))
        {
            return singletonContext;
        }

        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void startContext(Class<? extends Annotation> scopeType, Object startParameter) throws ContextException
    {
        if (scopeType.equals(RequestScoped.class))
        {
            initRequestContext(startParameter);
        }
        else if (scopeType.equals(SessionScoped.class))
        {
            initSessionContext(startParameter);
        }
        else if (scopeType.equals(ApplicationScoped.class))
        {
            initApplicationContext(startParameter);
        }
        else if (supportsConversation && scopeType.equals(ConversationScoped.class))
        {
            initConversationContext(startParameter);
        }
        else if (scopeType.equals(Dependent.class))
        {
            //Do nothing
        }
        else if (scopeType.equals(Singleton.class))
        {
            initSingletonContext(startParameter);
        }
        else
        {
            logger.warning("CDI-OpenWebBeans container does not support context scope "
                    + scopeType.getSimpleName()
                    + ". Scopes @Dependent, @RequestScoped, @ApplicationScoped and @Singleton are supported scope types");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean supportsContext(Class<? extends Annotation> scopeType)
    {
        if (scopeType.equals(RequestScoped.class) ||
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
     * @param startupObject http servlet request event or system specific payload
     */
    protected void initRequestContext(Object startupObject )
    {
        
        ServletRequestContext requestContext = new ServletRequestContext();
        requestContext.setActive(true);

        requestContexts.set(requestContext);// set thread local

        Object payload = null;

        if(startupObject != null && startupObject instanceof ServletRequestEvent)
        {
            HttpServletRequest request = (HttpServletRequest) ((ServletRequestEvent) startupObject).getServletRequest();
            requestContext.setServletRequest(request);
            
            if (request != null)
            {
                payload = request;
            }
        }
        webBeansContext.getBeanManagerImpl().fireEvent(payload != null ? payload : new Object(), InitializedLiteral.INSTANCE_REQUEST_SCOPED);
    }
    
    /**
     * Destroys the request context and all of its components. 
     * @param endObject http servlet request object or other payload
     */
    protected void destroyRequestContext(Object endObject)
    {
        //Get context
        RequestContext context = getRequestContext(true);

        if (context == null)
        {
            return;
        }

            // cleanup open conversations first
        if (supportsConversation)
        {
            destroyOutdatedConversations(conversationContexts.get());
        }

        context.destroy();

        // clean up the EL caches after each request
        ELContextStore elStore = ELContextStore.getInstance(false);
        if (elStore != null)
        {
            elStore.destroyELContextStore();
        }

        Object payload = null;

        if (endObject != null && endObject instanceof ServletRequestEvent)
        {
            payload = ((ServletRequestEvent) endObject).getServletRequest();
        }
        webBeansContext.getBeanManagerImpl().fireEvent(payload != null ? payload : new Object(), DestroyedLiteral.INSTANCE_REQUEST_SCOPED);

        //Clear thread locals
        conversationContexts.set(null);
        conversationContexts.remove();

        sessionContexts.set(null);
        sessionContexts.remove();

        requestContexts.set(null);
        requestContexts.remove();

        RequestScopedBeanInterceptorHandler.removeThreadLocals();
    }


    /**
     * Creates the session context at the session start.
     * Or assign a
     * @param startupObject HttpSession object or other startup
     */
    protected void initSessionContext(Object startupObject)
    {
        SessionContext currentSessionContext;

        HttpSession session = startupObject instanceof HttpSession ? (HttpSession) startupObject : null;

        if (session == null)
        {
            // no session -> create a dummy SessionContext
            // this is handy if you create asynchronous tasks or
            // batches which use a 'admin' user.
            currentSessionContext = new SessionContext();
            currentSessionContext.setActive(true);

            webBeansContext.getBeanManagerImpl().fireEvent(new Object(), InitializedLiteral.INSTANCE_SESSION_SCOPED);
        }
        else
        {
            // we need to get it latest here to make sure we work on the same instance
            currentSessionContext = (SessionContext) session.getAttribute(OWB_SESSION_CONTEXT_ATTRIBUTE_NAME);

            if (currentSessionContext == null)
            {
                // no current context, so lets create a new one
                synchronized (OWB_SESSION_CONTEXT_ATTRIBUTE_NAME)
                {
                    currentSessionContext = (SessionContext) session.getAttribute(OWB_SESSION_CONTEXT_ATTRIBUTE_NAME);
                    if (currentSessionContext == null)
                    {
                        currentSessionContext = new SessionContext();
                        currentSessionContext.setActive(true);
                        webBeansContext.getBeanManagerImpl().fireEvent(session, InitializedLiteral.INSTANCE_SESSION_SCOPED);
                        session.setAttribute(OWB_SESSION_CONTEXT_ATTRIBUTE_NAME, currentSessionContext);
                    }
                }
            }
            else
            {
                // we do that in any case.
                // This is needed to trigger delta-replication on most servers
                session.setAttribute(OWB_SESSION_CONTEXT_ATTRIBUTE_NAME, currentSessionContext);
            }
        }


        //Set thread local
        sessionContexts.set(currentSessionContext);
    }

    /**
     * Destroys the session context and all of its components at the end of the
     * session. 
     * @param endObject http session object. Can be {@code null} or different object for non-http SessionContexts. Such a context only lives for one thread.
     */
    protected void destroySessionContext(Object endObject)
    {
        // Get current session context from ThreadLocal
        SessionContext context = sessionContexts.get();

        Object payload = null;

        if (endObject != null && endObject instanceof HttpSession)
        {
            HttpSession session = (HttpSession) endObject;
            if (context == null)
            {
                // init in this case only attaches the existing session to the ThreadLocal
                initSessionContext(session);
                context = sessionContexts.get();
            }
            payload = session;
        }

        // Destroy context
        if (context != null)
        {
            if (supportsConversation)
            {
                // get all conversations stored in the Session and destroy them
                // also set the current conversation (if any) to transient
                ConversationContext currentConversationContext = getConversationContext(true, true);
                if (currentConversationContext != null && !currentConversationContext.getConversation().isTransient())
                {
                    // an active conversation will now be set to transient
                    // note that ConversationImpl#end() also removes the conversation from the Session
                    currentConversationContext.getConversation().end();
                }
            }
            context.destroy();
            webBeansContext.getBeanManagerImpl().fireEvent(payload != null ? payload : new Object(), DestroyedLiteral.INSTANCE_SESSION_SCOPED);
        }

        // Clear thread locals
        sessionContexts.set(null);
        sessionContexts.remove();
    }

    /**
     * Creates the application context at the application startup 
     * @param startupObject servlet context object or other startup
     *
     */
    protected void initApplicationContext(Object startupObject)
    {
        if (applicationContext != null)
        {
            applicationContext.setActive(true);
            return;
        }

        ApplicationContext newApplicationContext = new ApplicationContext();
        newApplicationContext.setActive(true);

        if (applicationContext == null)
        {
            applicationContext = newApplicationContext;
        }
    }

    /**
     * Destroys the application context and all of its components at the end of
     * the application. 
     * @param endObject servlet context object or other payload
     */
    protected void destroyApplicationContext(Object endObject)
    {
        //Destroy context
        if(applicationContext != null)
        {
            applicationContext.destroy();
            // this is needed to get rid of ApplicationScoped beans which are cached inside the proxies...
            webBeansContext.getBeanManagerImpl().clearCacheProxies();

            Object payload = endObject != null && endObject instanceof ServletContext ? endObject : new Object();
            webBeansContext.getBeanManagerImpl().fireEvent(payload, DestroyedLiteral.INSTANCE_APPLICATION_SCOPED);
        }
    }
    
    /**
     * Initialize singleton context.
     * @param startupObject servlet context
     */
    protected void initSingletonContext(Object startupObject)
    {
        if (singletonContext != null)
        {
            return;
        }

        synchronized (this)
        {
            if (singletonContext == null)
            {
                singletonContext = new SingletonContext();
                singletonContext.setActive(true);
                Object payLoad = startupObject != null && startupObject instanceof ServletContext ? (ServletContext) startupObject : new Object();
                webBeansContext.getBeanManagerImpl().fireEvent(payLoad, InitializedLiteral.INSTANCE_SINGLETON_SCOPED);
            }
        }
    }
    
    /**
     * Destroy singleton context.
     * @param endObject servlet context or other payload
     */
    protected void destroySingletonContext(Object endObject)
    {
        if (singletonContext != null)
        {
            singletonContext.destroy();
            singletonContext = null;
            Object payload = endObject != null ? endObject : new Object();
            webBeansContext.getBeanManagerImpl().fireEvent(payload, DestroyedLiteral.INSTANCE_SINGLETON_SCOPED);
        }
    }

    /**
     * Initialize conversation context.
     * @param startObject either a ServletRequest or a ConversationContext
     */
    protected void initConversationContext(Object startObject)
    {
        if (conversationContexts.get() != null)
        {
            return;
        }

        if (startObject != null && startObject instanceof ConversationContext)
        {
            //X TODO check if this branch is still needed
            ConversationContext context = (ConversationContext) startObject;
            context.setActive(true);
            conversationContexts.set(context);
        }
    }

    /**
     * Destroy conversation context.
     */
    protected void destroyConversationContext()
    {
        if (conversationContexts.get() == null)
        {
            return;
        }

        ConversationContext context = getConversationContext(false, true);

        if (context != null)
        {
            context.destroy();
            webBeansContext.getBeanManagerImpl().fireEvent(new Object(), DestroyedLiteral.INSTANCE_SINGLETON_SCOPED);
        }

        conversationContexts.set(null);
        conversationContexts.remove();
    }

    
    /**
     * Get current request ctx.
     * @return request context
     */
    public ServletRequestContext getRequestContext(boolean create)
    {
        ServletRequestContext requestContext = requestContexts.get();
        if (requestContext == null && create)
        {
            initRequestContext(null);
        }
        return requestContext;
    }

    /**
     * Get current session ctx.
     * @return session context
     */
    public SessionContext getSessionContext(boolean create)
    {
        SessionContext context = sessionContexts.get();
        if (null == context && create)
        {
            lazyStartSessionContext();
            context = sessionContexts.get();
        }

        return context;
    }

    /**
     * Get current conversation ctx.
     * @return conversation context
     */
    public  ConversationContext getConversationContext(boolean create, boolean ignoreProblems)
    {
        ConversationContext conversationContext = conversationContexts.get();
        if (conversationContext == null && create)
        {
            conversationContext = conversationManager.getConversationContext(getSessionContext(true));
            conversationContexts.set(conversationContext);

            if (!ignoreProblems && conversationContext.getConversation().getProblemDuringCreation() != null)
            {
                throw conversationContext.getConversation().getProblemDuringCreation();
            }
        }

        return conversationContext;
    }



    /**
     * Try to lazily start the sessionContext
     */
    private void lazyStartSessionContext()
    {

        if (logger.isLoggable(Level.FINE))
        {
            logger.log(Level.FINE, ">lazyStartSessionContext");
        }

        RequestContext context = getRequestContext(true);
        if (context == null)
        {
            logger.log(Level.WARNING, "Could NOT lazily initialize session context because NO active request context");
        }

        if (context instanceof ServletRequestContext)
        {
            ServletRequestContext requestContext = (ServletRequestContext) context;
            HttpServletRequest servletRequest = requestContext.getServletRequest();
            if (null != servletRequest)
            { // this could be null if there is no active request context
                try
                {
                    HttpSession currentSession = servletRequest.getSession(true);
                    initSessionContext(currentSession);

                    if (logger.isLoggable(Level.FINE))
                    {
                        logger.log(Level.FINE, "Lazy SESSION context initialization SUCCESS");
                    }

                    return;
                }
                catch (Exception e)
                {
                    logger.log(Level.SEVERE, WebBeansLoggerFacade.constructMessage(OWBLogConst.ERROR_0013, e));
                }
            }
        }

        // in any other case
        initSessionContext(null);
        logger.log(Level.FINE, "Starting a non-web backed SessionContext");
    }


    /**
     * This might be needed when you aim to start a new thread in a WebApp.
     * @param scopeType
     */
    @Override
    public void activateContext(Class<? extends Annotation> scopeType)
    {
        if (scopeType.equals(SessionScoped.class))
        {
            // getSessionContext() implicitely creates and binds the SessionContext
            // to the current Thread if it doesn't yet exist.
            getSessionContext(true).setActive(true);
        }
        else
        {
            super.activateContext(scopeType);
        }
    }

}
