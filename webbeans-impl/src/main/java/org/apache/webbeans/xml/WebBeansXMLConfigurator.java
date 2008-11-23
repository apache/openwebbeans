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
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.webbeans.AfterTransactionCompletion;
import javax.webbeans.AfterTransactionFailure;
import javax.webbeans.AfterTransactionSuccess;
import javax.webbeans.BeforeTransactionCompletion;
import javax.webbeans.BindingType;
import javax.webbeans.DeploymentException;
import javax.webbeans.DeploymentType;
import javax.webbeans.Destructor;
import javax.webbeans.Disposes;
import javax.webbeans.IfExists;
import javax.webbeans.Initializer;
import javax.webbeans.InterceptorBindingType;
import javax.webbeans.Named;
import javax.webbeans.NonexistentFieldException;
import javax.webbeans.NonexistentMethodException;
import javax.webbeans.NonexistentTypeException;
import javax.webbeans.Observes;
import javax.webbeans.Produces;
import javax.webbeans.Production;
import javax.webbeans.ScopeType;

import org.apache.webbeans.WebBeansConstants;
import org.apache.webbeans.component.AbstractComponent;
import org.apache.webbeans.component.xml.XMLComponentImpl;
import org.apache.webbeans.config.DefinitionUtil;
import org.apache.webbeans.config.SimpleWebBeansConfigurator;
import org.apache.webbeans.decorator.DecoratorsManager;
import org.apache.webbeans.deployment.DeploymentTypeManager;
import org.apache.webbeans.ejb.EJBUtil;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.intercept.InterceptorsManager;
import org.apache.webbeans.util.AnnotationUtil;
import org.apache.webbeans.util.Asserts;
import org.apache.webbeans.util.ClassUtil;
import org.apache.webbeans.util.WebBeansUtil;
import org.dom4j.Element;

/**
 * Configures the web beans from the xml declerations.
 * 
 * @author <a href="mailto:gurkanerdogdu@yahoo.com">Gurkan Erdogdu</a>
 * @since 1.0
 */
@SuppressWarnings("unchecked")
public final class WebBeansXMLConfigurator
{
	private static boolean DEPLOY_IS_DEFINED = false;

	private static boolean INTERCEPTORS_IS_DEFINED = false;

	private static boolean DECORATORS_IS_DEFINED = false;

	private static String CURRENT_SCAN_FILE_NAME = null;

	/**
	 * Private constructor
	 */
	private WebBeansXMLConfigurator()
	{
		throw new UnsupportedOperationException();
	}

	/**
	 * Configures the web beans from the given input stream.
	 * 
	 * @param xmlStream
	 *            xml file containing the web beans definitions.
	 */
	public static void configure(InputStream xmlStream, String fileName)
	{
		Asserts.assertNotNull(xmlStream);
		Asserts.assertNotNull(fileName);

		CURRENT_SCAN_FILE_NAME = fileName;

		Element webBeansRoot = XMLUtil.getRootElement(xmlStream);

		configure(webBeansRoot);
	}

	private static void configure(Element webBeansRoot)
	{
		List<Element> webBeanDeclerationList = new ArrayList<Element>();
		List<Element> childs = webBeansRoot.elements();
		Iterator<Element> it = childs.iterator();

		Element child = null;
		while (it.hasNext())
		{
			child = it.next();

			if (XMLUtil.isElementWebBeanDeclaration(child))
			{
				webBeanDeclerationList.add(child);

			} else if (XMLUtil.isElementDeployDeclaration(child))
			{
				if (DEPLOY_IS_DEFINED)
				{
					throw new DeploymentException("There can not be more than one web-beans.xml file that declares <Deploy> element");
				} else
				{
					if (!XMLUtil.isElementChildExist(child, WebBeansConstants.WEB_BEANS_XML_STANDART_ELEMENT))
					{
						throw new DeploymentException("<Deploy> element must have <Standard/> deployment type in the web-beans.xml");
					}

					configureDeploymentTypes(child);
					DEPLOY_IS_DEFINED = true;

				}
			} else if (XMLUtil.isElementInterceptorsDeclaration(child))
			{
				if (INTERCEPTORS_IS_DEFINED)
				{
					throw new WebBeansConfigurationException("There can not be more than one web-beans.xml file that declares <Interceptors> element");
				} else
				{
					configureInterceptorsElement(child);
					INTERCEPTORS_IS_DEFINED = true;

				}
			}else if (XMLUtil.isElementDecoratosDeclaration(child))
			{
				if (DECORATORS_IS_DEFINED)
				{
					throw new WebBeansConfigurationException("There can not be more than one web-beans.xml file that declares <Decorators> element");
				} else
				{
					configureDecoratorsElement(child);
					DECORATORS_IS_DEFINED = true;

				}
			}

			if (!DEPLOY_IS_DEFINED)
			{
				DeploymentTypeManager.getInstance().addNewDeploymentType(Production.class, 1);
			}
		}
		
		//Configures the WebBeans components
		configureWebBeansComponents(webBeanDeclerationList);
		
	}
	
	private static void configureWebBeansComponents(List<Element> webBeanDecleration)
	{
		if(!webBeanDecleration.isEmpty())
		{
			Iterator<Element> it = webBeanDecleration.iterator();
			while (it.hasNext())
			{
				Element child = it.next();
				if (XMLUtil.isElementJMSDeclaration(child))
				{
					configureJMSEndpointComponent(child);
				} else
				{
					configureNewWebBeanComponent(child);
				}				
			}
		}
	}

	private static void configureInterceptorsElement(Element interceptorsElement)
	{
		List<Element> childs = interceptorsElement.elements();
		Iterator<Element> itChilds = childs.iterator();

		InterceptorsManager manager = InterceptorsManager.getInstance();
		while (itChilds.hasNext())
		{
			Element child = itChilds.next();
			Class<?> clazz = XMLUtil.getElementJavaType(child);

			if (clazz == null)
			{
				throw new WebBeansConfigurationException("Interceptor class : " + XMLUtil.getName(child) + " not found");
			} else
			{
				if (!AnnotationUtil.isMetaAnnotationExist(clazz.getAnnotations(), InterceptorBindingType.class))
				{
					throw new WebBeansConfigurationException("Interceptor class : " + XMLUtil.getName(child) + " must have at least one @InterceptorBindingType");
				}

				if (manager.isInterceptorEnabled(clazz))
				{
					throw new WebBeansConfigurationException("Interceptor class : " + XMLUtil.getName(child) + " is already defined");
				}

				manager.addNewInterceptor(clazz);
			}

		}

	}
	
	private static void configureDecoratorsElement(Element decoratorsElement)
	{
		List<Element> childs = decoratorsElement.elements();
		Iterator<Element> itChilds = childs.iterator();

		DecoratorsManager manager = DecoratorsManager.getInstance();
		while (itChilds.hasNext())
		{
			Element child = itChilds.next();
			Class<?> clazz = XMLUtil.getElementJavaType(child);

			if (clazz == null)
			{
				throw new WebBeansConfigurationException("Decorator class : " + XMLUtil.getName(child) + " not found");
			} else
			{

				if (manager.isDecoratorEnabled(clazz))
				{
					throw new WebBeansConfigurationException("Decorator class : " + XMLUtil.getName(child) + " is already defined");
				}

				manager.addNewDecorator(clazz);
			}

		}

	}
	

	private static void configureDeploymentTypes(Element deployElement)
	{
		List<Element> childs = deployElement.elements();
		Iterator<Element> itChilds = childs.iterator();

		int j = 1;
		while (itChilds.hasNext())
		{
			Element child = itChilds.next();
			Class<?> clazz = XMLUtil.getElementJavaType(child);

			if (clazz == null)
			{
				throw new WebBeansConfigurationException("@DeploymentType annotation with name : " + XMLUtil.getName(child) + " not found");
			} else
			{
				if (!clazz.isAnnotation())
					throw new WebBeansConfigurationException("@DeploymentType annotation with name : " + XMLUtil.getName(child) + " is not annotation type");
				else
				{
					Annotation ann = clazz.getAnnotation(DeploymentType.class);
					if (ann == null)
					{
						throw new WebBeansConfigurationException("@DeploymentType annotation with name : " + XMLUtil.getName(child) + " is not deployment type annotation");
					} else
					{
						DeploymentTypeManager.getInstance().addNewDeploymentType((Class<? extends Annotation>) clazz, j++);
					}
				}
			}
		}
	}

	/**
	 * Configures web beans element.
	 * 
	 * @param webBeanElement
	 *            web beans element
	 */
	private static void configureNewWebBeanComponent(Element webBeanElement)
	{
		String ns = XMLUtil.getElementNameSpace(webBeanElement);
		String packageName = WebBeansNameSpaceContainer.getInstance().getPackageNameFromNameSpace(ns);

		String className = packageName + XMLUtil.getName(webBeanElement);

		Class<?> clazz = ClassUtil.getClassFromName(className);

		if (clazz == null)
		{
			throw new NonexistentTypeException(createConfigurationFailedMessage() + "Class : " + className + " is not found");
		}

		boolean ok = false;

		if (EJBUtil.isEJBClass(clazz))
		{
			// Configure for EJB
			configureEJBWebBean(clazz);
			ok = true;
		} else
		{
			if (ClassUtil.isConcrete(clazz) && !ClassUtil.isParametrized(clazz))
			{
				if (SimpleWebBeansConfigurator.isSimpleWebBean(clazz))
				{
					// Configure Simple WebBean
					configureSimpleWebBean(clazz, webBeanElement);
					ok = true;
				}

			}
		}

		if (!ok)
		{
			// Actually this does not happen!
			throw new WebBeansConfigurationException(createConfigurationFailedMessage() + "Given Java class : " + clazz.getName() + " is not resolved to any WebBeans type in {Simple WebBeans, Enterprise WebBeans}");
		}

	}

	/**
	 * Configures the simple web bean from the class.
	 * 
	 * @param simpleClass
	 *            concrete java class
	 */
	private static <T> void configureSimpleWebBean(Class<T> simpleClass, Element webBeanDecleration)
	{
		XMLComponentImpl<T> component = new XMLComponentImpl<T>(simpleClass);

		DefinitionUtil.defineApiTypes(component, simpleClass);
		configureWebBeanDeclerationChilds(component, webBeanDecleration);

		// Check if the deployment type is enabled.
		if (!DeploymentTypeManager.getInstance().isDeploymentTypeEnabled(component.getDeploymentType()))
		{
			component = null;
			return;
		} else
		{
			// Add to manager
		}

	}

	private static <T> void configureWebBeanDeclerationChilds(XMLComponentImpl<T> component, Element webBeanDecleration)
	{
		List<Element> childs = webBeanDecleration.elements();
		Iterator<Element> it = childs.iterator();

		Element child = null;

		/* Constructor api type */
		List<Class<?>> constTypeList = new ArrayList<Class<?>>();

		List<Element> constElement = new ArrayList<Element>();

		Set<Annotation> annotationSet = new HashSet<Annotation>();

		boolean isConstructor = false;
		String fieldOrMethodName = null;
		while (it.hasNext())
		{
			child = it.next();
			Class<?> type = XMLUtil.getElementJavaType(child);

			if (type != null)
			{
				if (type.isAnnotation())
				{
					Class<? extends Annotation> annot = (Class<Annotation>) type;
					Annotation annotType = ClassUtil.getAnnotationLiteral(annot);

					annotationSet.add(annotType);

				} else
				{
					if (!isConstructor)
					{
						isConstructor = true;
					}

					constTypeList.add(type);
					constElement.add(child);

				}
			} else
			{
				// Namespace check
				if (XMLUtil.getElementNameSpace(child).equals(XMLUtil.getElementNameSpace(webBeanDecleration)))
				{
					String name = XMLUtil.getName(child);

					if (fieldOrMethodName != null)
					{
						if (fieldOrMethodName.equals(name))
						{
							throw new WebBeansConfigurationException(createConfigurationFailedMessage() + "This field/method with name : " + fieldOrMethodName + " is already defined.");
						}
					} else
					{
						fieldOrMethodName = name;
						configureFieldMethodMetaData(component, child, webBeanDecleration);
					}
				} else
				{
					throw new WebBeansConfigurationException(createConfigurationFailedMessage());
				}

			}

		}

		// Configure type-level metadata
		configureTypeLevelMetaData(component, annotationSet, webBeanDecleration);

		if (isConstructor)
		{
			// Configure constructor parameters
			configureConstructorMetaData(component, constTypeList, constElement, webBeanDecleration);
		}

	}

	private static <T> void configureTypeLevelMetaData(XMLComponentImpl<T> component, Set<Annotation> annotationSet, Element webBeanDecleration)
	{
		Annotation[] anns = new Annotation[annotationSet.size()];
		anns = annotationSet.toArray(anns);

		configureDeploymentType(component, anns);
		configureScopeType(component, anns);
		configureBindingType(component, anns);
		configureInterceptorBindingType(component, anns);
		configureStereoType(component, anns);
		configureNamed(component, anns);
		configureSpecializes(component, anns);
		configureInterceptor(component, anns);
		configureDecorator(component, anns);

	}

	private static <T> void configureConstructorMetaData(XMLComponentImpl<T> component, List<Class<?>> typeList, List<Element> typeElement, Element webBeanDecleration)
	{
		Class<T> implClazz = component.getReturnType();
		Constructor<T> cons[] = ClassUtil.getConstructors(implClazz);

		boolean found = false;
		for (Constructor<T> constructor : cons)
		{
			Class<?>[] pt = constructor.getParameterTypes();

			// same parameter size
			if (pt.length == typeList.size())
			{
				int j = 0;
				boolean ok = false;

				for (Class<?> p : pt)
				{
					Class<?> type = typeList.get(j);

					if (ClassUtil.isAssignable(p, type))
					{
						if (!p.equals(type))
						{
							// sub-type
							component.addConstructorApiType(type);
						}

						ok = true;
					} else
					{
						ok = false;
					}

					j++;
				}

				if (ok)
				{
					if (found)
					{
						throw new WebBeansConfigurationException(createConfigurationFailedMessage() + "More than one constructor decleration exist.");
					} else
					{
						found = true;
					}
				}

			}

		}

		if (!found)
		{
			throw new WebBeansConfigurationException(createConfigurationFailedMessage() + "Constructor decleration not found in the class.");
		}

	}

	private static <T> void configureFieldMethodMetaData(XMLComponentImpl<T> component, Element child, Element webBeanDecleration)
	{
		if (XMLUtil.isElementField(child))
		{
			configureField(component, child, webBeanDecleration);
		} else if (XMLUtil.isElementMethod(child))
		{
			configureMethod(component, child, webBeanDecleration);
		}
	}

	private static <T> void configureField(XMLComponentImpl<T> component, Element child, Element webBeanDecleration)
	{
		Class<?> clazz = component.getReturnType();
		String fieldName = XMLUtil.getName(child);
		Field field = ClassUtil.getFieldWithName(clazz, fieldName);

		if (!ClassUtil.classHasFieldWithName(clazz, fieldName))
		{
			throw new NonexistentFieldException(createConfigurationFailedMessage() + "Field name : " + fieldName + " decleration not found in the class : " + clazz.getName());
		}

		boolean isValueElement = false;

		if (child.isTextOnly())
		{
			if (!isValueElement)
			{
				isValueElement = true;
			}
		}

		List<Element> directChilds = child.elements();
		Iterator<Element> itChilds = directChilds.iterator();

		boolean isTypeElement = false;

		// it has some other elements
		while (itChilds.hasNext())
		{
			Element directChild = itChilds.next();
			String directChildName = XMLUtil.getName(directChild);

			if (XMLUtil.isElementInWebBeansNameSpaceWithName(directChild, WebBeansConstants.WEB_BEANS_XML_VALUE_ELEMENT))
			{
				if (!isValueElement)
				{
					isValueElement = true;
				}

			} else
			{
				// TODO injection field
				Class<?> directChildType = ClassUtil.getClassFromName(directChildName);

				if (!ClassUtil.isAssignable(field.getType(), directChildType))
				{
					throw new WebBeansConfigurationException(createConfigurationFailedMessage() + "Declared field type is not assginable to class field type");
				} else
				{
					if (!directChildType.equals(field.getType()))
					{
						component.addFieldApiType(directChildType, fieldName);

					}

					isTypeElement = true;
				}
			}
		}

		if (directChilds.size() > 1)
		{
			if (!isValueElement)
			{
				throw new WebBeansConfigurationException(createConfigurationFailedMessage() + "One of the direct childs of the field must be element <value>");
			} else
			{
				if (isValueElement && isTypeElement)
				{
					throw new WebBeansConfigurationException(createConfigurationFailedMessage() + "Direct child of the field can not contains both value element and type element");
				}
			}
		}

		// value decleration
		configureFieldValues(component, field, child, webBeanDecleration, isValueElement);

	}

	private static <T> void configureMethod(XMLComponentImpl<T> component, Element child, Element webBeanDecleration)
	{
		if (!ClassUtil.isMethodExistWithName(component.getReturnType(), XMLUtil.getName(child)))
		{
			throw new NonexistentMethodException(createConfigurationFailedMessage() + "Method declaration with name " + XMLUtil.getName(child) + " is not found in the class : " + component.getReturnType().getName());
		}

		List<Element> methodChilds = child.elements();
		Iterator<Element> itMethodChilds = methodChilds.iterator();

		boolean isDefineDisposes = false;
		boolean isDefineObserves = false;
		boolean isDefineInitializes = false;
		boolean isDefineDestructor = false;
		boolean isDefineProduces = false;

		List<Class<?>> methodParameters = new ArrayList<Class<?>>();

		while (itMethodChilds.hasNext())
		{
			Element methodChild = itMethodChilds.next();
			Class<?> childClazz = XMLUtil.getElementJavaType(methodChild);

			if (childClazz == null)
			{
				throw new NonexistentTypeException(createConfigurationFailedMessage() + "Direct child element of method : " + XMLUtil.getName(methodChild) + " does not corresponds to any Java type");
			} else
			{
				if (childClazz.isAnnotation())
				{
					if (childClazz.equals(Disposes.class))
					{
						if (isDefineDisposes)
						{
							throw new WebBeansConfigurationException(createConfigurationFailedMessage() + XMLUtil.getName(methodChild) + "method declaration can not contain more than one @Disposes annotation");
						} else
						{
							configureDisposes(component, methodChild);
							isDefineDisposes = true;
						}

					} else if (childClazz.equals(Observes.class))
					{
						if (isDefineObserves)
						{
							throw new WebBeansConfigurationException(createConfigurationFailedMessage() + XMLUtil.getName(methodChild) + "method declaration can not contain more than one @Observes annotation");
						} else
						{
							configureObserves(component, methodChild);
							isDefineObserves = true;
						}
					} else
					{
						if (childClazz.equals(Initializer.class))
						{
							if (isDefineInitializes)
							{
								throw new WebBeansConfigurationException(createConfigurationFailedMessage() + XMLUtil.getName(methodChild) + "method declaration can not contain more than one @Initializer annotation");
							} else
							{
								isDefineInitializes = true;
							}

						} else if (childClazz.equals(Destructor.class))
						{
							if (isDefineDestructor)
							{
								throw new WebBeansConfigurationException(createConfigurationFailedMessage() + XMLUtil.getName(methodChild) + "method declaration can not contain more than one @Destructor annotation");
							} else
							{
								isDefineDestructor = true;
							}

						} else if (childClazz.equals(Produces.class))
						{
							if (isDefineProduces)
							{
								throw new WebBeansConfigurationException(createConfigurationFailedMessage() + XMLUtil.getName(methodChild) + "method declaration can not contain more than one @Produces annotation");
							} else
							{
								isDefineProduces = true;
							}

						} else
						{
							throw new WebBeansConfigurationException(createConfigurationFailedMessage() + "Direct child element of method : " + XMLUtil.getName(methodChild) + " with Java type : " + childClazz + " is unknown");
						}
					}
				}
				// Method parameter
				else
				{
					methodParameters.add(childClazz);
				}
			}
		}

		List<Method> definedMethods = ClassUtil.getClassMethodsWithTypes(component.getReturnType(), XMLUtil.getName(child), methodParameters);

		if (definedMethods.size() == 0)
		{
			throw new NonexistentMethodException(createConfigurationFailedMessage() + "Method declaration with name " + XMLUtil.getName(child) + " is not found in the class : " + component.getReturnType().getName());
		} else if (definedMethods.size() > 1)
		{
			throw new WebBeansConfigurationException(createConfigurationFailedMessage() + "More than one method : " + XMLUtil.getName(child) + " is found in the class : " + component.getReturnType().getName());
		} else
		{
			Method result = definedMethods.get(0);
			Class<?>[] actualParams = result.getParameterTypes();
			int j = 0;
			for (Class<?> paramClazz : methodParameters)
			{
				if (actualParams[j].isAssignableFrom(paramClazz))
				{
					if (!actualParams[j].equals(paramClazz))
					{
						component.addMethodApiType(paramClazz, XMLUtil.getName(child));
					}
				}
			}
		}
	}

	private static <T> void configureDisposes(AbstractComponent<T> component, Element disposes)
	{
		List<Element> disposesChilds = disposes.elements();

		if (disposesChilds.size() > 1)
		{
			throw new WebBeansConfigurationException(createConfigurationFailedMessage() + "Disposes element : " + XMLUtil.getName(disposes) + " can not contain more than one direct child elements");
		}

	}

	private static <T> void configureObserves(AbstractComponent<T> component, Element observes)
	{
		List<Element> observesChilds = observes.elements();

		if (observesChilds.size() > 1)
		{
			throw new WebBeansConfigurationException(createConfigurationFailedMessage() + "Disposes element : " + XMLUtil.getName(observes) + " can not contain more than one direct child elements");
		} else
		{
			Element child = observesChilds.iterator().next();
			Class<?> clazz = XMLUtil.getElementJavaType(child);

			if (!clazz.equals(IfExists.class) || !clazz.equals(AfterTransactionCompletion.class) || !clazz.equals(AfterTransactionSuccess.class) || !clazz.equals(AfterTransactionFailure.class) || !clazz.equals(BeforeTransactionCompletion.class))
			{
				throw new WebBeansConfigurationException(createConfigurationFailedMessage() + "Disposes element : " + XMLUtil.getName(observes) + " must have one of the {<IfExists>, <AfterTransactionCompletion>, <AfterTransactionSuccess>, <AfterTransactionFailure>, <BeforeTransactionCompletion>} as a direct child.");
			}
		}

	}

	private static <T> void configureDeploymentType(XMLComponentImpl<T> component, Annotation[] anns)
	{
		Class<? extends Annotation> deploymentType = DefinitionUtil.defineDeploymentType(component, anns, createConfigurationFailedMessage() + "@DeploymentType annotation is not configured correctly");

		if (deploymentType == null)
		{
			DefinitionUtil.defineDeploymentType(component, component.getReturnType().getAnnotations(), createConfigurationFailedMessage() + "@DeploymentType annotation is not configured correctly");
		}
	}

	private static <T> void configureScopeType(XMLComponentImpl<T> component, Annotation[] anns)
	{
		boolean isDefined = AnnotationUtil.isMetaAnnotationExist(anns, ScopeType.class);
		if (isDefined)
		{
			DefinitionUtil.defineScopeType(component, anns, createConfigurationFailedMessage() + "@ScopeType annotation is not configured correctly");
		} else
		{
			DefinitionUtil.defineScopeType(component, component.getReturnType().getAnnotations(), createConfigurationFailedMessage() + "@ScopeType annotation is not configured correctly");
		}

	}

	/**
	 * Configures the binding types of the web beans component.
	 * 
	 * @param component web beans xml component
	 * @param anns annotations defined in the xml documents
	 */
	private static <T> void configureBindingType(XMLComponentImpl<T> component, Annotation[] anns)
	{
		boolean isDefined = AnnotationUtil.isMetaAnnotationExist(anns, BindingType.class);
		if (isDefined)
		{
			DefinitionUtil.defineBindingTypes(component, anns);
		} else
		{
			DefinitionUtil.defineBindingTypes(component, component.getReturnType().getAnnotations());
		}

	}

	private static <T> void configureInterceptorBindingType(XMLComponentImpl<T> component, Annotation[] anns)
	{

	}

	private static <T> void configureStereoType(XMLComponentImpl<T> component, Annotation[] anns)
	{
		WebBeansUtil.checkSteroTypeRequirements(component.getTypes(), component.getScopeType(), anns, "WebBeans XML configuration defined in " + CURRENT_SCAN_FILE_NAME + " is failed. Defined WebBeans ");

	}

	private static <T> void configureNamed(XMLComponentImpl<T> component, Annotation[] anns)
	{
		boolean isDefined = AnnotationUtil.isAnnotationExist(anns, Named.class);
		if (isDefined)
		{
			DefinitionUtil.defineName(component, anns, WebBeansUtil.getSimpleWebBeanDefaultName(component.getReturnType().getName()));
		} else
		{
			DefinitionUtil.defineName(component, component.getReturnType().getAnnotations(), WebBeansUtil.getSimpleWebBeanDefaultName(component.getReturnType().getName()));
		}
	}

	private static <T> void configureSpecializes(XMLComponentImpl<T> component, Annotation[] anns)
	{

	}

	private static <T> void configureInterceptor(XMLComponentImpl<T> component, Annotation[] anns)
	{

	}

	private static <T> void configureDecorator(XMLComponentImpl<T> component, Annotation[] anns)
	{

	}

	/**
	 * Configures the enterprise web bean from ejb class.
	 * 
	 * @param ejbClass
	 *            ejb class
	 */
	private static void configureEJBWebBean(Class<?> ejbClass)
	{
		// TODO EJB Decleration
	}

	/**
	 * Configures JMS endpoint.
	 * 
	 * @param webBeanElement
	 *            element
	 */
	private static void configureJMSEndpointComponent(Element webBeanElement)
	{
		// TODO JMS Endpoint
	}

	private static <T> void configureFieldValues(XMLComponentImpl<T> component, Field field, Element child, Element webBeanDecleration, boolean isValueElement)
	{
		if (isValueElement)
		{
			String errorMessage = createConfigurationFailedMessage() + "Field value of field name : " + field.getName() + " is not applicable for initial value assignment";
			Class<?> fieldType = field.getType();

			if (!ClassUtil.isInValueTypes(fieldType))
			{
				throw new WebBeansConfigurationException(createConfigurationFailedMessage() + "Field type with field name : " + field.getName() + " is not compatible for initial value assignment");
			}

			if (ClassUtil.isPrimitive(fieldType))
			{
				String value = child.getTextTrim();
				Object objVal = null;
				if ((objVal = ClassUtil.isValueOkForPrimitive(fieldType, value)) != null)
				{
					component.addFieldValue(field.getName(), objVal);
				} else
				{
					throw new WebBeansConfigurationException(errorMessage);
				}
			} else if (ClassUtil.isEnum(fieldType))
			{
				String value = child.getTextTrim();
				Enum enVal = ClassUtil.isValueOkForEnum(fieldType, value);

				if (enVal == null)
				{
					throw new WebBeansConfigurationException(errorMessage);
				}

				component.addFieldValue(field.getName(), enVal);
			} else if (fieldType.equals(String.class))
			{
				String value = child.getTextTrim();
				component.addFieldValue(field.getName(), value);

			} else if (fieldType.equals(Date.class) || fieldType.equals(Calendar.class))
			{
				String value = child.getTextTrim();
				Date date = ClassUtil.isValueOkForDate(value);

				if (date == null)
				{
					throw new WebBeansConfigurationException(errorMessage);
				} else
				{
					component.addFieldValue(field.getName(), value);
				}

			} else if (fieldType.equals(Class.class))
			{
				String value = child.getTextTrim();
				Class<?> clazz = ClassUtil.getClassFromName(value);

				if (clazz == null)
				{
					throw new WebBeansConfigurationException(errorMessage);
				} else
				{
					component.addFieldValue(field.getName(), value);
				}
			} else if (List.class.isAssignableFrom(fieldType))
			{
				boolean isString = false;
				Type type = field.getGenericType();
				Class<?> argClazz = null;
				List list = null;
				if (type instanceof ParameterizedType)
				{
					ParameterizedType pt = (ParameterizedType) type;
					Type arg = pt.getActualTypeArguments()[0];
					argClazz = (Class<?>) arg;

					if (((Class) arg).equals(String.class))
					{
						isString = true;
						list = new ArrayList<String>();
					} else if (Enum.class.isAssignableFrom(((Class) arg)))
					{
						isString = false;
						list = new ArrayList<Enum>();
					}
				} else
				{
					throw new WebBeansConfigurationException(createConfigurationFailedMessage() + "List field type with field name : " + field.getName() + " must be declared as ParametrizedType");
				}

				List<Element> valueElements = child.elements();
				for (Element valueElement : valueElements)
				{
					String value = valueElement.getTextTrim();
					if (isString)
					{
						list.add(value);
					} else
					{
						Enum en = ClassUtil.isValueOkForEnum(argClazz, value);
						if (en == null)
						{
							throw new WebBeansConfigurationException(errorMessage);
						} else
						{
							list.add(en);
						}
					}
				}

				component.addFieldValue(field.getName(), list);
			}
		}
	}

	private static String createConfigurationFailedMessage()
	{
		return "WebBeans XML configuration defined in " + CURRENT_SCAN_FILE_NAME + " is failed. Reason is : ";
	}

}
