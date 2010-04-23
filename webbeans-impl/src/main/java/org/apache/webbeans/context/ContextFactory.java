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

import javax.enterprise.context.*;
import javax.enterprise.context.spi.Context;
import javax.inject.Singleton;

import org.apache.webbeans.container.BeanManagerImpl;
import org.apache.webbeans.context.type.ContextTypes;
import org.apache.webbeans.corespi.ServiceLoader;
import org.apache.webbeans.logger.WebBeansLogger;
import org.apache.webbeans.spi.ContextsService;

/**
 * JSR-299 based standard context
 * related operations.
 */
public final class ContextFactory
{
    /**Logger instance*/
    private static final WebBeansLogger logger = WebBeansLogger.getLogger(ContextFactory.class);
    
    /**Underlying context service*/
    private static ContextsService contextService = ServiceLoader.getService(ContextsService.class);
    
    /**
     * Not-instantiate
     */
    private ContextFactory()
    {
        throw new UnsupportedOperationException();
    }
    
    public static void initRequestContext(Object request)
    {
        try
        {
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
        contextService.endContext(RequestScoped.class, request);
    }

    public static void initSessionContext(Object session)
    {
        try
        {
            contextService.startContext(SessionScoped.class, session);
        }
        catch (Exception e)
        {
            logger.error(e);
        }
    }

    public static void destroySessionContext(Object session)
    {
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
        contextService.endContext(ApplicationScoped.class, parameter);
    }
    
    public static void initSingletonContext(Object parameter)
    {
        try
        {
            contextService.startContext(Singleton.class, parameter);
        }
        catch (Exception e)
        {
            logger.error(e);            
        }
    }
    
    public static void destroySingletonContext(Object parameter)
    {
        contextService.endContext(Singleton.class, parameter);
    }

    public static void initConversationContext(Object context)
    {
        try
        {
            contextService.startContext(ConversationScoped.class, context);
        }
        catch (Exception e)
        {
            logger.error(e);            
        }
    }

    public static void destroyConversationContext()
    {
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
        Context context = null;

        if (scopeType.equals(RequestScoped.class))
        {
            context = getStandardContext(ContextTypes.REQUEST);
        }
        else if (scopeType.equals(SessionScoped.class))
        {
            context = getStandardContext(ContextTypes.SESSION);
        }
        else if (scopeType.equals(ApplicationScoped.class))
        {
            context = getStandardContext(ContextTypes.APPLICATION);
        }
        else if (scopeType.equals(ConversationScoped.class))
        {
            context = getStandardContext(ContextTypes.CONVERSATION);

        }
        else if (scopeType.equals(Dependent.class))
        {
            context = getStandardContext(ContextTypes.DEPENDENT);
        }
        else if (scopeType.equals(Singleton.class))
        {
            context = getStandardContext(ContextTypes.SINGLETON);
        }
        
        return context;
    }
    
    /**
     * Activate context. 
     */
    public static void activateContext(Class<? extends Annotation> scopeType)
    {
        contextService.activateContext(scopeType);
    }
    
    /**
     * Deactivate context.
     */
    public static void deActivateContext(Class<? extends Annotation> scopeType)
    {
        contextService.deActivateContext(scopeType);
    }
    
}