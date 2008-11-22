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

import java.io.InputStream;
import java.util.Iterator;
import java.util.List;

import javax.webbeans.InterceptorBindingType;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.webbeans.WebBeansConstants;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.exception.WebBeansException;
import org.apache.webbeans.util.Asserts;
import org.apache.webbeans.util.ClassUtil;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.ElementHandler;
import org.dom4j.ElementPath;
import org.dom4j.Namespace;
import org.dom4j.io.SAXReader;



/**
 * Used for getting information contained in the file web-beans.xml.
 * 
 * @author <a href="mailto:gurkanerdogdu@yahoo.com">Gurkan Erdogdu</a>
 * @since 1.0
 */
@SuppressWarnings("unchecked")
public class XMLUtil
{

	private static final Logger log = LogManager.getLogger(XMLUtil.class);

	/**
	 * Gets new {@link SAXReader} instance.
	 * @return sax reader instance
	 */
	public static SAXReader getSaxReader()
	{
		return new SAXReader();
	}

	/**
	 * Gets the root element of the parsed document.
	 *
	 * @param stream parsed document
	 * @return root element of the document
	 * @throws WebBeansException if any runtime exception occurs
	 */
	public static Element getRootElement(InputStream stream) throws WebBeansException
	{
		try
		{
			SAXReader saxReader = getSaxReader();
			saxReader.setMergeAdjacentText(true);
			saxReader.setStripWhitespaceText(true);
			saxReader.setErrorHandler(new WebBeansErrorHandler());
			saxReader.setEntityResolver(new WebBeansResolver());
			saxReader.setValidation(false);
			saxReader.setDefaultHandler(new ElementHandler()
			{
				public void onEnd(ElementPath path)
				{
					
				}

				public void onStart(ElementPath path)
				{
					Element element =  path.getCurrent();
					if(element.getNamespaceURI() == null || element.getNamespaceURI().equals(""))
					{
						throw new WebBeansConfigurationException("All elements in the web-beans.xml file must have declared name space");
					}
					else
					{
						if(element.isRootElement())
						{
							WebBeansNameSpaceContainer.getInstance().addNewPackageNameSpace(element.getNamespace().getURI());
							
							List allNs = element.declaredNamespaces();
							Iterator itNs = allNs.iterator();
							
							while(itNs.hasNext())
							{
								WebBeansNameSpaceContainer.getInstance().addNewPackageNameSpace(((Namespace)itNs.next()).getURI());
							}
						}
					}
				}
				
			});
			
			Document document = saxReader.read(stream);

			return document.getRootElement();

		} catch (DocumentException e)
		{
			log.fatal("Unable to read root element of the given input stream", e);
			throw new WebBeansException("Unable to read root element of the given input stream", e);
		}
	}
	
	public static boolean isElementInNamespace(Element element, String namespace)
	{
		Asserts.assertNotNull(element,"element parameter can not be null");
		Asserts.assertNotNull(namespace,"namespace parameter can not be null");
		
		Namespace ns = element.getNamespace();
		if(!Namespace.NO_NAMESPACE.equals(ns))
		{
			if(ns.getURI().equals(namespace))
			{
				return true;
			}
		}
		
		return false;
	}
	
	public static boolean isElementInWebBeansNameSpace(Element element)
	{
		nullCheckForElement(element);
		String ns = getElementNameSpace(element);
		
		if(ns != null && ns.equals(WebBeansConstants.WEB_BEANS_NAMESPACE))
		{
			return true;
		}
		
		return false;
	}
	
	public static boolean isElementInWebBeansNameSpaceWithName(Element element,String name)
	{
		nullCheckForElement(element);
		
		if(isElementInWebBeansNameSpace(element))
		{
			String txtName = element.getName();
			
			if(name.equals(txtName))
			{
				return true;	
			}
		}		

		return false;
	}
	
	
	public static String getElementNameSpace(Element element)
	{
		nullCheckForElement(element);
		
		Namespace ns = element.getNamespace();
		if(!Namespace.NO_NAMESPACE.equals(ns))
		{
			return ns.getURI();
		}
		
		return null;
	}
		
	public static boolean isElementWebBeanDeclaration(Element element)
	{
		nullCheckForElement(element);
		
		if(!isElementInWebBeansNameSpaceWithName(element,WebBeansConstants.WEB_BEANS_XML_DEPLOY_ELEMENT) &&
				!isElementInWebBeansNameSpaceWithName(element,WebBeansConstants.WEB_BEANS_XML_INTERCEPTORS_ELEMENT) &&
				!isElementInWebBeansNameSpaceWithName(element,WebBeansConstants.WEB_BEANS_XML_DECORATORS_ELEMENT))
		{
			return true;
		}
		
		return false;
		
	}
	
	public static boolean isElementDeployDeclaration(Element element)
	{
		nullCheckForElement(element);
		
		if(isElementInWebBeansNameSpaceWithName(element,WebBeansConstants.WEB_BEANS_XML_DEPLOY_ELEMENT))
		{
			return true;
		}
		
		return false;
		
	}
	
	public static boolean isElementInterceptorsDeclaration(Element element)
	{
		nullCheckForElement(element);
		
		if(isElementInWebBeansNameSpaceWithName(element,WebBeansConstants.WEB_BEANS_XML_INTERCEPTORS_ELEMENT))
		{
			return true;
		}
		
		return false;
		
	}
	
	public static boolean isElementDecoratosDeclaration(Element element)
	{
		nullCheckForElement(element);
		
		if(isElementInWebBeansNameSpaceWithName(element,WebBeansConstants.WEB_BEANS_XML_DECORATORS_ELEMENT))
		{
			return true;
		}
		
		return false;
		
	}
	
	
	public static boolean isElementJMSDeclaration(Element element)
	{
		nullCheckForElement(element);
		
		if(isElementWebBeanDeclaration(element))
		{
			if(isElementInWebBeansNameSpaceWithName(element, WebBeansConstants.WEB_BEANS_XML_QUEUE_ELEMENT) ||
					isElementInWebBeansNameSpaceWithName(element, WebBeansConstants.WEB_BEANS_XML_TOPIC_ELEMENT))
				
				return true;
		}
		
		return false;
		
	}
	
	public static boolean isElementField(Element element)
	{
		nullCheckForElement(element);
		
		List<Element> childs = element.elements();
		
		for(Element child : childs)
		{
			if(!isElementInWebBeansNameSpaceWithName(child,WebBeansConstants.WEB_BEANS_XML_INITIALIZER_ELEMENT) &&
					!isElementInWebBeansNameSpaceWithName(child,WebBeansConstants.WEB_BEANS_XML_DESTRUCTOR_ELEMENT) &&
					!isElementInWebBeansNameSpaceWithName(child,WebBeansConstants.WEB_BEANS_XML_PRODUCES_ELEMENT) &&
					!isElementInWebBeansNameSpaceWithName(child,WebBeansConstants.WEB_BEANS_XML_DISPOSES_ELEMENT) &&
					!isElementInWebBeansNameSpaceWithName(child,WebBeansConstants.WEB_BEANS_XML_OBSERVES_ELEMENT) &&
					!isElementInWebBeansNameSpaceWithName(child,WebBeansConstants.WEB_BEANS_XML_DECORATES_ELEMENT))
			{
				
				Class<?> clazz = getElementJavaType(child);
				if(clazz != null)
				{
					if(clazz.isAnnotation())
					{
						if(clazz.getAnnotation(InterceptorBindingType.class) != null)
						{
							return false;
						}
					}
					
				}
				
			}
			else
			{
				return false;
			}
			
		}
		
		return true;
		
	}
	
	public static boolean isElementMethod(Element element)
	{
		Asserts.nullCheckForDomElement(element);
		
		List<Element> childs = element.elements();
		
		for(Element child : childs)
		{
			if(isElementInWebBeansNameSpaceWithName(child,WebBeansConstants.WEB_BEANS_XML_INITIALIZER_ELEMENT) ||
					isElementInWebBeansNameSpaceWithName(child,WebBeansConstants.WEB_BEANS_XML_DESTRUCTOR_ELEMENT) ||
					isElementInWebBeansNameSpaceWithName(child,WebBeansConstants.WEB_BEANS_XML_PRODUCES_ELEMENT) ||
					isElementInWebBeansNameSpaceWithName(child,WebBeansConstants.WEB_BEANS_XML_DISPOSES_ELEMENT) ||
					isElementInWebBeansNameSpaceWithName(child,WebBeansConstants.WEB_BEANS_XML_OBSERVES_ELEMENT))
			{
				return true;
				
			}
			else
			{
				Class<?> clazz = getElementJavaType(child);
				if(clazz != null)
				{
					if(clazz.isAnnotation())
					{
						if(clazz.getAnnotation(InterceptorBindingType.class) != null)
						{
							return true;
						}
					}
					
				}
			}
			
		}
		
		return false;
		
	}
	
	
	public static String getName(Element element)
	{
		nullCheckForElement(element);
		
		return element.getName();
	}
	
	public static Class<?> getElementJavaType(Element element)
	{
		String ns  = getElementNameSpace(element);
		String packageName = WebBeansNameSpaceContainer.getInstance().getPackageNameFromNameSpace(ns);
		
		String className = packageName + XMLUtil.getName(element);
		
		Class<?> clazz = ClassUtil.getClassFromName(className);
		
		return clazz;
	}
	
	private static void nullCheckForElement(Element element)
	{
		Asserts.nullCheckForDomElement(element);
	}
	

	public static boolean isElementChildExist(Element parent, String childName)
	{
		Asserts.assertNotNull(parent, "parent parameter can not be null");
		Asserts.assertNotNull(childName, "childName parameter can not be null");
		
		return parent.element(childName) != null ? true : false;
	}
	
	public Class<?> getJavaClassForElement(Element element)
	{
		nullCheckForElement(element);
		
		String ns = getElementNameSpace(element);
		String packageName = WebBeansNameSpaceContainer.getInstance().getPackageNameFromNameSpace(ns);
		
		String className = packageName + getName(element);
		
		return ClassUtil.getClassFromName(className);
		
	}
}
