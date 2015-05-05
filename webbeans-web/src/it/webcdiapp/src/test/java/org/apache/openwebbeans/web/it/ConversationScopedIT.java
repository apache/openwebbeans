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
package org.apache.openwebbeans.web.it;

import javax.enterprise.context.NonexistentConversationException;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.Assert;
import org.junit.Test;

public class ConversationScopedIT extends OwbITBase
{

    @Test
    public void testStandardConversation() throws Exception
    {
        DefaultHttpClient client = new DefaultHttpClient();

        // GET http://localhost:8089/webbeanswebCdiApp/conversation/info etc

        ConversationInfo previousInfo;
        {
            String content = httpGet(client, "conversation/info", HttpServletResponse.SC_OK);
            ConversationInfo info = assertConversationInfo(content, "null", true, "empty", null);
            previousInfo = info;
        }

        {
            // once again, we like to make sure we really get different instances
            String content = httpGet(client, "conversation/info", HttpServletResponse.SC_OK);
            ConversationInfo info = assertConversationInfo(content, "null", true, "empty", null);
            Assert.assertTrue(!info.instanceHash.equals(previousInfo.instanceHash));
        }

        {
            // now we begin the transaction
            String content = httpGet(client, "conversation/begin", HttpServletResponse.SC_OK);
            ConversationInfo info = assertConversationInfo(content, null, false, "empty", null);
            Assert.assertTrue(!info.instanceHash.equals(previousInfo.instanceHash));
            Assert.assertTrue(!"null".equals(info.cid));
            previousInfo = info;
        }

        {
            // let's look what we got.
            String content = httpGet(client, "conversation/info?cid=" + previousInfo.cid, HttpServletResponse.SC_OK);
            ConversationInfo info = assertConversationInfo(content, previousInfo.cid, false, "empty", previousInfo.instanceHash);
            previousInfo = info;
        }

        {
            // now let's set a value
            String content = httpGet(client, "conversation/set?cid=" + previousInfo.cid + "&content=full", HttpServletResponse.SC_OK);
            ConversationInfo info = assertConversationInfo(content, previousInfo.cid, false, "full", previousInfo.instanceHash);
            previousInfo = info;
        }

        {
            // and look again
            String content = httpGet(client, "conversation/info?cid=" + previousInfo.cid, HttpServletResponse.SC_OK);
            ConversationInfo info = assertConversationInfo(content, previousInfo.cid, false, "full", previousInfo.instanceHash);
            previousInfo = info;
        }

        {
            // and end the conversation
            String content = httpGet(client, "conversation/end?cid=" + previousInfo.cid, HttpServletResponse.SC_OK);

            // we STILL should see 'full' and the old instance
            // as the ConversationContext only needs to destroyed at the END of the request!
            ConversationInfo info = assertConversationInfo(content, "null", true, "full", previousInfo.instanceHash);
            previousInfo = info;
        }

        {
            // the last request should result in a new ConversationScoped instance
            String content = httpGet(client, "conversation/info", HttpServletResponse.SC_OK);
            ConversationInfo info = assertConversationInfo(content, "null", true, "empty", null);
            Assert.assertTrue(!info.instanceHash.equals(previousInfo.instanceHash));
        }

    }



    @Test
    public void testSessionInvalidationDuringConversation() throws Exception
    {
        DefaultHttpClient client = new DefaultHttpClient();

        // GET http://localhost:8089/webbeanswebCdiApp/conversation/info etc

        ConversationInfo previousInfo;
        {
            String content = httpGet(client, "conversation/info", HttpServletResponse.SC_OK);
            ConversationInfo info = assertConversationInfo(content, "null", true, "empty", null);
            previousInfo = info;
        }

        {
            // once again, we like to make sure we really get different instances
            String content = httpGet(client, "conversation/info", HttpServletResponse.SC_OK);
            ConversationInfo info = assertConversationInfo(content, "null", true, "empty", null);
            Assert.assertTrue(!info.instanceHash.equals(previousInfo.instanceHash));
        }

        {
            // now we begin the transaction
            String content = httpGet(client, "conversation/begin", HttpServletResponse.SC_OK);
            ConversationInfo info = assertConversationInfo(content, null, false, "empty", null);
            Assert.assertTrue(!info.instanceHash.equals(previousInfo.instanceHash));
            Assert.assertTrue(!"null".equals(info.cid));
            previousInfo = info;
        }

        {
            // let's look what we got.
            String content = httpGet(client, "conversation/info?cid=" + previousInfo.cid, HttpServletResponse.SC_OK);
            ConversationInfo info = assertConversationInfo(content, previousInfo.cid, false, "empty", previousInfo.instanceHash);
            previousInfo = info;
        }

        {
            // and set a value
            String content = httpGet(client, "conversation/set?cid=" + previousInfo.cid + "&content=full", HttpServletResponse.SC_OK);
            ConversationInfo info = assertConversationInfo(content, previousInfo.cid, false, "full", previousInfo.instanceHash);
            previousInfo = info;
        }
        String oldCid = previousInfo.cid;

        {
            // and now we invalidate the session
            // For the first request we should STILL get the old values as per spec
            // the Conversation only gets destroyed at the end of the Request
            // But the Conversation got ended (now is transient) and the cid is null
            String content = httpGet(client, "conversation/invalidateSession?cid=" + previousInfo.cid, HttpServletResponse.SC_OK);
            ConversationInfo info = assertConversationInfo(content, "null", true, "full", previousInfo.instanceHash);
            previousInfo = info;
        }


        {
            // and look again with the same
            String content = httpGet(client, "conversation/info?cid=" + oldCid, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            Assert.assertTrue(content.contains(NonexistentConversationException.class.getName()));
        }

    }


    private ConversationInfo assertConversationInfo(String content, String expectedCid, boolean expectedIsTransient, String expectedValue, Object expectedInstanceHash)
    {
        Assert.assertNotNull(content);
        ConversationInfo info = new ConversationInfo(content.split("/"));

        if (expectedCid != null)
        {
            Assert.assertEquals(expectedCid, info.cid);
        }

        Assert.assertEquals(expectedIsTransient, info.isTransient);

        if (expectedValue != null)
        {
            Assert.assertEquals(expectedValue, info.content);
        }

        if (expectedInstanceHash != null)
        {
            Assert.assertEquals(expectedInstanceHash, info.instanceHash);
        }
        return info;
    }


    public static class ConversationInfo
    {
        public String cid;
        public boolean isTransient;
        public String content;
        public String instanceHash;

        public ConversationInfo(String[] info)
        {
            Assert.assertEquals(4, info.length);
            cid = info[0];
            isTransient = Boolean.parseBoolean(info[1]);
            content = info[2];
            instanceHash = info[3];

        }
    }
}
