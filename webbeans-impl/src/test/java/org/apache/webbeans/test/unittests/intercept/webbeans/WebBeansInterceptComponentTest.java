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
package org.apache.webbeans.test.unittests.intercept.webbeans;

import java.util.List;

import javax.servlet.ServletContext;


import junit.framework.Assert;

import org.apache.webbeans.component.AbstractComponent;
import org.apache.webbeans.context.ContextFactory;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.test.component.intercept.webbeans.WInterceptorComponent;
import org.apache.webbeans.test.component.intercept.webbeans.WMetaInterceptorComponent;
import org.apache.webbeans.test.component.intercept.webbeans.WebBeansInterceptor;
import org.apache.webbeans.test.component.intercept.webbeans.WebBeanswithMetaInterceptor;
import org.apache.webbeans.test.servlet.TestContext;
import org.junit.Before;
import org.junit.Test;


public class WebBeansInterceptComponentTest extends TestContext
{
	boolean init = false;
	
	public WebBeansInterceptComponentTest()
	{
		super(WebBeansInterceptComponentTest.class.getName());
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
	
	@Test
	public void testInterceptedComponent()
	{
		WebBeansConfigurationException exc = null;
		
		try
		{
			defineSimpleWebBeanInterceptor(WebBeansInterceptor.class);
			defineSimpleWebBean(WInterceptorComponent.class);
			
		}catch(WebBeansConfigurationException e)
		{
			System.out.println(e.getMessage());
			exc = e;

		}
		
		Assert.assertNull(exc);
	}

	@Test
	public void testInterceptorCalls()
	{
		getComponents().clear();

		defineSimpleWebBeanInterceptor(WebBeansInterceptor.class);
		defineSimpleWebBean(WInterceptorComponent.class);
		
		ContextFactory.initRequestContext(null);
		List<AbstractComponent<?>> comps = getComponents();
		
		Object object = getManager().getInstance(comps.get(0));
		
		Assert.assertTrue(object instanceof WInterceptorComponent);
		
		WInterceptorComponent comp = (WInterceptorComponent)object;
		int s = comp.hello();
		
		Assert.assertEquals(5, s);
		
		ContextFactory.destroyRequestContext(null);
	}

	@Test
	public void testMetaInterceptorCalls()
	{
		getComponents().clear();
		
		defineSimpleWebBeanInterceptor(WebBeansInterceptor.class);
		defineSimpleWebBeanInterceptor(WebBeanswithMetaInterceptor.class);
		defineSimpleWebBean(WMetaInterceptorComponent.class);
		
		ContextFactory.initRequestContext(null);
		List<AbstractComponent<?>> comps = getComponents();
		
		Object object =getManager().getInstance(comps.get(0));
		
		Assert.assertTrue(object instanceof WMetaInterceptorComponent);
		
		WMetaInterceptorComponent comp = (WMetaInterceptorComponent)object;
		int s = comp.hello();
		
		Assert.assertEquals(5, s);
		
		s = comp.hello2();
		
		Assert.assertEquals(10, s);
		
		ContextFactory.destroyRequestContext(null);
	}

}
