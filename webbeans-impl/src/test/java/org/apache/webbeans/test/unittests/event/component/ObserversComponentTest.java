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
package org.apache.webbeans.test.unittests.event.component;

import java.lang.annotation.Annotation;

import javax.webbeans.AnnotationLiteral;


import junit.framework.Assert;

import org.apache.webbeans.annotation.CurrentLiteral;
import org.apache.webbeans.component.AbstractComponent;
import org.apache.webbeans.context.ContextFactory;
import org.apache.webbeans.test.annotation.binding.Check;
import org.apache.webbeans.test.annotation.binding.Role;
import org.apache.webbeans.test.component.CheckWithCheckPayment;
import org.apache.webbeans.test.component.CheckWithMoneyPayment;
import org.apache.webbeans.test.component.PaymentProcessorComponent;
import org.apache.webbeans.test.component.event.normal.ComponentWithObservable1;
import org.apache.webbeans.test.component.event.normal.ComponentWithObserves1;
import org.apache.webbeans.test.component.event.normal.ComponentWithObserves2;
import org.apache.webbeans.test.event.LoggedInEvent;
import org.apache.webbeans.test.servlet.TestContext;
import org.junit.Before;
import org.junit.Test;

public class ObserversComponentTest extends TestContext
{
	public ObserversComponentTest()
	{
		super(ObserversComponentTest.class.getName());
	}

	@Before
	public void init()
	{
		super.init();
	}
	
	@Test
	public void testObserves()
	{
		clear();
		
		AbstractComponent<ComponentWithObserves1> component = defineSimpleWebBean(ComponentWithObserves1.class);
		ContextFactory.initRequestContext(null);
		
		LoggedInEvent event = new LoggedInEvent("Gurkan");
		
		Annotation[] anns = new Annotation[1];
		anns[0] = new CurrentLiteral();
		
		getManager().fireEvent(event, anns);
		
		ComponentWithObserves1 instance = getManager().getInstance(component);
		
		Assert.assertEquals("Gurkan", instance.getUserName());
	}
	
	@Test
	public void testWithObservable()
	{
		clear();
		
		AbstractComponent<ComponentWithObserves1> component = defineSimpleWebBean(ComponentWithObserves1.class);
		AbstractComponent<ComponentWithObservable1> componentObservable = defineSimpleWebBean(ComponentWithObservable1.class);

		ContextFactory.initRequestContext(null);
		
		ComponentWithObserves1 instance = getManager().getInstance(component);
		ComponentWithObservable1 observable = getManager().getInstance(componentObservable);
		
		observable.afterLoggedIn();
		
		Assert.assertEquals("Gurkan", instance.getUserName());
	}
	
	@Test
	public void testObservesWithBindingMember()
	{
		clear();
		
		AbstractComponent<ComponentWithObserves1> component = defineSimpleWebBean(ComponentWithObserves1.class);
		ContextFactory.initRequestContext(null);
		
		LoggedInEvent event = new LoggedInEvent("Gurkan");
		
		class CheckLiteral extends AnnotationLiteral<Check> implements Check
		{

			public String type()
			{
				return "CHECK";
			}
			
		}
		
		Annotation[] anns = new Annotation[1];
		anns[0] = new CheckLiteral();
		
		getManager().fireEvent(event, anns);
		
		ComponentWithObserves1 instance = getManager().getInstance(component);
		
		Assert.assertNull(instance.getUserName());
		
		Assert.assertEquals("Gurkan", instance.getUserNameWithMember());
	}
	
	@Test
	public void testObservesWithBindingMember2()
	{
		clear();
		
		defineSimpleWebBean(CheckWithCheckPayment.class);
		defineSimpleWebBean(CheckWithMoneyPayment.class);
		defineSimpleWebBean(PaymentProcessorComponent.class);
		AbstractComponent<ComponentWithObserves2> component = defineSimpleWebBean(ComponentWithObserves2.class);
		ContextFactory.initRequestContext(null);
		
		LoggedInEvent event = new LoggedInEvent("USER");
		
		class RoleUser extends AnnotationLiteral<Role> implements Role
		{

			public String value()
			{
				return "USER";
			}
			
		}
		
		class RoleAdmin extends AnnotationLiteral<Role> implements Role
		{

			public String value()
			{
				return "ADMIN";
			}
			
		}
		
		Annotation[] anns = new Annotation[1];
		anns[0] = new RoleUser();
		
		getManager().fireEvent(event, anns);
		ComponentWithObserves2 instance = getManager().getInstance(component);
		
		Assert.assertNotNull(instance.getPayment());
		Assert.assertEquals("USER", instance.getUser());
		
		anns[0] = new RoleAdmin();
		event = new LoggedInEvent("ADMIN");
		
		getManager().fireEvent(event, anns);
		instance = getManager().getInstance(component);
		
		Assert.assertNotNull(instance.getPayment());
		Assert.assertEquals("ADMIN", instance.getUser());
		
	}
	
}
