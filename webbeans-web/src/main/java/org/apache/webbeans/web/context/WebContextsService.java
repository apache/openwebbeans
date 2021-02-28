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

import org.apache.webbeans.annotation.BeforeDestroyedLiteral;
import org.apache.webbeans.annotation.DestroyedLiteral;
import org.apache.webbeans.annotation.InitializedLiteral;
import org.apache.webbeans.config.OWBLogConst;
import org.apache.webbeans.config.OpenWebBeansConfiguration;
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
import org.apache.webbeans.intercept.SessionScopedBeanInterceptorHandler;
import org.apache.webbeans.logger.WebBeansLoggerFacade;
import org.apache.webbeans.intercept.RequestScopedBeanInterceptorHandler;

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
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Web container {@link org.apache.webbeans.spi.ContextsService}
 * implementation.
 */
public class WebContextsService extends AbstractContextsService
{
    /**Logger instance*/
    private static final Logger logger = WebBeansLoggerFacade.getLogger(WebContextsService.class);

    private static final String OWB_SESSION_CONTEXT_ATTRIBUTE_NAME = "openWebBeansSessionContext";

    /**
     * TODO implement later: optional immediate destroy
     */
    private final boolean destroySessionImmediately = false;

    /**
     * A single applicationContext
     */
    protected ApplicationContext applicationContext;

    protected SingletonContext singletonContext;

    /**Current request context*/
    protected ThreadLocal<ServletRequestContext> requestContexts;

    /**Current session context*/
    protected ThreadLocal<SessionContext> sessionContexts;

    /**Current conversation context*/
    protected ThreadLocal<ConversationContext> conversationContexts;
    
    /**Current dependent context*/
    protected DependentContext dependentContext;

    /**Conversation context manager*/
    protected final ConversationManager conversationManager;

    protected Boolean eagerSessionInitialisation;
    protected Pattern eagerSessionPattern;


    /**
     * Creates a new instance.
     */
    public WebContextsService(WebBeansContext webBeansContext)
    {
        super(webBeansContext);
        conversationManager = webBeansContext.getConversationManager();

        applicationContext = new ApplicationContext();
        applicationContext.setActive(true);

        requestContexts = new ThreadLocal<>();
        sessionContexts = new ThreadLocal<>();
        conversationContexts = new ThreadLocal<>();

        //Dependent context is always active
        dependentContext = new DependentContext();
        dependentContext.setActive(true);

        configureEagerSessionInitialisation(webBeansContext);
    }

    protected void configureEagerSessionInitialisation(WebBeansContext webBeansContext)
    {
        String val = webBeansContext.getOpenWebBeansConfiguration().getProperty(OpenWebBeansConfiguration.EAGER_SESSION_INITIALISATION);
        if (val == null || val.isEmpty() || "false".equalsIgnoreCase(val))
        {
            eagerSessionInitialisation = Boolean.FALSE;
            logger.fine("EagerSessionInitialisation is configured to FALSE (Session will get created lazily on first use)");
        }
        else if ("true".equalsIgnoreCase(val))
        {
            eagerSessionInitialisation = Boolean.TRUE;
            logger.fine("EagerSessionInitialisation is configured to TRUE (Session will get created at the beginning of a request)");
        }
        else
        {
            logger.fine("EagerSessionInitialisation used for all URIs with RegExp " + val);
            eagerSessionPattern = Pattern.compile(val);
        }
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
        //Start signelton context
        startContext(Singleton.class, initializeObject);

        //Start application context
        startContext(ApplicationScoped.class, initializeObject);
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
            return;
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
        if(scopeType.equals(RequestScoped.class))
        {
            return getRequestContext(createIfNotExists);
        }

        if(scopeType.equals(SessionScoped.class))
        {
            return getSessionContext(createIfNotExists);
        }

        return getCurrentContext(scopeType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Context getCurrentContext(Class<? extends Annotation> scopeType)
    {
        if(scopeType.equals(RequestScoped.class))
        {
            return getRequestContext(false);
        }
        else if(scopeType.equals(SessionScoped.class))
        {
            // session gets created lazily, so we need to force the creation
            return getSessionContext(true);
        }
        else if(scopeType.equals(ApplicationScoped.class))
        {
            return applicationContext;
        }
        else if(scopeType.equals(ConversationScoped.class))
        {
            return getConversationContext(false, false);
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
            return;
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
     * Initialize requext context with the given request object.
     * @param startupObject http servlet request event or system specific payload
     */
    protected void initRequestContext(Object startupObject )
    {
        
        ServletRequestContext requestContext = new ServletRequestContext();
        requestContext.setActive(true);

        requestContexts.set(requestContext);// set thread local

        Object payload = null;

        if(startupObject instanceof ServletRequestEvent)
        {
            HttpServletRequest request = (HttpServletRequest) ((ServletRequestEvent) startupObject).getServletRequest();
            requestContext.setServletRequest(request);
            
            if (request != null)
            {
                payload = request;

                if (shouldEagerlyInitializeSession(request))
                {
                    request.getSession(true);
                }
            }
        }
        if (shouldFireRequestLifecycleEvents())
        {
            webBeansContext.getBeanManagerImpl().fireContextLifecyleEvent(
                payload != null ? payload : new Object(), InitializedLiteral.INSTANCE_REQUEST_SCOPED);
        }
    }

    protected boolean shouldEagerlyInitializeSession(HttpServletRequest request)
    {
        if (eagerSessionPattern != null)
        {
            String requestURI = request.getRequestURI();
            Matcher matcher = eagerSessionPattern.matcher(requestURI);
            return matcher.matches();
        }
        return eagerSessionInitialisation;
    }

    /**
     * Destroys the request context and all of its components. 
     * @param endObject http servlet request object or other payload
     */
    protected void destroyRequestContext(Object endObject)
    {
        //Get context
        ServletRequestContext context = getRequestContext(false);

        if (context == null)
        {
            return;
        }

            // cleanup open conversations first
        if (supportsConversation)
        {
            destroyOutdatedConversations(conversationContexts.get());
        }

        if (context.getPropagatedSessionContext() != null)
        {
            SessionContext sessionContext = context.getPropagatedSessionContext();

            Object payload = null;
            if (context.getServletRequest() != null)
            {
                payload = context.getHttpSession();
                if (payload == null)
                {
                    // in tomcat it will be null if invalidate was called
                    payload = context.getServletRequest().getSession(false);
                }
            }

            webBeansContext.getBeanManagerImpl().fireContextLifecyleEvent(
                payload != null ? payload : new Object(), BeforeDestroyedLiteral.INSTANCE_SESSION_SCOPED);

            sessionContext.destroy();

            webBeansContext.getBeanManagerImpl().fireContextLifecyleEvent(
                payload != null ? payload : new Object(), DestroyedLiteral.INSTANCE_SESSION_SCOPED);

        }

        Object payload = null;
        if (shouldFireRequestLifecycleEvents())
        {
            if (endObject != null && endObject instanceof ServletRequestEvent)
            {
                payload = ((ServletRequestEvent) endObject).getServletRequest();
            }

            webBeansContext.getBeanManagerImpl().fireContextLifecyleEvent(
                    payload != null ? payload : new Object(), BeforeDestroyedLiteral.INSTANCE_REQUEST_SCOPED);

        }


        context.destroy();

        // clean up the EL caches after each request
        ELContextStore elStore = ELContextStore.getInstance(false);
        if (elStore != null)
        {
            elStore.destroyELContextStore();
        }

        if (shouldFireRequestLifecycleEvents())
        {
            webBeansContext.getBeanManagerImpl().fireContextLifecyleEvent(
                payload != null ? payload : new Object(), DestroyedLiteral.INSTANCE_REQUEST_SCOPED);
        }

        // clean the proxy cache ThreadLocals
        RequestScopedBeanInterceptorHandler.removeThreadLocals();
        SessionScopedBeanInterceptorHandler.removeThreadLocals();

        //Clear thread locals
        requestContexts.set(null);
        requestContexts.remove();
    }


    /**
     * Creates the session context at the session start.
     * @param startupObject HttpSession object
     */
    protected void initSessionContext(Object startupObject)
    {
        SessionContext currentSessionContext;

        HttpSession session = startupObject instanceof HttpSession ? (HttpSession) startupObject : null;

        if (session != null)
        {
            // we need to get it latest here to make sure we work on the same instance
            currentSessionContext = (SessionContext) session.getAttribute(OWB_SESSION_CONTEXT_ATTRIBUTE_NAME);

            if (currentSessionContext == null)
            {
                // no current context, so lets create a new one
                synchronized (session)
                {
                    currentSessionContext = (SessionContext) session.getAttribute(OWB_SESSION_CONTEXT_ATTRIBUTE_NAME);
                    if (currentSessionContext == null)
                    {
                        currentSessionContext = new SessionContext();
                        currentSessionContext.setActive(true);
                        
                        // init context before fire @Initialized(SessionScoped)
                        // so that SessionScoped beans are already available inside the observer
                        session.setAttribute(OWB_SESSION_CONTEXT_ATTRIBUTE_NAME, currentSessionContext);
                        sessionContexts.set(currentSessionContext);
                        
                        webBeansContext.getBeanManagerImpl().fireContextLifecyleEvent(
                            session, InitializedLiteral.INSTANCE_SESSION_SCOPED);
                    }
                }
            }
            else
            {
                // we do that in any case.
                // This is needed to trigger delta-replication on most servers
                session.setAttribute(OWB_SESSION_CONTEXT_ATTRIBUTE_NAME, currentSessionContext);
                currentSessionContext.setActive(true);

                //Set thread local
                sessionContexts.set(currentSessionContext);
            }
        }
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
        HttpSession session = null;

        // whether the session is destroyed because it is expired
        boolean sessionIsExpiring = false;

        if (endObject instanceof HttpSession)
        {
            session = (HttpSession) endObject;
            if (context == null && session.getAttribute(OWB_SESSION_CONTEXT_ATTRIBUTE_NAME) != null)
            {
                if (!destroySessionImmediately)
                {
                    sessionIsExpiring = sessionIsExpiring(session);
                }

                // init in this case only attaches the existing session to the ThreadLocal
                initSessionContext(session);
                context = sessionContexts.get();
            }
        }

        // Destroy context
        if (context != null && context.isActive())
        {
            // we need to mark the conversation to get destroyed at the end of the request
            ServletRequestContext requestContext = getRequestContext(true);

            if (destroySessionImmediately
                || requestContext == null || requestContext.getServletRequest() == null
                || requestContext.getServletRequest().getSession(false) == null
                || sessionIsExpiring)
            {
                webBeansContext.getBeanManagerImpl().fireContextLifecyleEvent(
                    session != null ? session : new Object(), BeforeDestroyedLiteral.INSTANCE_SESSION_SCOPED);

                context.destroy();

                webBeansContext.getBeanManagerImpl().fireContextLifecyleEvent(
                    session != null ? session : new Object(), DestroyedLiteral.INSTANCE_SESSION_SCOPED);

                // Clear thread locals
                sessionContexts.set(null);
                sessionContexts.remove();
            }
            else
            {
                requestContext.setPropagatedSessionContext(context);
                // this is to be spec compliant but depending the servlet container
                // it can be dangerous if sessions are pooled (ie you can fire a session used by another request)
                requestContext.setHttpSession(session);
            }
        }

        SessionScopedBeanInterceptorHandler.removeThreadLocals();
    }


    /**
     * @return {@code true} if the sessino is currently expiring or has already expired
     */
    protected boolean sessionIsExpiring(HttpSession session)
    {
        int maxInactiveInterval = session.getMaxInactiveInterval();
        if (maxInactiveInterval > 0)
        {
            long inactiveSince = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - session.getLastAccessedTime());
            if (inactiveSince >= maxInactiveInterval)
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Creates the application context at the application startup 
     * @param startupObject servlet context object or other startup
     *
     */
    protected void initApplicationContext(Object startupObject)
    {
        if (applicationContext != null && !applicationContext.isDestroyed())
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
        if(applicationContext != null && !applicationContext.isDestroyed())
        {
            webBeansContext.getBeanManagerImpl().fireContextLifecyleEvent(
                endObject != null ? endObject : new Object(),
                BeforeDestroyedLiteral.INSTANCE_APPLICATION_SCOPED);

            applicationContext.destroy();
            // this is needed to get rid of ApplicationScoped beans which are cached inside the proxies...
            webBeansContext.getBeanManagerImpl().clearCacheProxies();

            Object payload = endObject instanceof ServletContext ? endObject : new Object();
            webBeansContext.getBeanManagerImpl().fireContextLifecyleEvent(
                payload, DestroyedLiteral.INSTANCE_APPLICATION_SCOPED);
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
                Object payLoad = startupObject instanceof ServletContext
                    ? (ServletContext) startupObject : new Object();
                webBeansContext.getBeanManagerImpl().fireContextLifecyleEvent(
                    payLoad, InitializedLiteral.INSTANCE_SINGLETON_SCOPED);
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
            Object payload = endObject != null ? endObject : new Object();
            webBeansContext.getBeanManagerImpl().fireContextLifecyleEvent(
                payload, BeforeDestroyedLiteral.INSTANCE_SINGLETON_SCOPED);

            singletonContext.destroy();

            singletonContext = null;
            webBeansContext.getBeanManagerImpl().fireContextLifecyleEvent(
                payload, DestroyedLiteral.INSTANCE_SINGLETON_SCOPED);
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

        if (startObject instanceof ConversationContext)
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
            webBeansContext.getBeanManagerImpl().fireContextLifecyleEvent(
                new Object(), DestroyedLiteral.INSTANCE_SINGLETON_SCOPED);
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
            requestContext = requestContexts.get();
        }
        return requestContext;
    }

    /**
     * Get current session ctx or lazily create one.
     * @return session context
     * @param forceCreate if {@code true} we will force creating a session if not yet exists.
     *                    if {@code false} we will only create a SessionContext if a HttpSession already exists
     */
    public SessionContext getSessionContext(boolean forceCreate)
    {
        SessionContext context = sessionContexts.get();
        if (null == context && forceCreate)
        {
            lazyStartSessionContext(true);
            context = sessionContexts.get();
        }

        return context;
    }

    /**
     * Get current conversation ctx.
     * @return conversation context
     */
    public ConversationContext getConversationContext(boolean create, boolean ignoreProblems)
    {
        ConversationContext conversationContext = conversationContexts.get();
        if (conversationContext == null)
        {
            SessionContext sessionContext = getSessionContext(true);

            if (sessionContext != null)
            {
                conversationContext = conversationManager.getConversationContext(sessionContext);
                conversationContexts.set(conversationContext);

                if (conversationContext.getConversation().isTransient())
                {
                    webBeansContext.getBeanManagerImpl().fireContextLifecyleEvent(
                        conversationManager.getLifecycleEventPayload(conversationContext),
                        InitializedLiteral.INSTANCE_CONVERSATION_SCOPED);
                }


                if (!ignoreProblems && conversationContext.getConversation().getProblemDuringCreation() != null)
                {
                    throw conversationContext.getConversation().getProblemDuringCreation();
                }
            }
        }

        return conversationContext;
    }


    /**
     * Try to lazily start the sessionContext.
     * First we try to find a real HttpSession and create the SessionContext in there.
     * If this is not possible and the {@param allowSynthecticSession} is {@code true} then
     * we will
     * @param createSession if {@code false} then we will only create a SessionContext if a HttpSession already exists
     */
    private void lazyStartSessionContext(boolean createSession)
    {

        if (logger.isLoggable(Level.FINE))
        {
            logger.log(Level.FINE, ">lazyStartSessionContext");
        }

        ServletRequestContext requestContext = getRequestContext(false);
        if (requestContext == null)
        {
            if (createSession)
            {
                logger.log(Level.WARNING, "Could NOT lazily initialize session context because NO active request context");
            }
            return;
        }

        HttpServletRequest servletRequest = requestContext.getServletRequest();
        // this could be null if there is no active request context
        if (servletRequest != null)
        {
            try
            {
                HttpSession currentSession = servletRequest.getSession(createSession);
                if (currentSession != null)
                {
                    initSessionContext(currentSession);

                    if (logger.isLoggable(Level.FINE))
                    {
                        logger.log(Level.FINE, "Lazy SESSION context initialization SUCCESS");
                    }

                    return;
                }
            }
            catch (Exception e)
            {
                logger.log(Level.SEVERE, WebBeansLoggerFacade.constructMessage(OWBLogConst.ERROR_0013, e));
            }
        }
    }

}
