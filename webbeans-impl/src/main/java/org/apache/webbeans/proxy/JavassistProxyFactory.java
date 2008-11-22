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
import java.lang.reflect.Method;

import org.apache.webbeans.component.AbstractComponent;
import org.apache.webbeans.exception.WebBeansException;
import org.apache.webbeans.intercept.InterceptorHandler;
import org.apache.webbeans.util.ClassUtil;

import javassist.util.proxy.MethodFilter;
import javassist.util.proxy.ProxyFactory;

public final class JavassistProxyFactory
{
	private JavassistProxyFactory()
	{
		
	}
	
	
	public static Object createNewProxyInstance(Class<?> superClazz, Class<?>[] paramTypes, Object[] args, AbstractComponent<?> component)
	{
		Object result = null;
		try
		{
			ProxyFactory fact = new ProxyFactory();
			fact.setInterfaces(new Class[]{Serializable.class});
			fact.setSuperclass(superClazz);
			fact.setHandler(new InterceptorHandler(component));
			fact.setFilter(new MethodFilter(){

				public boolean isHandled(Method arg0)
				{
					if(ClassUtil.isObjectMethod(arg0.getName()))
						return false;
					return true;
				}
				
			});
			
			result = fact.create(paramTypes, args); 
		}catch(Throwable e)
		{
			throw new WebBeansException(e);
		}
		
		return result;
	}

}
