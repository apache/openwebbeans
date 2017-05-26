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
package org.apache.webbeans.test.decorators.tests;

import java.util.ArrayList;
import java.util.Collection;

import javax.enterprise.context.Conversation;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.util.AnnotationLiteral;

import org.junit.Assert;

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.conversation.DefaultConversationService;
import org.apache.webbeans.test.AbstractUnitTest;
import org.apache.webbeans.test.decorators.common.ConversationDecorator;
import org.apache.webbeans.spi.ConversationService;
import org.junit.Ignore;
import org.junit.Test;

@Ignore("Should it really be possible to decorate a Conversation?")
public class ConversationDecoratorTest extends AbstractUnitTest
{
    public static final String PACKAGE_NAME = ConversationDecoratorTest.class.getPackage().getName();

    
    @Test
    public void testConversationDecorator()
    {
        Collection<Class<?>> classes = new ArrayList<Class<?>>();
        classes.add(ConversationDecorator.class);
        
        Collection<String> xmls = new ArrayList<String>();
        xmls.add(getXmlPath(PACKAGE_NAME, "ConversationDecoratorTest"));
        
        startContainer(classes,xmls);
        WebBeansContext.getInstance().getOpenWebBeansConfiguration().setProperty("org.apache.webbeans.spi.ConversationService", DefaultConversationService.class.getName());

        Bean<?> bean = getBeanManager().getBeans(Conversation.class , new AnnotationLiteral<Default>(){}).iterator().next();
        Object instance = getBeanManager().getReference(bean, Conversation.class, getBeanManager().createCreationalContext(bean));


        Assert.assertTrue(instance instanceof Conversation);
        Conversation conversation = (Conversation)instance;
        conversation.begin();
        
        Assert.assertTrue(ConversationDecorator.CALLED);
        
        shutDownContainer();
    }
}
