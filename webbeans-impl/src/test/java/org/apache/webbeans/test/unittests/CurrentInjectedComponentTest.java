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
import org.apache.webbeans.test.component.ContaintsCurrentComponent;
import org.apache.webbeans.test.component.CurrentBindingComponent;
import org.apache.webbeans.test.component.service.ITyped2;
import org.apache.webbeans.test.component.service.Typed2;
import org.apache.webbeans.test.servlet.TestContext;
import org.junit.Before;
import org.junit.Test;


public class CurrentInjectedComponentTest extends TestContext
{
	public CurrentInjectedComponentTest()
	{
		super(CurrentInjectedComponentTest.class.getSimpleName());
	}

	public void endTests(ServletContext ctx)
	{
		
	}

	@Before
	public void init()
	{
		super.init();
	}

	public void startTests(ServletContext ctx)
	{
		
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testTypedComponent() throws Throwable
	{
		clear();
		
		defineSimpleWebBean(Typed2.class);
		defineSimpleWebBean(CurrentBindingComponent.class);
		defineSimpleWebBean(ContaintsCurrentComponent.class);
		List<AbstractComponent<?>> comps = getComponents();
		
		HttpSession session = getSession();
		ContextFactory.initRequestContext(null);
		ContextFactory.initSessionContext(session);
		
		Assert.assertEquals(3, comps.size());
		
		ManagerImpl.getManager().getContext(SessionScoped.class).get(comps.get(0), true);
		ManagerImpl.getManager().getContext(RequestScoped.class).get(comps.get(1), true);
		
		Object object = ManagerImpl.getManager().getContext(RequestScoped.class).get(comps.get(2), true);
		
		Assert.assertTrue(object instanceof ContaintsCurrentComponent);
		
		ContaintsCurrentComponent i = (ContaintsCurrentComponent)object;
		
		Assert.assertTrue(i.getInstance() instanceof CurrentBindingComponent);
		
		Object obj2 = ManagerImpl.getManager().getContext(RequestScoped.class).get(comps.get(1), false);
		
		Assert.assertEquals(i.getInstance(), obj2);
		
		CurrentBindingComponent bc = (CurrentBindingComponent)obj2;
		ITyped2 typed2 = bc.getTyped2();
		
		Assert.assertNotNull(typed2);
		
		ContextFactory.destroyRequestContext(null);
		ContextFactory.destroySessionContext(session);
 	}	

}
