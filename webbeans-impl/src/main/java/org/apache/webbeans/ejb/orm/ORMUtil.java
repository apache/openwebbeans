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
package org.apache.webbeans.ejb.orm;

import org.apache.webbeans.util.Asserts;

/**
 * Some utility methods for parsing orm.xml file
 * in folders META-INF/orm.xml.
 * 
 * @author <a href="mailto:gurkanerdogdu@yahoo.com">Gurkan Erdogdu</a>
 * @since 1.0
 */
public final class ORMUtil
{
	private ORMUtil()
	{
		
	}

	/**
	 * Return true if it is defined in the ejb-jar.xml
	 * false otherwise.
	 * 
	 * @param clazzName class name
	 * @return true if it is defined in the orm.xml
	 */
	public static boolean isDefinedInXML(String clazzName)
	{
		Asserts.assertNotNull(clazzName,"clazzName parameter can not be null");
		
		return false;
	}

	
}
