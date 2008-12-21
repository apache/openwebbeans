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
package org.apache.webbeans.intercept.ejb;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptors;

import org.apache.webbeans.intercept.InterceptorData;
import org.apache.webbeans.util.AnnotationUtil;
import org.apache.webbeans.util.Asserts;
import org.apache.webbeans.util.ClassUtil;
import org.apache.webbeans.util.WebBeansUtil;

/**
 * Configures the EJB related interceptors.
 * 
 * @author <a href="mailto:gurkanerdogdu@yahoo.com">Gurkan Erdogdu</a>
 * @since 1.0
 */
public final class EJBInterceptorConfig
{
	/*
	 * Private constructor
	 */
	private EJBInterceptorConfig()
	{
		
	}
	
	/**
	 * Configures the given class for applicable interceptors.
	 * 
	 * @param clazz configuration interceptors for this
	 */
	public static void configure(Class<?> clazz, List<InterceptorData> stack)
	{
		Asserts.assertNotNull(clazz, "Clazz argument can not be null");
		
		
		if(AnnotationUtil.isAnnotationExistOnClass(clazz, Interceptors.class))
		{
			Interceptors incs = clazz.getAnnotation(Interceptors.class);
			Class<?>[] intClasses = incs.value();
			
			for(Class<?> intClass : intClasses)
			{
				configureInterceptorAnnots(intClass, stack, false,null);	
			}
			
		}
		
		
		configureBeanAnnots(clazz, stack);
	}
	
	/*
	 * Configure interceptor class 
	 */
	private static void configureInterceptorAnnots(Class<?> clazz,List<InterceptorData> stack, boolean isMethod, Method m)
	{
		//1- Look interceptor class super class
		//2- Look interceptor class
		Class<?> superClass = clazz.getSuperclass();
		if(superClass != null && !superClass.equals(Object.class))
		{
			configureInterceptorAnnots(superClass, stack, false,null);			
		}

		WebBeansUtil.configureInterceptorMethods(null,clazz, AroundInvoke.class,true,isMethod,stack,m,false);
		WebBeansUtil.configureInterceptorMethods(null,clazz, PostConstruct.class,true,isMethod,stack,m,false);
		WebBeansUtil.configureInterceptorMethods(null,clazz, PreDestroy.class,true,isMethod,stack,m,false);
		
	}
		
	/*
	 * Configure bean class
	 */
	private static void configureBeanAnnots(Class<?> clazz,List<InterceptorData> stack)
	{
		//1- Look method intercepor class annotations
		//2- Look super class around invoke
		//3- Look bean around invoke
		
		//1-
		Method[] methods = clazz.getDeclaredMethods();
		
		for(Method method : methods)
		{
			Interceptors incs = method.getAnnotation(Interceptors.class);
			if(incs != null)
			{
				Class<?>[] intClasses = incs.value();
				
				for(Class<?> intClass : intClasses)
				{
					configureInterceptorAnnots(intClass, stack, true,method);
				}		
				
			}
		} 
		
		//2- Super clazz
		List<Class<?>> listSuperClazz = ClassUtil.getSuperClasses(clazz, new ArrayList<Class<?>>());
		configureBeanSuperClassAnnots(listSuperClazz,stack);
		
		//3- Bean itself
		WebBeansUtil.configureInterceptorMethods(null,clazz, AroundInvoke.class,false,false,stack,null,false);
		WebBeansUtil.configureInterceptorMethods(null,clazz, PostConstruct.class,false,false,stack,null,false);
		WebBeansUtil.configureInterceptorMethods(null,clazz, PreDestroy.class,false,false,stack,null,false);
		
	}
	
	/*
	 * Super class annots.
	 */
	private static void configureBeanSuperClassAnnots(List<Class<?>> list, List<InterceptorData> stack)
	{
		int i = list.size();
		
		for(int j = i-1; j>=0;j--)
		{
			Class<?> clazz = list.get(j);
			if(!clazz.equals(Object.class))
			{
				WebBeansUtil.configureInterceptorMethods(null,clazz, AroundInvoke.class,false,false,stack,null,false);
				WebBeansUtil.configureInterceptorMethods(null,clazz, PostConstruct.class,false,false,stack,null,false);
				WebBeansUtil.configureInterceptorMethods(null,clazz, PreDestroy.class,false,false,stack,null,false);				
			}
		}
	}
		
}