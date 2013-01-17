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
package org.apache.webbeans.component;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;

import javax.enterprise.context.Conversation;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.spi.CreationalContext;

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.conversation.ConversationImpl;
import org.apache.webbeans.spi.ConversationService;
import org.apache.webbeans.util.AnnotationUtil;
import org.apache.webbeans.util.CollectionUtil;

/**
 * Conversation bean implementation.
 * @version $Rev$ $Date$
 *
 */
public class ConversationBean extends InjectionTargetBean<Conversation>
{
    /**
     * Default constructor.
     * @param webBeansContext
     */
    public ConversationBean(WebBeansContext webBeansContext)
    {
        super(webBeansContext,
              WebBeansType.CONVERSATION,
              webBeansContext.getAnnotatedElementFactory().newAnnotatedType(Conversation.class),
              CollectionUtil.<Type>unmodifiableSet(Conversation.class, ConversationImpl.class, Object.class),
              AnnotationUtil.DEFAULT_AND_ANY_ANNOTATION,
              RequestScoped.class,
              "javax.enterprise.context.conversation",
              Conversation.class,
              Collections.<Class<? extends Annotation>>emptySet(),
              false);
        setEnabled(true);
    }
}
