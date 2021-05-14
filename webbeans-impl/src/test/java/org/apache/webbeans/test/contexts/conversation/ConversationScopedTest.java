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

import javax.enterprise.context.ConversationScoped;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.spi.Context;

import org.junit.Assert;
import org.apache.webbeans.config.OpenWebBeansConfiguration;
import org.apache.webbeans.test.AbstractUnitTest;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * test for CDI Conversations
 */
public class ConversationScopedTest extends AbstractUnitTest
{

    @Test
    public void testTransientConversation() throws Exception
    {
        addConfiguration(OpenWebBeansConfiguration.APPLICATION_SUPPORTS_CONVERSATION, "true");
        startContainer(ConversationScopedBean.class);

        ConversationScopedBean instance = getInstance(ConversationScopedBean.class);
        instance.setValue("a");
        instance.begin();
        ensureSerialisableContext();

        restartContext(RequestScoped.class);


        instance.end();
        Assert.assertNull(instance.getValue());
    }


    @Test
    public void testConversationEvents() throws Exception
    {
        ConversationScopedInitBean.gotStarted = false;
        EndConversationObserver.endConversationCalled = false;

        addConfiguration(OpenWebBeansConfiguration.APPLICATION_SUPPORTS_CONVERSATION, "true");
        startContainer(ConversationScopedInitBean.class, EndConversationObserver.class);

        ConversationScopedInitBean instance = getInstance(ConversationScopedInitBean.class);
        instance.ping();

        Assert.assertTrue(ConversationScopedInitBean.gotStarted);

        ensureSerialisableContext();

        shutDownContainer();

        Assert.assertTrue(EndConversationObserver.endConversationCalled);
    }

    private void ensureSerialisableContext() throws IOException, ClassNotFoundException
    {
        Context context = getBeanManager().getContext(ConversationScoped.class);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(context);
        byte[] ba = baos.toByteArray();

        ByteArrayInputStream bais = new ByteArrayInputStream(ba);
        ObjectInputStream ois = new ObjectInputStream(bais);
        Context newContext =  (Context) ois.readObject();
        Assert.assertNotNull(newContext);
    }


}
