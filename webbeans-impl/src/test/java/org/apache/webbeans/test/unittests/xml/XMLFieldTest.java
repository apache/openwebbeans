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
package org.apache.webbeans.test.unittests.xml;

import java.io.InputStream;

import javax.servlet.ServletContext;
import javax.webbeans.manager.Manager;

import junit.framework.Assert;

import org.apache.webbeans.container.ManagerImpl;
import org.apache.webbeans.test.servlet.TestContext;
import org.apache.webbeans.xml.WebBeansXMLConfigurator;
import org.junit.Before;
import org.junit.Test;


public class XMLFieldTest extends TestContext
{
	Manager container = null;

	public XMLFieldTest()
	{
		super(XMLFieldTest.class.getSimpleName());
	}

	public void endTests(ServletContext ctx)
	{
		
	}

	@Before
	public void init()
	{
		this.container = ManagerImpl.getManager();
	}

	public void startTests(ServletContext ctx)
	{
		
	}
	
	
	@Test
	public void nameSpacesNotDeclared()
	{
		Throwable e = null;
		try
		{
			InputStream stream = XMLFieldTest.class.getClassLoader().getResourceAsStream("org/apache/webbeans/test/xml/fieldTest.xml");			
			Assert.assertNotNull(stream);
			
			WebBeansXMLConfigurator.configure(stream,"fieldTest.xml");
			
		}catch(Throwable e1)
		{
			e = e1;
		}
		
		Assert.assertNull(e);
	}
	
}
