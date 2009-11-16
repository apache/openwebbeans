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
package org.apache.webbeans.newtests.decorators.tests;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;

import javax.enterprise.context.Conversation;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.util.AnnotationLiteral;

import junit.framework.Assert;

import org.apache.webbeans.common.AbstractUnitTest;
import org.apache.webbeans.config.OpenWebBeansConfiguration;
import org.apache.webbeans.newtests.decorators.common.ConversationDecorator;
import org.apache.webbeans.spi.ServiceLoader;
import org.apache.webbeans.spi.conversation.ConversationService;
import org.junit.Test;

public class ConversationDecoratorTest extends AbstractUnitTest
{
    public static final String PACKAGE_NAME = ConversationDecoratorTest.class.getPackage().getName();
    
    public static class DummyConversationService implements ConversationService
    {

        @Override
        public String getConversationId()
        {
            return null;
        }

        @Override
        public String getConversationSessionId()
        {
            return null;
        }
        
    }
    
    @Test
    public void testConversationDecorator()
    {
        Collection<Class<?>> classes = new ArrayList<Class<?>>();
        classes.add(ConversationDecorator.class);
        
        Collection<URL> xmls = new ArrayList<URL>();
        xmls.add(getXMLUrl(PACKAGE_NAME, "ConversationDecoratorTest"));
        
        startContainer(classes,xmls);
        
        Bean<?> bean = getBeanManager().getBeans(Conversation.class , new AnnotationLiteral<Default>(){}).iterator().next();
        Object instance = getBeanManager().getReference(bean, Conversation.class, getBeanManager().createCreationalContext(bean));
        
        OpenWebBeansConfiguration.getInstance().setProperty("org.apache.webbeans.spi.conversation.ConversationService", DummyConversationService.class.getName());

        Assert.assertTrue(instance instanceof Conversation);
        Conversation conversation = (Conversation)instance;
        conversation.begin();
        
        Assert.assertTrue(ConversationDecorator.CALLED);
        
        shutDownContainer();
    }
}
