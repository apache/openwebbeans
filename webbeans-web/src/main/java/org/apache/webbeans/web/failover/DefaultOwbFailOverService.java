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

import java.util.UUID;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import javassist.util.proxy.ProxyObjectOutputStream;

import javax.enterprise.inject.spi.Bean;
import javax.servlet.http.HttpSession;

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.logger.WebBeansLogger;
import org.apache.webbeans.spi.FailOverService;
import org.apache.webbeans.util.WebBeansUtil;

public class DefaultOwbFailOverService implements FailOverService 
{
    /**Logger instance*/
    private static final WebBeansLogger logger = WebBeansLogger.getLogger(DefaultOwbFailOverService.class);

    private static final String OWB_FAILOVER_JVM_ID = 
        UUID.randomUUID().toString() + "_" + System.currentTimeMillis();
    
    private static final String OWB_FAILOVER_PROPERTY_NAME = 
        "org.apache.webbeans.web.failover"; 
    
    private static final String OWB_FAILOVER_IS_SUPPORT_FAILOVER = 
        "org.apache.webbeans.web.failover.issupportfailover";
    
    private static final String OWB_FAILOVER_IS_SUPPORT_PASSIVATE = 
        "org.apache.webbeans.web.failover.issupportpassivation";

    private static final String OWB_FAILOVER_RESOURCSES_SERIALIZATION_HANDLER =
        "org.apache.webbeans.web.failover.resources.serialization.handler.v10";
    
    boolean isSupportFailOver;
    
    boolean isSupportPassivation;

    SerializationHandlerV10 handler;
    
    ThreadLocal<Boolean> isForPassivation = new ThreadLocal<Boolean>();
    
    public DefaultOwbFailOverService () 
    {
        String value;
        value = WebBeansContext.getInstance().getOpenWebBeansConfiguration().
                getProperty(OWB_FAILOVER_IS_SUPPORT_FAILOVER);
        if (value != null && value.equalsIgnoreCase("true"))
        {
            isSupportFailOver = true;
        }

        value = WebBeansContext.getInstance().getOpenWebBeansConfiguration().
                getProperty(OWB_FAILOVER_IS_SUPPORT_PASSIVATE);
        if (value != null && value.equalsIgnoreCase("true"))
        {
            isSupportPassivation = true;
        }
        if (isSupportFailOver || isSupportPassivation)
        {
            WebBeansUtil.initProxyFactoryClassLoaderProvider();
            value = WebBeansContext.getInstance().getOpenWebBeansConfiguration().getProperty(OWB_FAILOVER_RESOURCSES_SERIALIZATION_HANDLER);
            try 
            {
                if (value != null) 
                {
                    handler = (SerializationHandlerV10) Class.forName(value).newInstance();
                }
            } 
            catch (Exception e) 
            {
                logger.error("DefaultOwbFailOverService could not instanciate: [{0}]", e, value);
            }
        }
        
        if (logger.wblWillLogDebug())
        {
            logger.debug("DefaultOwbFailOverService isSupportFailOver: [{0}]", String.valueOf(isSupportFailOver));
            logger.debug("DefaultOwbFailOverService isSupportPassivation: [{0}]", String.valueOf(isSupportPassivation));
        }
    }
    
    public String getJVMId() 
    {
        return OWB_FAILOVER_JVM_ID;
    }
    
    public String getFailOverAttributeName() 
    {
        return OWB_FAILOVER_PROPERTY_NAME;
    }
    
    public void sessionIsIdle(HttpSession session) 
    {
        if (session != null) 
        {
            FailOverBagWrapper bagWrapper = 
                (FailOverBagWrapper)session.getAttribute(getFailOverAttributeName());
            if (bagWrapper == null) 
            {
                bagWrapper = new FailOverBagWrapper(session, this);
            } 
            else 
            {
                bagWrapper.updateOwbFailOverBag(session, this);
            }
            // store the bag as an attribute of the session. So when the 
            // session is fail over to other jvm or local disk, the attribute
            // could also be serialized.
            session.setAttribute(getFailOverAttributeName(), bagWrapper);
        }
        isForPassivation.remove();
        isForPassivation.set(null);
    }
    
    public void sessionIsInUse(HttpSession session)
    {
        if (session != null) 
        {
            FailOverBagWrapper bagWrapper = 
                (FailOverBagWrapper)session.getAttribute(getFailOverAttributeName());
            if (bagWrapper != null)
            {
                bagWrapper.sessionIsInUse();
            }
        }
    }
    
    public void sessionWillPassivate(HttpSession session) 
    {
        FailOverBagWrapper bagWrapper = new FailOverBagWrapper(session, this);
        session.setAttribute(getFailOverAttributeName(), bagWrapper);
        isForPassivation.set(Boolean.TRUE);
    }
    
    public void restoreBeans(HttpSession session)
    {
        FailOverBagWrapper bagWrapper = 
            (FailOverBagWrapper)session.getAttribute(getFailOverAttributeName());
        if (bagWrapper != null) 
        {
            if (logger.wblWillLogDebug())
            {
                logger.debug("DefaultOwbFailOverService restoreBeans for session: [{0}]", session);
            }
            bagWrapper.restore();
            session.removeAttribute(getFailOverAttributeName());
        }
    }
    
    public boolean isSupportFailOver() 
    {
        return isSupportFailOver;
    }

    public boolean isSupportPassivation() 
    {
        return isSupportPassivation;
    }

    public void enableFailOverSupport(boolean flag) 
    {
        isSupportFailOver = flag;
    }

    public void enablePassivationSupport(boolean flag) 
    {
        isSupportPassivation = flag;
    }
    
    /**
     * Get object input stream. Note, the stream should support deserialize javassist objects.
     * 
     * @return custom object input stream.
     */
    public ObjectInputStream getObjectInputStream(InputStream in) throws IOException 
    {
        return new OwbProxyObjectInputStream(in);
    }
    
    /**
     * Get object output stream. Note, the stream should support deserialize javassist objects.
     * 
     * @return custom object output stream.
     */
    public ObjectOutputStream getObjectOutputStream(OutputStream out) throws IOException 
    {
        return new ProxyObjectOutputStream(out);
    }
    
    /**
     * Except the EJB remote stub, it is hard to handle other types of resources.
     * Here we delegate serialization/deserialization to the application provided
     * SerializationHandler.
     * 
     */
    public Object handleResource(
            Bean<?> bean,
            Object resourceObject,
            ObjectInput in,
            ObjectOutput out)
    {
        if (handler != null) 
        {
            return handler.handleResource(bean, resourceObject, in, out, 
                (Boolean.TRUE == isForPassivation.get()) ? 
                SerializationHandlerV10.TYPE_PASSIVATION : SerializationHandlerV10.TYPE_FAILOVER);
        }
        return NOT_HANDLED;
    }
}
