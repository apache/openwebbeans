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

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.Session;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;


import org.apache.webbeans.exception.WebBeansException;
import org.apache.webbeans.jms.JMSModel;
import org.apache.webbeans.jms.JMSModel.JMSType;
import org.apache.webbeans.jms.component.JmsComponentImpl;

import javassist.util.proxy.MethodHandler;

public class JmsProxyHandler implements MethodHandler
{
    private JmsComponentImpl<?> jmsComponent = null;
    
    private static ConnectionFactory connectionFactory = null;
    
    private AtomicBoolean cfSet = new AtomicBoolean(false);
    
    private static Map<JMSType,Connection> connections = new ConcurrentHashMap<JMSType, Connection>();
    
    private static Map<String,Topic> topics = new ConcurrentHashMap<String,Topic>();
    
    private static Map<String,Queue> queues = new ConcurrentHashMap<String,Queue>();
    
    public JmsProxyHandler(JmsComponentImpl<?> jmsComponent)
    {
        this.jmsComponent = jmsComponent;
    }

    public Object invoke(Object instance, Method method, Method proceed, Object[] arguments) throws Exception
    {
        Object cf = createOrReturnConnectionFactory();
        
        if(cf == null)
        {
            cf = createOrReturnQueueOrTopicConnection();
        }
        
        
        if(cf == null)
        {
            cf = createOrReturnQueueOrTopic();
            
        }        
                
        if(cf == null)
        {
            cf = createSession();
        }
        
        if(cf == null)
        {
            cf = createMessageProducers();
        }
        
        if(cf == null)
        {
            cf = createMessageConsumers();
        }
                
        if(method.getName().equals("close"))
        {
            throw new UnsupportedOperationException("close method is not supported for JMS resources");
        }
        
        this.jmsComponent.setJmsObject(cf);
        
        return method.invoke(cf, arguments);
    }

    private Object createOrReturnConnectionFactory()
    {
        if(ConnectionFactory.class.isAssignableFrom(jmsComponent.getJmsClass()))
        {
            if(connectionFactory != null)
            {
                return connectionFactory;
            }
            else
            {
                if(cfSet.compareAndSet(false, true))
                {
                    connectionFactory = JmsUtil.getConnectionFactory();
                    
                    return connectionFactory;
                }
            }
        }
        
        return null;
    }
    
    private Session createSession()
    {
        try
        {
            if(Session.class.isAssignableFrom(jmsComponent.getJmsClass()))
            {
                Connection connection = createOrReturnQueueOrTopicConnection();
                
                return connection.createSession(false , Session.AUTO_ACKNOWLEDGE);
            }
            
            
        }catch(JMSException e)
        {
            throw new WebBeansException("Unable to create jms session",e);
        }
                
        return null;
    }
   
    private MessageProducer createMessageProducers()
    {
        try
        {
            if(MessageProducer.class.isAssignableFrom(jmsComponent.getJmsClass()))
            {
                return createSession().createProducer(createOrReturnQueueOrTopic());   
            }
        }
        catch (JMSException e)
        {
            throw new WebBeansException("Unable to create jms message producer",e);
        }
        
        return null;
    }
    
    private MessageConsumer createMessageConsumers()
    {
        try
        {
            if(MessageConsumer.class.isAssignableFrom(jmsComponent.getJmsClass()))
            {
                return createSession().createConsumer(createOrReturnQueueOrTopic());   
            }
        }
        catch (JMSException e)
        {
            throw new WebBeansException("Unable to create jms message producer",e);
        }
        
        return null;
    }
    
    
    private Connection createOrReturnQueueOrTopicConnection()
    {
        JMSModel jmsModel = this.jmsComponent.getJmsModel();
        
        try
        {
            if(Connection.class.isAssignableFrom(jmsComponent.getJmsClass()))
            {
                if(jmsModel.getJmsType().equals(JMSType.QUEUE))
                {
                    if(connections.containsKey(JMSType.QUEUE))
                    {
                        return connections.get(JMSType.QUEUE);
                    }
                    else
                    {
                        QueueConnectionFactory ccf = (QueueConnectionFactory)connectionFactory;
                        QueueConnection qc = ccf.createQueueConnection();
                        connections.put(JMSType.QUEUE, qc);
                        
                        return qc;
                    }
                }
                else if(jmsModel.getJmsType().equals(JMSType.TOPIC))
                {
                    if(connections.containsKey(JMSType.TOPIC))
                    {
                        return connections.get(JMSType.TOPIC);
                    }
                    else
                    {
                        TopicConnectionFactory ccf = (TopicConnectionFactory)connectionFactory;
                        TopicConnection qc = ccf.createTopicConnection();
                        connections.put(JMSType.TOPIC, qc);
                        
                        return qc;
                    }
                    
                }
            }
            
            
        }catch(JMSException e)
        {
            throw new WebBeansException("Unable to create jms connection",e);
        }
        
         
        return null;
    }
    
    private Destination createOrReturnQueueOrTopic()
    {
        JMSModel jmsModel = this.jmsComponent.getJmsModel();
        String jndiName = jmsModel.isJndiNameDefined() ? jmsModel.getJndiName() : jmsModel.getMappedName();
        
        if(Topic.class.isAssignableFrom(jmsComponent.getJmsClass()))
        {
                        
            if(topics.get(jndiName) != null)
            {
                return topics.get(jndiName);
            }
                        
            Topic res = (Topic)JmsUtil.getInstanceFromJndi(this.jmsComponent.getJmsModel(), this.jmsComponent.getJmsClass());
            
            topics.put(jndiName , res);
            
            return res;
        }
        
        else if(Queue.class.isAssignableFrom(jmsComponent.getJmsClass()))
        {
                        
            if(queues.get(jndiName) != null)
            {
                return queues.get(jndiName);
            }
                        
            Queue res = (Queue)JmsUtil.getInstanceFromJndi(this.jmsComponent.getJmsModel(), this.jmsComponent.getJmsClass());
            
            queues.put(jndiName , res);
            
            return res;
        }
        
        
        return null;
    }
    
    public static void clearConnections()
    {
        try
        {
            connectionFactory = null;
            
            for(Connection connection : connections.values())
            {
                connection.close();
            }        
            
            connections = null;
            
            topics.clear();
            queues.clear();
            
            topics = null;
            queues = null;
            
        }catch(Exception e)
        {
            throw new WebBeansException(e);
        }
    }
    
}
