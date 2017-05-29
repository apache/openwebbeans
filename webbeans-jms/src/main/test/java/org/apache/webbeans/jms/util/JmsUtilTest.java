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

import static org.junit.Assert.*;

import javax.enterprise.context.BeforeDestroyed;
import javax.jms.ConnectionFactory;
import javax.jms.Topic;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicSubscriber;

import org.apache.webbeans.corespi.se.DefaultJndiService;
import org.apache.webbeans.exception.WebBeansException;
import org.apache.webbeans.jms.JMSModel;
import org.apache.xbean.asm5.commons.Method;
import org.junit.Test;

/**
 * Unit tests for class {@link JmsUtil org.apache.webbeans.jms.util.JmsUtil}.
 *
 * @author Michael Hausegger, hausegger.michael@googlemail.com
 * @date 29.05.2017
 * @see JmsUtil
 **/
public class JmsUtilTest {


	@Test
	public void testGetInstanceFromJndiReturningNull() {

		JMSModel.JMSType jMSModel_JMSType = JMSModel.JMSType.QUEUE;
		JMSModel jMSModel = new JMSModel(jMSModel_JMSType, "X{h=}lU%|w", "X{h=}lU%|w");
		Class<DefaultJndiService> clasz = DefaultJndiService.class;
		DefaultJndiService defaultJndiService = JmsUtil.getInstanceFromJndi(jMSModel, clasz);

		assertNull(defaultJndiService);
	}


	@Test
	public void testGetConnectionFactory() {

		ConnectionFactory connectionFactory = JmsUtil.getConnectionFactory();

		assertNull(connectionFactory);
	}


	@Test
	public void testGetInstanceFromJndiWithNonNull() {

		JMSModel.JMSType jMSModel_JMSType = JMSModel.JMSType.QUEUE;
		JMSModel jMSModel = new JMSModel(jMSModel_JMSType, null, null);
		Class<Object> clasz = Object.class;

		assertNull(JmsUtil.getInstanceFromJndi(jMSModel, clasz));
	}


	@Test
	public void testIsJmsTopicTypeResourceOne() {

		Class<Method> clasz = Method.class;
		boolean result = JmsUtil.isJmsTopicTypeResource(clasz);

		assertFalse(result);
	}


	@Test
	public void testIsJmsTopicTypeResourceTwo() {

		assertTrue(JmsUtil.isJmsTopicTypeResource(TopicConnectionFactory.class));

		assertTrue(JmsUtil.isJmsTopicTypeResource(TopicSubscriber.class));
	}


	@Test
	public void testIsJmsTopicResourceOne() {

		Class<Method> clasz = Method.class;
		boolean result = JmsUtil.isJmsTopicResource(clasz);

		assertFalse(result);
	}


	@Test
	public void testIsJmsTopicResourceTwo() {

		assertTrue(JmsUtil.isJmsTopicResource(Topic.class));
	}


	@Test
	public void testIsJmsQueueResource() {

		Class<Object> clasz = Object.class;
		boolean result = JmsUtil.isJmsQueueResource(clasz);

		assertFalse(result);
	}


	@Test
	public void testIsJmsQueueTypeResource() {

		Class<Object> clasz = Object.class;
		boolean result = JmsUtil.isJmsQueueTypeResource(clasz);

		assertFalse(result);
	}


	@Test
	public void testIsJmsResourceClass() {

		Class<BeforeDestroyed.Literal> clasz = BeforeDestroyed.Literal.class;
		boolean result = JmsUtil.isJmsResourceClass(clasz);

		assertFalse(result);
	}


	@Test(expected = WebBeansException.class)
	public void testCreateNewJmsProxyThrowsRuntimeException() {

		Class<Object> clasz = Object.class;

		JmsUtil.createNewJmsProxy(null, clasz);
	}
}