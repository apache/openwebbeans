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
package org.apache.webbeans.jsf;

import static org.junit.Assert.*;

import org.apache.webbeans.config.WebBeansContext;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * Unit tests for class {@link JsfConversationService org.apache.webbeans.jsf.JsfConversationService}.
 *
 * @author Michael Hausegger, hausegger.michael@googlemail.com
 * @date 29.05.2017
 * @see JsfConversationService
 **/
@RunWith(PowerMockRunner.class)
@PrepareForTest(JSFUtil.class)
public class JsfConversationServiceTest {


	@Test(expected = NullPointerException.class)
	public void testGetConversationIdThrowsNullPointerException() {

		WebBeansContext webBeansContext = WebBeansContext.getInstance();
		JsfConversationService jsfConversationService = new JsfConversationService(webBeansContext);

		jsfConversationService.getConversationId();
	}


	@Test
	public void testGetConversationIdOne() {

		WebBeansContext webBeansContext = WebBeansContext.getInstance();
		JsfConversationService jsfConversationService = new JsfConversationService(webBeansContext);

		PowerMockito.mockStatic(JSFUtil.class);
		PowerMockito.when(JSFUtil.getConversationId()).thenReturn("a");

		assertEquals("a", jsfConversationService.getConversationId());
	}
}