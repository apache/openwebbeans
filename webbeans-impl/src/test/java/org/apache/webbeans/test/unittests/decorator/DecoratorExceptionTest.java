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
import java.util.HashSet;
import java.util.Set;

import javax.servlet.ServletContext;

import junit.framework.Assert;

import org.apache.webbeans.annotation.RequestedScopeLiteral;
import org.apache.webbeans.test.annotation.binding.DummyAnnotationLiteral;
import org.apache.webbeans.test.component.CheckWithCheckPaymentDecoratorField;
import org.apache.webbeans.test.component.IPayment;
import org.apache.webbeans.test.component.decorator.broken.DelegateAttributeIsnotInterface;
import org.apache.webbeans.test.component.decorator.broken.DelegateAttributeMustImplementAllDecoratedTypes;
import org.apache.webbeans.test.component.decorator.broken.MoreThanOneDelegateAttribute;
import org.apache.webbeans.test.component.decorator.broken.PaymentDecorator;
import org.apache.webbeans.test.servlet.TestContext;
import org.apache.webbeans.util.Asserts;
import org.junit.Before;
import org.junit.Test;

public class DecoratorExceptionTest extends TestContext
{
	public DecoratorExceptionTest()
	{
		super(DecoratorExceptionTest.class.getName());
	}

	@Before
	public void init()
	{
		super.init();
	}

	public void endTests(ServletContext ctx)
	{

	}

	public void startTests(ServletContext ctx)
	{

	}

	@Test
	public void testDelegateAttributeIsnotInterface()
	{
		Exception exc = null;
		try
		{
			defineSimpleWebBeansDecorators(DelegateAttributeIsnotInterface.class);
		} catch (Exception e)
		{
			System.out.println(e.getMessage());
			exc = e;
		}

		Asserts.assertNotNull(exc);
	}

	@Test
	public void testMoreThanOneDelegateAttribute()
	{
		Exception exc = null;
		try
		{
			defineSimpleWebBeansDecorators(MoreThanOneDelegateAttribute.class);
		} catch (Exception e)
		{
			System.out.println(e.getMessage());
			exc = e;
		}

		Asserts.assertNotNull(exc);

	}

	@Test
	public void testApplyToSimpleWebBeanFinal()
	{
		Exception excp = null;

		Assert.assertNull(excp);

	}

	@Test
	public void testApplyToSimpleWebBeanFinalMethodsDecoratorImplements()
	{
		Exception exc = null;

		try
		{
			defineSimpleWebBeansDecorators(PaymentDecorator.class);
			defineSimpleWebBean(CheckWithCheckPaymentDecoratorField.class);

		} catch (Exception e)
		{
			System.out.println(e.getMessage());
			exc = e;
		}

		Assert.assertNotNull(exc);

	}

	@Test
	public void testDelegateAttributeMustImplementAllDecoratedTypes()
	{
		Exception exc = null;
		try
		{
			defineSimpleWebBeansDecorators(DelegateAttributeMustImplementAllDecoratedTypes.class);
		} catch (Exception e)
		{
			System.out.println(e.getMessage());
			exc = e;
		}

		Asserts.assertNotNull(exc);

	}

	@Test
	public void testResolveDuplicateBindingParameterType()
	{
		Exception exc = null;
		try
		{

			Set<Class<?>> api = new HashSet<Class<?>>();
			api.add(IPayment.class);

			Annotation[] anns = new Annotation[2];
			anns[0] = new DummyAnnotationLiteral();
			anns[1] = new DummyAnnotationLiteral();

			getManager().resolveDecorators(api, anns);
		} catch (Exception e)
		{
			System.out.println(e.getMessage());
			exc = e;
		}

		Asserts.assertNotNull(exc);

	}

	@Test
	public void testResolveNonBindingTypeAnnotation()
	{
		Exception exc = null;
		try
		{

			Set<Class<?>> api = new HashSet<Class<?>>();
			api.add(IPayment.class);

			Annotation[] anns = new Annotation[2];
			anns[0] = new RequestedScopeLiteral();

			getManager().resolveDecorators(api, anns);
		} catch (Exception e)
		{
			System.out.println(e.getMessage());
			exc = e;
		}

		Asserts.assertNotNull(exc);
		
	}

	@Test
	public void testResolveApiTypesEmpty()
	{
		Exception exc = null;
		try
		{

			Set<Class<?>> api = new HashSet<Class<?>>();

			Annotation[] anns = new Annotation[2];
			anns[0] = new DummyAnnotationLiteral();

			getManager().resolveDecorators(api, anns);
		} catch (Exception e)
		{
			System.out.println(e.getMessage());
			exc = e;
		}

		Asserts.assertNotNull(exc);


	}

}
