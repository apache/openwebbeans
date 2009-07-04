/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.webbeans.jms.component;

import java.lang.reflect.Method;

import javax.enterprise.context.spi.CreationalContext;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;

import org.apache.webbeans.component.AbstractBean;
import org.apache.webbeans.component.WebBeansType;
import org.apache.webbeans.exception.WebBeansException;
import org.apache.webbeans.jms.JMSModel;
import org.apache.webbeans.jms.util.Closable;
import org.apache.webbeans.jms.util.JmsUtil;

public class JmsComponentImpl<T> extends AbstractBean<T> 
{
    private JMSModel jmsModel = null;
    
    private Class<T> jmsClass = null;
        
    JmsComponentImpl(JMSModel jmsModel, Class<T> jmsClass)
    {
        super(WebBeansType.JMS);
        
        this.jmsModel = jmsModel;
        this.jmsClass = jmsClass;
    }

    @Override
    protected T createInstance(CreationalContext<T> creationalContext)
    {
        T jmsProxyInstance = JmsUtil.createNewJmsProxy(this);
         
        return jmsProxyInstance;
    }

    @Override
    protected void destroyInstance(T instance)
    {        
        if(Session.class.isAssignableFrom(jmsClass) ||
                MessageConsumer.class.isAssignableFrom(jmsClass) ||
                MessageProducer.class.isAssignableFrom(jmsClass))
        {
            try
            {
                if(instance != null)
                {
                    Method method = Closable.class.getMethod("closeJMSObject", new Class[]{});
                    
                    method.invoke(instance, new Object[]{});                    
                }
            }
            
            catch (Exception e)
            {
                throw new WebBeansException("Unable to destroy instance " + this.toString() ,e);
            }
            
        }
    }
    

    public Class<T> getJmsClass()
    {
        return this.jmsClass;
    }
    
    public JMSModel getJmsModel()
    {
        return this.jmsModel;
    }
}
