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
package org.apache.webbeans.jms.component;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

import jakarta.jms.Queue;
import jakarta.jms.QueueConnection;
import jakarta.jms.QueueReceiver;
import jakarta.jms.QueueSender;
import jakarta.jms.QueueSession;
import jakarta.jms.Topic;
import jakarta.jms.TopicConnection;
import jakarta.jms.TopicPublisher;
import jakarta.jms.TopicSession;
import jakarta.jms.TopicSubscriber;

import org.apache.webbeans.component.BeanAttributesImpl;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.jms.JMSModel;
import org.apache.webbeans.jms.JMSModel.JMSType;
import org.apache.webbeans.util.Asserts;

public final class JmsComponentFactory
{
    private static JmsComponentFactory instance = new JmsComponentFactory();

    private JmsComponentFactory()
    {
        
    }
    
    public static JmsComponentFactory getJmsComponentFactory()
    {
        return instance;
    }
    
    public <T> JmsBean<T> getJmsComponent(WebBeansContext webBeansContext, JMSModel model)
    {
        Asserts.assertNotNull(model,"model parameter");
        
        Set<Type> apiTypes = new HashSet<>();
        Set<Annotation> qualifiers = new HashSet<>();
        
        if(model.getJmsType() == JMSType.QUEUE)
        {
            apiTypes.add(Queue.class);
            apiTypes.add(QueueConnection.class);
            apiTypes.add(QueueSession.class);
            apiTypes.add(QueueSender.class);
            apiTypes.add(QueueReceiver.class);
        }
        else
        {
            apiTypes.add(Topic.class);
            apiTypes.add(TopicConnection.class);
            apiTypes.add(TopicSession.class);
            apiTypes.add(TopicPublisher.class);
            apiTypes.add(TopicSubscriber.class);
        }
        
        Annotation[] anns = model.getBindings();
        
        for(Annotation a : anns)
        {
            qualifiers.add(a);
        }
        
        return new JmsBean<>(webBeansContext, model, new BeanAttributesImpl<>(apiTypes, qualifiers));
    }
}
