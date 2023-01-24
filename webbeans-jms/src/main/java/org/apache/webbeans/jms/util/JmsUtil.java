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
package org.apache.webbeans.jms.util;

import java.io.Serializable;

import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.MessageConsumer;
import jakarta.jms.MessageProducer;
import jakarta.jms.Queue;
import jakarta.jms.QueueConnection;
import jakarta.jms.QueueConnectionFactory;
import jakarta.jms.QueueReceiver;
import jakarta.jms.QueueSender;
import jakarta.jms.QueueSession;
import jakarta.jms.Session;
import jakarta.jms.Topic;
import jakarta.jms.TopicConnection;
import jakarta.jms.TopicConnectionFactory;
import jakarta.jms.TopicPublisher;
import jakarta.jms.TopicSession;
import jakarta.jms.TopicSubscriber;

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.exception.WebBeansCreationException;
import org.apache.webbeans.exception.WebBeansException;
import org.apache.webbeans.jms.JMSModel;
import org.apache.webbeans.jms.component.JmsBean;
import org.apache.webbeans.spi.JNDIService;
import org.apache.webbeans.util.Asserts;

public final class JmsUtil
{
    private JmsUtil()
    {
        
    }

    public static boolean isJmsResourceClass(Class<?> clazz)
    {
        Asserts.assertNotNull(clazz,"clazz parameter");

        return ConnectionFactory.class.isAssignableFrom(clazz) ||
                Connection.class.isAssignableFrom(clazz) ||
                Queue.class.isAssignableFrom(clazz) ||
                Topic.class.isAssignableFrom(clazz) ||
                MessageProducer.class.isAssignableFrom(clazz) ||
                MessageConsumer.class.isAssignableFrom(clazz) ||
                Session.class.isAssignableFrom(clazz);
    }
    
    public static boolean isJmsQueueTypeResource(Class<?> clazz)
    {
        return QueueConnectionFactory.class.isAssignableFrom(clazz) ||
                QueueConnection.class.isAssignableFrom(clazz) ||
                QueueSender.class.isAssignableFrom(clazz) ||
                QueueReceiver.class.isAssignableFrom(clazz) ||
                QueueSession.class.isAssignableFrom(clazz);
    }
    
    public static boolean isJmsQueueResource(Class<?> clazz)
    {
        return Queue.class.isAssignableFrom(clazz);
    }
    
    public static boolean isJmsTopicResource(Class<?> clazz)
    {
        return Topic.class.isAssignableFrom(clazz);
    }
    
    
    public static boolean isJmsTopicTypeResource(Class<?> clazz)
    {
        return TopicConnectionFactory.class.isAssignableFrom(clazz) ||
                TopicConnection.class.isAssignableFrom(clazz) ||
                TopicSubscriber.class.isAssignableFrom(clazz) ||
                TopicPublisher.class.isAssignableFrom(clazz) ||
                TopicSession.class.isAssignableFrom(clazz);
    }
    
    private static JNDIService getJNDIService()
    {
       JNDIService jndiService = WebBeansContext.getInstance().getService(JNDIService.class);
        
        if(jndiService == null)
        {
            throw new WebBeansCreationException("JNDI service is not available");            
        }
        
        return jndiService;
    }
    
    public static ConnectionFactory getConnectionFactory()
    {
        String connectionFactoryJndiName = WebBeansContext.getInstance().getOpenWebBeansConfiguration().getProperty(JNDIService.JMS_CONNECTION_FACTORY_JNDI_KEY);
        
        if(connectionFactoryJndiName == null || connectionFactoryJndiName.equals(""))
        {
            connectionFactoryJndiName = JNDIService.JMS_CONNECTION_FACTORY_JNDI_DEFAULT_VALUE;
        }
        
        return getJNDIService().lookup(connectionFactoryJndiName, ConnectionFactory.class);
    }
    
    public static <T> T getInstanceFromJndi(JMSModel jmsModel, Class<T> jmsClass)
    {
        String jndiName = jmsModel.isJndiNameDefined() ? jmsModel.getJndiName() : jmsModel.getMappedName();
        
         
        T instance = getJNDIService().lookup(jndiName, jmsClass);
        
        return instance;
        
    }
    
    /**
     * Gets jms related object.
     * @param jmsComponent jms bean
     * @param intf injection point class
     * @return proxy object
     */
    public static Object createNewJmsProxy(JmsBean<?> jmsComponent, Class<?> intf)
    {
        try
        {
            Class<?>[] interfaces = {Closable.class, Serializable.class, intf};

            //X TODO do we still need this?
            throw new WebBeansException("Support got temporarily removed while moving from Javassist to ASM");

        }
        catch (Exception e)
        {
            throw new WebBeansException(e);
        }
    }

}
