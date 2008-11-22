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
package org.apache.webbeans.test.unittests;

import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.webbeans.RequestScoped;
import javax.webbeans.SessionScoped;

import junit.framework.Assert;

import org.apache.webbeans.component.AbstractComponent;
import org.apache.webbeans.container.ManagerImpl;
import org.apache.webbeans.context.ContextFactory;
import org.apache.webbeans.test.component.CheckWithCheckPayment;
import org.apache.webbeans.test.component.producer.ScopeAdaptorComponent;
import org.apache.webbeans.test.component.producer.ScopeAdaptorInjectorComponent;
import org.apache.webbeans.test.servlet.TestContext;
import org.junit.Before;
import org.junit.Test;


public class ScopeAdapterTest extends TestContext
{
	public ScopeAdapterTest()
	{
		super(ScopeAdapterTest.class.getName());
	}

	public void endTests(ServletContext ctx)
	{
		// TODO Auto-generated method stub
		
	}

	@Before
	public void init()
	{
		super.init();
		
	}

	public void startTests(ServletContext ctx)
	{
		// TODO Auto-generated method stub
		
	}
	
	@Test
	public void testDependent()
	{
		clear();
		
		defineSimpleWebBean(CheckWithCheckPayment.class);
		defineSimpleWebBean(ScopeAdaptorComponent.class);
		defineSimpleWebBean(ScopeAdaptorInjectorComponent.class);
	
		HttpSession session = getSession();
		ContextFactory.initRequestContext(null);
		ContextFactory.initSessionContext(session);
		ContextFactory.initApplicationContext(null);

		List<AbstractComponent<?>> comps = getComponents();
		
		Assert.assertEquals(4,getDeployedComponents());
		
		ManagerImpl.getManager().getContext(RequestScoped.class).get(comps.get(0), true);
		ManagerImpl.getManager().getContext(SessionScoped.class).get(comps.get(1), true);
		getInstanceByName("scope");
		ManagerImpl.getManager().getContext(SessionScoped.class).get(comps.get(2), true);
		
		ContextFactory.destroyApplicationContext(null);
		ContextFactory.destroySessionContext(session);
		ContextFactory.destroyRequestContext(null);
		
	}

}
