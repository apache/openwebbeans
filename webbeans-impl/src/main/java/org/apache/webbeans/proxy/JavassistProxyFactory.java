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
package org.apache.webbeans.proxy;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.webbeans.manager.Bean;

import org.apache.webbeans.annotation.WebBeansAnnotation;
import org.apache.webbeans.component.AbstractComponent;
import org.apache.webbeans.decorator.WebBeansDecorator;
import org.apache.webbeans.exception.WebBeansException;
import org.apache.webbeans.intercept.InterceptorHandler;
import org.apache.webbeans.intercept.webbeans.WebBeansInterceptor;

import javassist.util.proxy.ProxyFactory;

public final class JavassistProxyFactory
{
	private JavassistProxyFactory()
	{
		
	}
	
	
	public static <T> Object createNewProxyInstance(Bean<T> bean)
	{
		Object result = null;
		try
		{
			Set<Class<?>> types = bean.getTypes();
			List<Class<?>> interfaceList = new ArrayList<Class<?>>();
			Class<?> superClass = null;
			for(Class<?> type : types)
			{
				if(type.isInterface())
				{
					interfaceList.add(type);
				}
		        else if ((superClass == null) 
		        		|| (superClass.isAssignableFrom(type) && type != Object.class ))
		         {
		        	superClass = type;
		         }
				
			}
			
			interfaceList.add(Serializable.class);
			
			Class<?>[] interfaceArray = new Class<?>[interfaceList.size()];
			interfaceArray = interfaceList.toArray(interfaceArray);
			
			ProxyFactory fact = new ProxyFactory();
			fact.setInterfaces(interfaceArray);
			fact.setSuperclass(superClass);
			
			if(!(bean instanceof WebBeansDecorator) && !(bean instanceof WebBeansInterceptor))
			{
				fact.setHandler(new InterceptorHandler((AbstractComponent<?>)bean));
			}
			
			result = fact.createClass().newInstance();
			
		}catch(Throwable e)
		{
			throw new WebBeansException(e);
		}
		
		return result;
	}
	
	public static WebBeansAnnotation createNewAnnotationProxy(Class<? extends Annotation> annotationType)
	{	
		WebBeansAnnotation result = null;
		
		try
		{
			ProxyFactory pf = new ProxyFactory();
			pf.setInterfaces(new Class<?>[]{annotationType,Annotation.class});
			pf.setSuperclass(WebBeansAnnotation.class);
			pf.setHandler(new WebBeansAnnotation(annotationType));
			
			result = (WebBeansAnnotation)pf.create(new Class[]{Class.class}, new Object[]{annotationType});
			
		}catch(Exception e)
		{
			throw new WebBeansException(e);
		}
		
		return result;
	}

}
