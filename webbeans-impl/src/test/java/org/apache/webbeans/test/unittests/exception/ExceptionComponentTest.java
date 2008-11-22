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
package org.apache.webbeans.test.unittests.exception;

import javax.servlet.ServletContext;
import javax.webbeans.manager.Bean;

import junit.framework.Assert;

import org.apache.webbeans.component.AbstractComponent;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.test.component.exception.AroundInvokeWithFinalMethodComponent;
import org.apache.webbeans.test.component.exception.AroundInvokeWithSameMethodNameComponent;
import org.apache.webbeans.test.component.exception.AroundInvokeWithStaticMethodComponent;
import org.apache.webbeans.test.component.exception.AroundInvokeWithWrongReturnTypeComponent;
import org.apache.webbeans.test.component.exception.AroundInvokeWithoutExceptionComponent;
import org.apache.webbeans.test.component.exception.AroundInvokeWithoutParameterComponent;
import org.apache.webbeans.test.component.exception.AroundInvokeWithoutReturnTypeComponent;
import org.apache.webbeans.test.component.exception.ComponentTypeExceptionComponent;
import org.apache.webbeans.test.component.exception.FinalComponent;
import org.apache.webbeans.test.component.exception.HasFinalMethodComponent;
import org.apache.webbeans.test.component.exception.MoreThanOneAroundInvokeComponent;
import org.apache.webbeans.test.component.exception.MoreThanOneConstructureComponent;
import org.apache.webbeans.test.component.exception.MoreThanOneConstructureComponent2;
import org.apache.webbeans.test.component.exception.MoreThanOnePostConstructComponent;
import org.apache.webbeans.test.component.exception.MultipleDisposalMethodComponent;
import org.apache.webbeans.test.component.exception.NewComponentBindingComponent;
import org.apache.webbeans.test.component.exception.NewComponentInterfaceComponent;
import org.apache.webbeans.test.component.exception.NewMethodComponentBindingComponent;
import org.apache.webbeans.test.component.exception.NoConstructureComponent;
import org.apache.webbeans.test.component.exception.PostContructMethodHasCheckedExceptionComponent;
import org.apache.webbeans.test.component.exception.PostContructMethodHasParameterComponent;
import org.apache.webbeans.test.component.exception.PostContructMethodHasReturnTypeComponent;
import org.apache.webbeans.test.component.exception.PostContructMethodHasStaticComponent;
import org.apache.webbeans.test.component.exception.ProducerTypeExceptionComponent;
import org.apache.webbeans.test.component.exception.ProducerTypeStaticComponent;
import org.apache.webbeans.test.component.exception.InnerComponent.InnerInnerComponent;
import org.apache.webbeans.test.component.intercept.NoArgConstructorInterceptorComponent;
import org.apache.webbeans.test.servlet.TestContext;
import org.apache.webbeans.util.WebBeansUtil;
import org.junit.Before;
import org.junit.Test;


public class ExceptionComponentTest extends TestContext
{
	
	public ExceptionComponentTest()
	{
		super(ExceptionComponentTest.class.getName());
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
	public void testComponentTypeException()
	{
		WebBeansConfigurationException exc = null;
		
		try
		{
			defineSimpleWebBean(ComponentTypeExceptionComponent.class);
			
		}catch(WebBeansConfigurationException e)
		{
			System.out.println(e.getMessage());
			exc = e;

		}
		
		Assert.assertNotNull(exc);
	}
	
	@Test
	public void testProducerMethodComponentTypeException()
	{
		WebBeansConfigurationException exc = null;
		try
		{
			defineSimpleWebBean(ProducerTypeExceptionComponent.class);
			
		}catch(WebBeansConfigurationException e)
		{
			System.out.println(e.getMessage());		
			exc = e;
		}
		
		Assert.assertNotNull(exc);
		
		
	}
	
	@Test
	public void testFinal()
	{
		WebBeansConfigurationException exc = null;
		try
		{
			defineSimpleWebBean(FinalComponent.class);
			
		}catch(WebBeansConfigurationException e)
		{
			System.out.println(e.getMessage());		
			exc = e;
		}
		
		Assert.assertNotNull(exc);
		
	}
	
	@Test
	public void testAbstract()
	{
		WebBeansConfigurationException exc = null;
		try
		{
			defineSimpleWebBean(AbstractComponent.class);
			
		}catch(WebBeansConfigurationException e)
		{
			System.out.println(e.getMessage());		
			exc = e;
		}
		
		Assert.assertNotNull(exc);
		
	}
	
	@Test
	public void testInner()
	{
		WebBeansConfigurationException exc = null;
		try
		{
			defineSimpleWebBean(InnerInnerComponent.class);
			
		}catch(WebBeansConfigurationException e)
		{
			System.out.println(e.getMessage());		
			exc = e;
		}
		
		Assert.assertNotNull(exc);
		
	}
	
	@Test
	public void testHasFinalMethod()
	{
		WebBeansConfigurationException exc = null;
		try
		{
			defineSimpleWebBean(HasFinalMethodComponent.class);
			
		}catch(WebBeansConfigurationException e)
		{
			System.out.println(e.getMessage());		
			exc = e;
		}
		
		Assert.assertNotNull(exc);
		
	}
	
	@Test
	public void constructorTest()
	{
		WebBeansConfigurationException exc = null;
		try
		{
			defineSimpleWebBean(MoreThanOneConstructureComponent.class);
			
		}catch(WebBeansConfigurationException e)
		{
			System.out.println(e.getMessage());		
			exc = e;
		}
		
		Assert.assertNotNull(exc);
		exc = null;
		
		try
		{
			defineSimpleWebBean(MoreThanOneConstructureComponent2.class);
			
		}catch(WebBeansConfigurationException e)
		{
			System.out.println(e.getMessage());		
			exc = e;
		}
		Assert.assertNotNull(exc);
		exc = null;
		
		try
		{
			defineSimpleWebBean(NoConstructureComponent.class);
			
		}catch(WebBeansConfigurationException e)
		{
			System.out.println(e.getMessage());		
			exc = e;
		}
		Assert.assertNull(exc);		
	}
	

	@Test
	public void testStaticProducerMethod()
	{
		WebBeansConfigurationException exc = null;
		try
		{
			defineSimpleWebBean(ProducerTypeStaticComponent.class);
			
		}catch(WebBeansConfigurationException e)
		{
			System.out.println(e.getMessage());		
			exc = e;
		}
		
		Assert.assertNotNull(exc);
		
	}
	
	@Test
	public void testDisposeMethod()
	{
		WebBeansConfigurationException exc = null;
		try
		{
			defineSimpleWebBean(MultipleDisposalMethodComponent.class);
			
		}catch(WebBeansConfigurationException e)
		{
			System.out.println(e.getMessage());		
			exc = e;
		}
		
		Assert.assertNotNull(exc);
		
	}
	

	@Test
	public void testNewInterface()
	{
		WebBeansConfigurationException exc = null;
		try
		{
			defineSimpleWebBean(NewComponentInterfaceComponent.class);
			
		}catch(WebBeansConfigurationException e)
		{
			System.out.println(e.getMessage());		
			exc = e;
		}
		
		Assert.assertNotNull(exc);
		
	}
	@Test
	public void testNewBinding()
	{
		WebBeansConfigurationException exc = null;
		try
		{
			defineSimpleWebBean(NewComponentBindingComponent.class);
			
		}catch(WebBeansConfigurationException e)
		{
			System.out.println(e.getMessage());		
			exc = e;
		}
		
		Assert.assertNotNull(exc);
		
	}
	
	@Test
	public void testNewMethod()
	{
		WebBeansConfigurationException exc = null;
		try
		{
			defineSimpleWebBean(NewMethodComponentBindingComponent.class);
			
		}catch(WebBeansConfigurationException e)
		{
			System.out.println(e.getMessage());		
			exc = e;
		}
		
		Assert.assertNotNull(exc);
		
	}
	
	@Test
	public void testMoreThanOnePostConstruct()
	{
		WebBeansConfigurationException exc = null;
		try
		{
			defineSimpleWebBean(MoreThanOnePostConstructComponent.class);
			
		}catch(WebBeansConfigurationException e)
		{
			System.out.println(e.getMessage());		
			exc = e;
		}
		
		Assert.assertNotNull(exc);
		
	}
	
	@Test
	public void testPostConstructHasParameter()
	{
		WebBeansConfigurationException exc = null;
		try
		{
			defineSimpleWebBean(PostContructMethodHasParameterComponent.class);
			
		}catch(WebBeansConfigurationException e)
		{
			System.out.println(e.getMessage());		
			exc = e;
		}
		
		Assert.assertNotNull(exc);
		
	}
	
	@Test
	public void testPostConstructHasReturnType()
	{
		WebBeansConfigurationException exc = null;
		try
		{
			defineSimpleWebBean(PostContructMethodHasReturnTypeComponent.class);
			
		}catch(WebBeansConfigurationException e)
		{
			System.out.println(e.getMessage());		
			exc = e;
		}
		
		Assert.assertNotNull(exc);
		
	}
	
	@Test
	public void testPostConstructHasCheckedException()
	{
		WebBeansConfigurationException exc = null;
		try
		{
			defineSimpleWebBean(PostContructMethodHasCheckedExceptionComponent.class);
			
		}catch(WebBeansConfigurationException e)
		{
			System.out.println(e.getMessage());		
			exc = e;
		}
		
		Assert.assertNotNull(exc);
		
	}
	
	@Test
	public void testPostConstructHasStatic()
	{
		WebBeansConfigurationException exc = null;
		try
		{
			defineSimpleWebBean(PostContructMethodHasStaticComponent.class);
			
		}catch(WebBeansConfigurationException e)
		{
			System.out.println(e.getMessage());		
			exc = e;
		}
		
		Assert.assertNotNull(exc);
		
	}
	
	@Test
	public void testMoreThanOneAroundInvoke()
	{
		WebBeansConfigurationException exc = null;
		try
		{
			defineSimpleWebBean(MoreThanOneAroundInvokeComponent.class);
			
		}catch(WebBeansConfigurationException e)
		{
			System.out.println(e.getMessage());		
			exc = e;
		}
		
		Assert.assertNotNull(exc);
		
	}
	
	@Test
	public void testAroundInvokeWithSameMethodName()
	{
		WebBeansConfigurationException exc = null;
		try
		{
			defineSimpleWebBean(AroundInvokeWithSameMethodNameComponent.class);
			Bean<?> comp =getComponents().get(0);
			
			Assert.assertEquals(0, ((AbstractComponent<?>)comp).getInterceptorStack().size());
			
			
		}catch(WebBeansConfigurationException e)
		{
			System.out.println(e.getMessage());		
			exc = e;
		}
		
		Assert.assertNull(exc);
		
	}
	
	@Test
	public void testAroundInvokeWithoutParameter()
	{
		WebBeansConfigurationException exc = null;
		try
		{
			defineSimpleWebBean(AroundInvokeWithoutParameterComponent.class);
			
		}catch(WebBeansConfigurationException e)
		{
			System.out.println(e.getMessage());		
			exc = e;
		}
		
		Assert.assertNotNull(exc);
		
	}

	@Test
	public void testAroundInvokeWithoutReturnType()
	{
		WebBeansConfigurationException exc = null;
		try
		{
			defineSimpleWebBean(AroundInvokeWithoutReturnTypeComponent.class);
			
		}catch(WebBeansConfigurationException e)
		{
			System.out.println(e.getMessage());		
			exc = e;
		}
		
		Assert.assertNotNull(exc);
		
	}

	@Test
	public void testAroundInvokeWithWrongReturnType()
	{
		WebBeansConfigurationException exc = null;
		try
		{
			defineSimpleWebBean(AroundInvokeWithWrongReturnTypeComponent.class);
			
		}catch(WebBeansConfigurationException e)
		{
			System.out.println(e.getMessage());		
			exc = e;
		}
		
		Assert.assertNotNull(exc);
		
	}
	
	@Test
	public void testAroundInvokeWithoutException()
	{
		WebBeansConfigurationException exc = null;
		try
		{
			defineSimpleWebBean(AroundInvokeWithoutExceptionComponent.class);
			
		}catch(WebBeansConfigurationException e)
		{
			System.out.println(e.getMessage());		
			exc = e;
		}
		
		Assert.assertNotNull(exc);
		
	}
	
	@Test
	public void testAroundInvokeWithStatic()
	{
		WebBeansConfigurationException exc = null;
		try
		{
			defineSimpleWebBean(AroundInvokeWithStaticMethodComponent.class);
			
		}catch(WebBeansConfigurationException e)
		{
			System.out.println(e.getMessage());		
			exc = e;
		}
		
		Assert.assertNotNull(exc);
		
	}

	@Test
	public void testAroundInvokeWithFinal()
	{
		WebBeansConfigurationException exc = null;
		try
		{
			defineSimpleWebBean(AroundInvokeWithFinalMethodComponent.class);
			
		}catch(WebBeansConfigurationException e)
		{
			System.out.println(e.getMessage());		
			exc = e;
		}
		
		Assert.assertNotNull(exc);
		
	}
	
	@Test
	public void testNoArgConstructorInterceptor()
	{
		WebBeansConfigurationException exc = null;
		try
		{
			defineSimpleWebBean(NoArgConstructorInterceptorComponent.class);
			
		}catch(WebBeansConfigurationException e)
		{
			System.out.println(e.getMessage());		
			exc = e;
		}
		
		Assert.assertNotNull(exc);
		
	}
	


}
