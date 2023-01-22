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
package org.apache.webbeans.portable;

import java.util.Collections;

import jakarta.enterprise.context.ContextNotActiveException;
import jakarta.enterprise.context.ConversationScoped;
import jakarta.enterprise.context.spi.Context;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.InjectionPoint;

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.context.ConversationContext;
import org.apache.webbeans.context.creational.CreationalContextImpl;
import org.apache.webbeans.conversation.ConversationImpl;
import org.apache.webbeans.spi.ContextsService;

/**
 * Producer for {@code &#064;Inject Conversation;}
 */
public class ConversationProducer extends InjectionTargetImpl<ConversationImpl>
{

    private final ContextsService contextsService;


    public ConversationProducer(AnnotatedType<ConversationImpl> annotatedType, WebBeansContext webBeansContext)
    {
        super(annotatedType, Collections.<InjectionPoint>emptySet(), webBeansContext, null, null);
        this.contextsService = webBeansContext.getContextsService();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected ConversationImpl newInstance(CreationalContextImpl<ConversationImpl> creationalContext)
    {
        Context currentContext = contextsService.getCurrentContext(ConversationScoped.class);
        if (currentContext != null && currentContext instanceof ConversationContext)
        {
            return ((ConversationContext) currentContext).getConversation();
        }
        else
        {
            throw new ContextNotActiveException("WebBeans context with scope type annotation @ConversationScoped"
                    + " does not exist within current thread");
        }
    }
}
