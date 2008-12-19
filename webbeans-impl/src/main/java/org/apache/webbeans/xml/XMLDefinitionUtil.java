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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.webbeans.Decorator;
import javax.webbeans.DeploymentType;
import javax.webbeans.Interceptor;
import javax.webbeans.Named;
import javax.webbeans.NonexistentTypeException;
import javax.webbeans.ScopeType;
import javax.webbeans.Specializes;

import org.apache.webbeans.WebBeansConstants;
import org.apache.webbeans.component.xml.XMLComponentImpl;
import org.apache.webbeans.component.xml.XMLProducerComponentImpl;
import org.apache.webbeans.deployment.StereoTypeManager;
import org.apache.webbeans.deployment.StereoTypeModel;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.proxy.JavassistProxyFactory;
import org.apache.webbeans.util.AnnotationUtil;
import org.apache.webbeans.util.Asserts;
import org.apache.webbeans.util.ClassUtil;
import org.apache.webbeans.util.WebBeansUtil;
import org.dom4j.Element;

@SuppressWarnings("unchecked")
public final class XMLDefinitionUtil
{
	private XMLDefinitionUtil()
	{
		
	}
	
	/**
	 * Checks the conditions for simple webbeans class defined
	 * in the XML file.
	 * 
	 * @param clazz simple webbeans class declared in XML
	 * 
	 * @throws WebBeansConfigurationException if check is fail
	 */
	public static void checkSimpleWebBeansInXML(Class<?> clazz, Element webBeanDecleration, String errorMessage) throws WebBeansConfigurationException
	{
		Asserts.nullCheckForClass(clazz);
		if(errorMessage == null)
		{
			errorMessage = "XML defined simple webbeans failed. ";
		}
		
		int modifier = clazz.getModifiers();
		
		if (ClassUtil.isParametrized(clazz))
		{
			throw new WebBeansConfigurationException(errorMessage + "Simple WebBeans component implementation class : " + clazz.getName() + " can not be parametrized type");	
		}			
		
		if (!ClassUtil.isStatic(modifier) && ClassUtil.isInnerClazz(clazz))
		{
			throw new WebBeansConfigurationException(errorMessage + "Simple WebBeans component implementation class : " + clazz.getName() + " can not be non-static inner class");	
		}				
		
		if(clazz.isAnnotationPresent(Interceptor.class))
		{
			boolean found = XMLUtil.isElementChildExistWithWebBeansNameSpace(webBeanDecleration, WebBeansConstants.WEB_BEANS_XML_INTERCEPTOR_ELEMENT);
			if(!found)
			{
				throw new WebBeansConfigurationException(errorMessage + "Simple WebBeans component implementation class : " + clazz.getName() + " must be declared as <Interceptor> element in the XML");
			}
		}
		
		if(clazz.isAnnotationPresent(Decorator.class))
		{
			boolean found = XMLUtil.isElementChildExistWithWebBeansNameSpace(webBeanDecleration, WebBeansConstants.WEB_BEANS_XML_DECORATOR_ELEMENT);
			if(!found)
			{
				throw new WebBeansConfigurationException(errorMessage + "Simple WebBeans component implementation class : " + clazz.getName() + " must be declared as <Decorator> element in the XML");
			}
		}
		
	}
	
	
	public static void checkTypeMetaDataClasses(Set<Class<? extends Annotation>> typeSet, String errorMessage)
	{
		if(typeSet != null && !typeSet.isEmpty())
		{
			Iterator<Class<? extends Annotation>> it = typeSet.iterator();
			while(it.hasNext())
			{
				Class<? extends Annotation> clazz = it.next();
				if(clazz.isAnnotationPresent(DeploymentType.class) ||
						clazz.isAnnotationPresent(ScopeType.class) ||
						AnnotationUtil.isBindingAnnotation(clazz) ||
						AnnotationUtil.isInterceptorBindingAnnotation(clazz) ||
						AnnotationUtil.isStereoTypeAnnotation(clazz) ||
						clazz.equals(Named.class) ||
						clazz.equals(Specializes.class) ||
						clazz.equals(Interceptor.class) ||
						clazz.equals(Decorator.class))
				{
					continue;
				}
				else
				{
					throw new WebBeansConfigurationException(errorMessage + " TypeLevelMeta data configuration is failed because of the class : " + clazz.getName() + " is not applicable type");
				}
			}
		}
		
	}

	public static <T> Class<? extends Annotation> defineXMLTypeMetaData(XMLComponentImpl<T> component, Set<Class<? extends Annotation>> annotationSet, Class<? extends Annotation> defineType, String errorMessage)
	{
		Class<? extends Annotation> metaType = null;
		
		Iterator<Class<? extends Annotation>> it = annotationSet.iterator();
		boolean found = false;
		while(it.hasNext())
		{
			Class<? extends Annotation> temp = it.next();
			if(temp.isAnnotationPresent(defineType))
			{
				if(found)
				{
					throw new WebBeansConfigurationException(errorMessage);
				}
				else
				{
					metaType = temp;
					found = true;
				}
			}
		}
		
		return metaType;
	}
	
	public static <T> boolean defineXMLBindingType(XMLComponentImpl<T> component, Set<Class<? extends Annotation>> annotationSet)
	{
		Iterator<Class<? extends Annotation>> it = annotationSet.iterator();
		boolean found = false;
		while(it.hasNext())
		{
			Class<? extends Annotation> temp = it.next();
			if(AnnotationUtil.isBindingAnnotation(temp))
			{
				if(!found)
				{
					found = true;
				}
				
				component.addBindingType(JavassistProxyFactory.createNewAnnotationProxy(temp));
			}
		}
		
		return found;
	}
	
	public static <T> boolean defineXMLInterceptorType(XMLComponentImpl<T> component, Set<Class<? extends Annotation>> annotationSet)
	{
		Iterator<Class<? extends Annotation>> it = annotationSet.iterator();
		boolean found = false;
		while(it.hasNext())
		{
			Class<? extends Annotation> temp = it.next();
			if(AnnotationUtil.isInterceptorBindingAnnotation(temp))
			{
				if(!found)
				{
					found = true;
				}
				
				//component.add(temp);
			}
		}
		
		return found;
	}
	
	public static <T> boolean defineXMLStereoType(XMLComponentImpl<T> component, Set<Class<? extends Annotation>> annotationSet)
	{
		Iterator<Class<? extends Annotation>> it = annotationSet.iterator();
		boolean found = false;
		while(it.hasNext())
		{
			Class<? extends Annotation> temp = it.next();
			if(AnnotationUtil.isStereoTypeAnnotation(temp))
			{
				if(!found)
				{
					found = true;
				}
				
				//Add new stereotype model
				StereoTypeManager.getInstance().addStereoTypeModel(new StereoTypeModel(temp));
				//component.addXMLStereoType(temp);
			}
		}
		
		return found;
	}
	
	public static <T> boolean defineXMLName(XMLComponentImpl<T> component, Set<Class<? extends Annotation>> annotationSet)
	{
		Iterator<Class<? extends Annotation>> it = annotationSet.iterator();
		while(it.hasNext())
		{
			Class<? extends Annotation> temp = it.next();
			if(temp.equals(Named.class))
			{
				return true;
			}
		}
		
		return false;
	}
	
	public static <T> void defineXMLSpecializes(XMLComponentImpl<T> component, Set<Class<? extends Annotation>> annotationSet)
	{
		Iterator<Class<? extends Annotation>> it = annotationSet.iterator();
		while(it.hasNext())
		{
			Class<? extends Annotation> temp = it.next();
			if(temp.equals(Specializes.class))
			{
				XMLSpecializesManager.getInstance().addXMLSpecializeClass(temp);
			}
		}
	}
	
	public static <T> void defineXMLInterceptors(XMLComponentImpl<T> component, Set<Class<? extends Annotation>> annotationSet, String errorMessage)
	{
		Iterator<Class<? extends Annotation>> it = annotationSet.iterator();
		boolean found = false;
		while(it.hasNext())
		{
			Class<? extends Annotation> temp = it.next();
			if(temp.equals(Interceptor.class))
			{
				if(found)
				{
					throw new WebBeansConfigurationException(errorMessage);
				}
				else
				{
					found = true;
					WebBeansUtil.defineSimpleWebBeansInterceptors(temp);
				}
			}
		}
	}
	
	public static <T> void defineXMLDecorators(XMLComponentImpl<T> component, Set<Class<? extends Annotation>> annotationSet, String errorMessage)
	{
		Iterator<Class<? extends Annotation>> it = annotationSet.iterator();
		boolean found = false;
		while(it.hasNext())
		{
			Class<? extends Annotation> temp = it.next();
			if(temp.equals(Decorator.class))
			{
				if(found)
				{
					throw new WebBeansConfigurationException(errorMessage);
				}
				else
				{
					found = true;
					WebBeansUtil.defineSimpleWebBeansDecorators(temp);
				}
			}
		}
	}
	
	public static <T> XMLProducerComponentImpl<T> defineXMLProducerMethod(XMLComponentImpl<T> component, Method producesMethod, Element producerMethodElement, String errorMessage)
	{
		List<Element> childElements = producerMethodElement.elements();
		boolean producesDefined = false;
		for(Element childElement : childElements)
		{
			if(XMLUtil.isElementInWebBeansNameSpaceWithName(childElement, WebBeansConstants.WEB_BEANS_XML_PRODUCES_ELEMENT))
			{
				if(producesDefined == false)
				{
					producesDefined = true;
				}
				else
				{
					throw new WebBeansConfigurationException(errorMessage + "More than one <Produces> element is defined");
				}
				
				List<Element> producesElementChilds = childElement.elements();
				boolean definedType = false;
				for(Element producesElementChild : producesElementChilds)
				{
					Class<?> type = XMLUtil.getElementJavaType(producesElementChild);
					if(type == null)
					{
						throw new NonexistentTypeException(errorMessage + "Java type : " + XMLUtil.getElementJavaClassName(producesElementChild) + " does not exist in the <Produces> element child");
					}
					else if(type.isArray() || type.isEnum())
					{
						throw new WebBeansConfigurationException(errorMessage + "Java type must be class, interface or annotation type in the <Produces> childs");
					}
					else if(type.isAnnotation())
					{
						//Class<? extends Annotation> annotationType = (Class<? extends Annotation>)type;
						
						
					}
					else
					{
						if(definedType)
						{
							throw new WebBeansConfigurationException(errorMessage + "More than one Java type in the <Produces> element");
						}
						else
						{
							definedType = true;
						}
					}
				}
				
				if(!definedType)
				{
					throw new WebBeansConfigurationException(errorMessage + "<Produces> element must define at least one java type child");
				}
			}
		}
		
		return null;
	}
	
}
