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

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.webbeans.config.WebBeansFinder;
import org.apache.webbeans.util.Asserts;

public class SessionContextManager
{
    private Map<String, SessionContext> sessionContexts = null;

    public SessionContextManager()
    {

    }

    public static SessionContextManager getInstance()
    {
        SessionContextManager sessionContextManager = (SessionContextManager)WebBeansFinder.getSingletonInstance(WebBeansFinder.SINGLETON_SESSION_CONTEXT_MANAGER);
        sessionContextManager.sessionContexts = new ConcurrentHashMap<String, SessionContext>();
        
        return sessionContextManager;
    }

    public void addNewSessionContext(String sessionId, SessionContext context)
    {
        Asserts.assertNotNull(sessionId, "sessionId parameter can not be null");
        Asserts.assertNotNull(context, "context parameter can not be null");

        sessionContexts.put(sessionId, context);
    }

    public SessionContext getSessionContextWithSessionId(String sessionId)
    {
        Asserts.assertNotNull(sessionId, "sessionId parameter can not be null");

        return sessionContexts.get(sessionId);
    }

    public void destroySessionContextWithSessionId(String sessionId)
    {
        Set<String> keySet = sessionContexts.keySet();
        Iterator<String> it = keySet.iterator();

        while (it.hasNext())
        {
            String id = it.next();
            if (id.equals(sessionId))
            {
                it.remove();
            }
        }
    }

    public void destroyAllSessions()
    {
        sessionContexts.clear();
    }
}
