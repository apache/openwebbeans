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
package org.apache.webbeans.jms.util;

import java.io.Serializable;

import javassist.util.proxy.ProxyFactory;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueReceiver;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicPublisher;
import javax.jms.TopicSession;
import javax.jms.TopicSubscriber;

import org.apache.webbeans.exception.WebBeansCreationException;
import org.apache.webbeans.exception.WebBeansException;
import org.apache.webbeans.jms.JMSModel;
import org.apache.webbeans.jms.component.JmsComponentImpl;
import org.apache.webbeans.spi.JNDIService;
import org.apache.webbeans.spi.ServiceLoader;
import org.apache.webbeans.util.Asserts;

public final class JmsUtil
{
    private JmsUtil()
    {
        
    }

    public static boolean isJmsResourceClass(Class<?> clazz)
    {
        Asserts.assertNotNull(clazz,"clazz parameter can not be null");
        
        if(ConnectionFactory.class.isAssignableFrom(clazz) ||
                Connection.class.isAssignableFrom(clazz) || 
                Queue.class.isAssignableFrom(clazz) || 
                Topic.class.isAssignableFrom(clazz) || 
                MessageProducer.class.isAssignableFrom(clazz) ||
                MessageConsumer.class.isAssignableFrom(clazz) ||
                Session.class.isAssignableFrom(clazz))
        {
            return true;
        }
        
        return false;
        
    }
    
    public static boolean isJmsQueueTypeResource(Class<?> clazz)
    {
        if(QueueConnectionFactory.class.isAssignableFrom(clazz) ||
                QueueConnection.class.isAssignableFrom(clazz) ||                 
                QueueSender.class.isAssignableFrom(clazz) ||
                QueueReceiver.class.isAssignableFrom(clazz) ||
                QueueSession.class.isAssignableFrom(clazz))
        {
            return true;
        }
        
        return false;
    }
    
    public static boolean isJmsQueueResource(Class<?> clazz)
    {
        if(Queue.class.isAssignableFrom(clazz))
        {
            return true;
        }
        
        return false;
    }
    
    public static boolean isJmsTopicResource(Class<?> clazz)
    {
        if(Topic.class.isAssignableFrom(clazz))
        {
            return true;
        }
        
        return false;
    }
    
    
    public static boolean isJmsTopicTypeResource(Class<?> clazz)
    {
        if(TopicConnectionFactory.class.isAssignableFrom(clazz) ||
                TopicConnection.class.isAssignableFrom(clazz) ||   
                TopicSubscriber.class.isAssignableFrom(clazz) ||
                TopicPublisher.class.isAssignableFrom(clazz) ||
                TopicSession.class.isAssignableFrom(clazz))
        {
            return true;
        }
        
        return false;
    }
    
    private static JNDIService getJNDIService()
    {
       JNDIService jndiService = ServiceLoader.getService(JNDIService.class);
        
        if(jndiService == null)
        {
            throw new WebBeansCreationException("JNDI service is not available");            
        }
        
        return jndiService;
    }
    
    public static ConnectionFactory getConnectionFactory()
    {
        return getJNDIService().getObject("ConnectionFactory", ConnectionFactory.class);
    }
    
    public static <T> T getInstanceFromJndi(JMSModel jmsModel, Class<T> jmsClass)
    {
        String jndiName = jmsModel.isJndiNameDefined() ? jmsModel.getJndiName() : jmsModel.getMappedName();
        
         
        T instance = getJNDIService().getObject(jndiName, jmsClass);
        
        return instance;
        
    }
    
    @SuppressWarnings("unchecked")
    public static <T> T createNewJmsProxy(JmsComponentImpl<T> jmsComponent)
    {
        T result = null;

        try
        {
            ProxyFactory pf = new ProxyFactory();
            pf.setInterfaces(new Class<?>[] {Closable.class,
                    Serializable.class,
                    QueueConnection.class,
                    Queue.class,
                    QueueSender.class,
                    QueueSession.class,
                    Topic.class,
                    TopicConnection.class,
                    TopicSession.class,
                    TopicPublisher.class});
            
            pf.setHandler(new JmsProxyHandler(jmsComponent));

            result = (T)pf.createClass().newInstance();

        }
        catch (Exception e)
        {
            throw new WebBeansException(e);
        }

        return result;
    }
    
    
 
}
