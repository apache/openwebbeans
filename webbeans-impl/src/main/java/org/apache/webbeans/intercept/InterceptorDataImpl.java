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
package org.apache.webbeans.intercept;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.interceptor.AroundInvoke;
import javax.webbeans.manager.Interceptor;

/**
 * Abstract implementation of the {@link InterceptorData} api
 * contract.
 *   
 * @author <a href="mailto:gurkanerdogdu@yahoo.com">Gurkan Erdogdu</a>
 * @since 1.0
 */
public class InterceptorDataImpl  implements InterceptorData
{
	/**Around invokes method*/
	private Method aroundInvoke = null;
	
	/**Post construct methods*/
	private Method postConstruct = null;
	
	/**Predestroy Method*/
	private  Method preDestroy = null;
	
	private Interceptor webBeansInterceptor;
	
	/**Instance of the method*/
	private Object interceptorInstance;
	
	/**Defined in the interceptor or bean*/
	private boolean definedInInterceptorClass;
	
	/**Whether the interceptor class is defined in the method*/
	private boolean definedInMethod;
	
	/**If defined in method true, then this method holds the interceptor annotated method*/
	private Method annotatedMethod;
	
	/**Defined with webbeans specific interceptor*/
	private boolean isDefinedWithWebBeansInterceptor;
	

	public InterceptorDataImpl(boolean isDefinedWithWebBeansInterceptor)
	{
		this.isDefinedWithWebBeansInterceptor = isDefinedWithWebBeansInterceptor;
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.apache.webbeans.intercept.InterceptorData#setInterceptor(java.lang.reflect.Method, java.lang.Class)
	 */
	public void setInterceptor(Method m, Class<? extends Annotation> annotation)
	{
		if(annotation.equals(AroundInvoke.class))
		{
			setAroundInvoke(m);
		}
		else if(annotation.equals(PostConstruct.class))
		{
			setPostConstruct(m);
		}
		else if(annotation.equals(PreDestroy.class))
		{
			setPreDestroy(m);
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.apache.webbeans.intercept.InterceptorData#addAroundInvoke(java.lang.reflect.Method)
	 */
	protected void setAroundInvoke(Method m)
	{
		this.aroundInvoke = m;
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.apache.webbeans.intercept.InterceptorData#addPostConstruct(java.lang.reflect.Method)
	 */
	protected void setPostConstruct(Method m)
	{
		this.postConstruct = m;
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.apache.webbeans.intercept.InterceptorData#addPreDestroy(java.lang.reflect.Method)
	 */
	protected void setPreDestroy(Method m)
	{
		this.preDestroy = m;	
	}
		
	/*
	 * (non-Javadoc)
	 * @see org.apache.webbeans.intercept.InterceptorData#getPostConstruct()
	 */
	public Method getPostConstruct()
	{
		return this.postConstruct;
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.apache.webbeans.intercept.InterceptorData#getPreDestroy()
	 */
	public Method getPreDestroy()
	{
		return this.preDestroy;
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.apache.webbeans.intercept.InterceptorData#getAroundInvoke()
	 */
	public Method getAroundInvoke()
	{
		return this.aroundInvoke;
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.apache.webbeans.intercept.InterceptorData#getInterceptorInstance()
	 */
	public Object getInterceptorInstance()
	{
		return interceptorInstance;
	}

	/*
	 * (non-Javadoc)
	 * @see org.apache.webbeans.intercept.InterceptorData#setInterceptorInstance(java.lang.Object)
	 */
	public void setInterceptorInstance(Object interceptorInstance)
	{
		this.interceptorInstance = interceptorInstance;
	}

	/*
	 * (non-Javadoc)
	 * @see org.apache.webbeans.intercept.InterceptorData#isDefinedInInterceptorClass()
	 */
	public boolean isDefinedInInterceptorClass()
	{
		return definedInInterceptorClass;
	}

	/*
	 * (non-Javadoc)
	 * @see org.apache.webbeans.intercept.InterceptorData#setDefinedInInterceptorClass(boolean)
	 */
	public void setDefinedInInterceptorClass(boolean definedInInterceptorClass)
	{
		this.definedInInterceptorClass = definedInInterceptorClass;
	}

	/*
	 * (non-Javadoc)
	 * @see org.apache.webbeans.intercept.InterceptorData#isDefinedInMethod()
	 */
	public boolean isDefinedInMethod()
	{
		return definedInMethod;
	}

	/*
	 * (non-Javadoc)
	 * @see org.apache.webbeans.intercept.InterceptorData#setDefinedInMethod(boolean)
	 */
	public void setDefinedInMethod(boolean definedInMethod)
	{
		this.definedInMethod = definedInMethod;
	}

	/*
	 * (non-Javadoc)
	 * @see org.apache.webbeans.intercept.InterceptorData#getAnnotatedMethod()
	 */
	public Method getAnnotatedMethod()
	{
		return annotatedMethod;
	}

	/*
	 * (non-Javadoc)
	 * @see org.apache.webbeans.intercept.InterceptorData#setAnnotatedMethod(java.lang.reflect.Method)
	 */
	public void setAnnotatedMethod(Method annotatedMethod)
	{
		this.annotatedMethod = annotatedMethod;
	}

	/*
	 * (non-Javadoc)
	 * @see org.apache.webbeans.intercept.InterceptorData#isDefinedWithWebBeansInterceptor()
	 */
	public boolean isDefinedWithWebBeansInterceptor()
	{
		return isDefinedWithWebBeansInterceptor;
	}

	/**
	 * @return the webBeansInterceptor
	 */
	public Interceptor getWebBeansInterceptor()
	{
		return webBeansInterceptor;
	}

	/**
	 * @param webBeansInterceptor the webBeansInterceptor to set
	 */
	public void setWebBeansInterceptor(Interceptor webBeansInterceptor)
	{
		this.webBeansInterceptor = webBeansInterceptor;
	}

	
}