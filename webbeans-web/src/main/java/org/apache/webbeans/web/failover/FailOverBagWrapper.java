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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionActivationListener;
import javax.servlet.http.HttpSessionEvent;

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.logger.WebBeansLoggerFacade;
import org.apache.webbeans.spi.FailOverService;

/**
 * Use javassist Proxy streams to serialize and restore failover bean bag.
 * 
 */
public class FailOverBagWrapper implements Serializable, Externalizable, HttpSessionActivationListener
{
    /**Logger instance*/
    protected  final Logger logger = WebBeansLoggerFacade.getLogger(FailOverBagWrapper.class);

    private transient FailOverService failoverService;

    protected FailOverBag bag;

    protected String sessionId;

    protected boolean isSessionInUse;
 
    //do not remove, used by serialization. 
    public FailOverBagWrapper()
    {
        failoverService = WebBeansContext.getInstance().getService(FailOverService.class);
    }
    
    public FailOverBagWrapper(HttpSession session, FailOverService service) 
    {
        failoverService = service;
        isSessionInUse = false;
        sessionId = session.getId();
        bag = new FailOverBag(session, service);
    }

    public void updateOwbFailOverBag(HttpSession session, FailOverService service) 
    {
        isSessionInUse = false;
        bag.updateOwbFailOverBag(session, service);
    }
    
    public void restore()
    {
        if (!isSessionInUse) 
        {
            bag.restore();
            if (logger.isLoggable(Level.FINE))
            {
                logger.log(Level.FINE, sessionId + " from " + bag.getJVMId()
                        + "is restored successfully.");
            }
        } 
        else 
        {
            if (logger.isLoggable(Level.FINE))
            {
                logger.log(Level.FINE, "restore is skipped because isSessionInUse is true for session " + sessionId);
            }
        }
    }
    
    public synchronized void sessionIsInUse() 
    {
        isSessionInUse = true;
    }
    
    public void readExternal(ObjectInput in) throws IOException,
            ClassNotFoundException 
    {
        isSessionInUse = in.readBoolean();
        sessionId = (String)in.readObject();
        if (!isSessionInUse) 
        {
            byte[] buf = (byte[])in.readObject();
            ByteArrayInputStream bais = new ByteArrayInputStream(buf);
            ObjectInputStream ois = failoverService.getObjectInputStream(bais);
            bag = (FailOverBag) ois.readObject();
            ois.close();
        }
    }

    public synchronized void writeExternal(ObjectOutput out) throws IOException 
    {
        out.writeBoolean(isSessionInUse);
        out.writeObject(sessionId);
        if (isSessionInUse)
        {
            if (logger.isLoggable(Level.FINE))
            {
                logger.log(Level.FINE, "writeExternal skip writing because session is in use for sessionid" +
                    sessionId);
            }
            return;
        }

        // We could not directly use java object stream since we are
        // using javassist. Serialize the bag by use javassist object
        // stream.
        ByteArrayOutputStream baos = null;
        ObjectOutputStream oos = null;
        byte[] buf = null;
        baos = new ByteArrayOutputStream();
        oos = failoverService.getObjectOutputStream(baos);
        oos.writeObject(bag);
        oos.flush();
        buf = baos.toByteArray();
        oos.close();
        baos.close();
        out.writeObject(buf);
    }
    
    public void sessionWillPassivate(HttpSessionEvent event)
    {
        if (failoverService != null && failoverService.isSupportPassivation())
        {
            HttpSession session = event.getSession();
            failoverService.sessionWillPassivate(session);
        }

    }

    public void sessionDidActivate(HttpSessionEvent event)
    {
        if (failoverService != null && (failoverService.isSupportFailOver() || failoverService.isSupportPassivation()))
        {
            HttpSession session = event.getSession();
            failoverService.restoreBeans(session);
        }
    }
}
