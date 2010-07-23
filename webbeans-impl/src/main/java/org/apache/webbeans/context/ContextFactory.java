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
package org.apache.webbeans.context;

import java.lang.annotation.Annotation;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.context.*;
import javax.enterprise.context.spi.Context;
import javax.inject.Singleton;

import org.apache.webbeans.container.BeanManagerImpl;
import org.apache.webbeans.context.type.ContextTypes;
import org.apache.webbeans.corespi.ServiceLoader;
import org.apache.webbeans.logger.WebBeansLogger;
import org.apache.webbeans.spi.ContextsService;
import org.apache.webbeans.util.WebBeansUtil;

/**
 * JSR-299 based standard context
 * related operations.
 */
public final class ContextFactory
{
    /**Logger instance*/
    private static final WebBeansLogger logger = WebBeansLogger.getLogger(ContextFactory.class);

    /**
     * Underlying context service per ClassLoader
     * This distinction is necessary for application servers
     * with multiple WAR deployments having different
     * ContextsServices configured.
     */
    private static ConcurrentHashMap<ClassLoader, ContextsService> contextServices
            = new ConcurrentHashMap<ClassLoader, ContextsService>();


    /**
     * Not-instantiate
     */
    private ContextFactory()
    {
        throw new UnsupportedOperationException();
    }

    /**
     * This must be called before a WebApp shuts down.
     * It makes sure that all caches get cleaned up.
     */
    public static void cleanUpContextFactory()
    {
        ClassLoader cl = WebBeansUtil.getCurrentClassLoader();
        contextServices.remove(cl);
    }

    /**
     * @return the ContextService for the current ClassLoader
     */
    private static ContextsService getContextsService()
    {
        ClassLoader cl = WebBeansUtil.getCurrentClassLoader();
        ContextsService cs = contextServices.get(cl);
        if (cs == null)
        {
            cs = ServiceLoader.getService(ContextsService.class);
            contextServices.put(cl, cs);
        }
        
        return cs;
    }
    
    
    public static void initRequestContext(Object request)
    {
        try
        {
            ContextsService contextService = getContextsService();
            contextService.startContext(RequestScoped.class, request);
        }
        catch (Exception e)
        {
            logger.error(e);
        }
    }

    public static Context getCustomContext(Context context)
    {
        if (BeanManagerImpl.getManager().isPassivatingScope(context.getScope()))
        {
            return new CustomPassivatingContextImpl(context);
        }
        
        return new CustomContextImpl(context);
    }
    
    public static void destroyRequestContext(Object request)
    {
        ContextsService contextService = getContextsService();
        contextService.endContext(RequestScoped.class, request);
    }

    public static void initSessionContext(Object session)
    {
        try
        {
            ContextsService contextService = getContextsService();
            contextService.startContext(SessionScoped.class, session);
        }
        catch (Exception e)
        {
            logger.error(e);
        }
    }

    public static void destroySessionContext(Object session)
    {
        ContextsService contextService = getContextsService();
        contextService.endContext(SessionScoped.class, session);
    }

    /**
     * Creates the application context at the application startup
     * 
     * @param parameter parameter object
     */
    public static void initApplicationContext(Object parameter)
    {
        try
        {
            ContextsService contextService = getContextsService();
            contextService.startContext(ApplicationScoped.class, parameter);
        }
        catch (Exception e)
        {
            logger.error(e);
        }
    }

    /**
     * Destroys the application context and all of its components at the end of
     * the application.
     * 
     * @param parameter parameter object
     */
    public static void destroyApplicationContext(Object parameter)
    {
        ContextsService contextService = getContextsService();
        contextService.endContext(ApplicationScoped.class, parameter);
    }
    
    public static void initSingletonContext(Object parameter)
    {
        try
        {
            ContextsService contextService = getContextsService();
            contextService.startContext(Singleton.class, parameter);
        }
        catch (Exception e)
        {
            logger.error(e);            
        }
    }
    
    public static void destroySingletonContext(Object parameter)
    {
        ContextsService contextService = getContextsService();
        contextService.endContext(Singleton.class, parameter);
    }

    public static void initConversationContext(Object context)
    {
        try
        {
            ContextsService contextService = getContextsService();
            contextService.startContext(ConversationScoped.class, context);
        }
        catch (Exception e)
        {
            logger.error(e);            
        }
    }

    public static void destroyConversationContext()
    {
        ContextsService contextService = getContextsService();
        contextService.endContext(ConversationScoped.class, null);
    }

    /**
     * Gets the current context with given type.
     * 
     * @return the current context
     * @throws ContextNotActiveException if context is not active
     * @throws IllegalArgumentException if the type is not a standard context
     */
    public static Context getStandardContext(ContextTypes type) throws ContextNotActiveException
    {
        Context context = null;
        ContextsService contextService = getContextsService();
        switch (type.getCardinal())
        {
            case 0:
                context = contextService.getCurrentContext(RequestScoped.class);
                break;
    
            case 1:
                context = contextService.getCurrentContext(SessionScoped.class);
                break;
    
            case 2:
                context = contextService.getCurrentContext(ApplicationScoped.class);
                break;
    
            case 3:
                context = contextService.getCurrentContext(ConversationScoped.class);
                break;
                
            case 4:
                context = contextService.getCurrentContext(Dependent.class);
                break;

            case 5:
                context = contextService.getCurrentContext(Singleton.class);
                break;
                
            default:
                throw new IllegalArgumentException("There is no such a standard context with context id=" + type.getCardinal());
        }

        return context;
    }

    /**
     * Gets the standard context with given scope type.
     * 
     * @return the current context, or <code>null</code> if no standard context exists for the given scopeType
     */
    public static Context getStandardContext(Class<? extends Annotation> scopeType)
    {
        if (scopeType.equals(RequestScoped.class))
        {
            return getStandardContext(ContextTypes.REQUEST);
        }
        if (scopeType.equals(SessionScoped.class))
        {
            return getStandardContext(ContextTypes.SESSION);
        }
        if (scopeType.equals(ApplicationScoped.class))
        {
            return getStandardContext(ContextTypes.APPLICATION);
        }
        if (scopeType.equals(ConversationScoped.class))
        {
            return getStandardContext(ContextTypes.CONVERSATION);
        }
        if (scopeType.equals(Dependent.class))
        {
            return getStandardContext(ContextTypes.DEPENDENT);
        }
        if (scopeType.equals(Singleton.class))
        {
            return getStandardContext(ContextTypes.SINGLETON);
        }
        
        return null;
    }
    
    /**
     * Activate context. 
     */
    public static void activateContext(Class<? extends Annotation> scopeType)
    {
        ContextsService contextService = getContextsService();
        contextService.activateContext(scopeType);
    }
    
    /**
     * Deactivate context.
     */
    public static void deActivateContext(Class<? extends Annotation> scopeType)
    {
        ContextsService contextService = getContextsService();
        contextService.deActivateContext(scopeType);
    }
    
}
