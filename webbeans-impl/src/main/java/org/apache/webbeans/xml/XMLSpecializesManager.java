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
package org.apache.webbeans.xml;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.apache.webbeans.util.Asserts;

public class XMLSpecializesManager
{

	private static XMLSpecializesManager instance = null;
	
	private Set<Class<?>> specializeClasses = new CopyOnWriteArraySet<Class<?>>();
	
	private XMLSpecializesManager()
	{
		
	}
	
	
	public static XMLSpecializesManager getInstance()
	{
		if(instance == null)
		{
			instance = new XMLSpecializesManager();
		}
		
		return instance;
	}
	
	public Set<Class<?>> getXMLSpecializationClasses()
	{
		return Collections.unmodifiableSet(specializeClasses);
	}
	
	public void addXMLSpecializeClass(Class<?> clazz)
	{
		Asserts.assertNotNull(clazz,"clazz parameter can not be null");
		specializeClasses.add(clazz);
	}	
}