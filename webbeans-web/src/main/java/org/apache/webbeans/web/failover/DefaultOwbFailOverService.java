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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import javax.servlet.http.HttpSession;

import org.apache.webbeans.config.OpenWebBeansConfiguration;
import org.apache.webbeans.logger.WebBeansLogger;
import org.apache.webbeans.spi.FailOverService;

public class DefaultOwbFailOverService implements FailOverService 
{
    /**Logger instance*/
    private static final WebBeansLogger logger = WebBeansLogger.getLogger(DefaultOwbFailOverService.class);

    private static final String OWB_FAILOVER_JVM_ID = 
        UUID.randomUUID().toString() + "_" + 
            String.valueOf(System.currentTimeMillis());
    
    private static final String OWB_FAILOVER_PROPERTY_NAME = 
        "org.apache.webbeans.web.failover"; 
    
    private static final String OWB_FAILOVER_IS_SUPPORT_FAILOVER = 
        "org.apache.webbeans.web.failover.issupportfailover";
    
    private static final String OWB_FAILOVER_IS_SUPPORT_PASSIVATE = 
        "org.apache.webbeans.web.failover.issupportpassivation";

    boolean isSupportFailOver;
    
    boolean isSupportPassivation;

    public DefaultOwbFailOverService () 
    {
        String value;
        value = OpenWebBeansConfiguration.getInstance().
            getProperty(OWB_FAILOVER_IS_SUPPORT_FAILOVER);
        if (value != null && value.equalsIgnoreCase("true"))
        {
            isSupportFailOver = true;
        }
        
        value = OpenWebBeansConfiguration.getInstance().
        getProperty(OWB_FAILOVER_IS_SUPPORT_PASSIVATE);
        if (value != null && value.equalsIgnoreCase("true"))
        {
            isSupportPassivation = true;
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
    
    @Override
    public void sessionWillPassivate(HttpSession session) 
    {
        FailOverBagWrapper bagWrapper = new FailOverBagWrapper(session, this);
        session.setAttribute(getFailOverAttributeName(), bagWrapper);
    }
    
    public void restoreBeans(HttpSession session)
    {
        FailOverBagWrapper bagWrapper = 
            (FailOverBagWrapper)session.getAttribute(getFailOverAttributeName());
        if (bagWrapper != null) 
        {
            logger.debug("DefaultOwbFailOverService restoreBeans for session: [{0}]", session);
            bagWrapper.restore();
            session.removeAttribute(getFailOverAttributeName());
        }
    }
    
    @Override
    public boolean isSupportFailOver() 
    {
        return isSupportFailOver;
    }

    @Override
    public boolean isSupportPassivation() 
    {
        return isSupportPassivation;
    }

    @Override
    public void enableFailOverSupport(boolean flag) 
    {
        isSupportFailOver = flag;
    }

    @Override
    public void enablePassivationSupport(boolean flag) 
    {
        isSupportPassivation = flag;
    }
    
    private static void verifyTest(FailOverBagWrapper bagWrapper) 
    {        //test code
        byte[] bytes = getBytes(bagWrapper);
        FailOverBagWrapper bagWrapper3 = (FailOverBagWrapper)getObject(bytes);
        System.out.println(bagWrapper3);
    }
    
    private static byte[] getBytes(Object obj) 
    {
        ByteArrayOutputStream baos = null;
        ObjectOutputStream oos = null;
        byte[] buf = new byte[0];

        try 
        {
            baos = new ByteArrayOutputStream();
            oos = new ObjectOutputStream(baos);
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
            ObjectInputStream ois = new ObjectInputStream(bais);
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
