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

import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.webbeans.Decorator;
import javax.webbeans.Interceptor;
import javax.webbeans.Production;
import javax.webbeans.manager.Context;

import org.apache.webbeans.component.AbstractComponent;
import org.apache.webbeans.component.ComponentImpl;
import org.apache.webbeans.component.WebBeansType;
import org.apache.webbeans.config.DefinitionUtil;
import org.apache.webbeans.config.SimpleWebBeansConfigurator;
import org.apache.webbeans.decorator.DecoratorUtil;
import org.apache.webbeans.decorator.DecoratorsManager;
import org.apache.webbeans.decorator.WebBeansDecoratorConfig;
import org.apache.webbeans.deployment.DeploymentTypeManager;
import org.apache.webbeans.deployment.StereoTypeManager;
import org.apache.webbeans.deployment.StereoTypeModel;
import org.apache.webbeans.intercept.InterceptorUtil;
import org.apache.webbeans.intercept.InterceptorsManager;
import org.apache.webbeans.intercept.WebBeansInterceptorConfig;
import org.apache.webbeans.test.component.decorator.broken.DelegateAttributeIsnotInterface;
import org.apache.webbeans.test.component.decorator.broken.DelegateAttributeMustImplementAllDecoratedTypes;
import org.apache.webbeans.test.component.decorator.broken.MoreThanOneDelegateAttribute;
import org.apache.webbeans.test.component.decorator.broken.PaymentDecorator;
import org.apache.webbeans.test.component.decorator.clean.LargeTransactionDecorator;
import org.apache.webbeans.test.component.decorator.clean.ServiceDecorator;
import org.apache.webbeans.test.component.intercept.webbeans.WebBeansInterceptor;
import org.apache.webbeans.test.component.intercept.webbeans.WebBeanswithMetaInterceptor;
import org.apache.webbeans.test.mock.MockHttpSession;
import org.apache.webbeans.test.mock.MockManager;
import org.apache.webbeans.test.sterotype.StereoWithNonScope;
import org.apache.webbeans.test.sterotype.StereoWithRequestScope;
import org.apache.webbeans.test.sterotype.StereoWithSessionScope;
import org.apache.webbeans.test.sterotype.StereoWithSessionScope2;
import org.apache.webbeans.util.WebBeansUtil;
import org.apache.webbeans.xml.WebBeansXMLConfigurator;

public abstract class TestContext implements ITestContext
{
	private static Set<ITestContext> testContexts = new HashSet<ITestContext>();

	private String clazzName;

	private MockManager manager;

	private static boolean init;

	protected TestContext(String clazzName)
	{
		this.clazzName = clazzName;
		testContexts.add(this);
		this.manager = MockManager.getInstance();
	}

	public void init()
	{
		if (!init)
		{
			initializeDeploymentType(Production.class,1);
			
			initializeStereoType(Interceptor.class);
			initializeStereoType(Decorator.class);
			initializeStereoType(StereoWithNonScope.class);
			initializeStereoType(StereoWithRequestScope.class);
			initializeStereoType(StereoWithSessionScope.class);
			initializeStereoType(StereoWithSessionScope2.class);

			initializeInterceptorType(WebBeansInterceptor.class);
			initializeInterceptorType(WebBeanswithMetaInterceptor.class);
			
			initializeDecoratorType(DelegateAttributeIsnotInterface.class);
			initializeDecoratorType(MoreThanOneDelegateAttribute.class);
			initializeDecoratorType(PaymentDecorator.class);
			initializeDecoratorType(DelegateAttributeMustImplementAllDecoratedTypes.class);
			initializeDecoratorType(ServiceDecorator.class);
			initializeDecoratorType(LargeTransactionDecorator.class);

			init = true;
		}

	}
	
	protected void beforeTest(){}

	public void fail(String methodName)
	{
		System.err.println("Test Class: " + clazzName + ",Method Name: " + methodName + " is FAILED");
	}

	public void pass(String methodName)
	{
		System.out.println("Test Class: " + clazzName + ",Method Name: " + methodName + " is PASSED");
	}

	public static void initTests()
	{
		Iterator<ITestContext> it = testContexts.iterator();
		while (it.hasNext())
		{
			it.next().init();
		}

	}

	public static void startAllTests(ServletContext ctx)
	{
		Iterator<ITestContext> it = testContexts.iterator();
		while (it.hasNext())
		{
			it.next().startTests(ctx);
		}

	}

	public static void endAllTests(ServletContext ctx)
	{
		Iterator<ITestContext> it = testContexts.iterator();
		while (it.hasNext())
		{
			it.next().endTests(ctx);
		}

	}

	protected <T> AbstractComponent<T> defineSimpleWebBean(Class<T> clazz)
	{
		ComponentImpl<T> bean = null;
		
		
		SimpleWebBeansConfigurator.checkSimpleWebBeanCondition(clazz);
		bean = SimpleWebBeansConfigurator.define(clazz, WebBeansType.SIMPLE);
		
		if(bean != null)
		{
			DecoratorUtil.checkSimpleWebBeanDecoratorConditions(bean);
			DefinitionUtil.defineSimpleWebBeanInterceptorStack(bean);
			
			getComponents().add((AbstractComponent<?>)bean);
			manager.addBean(bean);
		}			
		
		return bean;
	}
	
	
	@SuppressWarnings("unchecked")
	protected <T> AbstractComponent<T> defineSimpleWebBeanInterceptor(Class<T> clazz)
	{
		ComponentImpl<T> component = null;
		
		SimpleWebBeansConfigurator.checkSimpleWebBeanCondition(clazz);
		{
			//This is the interceptor class
			if(InterceptorsManager.getInstance().isInterceptorEnabled(clazz))
			{
				InterceptorUtil.checkInterceptorConditions(clazz);
				component = SimpleWebBeansConfigurator.define(clazz, WebBeansType.INTERCEPTOR);
				WebBeansInterceptorConfig.configureInterceptorClass((ComponentImpl<Object>)component);
			}
			
		}
		
		return component;
	}
	
	@SuppressWarnings("unchecked")
	protected  <T> AbstractComponent<T> defineSimpleWebBeansDecorators(Class<T> clazz)
	{
		ComponentImpl<T> component = null;
		
		if(DecoratorsManager.getInstance().isDecoratorEnabled(clazz))
		{			
			DecoratorUtil.checkDecoratorConditions(clazz);
			component = SimpleWebBeansConfigurator.define(clazz, WebBeansType.DECORATOR);
			
			if(component != null)
			{
				WebBeansDecoratorConfig.configureDecoratorClass((ComponentImpl<Object>)component);
			}			
		}	
		
		
		return component;
	}
	
	

	protected void clear()
	{
		manager.clear();
	}

	protected AbstractComponent<?> getComponent(int i)
	{
		return manager.getComponent(i);
	}

	protected List<AbstractComponent<?>> getComponents()
	{
		return manager.getComponents();
	}

	protected int getDeployedComponents()
	{
		return manager.getDeployedCompnents();
	}

	protected Object getInstanceByName(String name)
	{
		return manager.getInstanceByName(name);
	}

	protected Context getContext(Class<? extends Annotation> scopeType)
	{
		return manager.getContext(scopeType);
	}

	protected MockManager getManager()
	{
		return manager;
	}
	
	protected HttpSession getSession()
	{
		return new MockHttpSession();
	}
	
	protected void configureFromXML(InputStream file, String fileName)
	{
		WebBeansXMLConfigurator.configure(file, fileName);
	}
	
	private void initializeDeploymentType(Class<? extends Annotation> deploymentType, int precedence)
	{
		DeploymentTypeManager.getInstance().addNewDeploymentType(deploymentType, precedence);
		
	}

	private void initializeStereoType(Class<?> stereoClass)
	{
		WebBeansUtil.checkStereoTypeClass(stereoClass);
		StereoTypeModel model = new StereoTypeModel(stereoClass);
		StereoTypeManager.getInstance().addStereoTypeModel(model);
	}

	private void initializeInterceptorType(Class<?> interceptorClazz)
	{
		InterceptorsManager.getInstance().addNewInterceptor(interceptorClazz);

	}
	
	private void initializeDecoratorType(Class<?> decoratorClazz)
	{
		DecoratorsManager.getInstance().addNewDecorator(decoratorClazz);

	}
	
	public void endTests(ServletContext ctx)
	{
	}

	public void startTests(ServletContext ctx)
	{
	}
	
	
}
