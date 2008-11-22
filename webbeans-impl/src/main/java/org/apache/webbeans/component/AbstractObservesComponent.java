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
package org.apache.webbeans.component;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;


public abstract class AbstractObservesComponent<T> extends AbstractComponent<T> implements ObservesMethodsOwner<T>
{
	private Set<Method> observableMethods = new HashSet<Method>();
	
	
	protected AbstractObservesComponent(WebBeansType webBeansType, Class<T> returnType)
	{
		super(webBeansType, returnType);
	}

	/* (non-Javadoc)
	 * @see org.apache.webbeans.component.ObservableComponent#addObservableMethod(java.lang.reflect.Method)
	 */
	public void addObservableMethod(Method observerMethod)
	{
		this.observableMethods.add(observerMethod);
	}

	/* (non-Javadoc)
	 * @see org.apache.webbeans.component.ObservableComponent#getObservableMethods()
	 */
	public Set<Method> getObservableMethods()
	{
		return this.observableMethods;
	}

}
