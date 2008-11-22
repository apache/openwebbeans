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
package org.apache.webbeans.component.xml;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.webbeans.component.ComponentImpl;

public class XMLComponentImpl<T> extends ComponentImpl<T>
{
	private List<Type> constructorApiTypes = new ArrayList<Type>();
	
	private Map<String, Type> fieldApiType = new HashMap<String, Type>();
	
	private Map<String, Type> methodApiType = new HashMap<String, Type>();
	
	private Map<String, Object> fieldValues = new HashMap<String, Object>();
	
	
	public XMLComponentImpl(Class<T> returnType)
	{
		super(returnType);
	}
	
	public void addConstructorApiType(Type apiType)
	{
		constructorApiTypes.add(apiType);
	}
	
	public void addFieldApiType(Type apiType, String name)
	{
		fieldApiType.put(name, apiType);
	}
	
	public void addMethodApiType(Type apiType, String name)
	{
		methodApiType.put(name, apiType);
	}
	
	
	public void addFieldValue(String name, Object value)
	{
		fieldValues.put(name, value);
	}

	/**
	 * @return the constructorApiTypes
	 */
	public List<Type> getConstructorApiTypes()
	{
		return constructorApiTypes;
	}

	/**
	 * @return the fieldApiTypes
	 */
	public Map<String, Type> getFieldApiType()
	{
		return fieldApiType;
	}

	/**
	 * @return the fieldValues
	 */
	public Map<String, Object> getFieldValues()
	{
		return fieldValues;
	}

	/**
	 * @return the methodApiType
	 */
	public Map<String, Type> getMethodApiType()
	{
		return methodApiType;
	}
	
	
	
	

}
