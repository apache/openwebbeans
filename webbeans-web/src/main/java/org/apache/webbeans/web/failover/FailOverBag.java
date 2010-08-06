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
package org.apache.webbeans.web.failover;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Map;

import javassist.util.proxy.ProxyObjectInputStream;
import javassist.util.proxy.ProxyObjectOutputStream;

import javax.enterprise.context.Conversation;
import javax.servlet.http.HttpSession;

import org.apache.webbeans.context.ConversationContext;
import org.apache.webbeans.context.SessionContext;
import org.apache.webbeans.conversation.ConversationManager;
import org.apache.webbeans.logger.WebBeansLogger;
import org.apache.webbeans.spi.FailOverService;
import org.apache.webbeans.web.context.SessionContextManager;

/**
 * 
 * The bag that collects all conversation, session owb bean instances.
 * 
 */
public class FailOverBag implements Serializable 
{
    /**
     * 
     */
    private static final long serialVersionUID = -6314819837009653189L;
    
    /**Logger instance*/
    private static final WebBeansLogger logger = WebBeansLogger.getLogger(DefaultOwbFailOverService.class);

    String sessionId;

    String owbFailoverJVMId;
    
    SessionContext sessionContext;
    
    Map<Conversation, ConversationContext> conversationContextMap;
    
    public FailOverBag()
    {
    }
    
    public FailOverBag(HttpSession session, FailOverService service) 
    {
        sessionId = session.getId();
        owbFailoverJVMId = service.getJVMId();
        updateOwbFailOverBag(session, service);
    }
    
    public void updateOwbFailOverBag(HttpSession session, FailOverService service) 
    {
        // get the session context
        SessionContextManager sessionManager = SessionContextManager.getInstance();
        sessionContext = sessionManager.getSessionContextWithSessionId(session.getId());

        // get all conversation contexts 
        ConversationManager conversationManager = ConversationManager.getInstance();
        conversationContextMap = conversationManager.getConversationMapWithSessionId(session.getId());
    }
    
    public void restore() 
    {
        try 
        {
            if (sessionContext != null) 
            {
                SessionContextManager sessionManager = SessionContextManager.getInstance();
                sessionManager.addNewSessionContext(sessionId, sessionContext);
                sessionContext.setActive(true);
            }
            if (conversationContextMap != null && !conversationContextMap.isEmpty())
            {
                ConversationManager conversationManager = ConversationManager.getInstance();
                java.util.Iterator<Conversation> it = conversationContextMap.keySet().iterator();
                while(it.hasNext()) 
                {
                    Conversation c = it.next();
                    ConversationContext cc = conversationContextMap.get(c);
                    conversationManager.addConversationContext(c, cc);
                }
            }
        } 
        catch (Exception e)
        {
            
        }
    }

    public String getSessionId() 
    {
        return this.sessionId;
    }
    
    public String getJVMId() 
    {
        return this.owbFailoverJVMId;
    }
    
    public static Object testSerializable(Object obj0) 
    {
        byte[] buf = getBytes(obj0);
        Object obj1 = getObject(buf);
        return obj1;
    }

    /**
     * Method getBytes
     * This method accepts an object and converts it to a byte array.
     *
     * @param obj Object instance
     * @return byte[]
     */
    private static byte[] getBytes(Object obj) 
    {

        ByteArrayOutputStream baos = null;
        ObjectOutputStream oos = null;
        byte[] buf = new byte[0];

        try 
        {
            baos = new ByteArrayOutputStream();
            oos = new ProxyObjectOutputStream(baos);
            oos.writeObject(obj);
            buf = baos.toByteArray();
    
            oos.close();
            baos.close();
        } 
        catch (Throwable e) 
        {
            e.printStackTrace();
        }

        return buf;
    }
    
    private static Object getObject(byte[] buf) 
    {
        try 
        {
            ByteArrayInputStream bais = new ByteArrayInputStream(buf);
            ObjectInputStream ois = new ProxyObjectInputStream(bais);
            Object obj = ois.readObject();
            ois.close();
            return obj;
        } 
        catch (Exception e) 
        {
            e.printStackTrace();
            return null;
        }
    }
}
