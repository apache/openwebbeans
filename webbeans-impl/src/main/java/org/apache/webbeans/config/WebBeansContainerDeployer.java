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
package org.apache.webbeans.config;

import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;



import javax.webbeans.Decorator;
import javax.webbeans.InconsistentSpecializationException;
import javax.webbeans.Interceptor;
import javax.webbeans.ScopeType;
import javax.webbeans.Specializes;
import javax.webbeans.Stereotype;
import javax.webbeans.manager.Bean;

import org.apache.webbeans.WebBeansConstants;
import org.apache.webbeans.component.ComponentImpl;
import org.apache.webbeans.component.WebBeansType;
import org.apache.webbeans.container.ManagerImpl;
import org.apache.webbeans.decorator.DecoratorUtil;
import org.apache.webbeans.deployment.StereoTypeManager;
import org.apache.webbeans.deployment.StereoTypeModel;
import org.apache.webbeans.exception.WebBeansDeploymentException;
import org.apache.webbeans.logger.WebBeansLogger;
import org.apache.webbeans.util.AnnotationUtil;
import org.apache.webbeans.util.ClassUtil;
import org.apache.webbeans.util.JNDIUtil;
import org.apache.webbeans.util.WebBeansUtil;
import org.apache.webbeans.xml.WebBeansXMLConfigurator;
import org.apache.webbeans.xml.XMLAnnotationTypeManager;
import org.apache.webbeans.xml.XMLSpecializesManager;


/**
 * Deploys the all components that are defined in the {@link WebBeansScanner} at 
 * the scanner phase.
 * 
 * @author <a href="mailto:gurkanerdogdu@yahoo.com">Gurkan Erdogdu</a>
 * @since 1.0
 */
@SuppressWarnings("unchecked")
public final class WebBeansContainerDeployer
{
	private static WebBeansLogger logger = WebBeansLogger.getLogger(WebBeansContainerDeployer.class);
	
	/**Check if deployment is already done*/
	private static boolean deployed = false;
	
	/**
	 * Private constructor
	 */
	private WebBeansContainerDeployer()
	{
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Deploys all the defined web beans components in the container startup.
	 * <p>
	 * It deploys from the web-beans.xml files and from the class files. It uses the
	 * {@link WebBeansScanner} class to get classes.
	 * </p>
	 * @throws WebBeansDeploymentException if any deployment exception occurs
	 */
	public static void deploy() throws WebBeansDeploymentException
	{
		WebBeansScanner scanner = WebBeansScanner.getScannerInstance();
		try
		{
			if(!deployed)
			{
				//Register Manager built-in component
				ManagerImpl.getManager().addBean(WebBeansUtil.getManagerComponent());
				
				//Register Conversation built-in component
				ManagerImpl.getManager().addBean(WebBeansUtil.getConversationComponent());
				
				//JNDI bind
				JNDIUtil.bind(WebBeansConstants.WEB_BEANS_MANAGER_JNDI_NAME, ManagerImpl.getManager());
				
				deployFromXML(scanner);
				checkStereoTypes();				
				configureInterceptors(scanner);
				configureDecorators(scanner);	
				deployFromClassPath(scanner);
				checkSpecializations();
				
				deployed = true;
			}
			
		}catch(Throwable e)
		{
			throw new WebBeansDeploymentException(e);
		}
	}
	
	private static void deployFromClassPath(WebBeansScanner scanner) throws ClassNotFoundException
	{
		logger.info("Deploying configurations from class files is started");
		
		//Start from the class
		Map<String, Set<String>> classIndex = scanner.getANNOTATION_DB().getClassIndex();
		if(classIndex != null)
		{
			Set<String> pathClasses = classIndex.keySet();
			Iterator<String> itPathClasses = pathClasses.iterator();
			
			while(itPathClasses.hasNext())
			{
				String componentClassName = itPathClasses.next();
				Class<?> implClass = Class.forName(componentClassName);
				
				if(SimpleWebBeansConfigurator.isSimpleWebBean(implClass))
				{
					logger.info("Simple WebBeans Component with class name : " + componentClassName + " is found");
					defineSimpleWebBeans(implClass);
				}
				else if(EJBWebBeansConfigurator.isEJBWebBean(implClass))
				{
					logger.info("Enterprise WebBeans Component with class name : " + componentClassName + " is found");
					defineEnterpriseWebBeans();
				}						
			}					
		}
		
		logger.info("Deploying configurations from class files is ended");
		
	}
	
	private static void deployFromXML(WebBeansScanner scanner)
	{
		logger.info("Deploying configurations from XML files is started");
		
		Map<String, InputStream> xmls = scanner.getWEBBEANS_XML_LOCATIONS();
		Set<String> keySet = xmls.keySet();
		Iterator<String> it = keySet.iterator();
		
		while(it.hasNext())
		{
			String fileName = it.next();
			WebBeansXMLConfigurator.configure(xmls.get(fileName), fileName);
		}
		
		logger.info("Deploying configurations from XML is ended succesfully");
	}
	
	private static void configureInterceptors(WebBeansScanner scanner) throws ClassNotFoundException
	{
		logger.info("Configuring the Interceptors is started");
		
		//Interceptors Set
		Map<String, Set<String>> annotIndex = scanner.getANNOTATION_DB().getAnnotationIndex();
		Set<String> classes = annotIndex.get(Interceptor.class.getName());
		
		if(classes != null)
		{
			for(String interceptorClazz : classes)
			{
				Class<?> implClass = Class.forName(interceptorClazz);
					
				logger.info("Simple WebBeans Interceptor Component with class name : " + interceptorClazz + " is found");
				
				defineSimpleWebBeansInterceptors(implClass);
			}	
		}
		
		logger.info("Configuring the Interceptors is ended");
		
	}
	
	private static void configureDecorators(WebBeansScanner scanner) throws ClassNotFoundException
	{
		logger.info("Configuring the Decorators is started");
		
		Map<String, Set<String>> annotIndex = scanner.getANNOTATION_DB().getAnnotationIndex();
		Set<String> classes = annotIndex.get(Decorator.class.getName());
		
		if(classes != null)
		{
			for(String decoratorClazz : classes)
			{
				Class<?> implClass = Class.forName(decoratorClazz);
				logger.info("Simple WebBeans Decorator Component with class name : " + decoratorClazz + " is found");
				
				defineSimpleWebBeansDecorators(implClass);
			}					
		}
		
		logger.info("Configuring the Decorators is ended");
		
	}
	
	private static void checkSpecializations()
	{
		logger.info("Checking Specialization constraints is started");
		
		  Map<String, Set<String>>  specialMap = WebBeansScanner.getScannerInstance().getANNOTATION_DB().getAnnotationIndex();
		  if(specialMap != null && specialMap.size() > 0)
		  {
			  if(specialMap.containsKey(Specializes.class.getName()))
			  {
				  Set<String> specialClassSet = specialMap.get(Specializes.class.getName());
				  Iterator<String> specialIterator = specialClassSet.iterator();
				  
				  Class<?> superClass = null;
				  while(specialIterator.hasNext())
				  {
					  String specialClassName = specialIterator.next();
					  Class<?> specialClass = ClassUtil.getClassFromName(specialClassName);
					  
					  if(superClass == null)
					  {
						  superClass = specialClass.getSuperclass();
					  }
					  else
					  {
						  if(superClass.equals(specialClass.getSuperclass()))
						  {
							  throw new InconsistentSpecializationException("More than one class that specialized the same super class : " + superClass.getName());
						  }
					  }
					  
					  WebBeansUtil.configureSpecializations(specialClass);
				  }				  
			  }
		  }
		  
		  //XML Defined Specializations
		  checkXMLSpecializations();
		  
		  logger.info("Checking Specialization constraints is ended");
	}
	
	private static void checkXMLSpecializations()
	{
		  //Check XML specializations
		  Set<Class<?>> clazzes = XMLSpecializesManager.getInstance().getXMLSpecializationClasses();
		  Iterator<Class<?>> it = clazzes.iterator();
		  Class<?> superClass = null;
		  Class<?> specialClass = null;
		  while(it.hasNext())
		  {
			  specialClass = it.next();
			  
			  if(superClass == null)
			  {
				  superClass = specialClass.getSuperclass();
			  }
			  else
			  {
				  if(superClass.equals(specialClass.getSuperclass()))
				  {
					  throw new InconsistentSpecializationException("XML Specialization Error : More than one class that specialized the same super class : " + superClass.getName());
				  }
			  }
			  
			  WebBeansUtil.configureSpecializations(specialClass);
			  
		  }		
	}
	
	public static void checkPassivationScopes()
	{
		Set<Bean<?>> beans = ManagerImpl.getManager().getBeans();
		
		if(beans != null && beans.size() > 0)
		{
			Iterator<Bean<?>> itBeans = beans.iterator();
			while(itBeans.hasNext())
			{
				Object beanObj = itBeans.next();
				if(beanObj instanceof ComponentImpl)
				{
					ComponentImpl<?> component = (ComponentImpl<?>)beanObj;
					ScopeType scope = component.getScopeType().getAnnotation(ScopeType.class);
					if(scope.passivating())
					{
						//Check constructor
						
						//Check non-transient fields
						
						//Check initializer methods
						
						//Check producer methods
					}
				}
			}
		}
	}
	
	public static void checkStereoTypes()
	{
		logger.info("Checking StereoTypes constraints is started");
		  
		Map<String, Set<String>>  stereotypeMap = WebBeansScanner.getScannerInstance().getANNOTATION_DB().getAnnotationIndex();
		  if(stereotypeMap != null && stereotypeMap.size() > 0)
		  {	
			  if(stereotypeMap.containsKey(Stereotype.class.getName()))
			  {
				  Set<String> stereoClassSet = stereotypeMap.keySet();
				  Iterator<String> steIterator = stereoClassSet.iterator();
				  while(steIterator.hasNext())
				  {
					  String steroClassName = steIterator.next();
					  Set<String> stereoSet = stereotypeMap.get(steroClassName);
					  
					  for(String clazz : stereoSet)
					  {
						  Class<? extends Annotation> stereoClass = (Class<? extends Annotation>)ClassUtil.getClassFromName(clazz);
						  
						  if(!XMLAnnotationTypeManager.getInstance().isStereoTypeExist(stereoClass))
						  {
							  WebBeansUtil.checkStereoTypeClass(stereoClass);
							  StereoTypeModel model = new StereoTypeModel(stereoClass);
							  StereoTypeManager.getInstance().addStereoTypeModel(model);					  							  
						  }
					  }					  
				  }
				  
			  }
		  }
		  
		  logger.info("Checking StereoTypes constraints is ended");
	}
	
	
	private static <T> void defineSimpleWebBeans(Class<T> clazz)
	{
		ComponentImpl<T> component = null;
		
		if(!AnnotationUtil.isAnnotationExistOnClass(clazz, Interceptor.class) 
				&& !AnnotationUtil.isAnnotationExistOnClass(clazz, Decorator.class))
		{
			component = SimpleWebBeansConfigurator.define(clazz, WebBeansType.SIMPLE);
			if(component != null)
			{	
				DecoratorUtil.checkSimpleWebBeanDecoratorConditions(component);
				DefinitionUtil.defineSimpleWebBeanInterceptorStack(component);
				
				ManagerImpl.getManager().addBean(component);					
			}			
		}
	}
	
	/**
	 * Defines the new interceptor with given class.
	 * 
	 * @param clazz interceptor class
	 */
	private static <T> void defineSimpleWebBeansInterceptors(Class<T> clazz)
	{
		WebBeansUtil.defineSimpleWebBeansInterceptors(clazz);
	}
	
	private static <T> void defineSimpleWebBeansDecorators(Class<T> clazz)
	{
		WebBeansUtil.defineSimpleWebBeansDecorators(clazz);
	}
	
	
	private static void defineEnterpriseWebBeans()
	{
		
	}
}