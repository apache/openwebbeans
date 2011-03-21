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
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.Session;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;

import org.apache.webbeans.exception.WebBeansException;
import org.apache.webbeans.jms.JMSModel;
import org.apache.webbeans.jms.JMSModel.JMSType;
import org.apache.webbeans.jms.component.JmsBean;
import org.apache.webbeans.util.ClassUtil;

import javassist.util.proxy.MethodHandler;

public class JmsProxyHandler implements MethodHandler
{
    private JmsBean<?> jmsComponent = null;

    private static volatile ConnectionFactory connectionFactory = null;

    private AtomicBoolean cfSet = new AtomicBoolean(false);

    private static Map<JMSType, Connection> connections = new ConcurrentHashMap<JMSType, Connection>();

    private static Map<String, Destination> dests = new ConcurrentHashMap<String, Destination>();
    
    private Object jmsObject = null;
    
    private Class<?> injectionClazz = null;

    public JmsProxyHandler(JmsBean<?> jmsComponent, Class<?> injectionClazz)
    {
        this.jmsComponent = jmsComponent;
        this.injectionClazz = injectionClazz;
    }

    public Object invoke(Object instance, Method method, Method proceed, Object[] arguments) throws Exception
    {
        if(method.getName().equals("closeJMSObject"))
        {
            close();
            
            return null;
        }
        
        if (!ClassUtil.isObjectMethod(method.getName()))
        {            
            Object cf = null;

            if (this.jmsObject == null)
            {
                Class<?> jmsClazz = this.injectionClazz;

                if (cf == null && Connection.class.isAssignableFrom(jmsClazz))
                {
                    cf = createOrReturnQueueOrTopicConnection();
                }

                if (cf == null && Destination.class.isAssignableFrom(jmsClazz))
                {
                    cf = createOrReturnQueueOrTopic();

                }

                if (cf == null && Session.class.isAssignableFrom(jmsClazz))
                {
                    cf = createSession();
                }

                if (cf == null && MessageProducer.class.isAssignableFrom(jmsClazz))
                {
                    cf = createMessageProducers();
                }

                if (cf == null && MessageConsumer.class.isAssignableFrom(jmsClazz))
                {
                    cf = createMessageConsumers();
                }

                if (method.getName().equals("close"))
                {
                    throw new UnsupportedOperationException("close method is not supported for JMS resources");
                }

                if (cf == null)
                {
                    throw new WebBeansException("JMS Resource type is not correct!. Does not create JMS resource object to handle request");
                }

                this.jmsObject = cf;
            }
            else
            {
                cf = this.jmsObject;
            }

            return method.invoke(cf, arguments);
        }
        else
        {
            return proceed.invoke(instance, arguments);
        }
    }

    private Object createOrReturnConnectionFactory()
    {
        if (connectionFactory != null)
        {
            return connectionFactory;
        }
        else
        {
            if (cfSet.compareAndSet(false, true))
            {
                connectionFactory = JmsUtil.getConnectionFactory();

                return connectionFactory;
            }
        }

        return null;
    }

    private Session createSession()
    {
        try
        {

            Connection connection = createOrReturnQueueOrTopicConnection();

            return connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

        }
        catch (JMSException e)
        {
            throw new WebBeansException("Unable to create jms session", e);
        }

    }

    private MessageProducer createMessageProducers()
    {
        try
        {
            Connection connection = createOrReturnQueueOrTopicConnection();

            return connection.createSession(false, Session.AUTO_ACKNOWLEDGE).createProducer(createOrReturnQueueOrTopic());
        }
        catch (JMSException e)
        {
            throw new WebBeansException("Unable to create jms message producer", e);
        }

    }

    private MessageConsumer createMessageConsumers()
    {
        try
        {
            Connection connection = createOrReturnQueueOrTopicConnection();

            return connection.createSession(false, Session.AUTO_ACKNOWLEDGE).createConsumer(createOrReturnQueueOrTopic());
        }
        catch (JMSException e)
        {
            throw new WebBeansException("Unable to create jms message producer", e);
        }

    }

    private Connection createOrReturnQueueOrTopicConnection()
    {
        JMSModel jmsModel = this.jmsComponent.getJmsModel();

        try
        {
            if (jmsModel.getJmsType().equals(JMSType.QUEUE))
            {
                if (connections.containsKey(JMSType.QUEUE))
                {
                    return connections.get(JMSType.QUEUE);
                }
                else
                {
                    QueueConnectionFactory ccf = (QueueConnectionFactory) createOrReturnConnectionFactory();
                    QueueConnection qc = ccf.createQueueConnection();
                    connections.put(JMSType.QUEUE, qc);

                    return qc;
                }
            }
            else if (jmsModel.getJmsType().equals(JMSType.TOPIC))
            {
                if (connections.containsKey(JMSType.TOPIC))
                {
                    return connections.get(JMSType.TOPIC);
                }
                else
                {
                    TopicConnectionFactory ccf = (TopicConnectionFactory) createOrReturnConnectionFactory();
                    TopicConnection qc = ccf.createTopicConnection();
                    connections.put(JMSType.TOPIC, qc);

                    return qc;
                }

            }

        }
        catch (JMSException e)
        {
            throw new WebBeansException("Unable to create jms connection", e);
        }

        return null;
    }
    
    private void close()
    {
        try
        {
            if(this.jmsObject != null)
            {
                Method method = this.jmsObject.getClass().getMethod("close", new Class[]{});
                
                if(!method.isAccessible())
                {
                    jmsComponent.getWebBeansContext().getSecurityService().doPrivilegedSetAccessible(method, true);
                }
                
                method.invoke(this.jmsObject, new Object[]{});                
            }
            
        }
        catch (Exception e)
        {
            throw new WebBeansException("Unable to close JMS resources");
        }
        
    }

    private Destination createOrReturnQueueOrTopic()
    {
        JMSModel jmsModel = this.jmsComponent.getJmsModel();
        String jndiName = jmsModel.isJndiNameDefined() ? jmsModel.getJndiName() : jmsModel.getMappedName();

        if (dests.get(jndiName) != null)
        {
            return dests.get(jndiName);
        }

        Destination res = (Destination) JmsUtil.getInstanceFromJndi(this.jmsComponent.getJmsModel(), this.injectionClazz);

        dests.put(jndiName, res);

        return res;

    }

    public static void clearConnections()
    {
        try
        {
            connectionFactory = null;

            for (Connection connection : connections.values())
            {
                connection.close();
            }

            connections = null;

            dests.clear();
            dests = null;

        }
        catch (Exception e)
        {
            throw new WebBeansException(e);
        }
    }

}
