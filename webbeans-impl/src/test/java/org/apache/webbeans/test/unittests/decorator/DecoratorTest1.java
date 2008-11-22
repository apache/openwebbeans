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
package org.apache.webbeans.test.unittests.decorator;

import java.lang.annotation.Annotation;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.webbeans.manager.Decorator;

import junit.framework.Assert;

import org.apache.webbeans.annotation.CurrentLiteral;
import org.apache.webbeans.component.AbstractComponent;
import org.apache.webbeans.context.ContextFactory;
import org.apache.webbeans.test.annotation.binding.Binding1Literal;
import org.apache.webbeans.test.component.decorator.clean.Account;
import org.apache.webbeans.test.component.decorator.clean.AccountComponent;
import org.apache.webbeans.test.component.decorator.clean.LargeTransactionDecorator;
import org.apache.webbeans.test.component.decorator.clean.ServiceDecorator;
import org.apache.webbeans.test.component.service.IService;
import org.apache.webbeans.test.component.service.ServiceImpl1;
import org.apache.webbeans.test.servlet.TestContext;
import org.junit.Before;
import org.junit.Test;

public class DecoratorTest1 extends TestContext
{

	public DecoratorTest1()
	{
		super(DecoratorTest1.class.getName());
	}

	public void endTests(ServletContext ctx)
	{
		
	}

	public void startTests(ServletContext ctx)
	{
		
	}
	
	@Before
	public void init()
	{
		super.init();
	}
	
	@Test
	public void test1()
	{
		defineSimpleWebBeansDecorators(ServiceDecorator.class);
		AbstractComponent<ServiceImpl1> component = defineSimpleWebBean(ServiceImpl1.class);
	
		ContextFactory.initRequestContext(null);
		ContextFactory.initApplicationContext(null);
		
		ServiceImpl1 serviceImpl = getManager().getInstance(component);
		String s = serviceImpl.service();
		
		Assert.assertEquals("ServiceImpl1", s);
		
		Set<Class<?>> apiTyeps = new HashSet<Class<?>>();
		apiTyeps.add(IService.class);
		
		List<Decorator> decs = getManager().resolveDecorators(apiTyeps, new Annotation[]{new Binding1Literal()});
		
		ServiceDecorator dec = (ServiceDecorator)getManager().getInstance(decs.get(0));
		Assert.assertEquals("ServiceImpl1", dec.getDelegateAttr());
		
	}
	
	@Test
	public void test2()
	{
		defineSimpleWebBeansDecorators(LargeTransactionDecorator.class);
		AbstractComponent<AccountComponent> component = defineSimpleWebBean(AccountComponent.class);
	
		ContextFactory.initRequestContext(null);
		
		AccountComponent account = getManager().getInstance(component);
		
		account.deposit(new BigDecimal(1500));
		account.withdraw(new BigDecimal(3000));
		
		
		Set<Class<?>> apiTyeps = new HashSet<Class<?>>();
		apiTyeps.add(Account.class);
		
		List<Decorator> decs = getManager().resolveDecorators(apiTyeps, new Annotation[]{new CurrentLiteral()});
		
		LargeTransactionDecorator dec = (LargeTransactionDecorator)getManager().getInstance(decs.get(0));
		Assert.assertEquals(new BigDecimal(1500), dec.getDepositeAmount());
		Assert.assertEquals(new BigDecimal(3000), dec.getWithDrawAmount());
		
	}
	
}
