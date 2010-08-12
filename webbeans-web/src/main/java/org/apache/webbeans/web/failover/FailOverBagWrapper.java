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
import java.io.InputStream;
import java.io.ObjectStreamClass;
import java.io.Serializable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;

import javax.servlet.http.HttpSession;

import org.apache.webbeans.corespi.ServiceLoader;
import org.apache.webbeans.logger.WebBeansLogger;
import org.apache.webbeans.spi.FailOverService;

import javassist.util.proxy.ProxyObjectInputStream;
import javassist.util.proxy.ProxyObjectOutputStream;

/**
 * Use javassist Proxy streams to serialize and restore failover bean bag.
 * 
 */
public class FailOverBagWrapper implements Serializable, Externalizable 
{
    /**Logger instance*/
    private static final WebBeansLogger logger = 
            WebBeansLogger.getLogger(FailOverBagWrapper.class);

    private transient FailOverService failoverService;

    FailOverBag bag;

    String sessionId;

    boolean isSessionInUse;
 
    //do not remove, used by serialization. 
    public FailOverBagWrapper()
    {
        failoverService = (FailOverService)ServiceLoader.getService(FailOverService.class);

    }
    
    public FailOverBagWrapper(HttpSession session, FailOverService service) 
    {
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
            if (logger.wblWillLogDebug())
            {
                logger.debug(sessionId + " from " + bag.getJVMId() 
                        + "is restored successfully." );
            }
        } 
        else 
        {
            if (logger.wblWillLogDebug())
            {
                logger.debug("restore is skipped because isSessionInUse is true for session " + sessionId);
            }
        }
    }
    
    public synchronized void sessionIsInUse() 
    {
        isSessionInUse = true;
    }
    
    @Override
    public void readExternal(ObjectInput in) throws IOException,
            ClassNotFoundException 
    {
        isSessionInUse = in.readBoolean();
        sessionId = (String)in.readObject();
        if (!isSessionInUse) 
        {
            byte[] buf = (byte[])in.readObject();
            ByteArrayInputStream bais = new ByteArrayInputStream(buf);
            ObjectInputStream ois = new OwbProxyObjectInputStream(bais);
            bag = (FailOverBag) ois.readObject();
            ois.close();
        }
    }

    @Override
    public synchronized void writeExternal(ObjectOutput out) throws IOException 
    {
        out.writeBoolean(isSessionInUse);
        out.writeObject(sessionId);
        if (isSessionInUse)
        {
            if (logger.wblWillLogDebug())
            {
                logger.debug("writeExternal skip writing because session is in use for sessionid" + 
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
        try 
        {
            baos = new ByteArrayOutputStream();
            oos = new ProxyObjectOutputStream(baos);
            oos.writeObject(bag);
            oos.flush();
            buf = baos.toByteArray();
            oos.close();
            baos.close();
            out.writeObject(buf);
        } 
        catch (Throwable e) 
        {
            e.printStackTrace();
        }        
    }
    
    /**
     * A little wrapper class to correct the class loader.
     */
    public static class OwbProxyObjectInputStream extends ProxyObjectInputStream 
    {
        public OwbProxyObjectInputStream(InputStream in) throws IOException 
        {
            super(in);
        }
        
        protected Class<?> resolveClass(ObjectStreamClass desc)
        throws IOException, ClassNotFoundException
        {
            String name = desc.getName();
            try 
            {
                return Class.forName(name, false, 
                    Thread.currentThread().getContextClassLoader());
            } 
            catch (ClassNotFoundException ex) 
            {
                return super.resolveClass(desc);
            }
        }
    }

}
