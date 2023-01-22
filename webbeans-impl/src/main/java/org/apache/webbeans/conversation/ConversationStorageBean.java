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
package org.apache.webbeans.conversation;

import jakarta.enterprise.context.SessionScoped;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.enterprise.inject.spi.PassivationCapable;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.context.ConversationContext;


/**
 * Bean used to create the map of conversations in a session
 */
public class ConversationStorageBean implements Bean<Set<ConversationContext>>, PassivationCapable, Serializable
{
    public static final String OWB_INTERNAL_CONVERSATION_STORAGE_BEAN_PASSIVATION_ID = "apache.openwebbeans.OwbInternalConversationStorageBean";
    private final transient WebBeansContext webBeansContext;

    public ConversationStorageBean()
    {
        webBeansContext = WebBeansContext.currentInstance();
    }

    public ConversationStorageBean(WebBeansContext webBeansContext)
    {
        this.webBeansContext = webBeansContext;
    }

    @Override
    public Set<ConversationContext> create(CreationalContext<Set<ConversationContext>> creationalContext)
    {
        return Collections.newSetFromMap(new ConcurrentHashMap<ConversationContext, Boolean>());
    }

    @Override
    public void destroy(Set<ConversationContext> instance, CreationalContext<Set<ConversationContext>> context)
    {
        if (instance == null || instance.isEmpty())
        {
            return;
        }

        ConversationManager conversationManager = webBeansContext.getConversationManager();
        for (ConversationContext conversationContext : instance)
        {
            conversationManager.destroyConversationContext(conversationContext);
        }
    }

    @Override
    public Set<InjectionPoint> getInjectionPoints()
    {
        return Collections.EMPTY_SET;
    }

    @Override
    public Class<?> getBeanClass()
    {
        return Set.class;
    }

    @Override
    public Set<Type> getTypes()
    {
        return Collections.EMPTY_SET; // this bean is only used manually
    }

    @Override
    public Set<Annotation> getQualifiers()
    {
        return Collections.EMPTY_SET; // this bean is only used manually
    }

    @Override
    public Class<? extends Annotation> getScope()
    {
        return SessionScoped.class;
    }

    @Override
    public String getName()
    {
        return OWB_INTERNAL_CONVERSATION_STORAGE_BEAN_PASSIVATION_ID;
    }

    @Override
    public Set<Class<? extends Annotation>> getStereotypes()
    {
        return null;
    }

    @Override
    public boolean isAlternative()
    {
        return false;
    }

    @Override
    public String getId()
    {
        return OWB_INTERNAL_CONVERSATION_STORAGE_BEAN_PASSIVATION_ID;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }

        if (o == null || getClass() != o.getClass())
        {
            return false;
        }
        ConversationStorageBean that = (ConversationStorageBean) o;

        return getId().equals(that.getId());
    }

    @Override
    public int hashCode()
    {
        return getId().hashCode();
    }


}
