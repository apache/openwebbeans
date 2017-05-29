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
package org.apache.webbeans.web.tomcat7;

import static org.junit.Assert.*;

import javax.servlet.Servlet;

import org.apache.webbeans.spi.SecurityService;
import org.junit.Test;

/**
 * Unit tests for class {@link TomcatWebPlugin org.apache.webbeans.web.tomcat7.TomcatWebPlugin}.
 *
 * @author Michael Hausegger, hausegger.michael@googlemail.com
 * @date 29.05.2017
 * @see TomcatWebPlugin
 **/
public class TomcatWebPluginTest {

	@Test
	public void testSupportServiceReturnsTrue() throws Exception {

		TomcatWebPlugin tomcatWebPlugin = new TomcatWebPlugin();

		assertTrue( tomcatWebPlugin.supportService( SecurityService.class ) );

	}


	@Test
	public void testSupportsJavaEeComponentInjections() throws Exception {

		TomcatWebPlugin tomcatWebPlugin = new TomcatWebPlugin();

		assertTrue( tomcatWebPlugin.supportsJavaEeComponentInjections( Servlet.class ) );

	}


	@Test
	public void testGetSupportedService() throws Exception {

		TomcatWebPlugin tomcatWebPlugin = new TomcatWebPlugin();

		assertTrue( tomcatWebPlugin.getSupportedService( SecurityService.class ) instanceof TomcatSecurityService );

	}


	@Test
	public void testGetSupportedServiceReturnsNull() throws Exception {

		TomcatWebPlugin tomcatWebPlugin = new TomcatWebPlugin();

		assertNull( tomcatWebPlugin.getSupportedService( String.class ) );

	}


}