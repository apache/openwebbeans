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
package org.apache.webbeans.test.contexts.conversation;

import javax.enterprise.context.RequestScoped;

import junit.framework.Assert;
import org.apache.webbeans.config.OpenWebBeansConfiguration;
import org.apache.webbeans.test.AbstractUnitTest;
import org.junit.Test;

/**
 * test for CDI Conversations
 */
public class ConversationScopedTest extends AbstractUnitTest
{

    @Test
    public void testTransientConversation()
    {
        try
        {
            System.setProperty(OpenWebBeansConfiguration.APPLICATION_SUPPORTS_CONVERSATION, "true");
            startContainer(ConversationScopedBean.class);

            ConversationScopedBean instance = getInstance(ConversationScopedBean.class);
            instance.setValue("a");

            restartContext(RequestScoped.class);

            Assert.assertNull(instance.getValue());

        }
        finally
        {
            System.clearProperty(OpenWebBeansConfiguration.APPLICATION_SUPPORTS_CONVERSATION);
        }
    }


    @Test
    public void testConversationEvents()
    {
        try
        {
            ConversationScopedInitBean.gotStarted = false;
            EndConversationObserver.endConversationCalled = false;

            System.setProperty(OpenWebBeansConfiguration.APPLICATION_SUPPORTS_CONVERSATION, "true");
            startContainer(ConversationScopedInitBean.class, EndConversationObserver.class);

            ConversationScopedInitBean instance = getInstance(ConversationScopedInitBean.class);
            instance.ping();

            Assert.assertTrue(ConversationScopedInitBean.gotStarted);

            shutDownContainer();

            Assert.assertTrue(EndConversationObserver.endConversationCalled);
        }
        finally
        {
            System.clearProperty(OpenWebBeansConfiguration.APPLICATION_SUPPORTS_CONVERSATION);
        }
    }


}
