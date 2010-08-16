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
package org.apache.webbeans.cditest.owb;

import java.lang.annotation.Annotation;
import java.util.Set;

import javax.enterprise.inject.ResolutionException;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;

import org.apache.webbeans.cditest.CdiTestContainer;
import org.apache.webbeans.context.ContextFactory;
import org.apache.webbeans.lifecycle.LifecycleFactory;
import org.apache.webbeans.spi.ContainerLifecycle;
import static org.apache.webbeans.util.InjectionExceptionUtils.*;

/**
 * OpenWebBeans specific implementation of {@link CdiTestContainer}.
 */
public class CdiTestOpenWebBeansContainer implements CdiTestContainer 
{

    private ContainerLifecycle  lifecycle = null;
    private MockServletContext  servletContext = null;
    private MockHttpSession     session = null;

    @Override
    public void bootContainer() throws Exception 
    {
        servletContext = new MockServletContext();
        session = new MockHttpSession();
        lifecycle = LifecycleFactory.getInstance().getLifecycle();
        lifecycle.startApplication(servletContext);
    }

    @Override
    public void shutdownContainer() throws Exception 
    {
        if (lifecycle != null) 
        {
            lifecycle.stopApplication(servletContext);
        }
    }

    @Override
    public void startContexts() throws Exception 
    {
        ContextFactory.initSingletonContext(servletContext);
        ContextFactory.initApplicationContext(servletContext);
        ContextFactory.initSessionContext(session);
        ContextFactory.initConversationContext(null);
        ContextFactory.initRequestContext(null);
    }

    @Override
    public void startApplicationScope() throws Exception 
    {
        ContextFactory.initApplicationContext(servletContext);
    }

    @Override
    public void startConversationScope() throws Exception 
    {
        ContextFactory.initConversationContext(null);
    }

    @Override
    public void startCustomScope(Class<? extends Annotation> scopeClass) throws Exception 
    {
        //X TODO
    }

    @Override
    public void startRequestScope() throws Exception 
    {
        ContextFactory.initRequestContext(null);
    }

    @Override
    public void startSessionScope() throws Exception 
    {
        ContextFactory.initSessionContext(session);
    }

    @Override
    public void stopContexts() throws Exception 
    {
        ContextFactory.destroyRequestContext(null);
        ContextFactory.destroyConversationContext();
        ContextFactory.destroySessionContext(session);
        ContextFactory.destroyApplicationContext(servletContext);
        ContextFactory.destroySingletonContext(servletContext);

        ContextFactory.cleanUpContextFactory();
    }

    @Override
    public void stopApplicationScope() throws Exception 
    {
        ContextFactory.destroyApplicationContext(servletContext);
    }

    @Override
    public void stopConversationScope() throws Exception 
    {
        ContextFactory.destroyConversationContext();
    }

    @Override
    public void stopCustomScope(Class<? extends Annotation> scopeClass) throws Exception 
    {
        //X TODO
    }

    @Override
    public void stopRequestScope() throws Exception 
    {
        ContextFactory.destroyRequestContext(null);
    }

    @Override
    public void stopSessionScope() throws Exception 
    {
        ContextFactory.destroySessionContext(session);
    }
    
    public  BeanManager getBeanManager() 
    {
        return lifecycle.getBeanManager();
    }

    @Override
    public <T> T getInstance(Class<T> type, Annotation... qualifiers)
    throws ResolutionException 
    {
        Set<Bean<?>> beans = getBeanManager().getBeans(type, qualifiers);
        if (beans == null || beans.isEmpty()) 
        {
            throwBeanNotFoundException(type, qualifiers);
        }

        if (beans.size() > 1) 
        {
            throwAmbiguousResolutionException(beans, type, qualifiers);
        }

        @SuppressWarnings("unchecked")
        Bean<T> bean = (Bean<T>)beans.iterator().next();

        @SuppressWarnings("unchecked")
        T instance = (T) getBeanManager().getReference(bean, type, getBeanManager().createCreationalContext(bean));
        return instance;
    }

    @Override
    public Object getInstance(String name)
    throws ResolutionException 
    {
        //X getBeanManager().getELResolver();
        // TODO implement
        return null;
    }

}
