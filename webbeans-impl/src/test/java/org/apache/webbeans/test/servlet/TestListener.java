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
package org.apache.webbeans.test.servlet;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.webbeans.test.containertests.ComponentResolutionByTypeTest;
import org.junit.Test;


/**
 * This test listener class is used for running the tests.
 * 
 * @author <a href="mailto:gurkanerdogdu@yahoo.com">Gurkan Erdogdu</a>
 * @since 1.0
 */
public class TestListener implements ServletContextListener
{
	private void init()
	{
		System.out.println("Initializing all of the test contexts");
		new ComponentResolutionByTypeTest();
		TestContext.initTests();
	}

	public void contextDestroyed(ServletContextEvent arg0)
	{
		TestContext.endAllTests(arg0.getServletContext());
	}

	
	@Test
	public void contextInitialized()
	{
		
	}
	
	public void contextInitialized(ServletContextEvent arg0)
	{
		init();
		TestContext.startAllTests(arg0.getServletContext());
	}

}
