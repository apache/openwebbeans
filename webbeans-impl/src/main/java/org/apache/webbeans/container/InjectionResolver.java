/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.apache.webbeans.container;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.webbeans.manager.Bean;

import org.apache.webbeans.annotation.CurrentLiteral;
import org.apache.webbeans.component.ProducerComponentImpl;
import org.apache.webbeans.config.WebBeansFinder;
import org.apache.webbeans.deployment.DeploymentTypeManager;
import org.apache.webbeans.util.AnnotationUtil;
import org.apache.webbeans.util.Asserts;
import org.apache.webbeans.util.ClassUtil;

@SuppressWarnings("unchecked")
public class InjectionResolver
{
	public InjectionResolver()
	{

	}

	public static InjectionResolver getInstance()
	{
		InjectionResolver instance = (InjectionResolver) WebBeansFinder.getSingletonInstance(WebBeansFinder.SINGLETON_INJECTION_RESOLVER);
		return instance;
	}

	public Set<Bean<?>> implResolveByName(String name)
	{
		Asserts.assertNotNull(name, "name parameter can not be null");

		ManagerImpl manager = ManagerImpl.getManager();

		Set<Bean<?>> resolvedComponents = new HashSet<Bean<?>>();
		Bean<?> resolvedComponent = null;
		Set<Bean<?>> deployedComponents = manager.getBeans();

		Iterator<Bean<?>> it = deployedComponents.iterator();
		while (it.hasNext())
		{
			Bean<?> component = it.next();

			if (component.getName() != null)
			{
				if (component.getName().equals(name))
				{
					if (resolvedComponent == null)
					{
						resolvedComponent = component;
						resolvedComponents.add(resolvedComponent);
					} else
					{
						if (DeploymentTypeManager.getInstance().comparePrecedences(component.getDeploymentType(), resolvedComponent.getDeploymentType()) > 0)
						{
							resolvedComponents.clear();
							resolvedComponent = component;
							resolvedComponents.add(resolvedComponent);
						} else if (DeploymentTypeManager.getInstance().comparePrecedences(component.getDeploymentType(), resolvedComponent.getDeploymentType()) == 0)
						{
							resolvedComponents.add(component);
						}
					}
				}
			}
		}

		return resolvedComponents;
	}

	public <T> Set<Bean<T>> implResolveByType(Class<?> apiType, Type[] actualTypeArguments, Annotation... binding)
	{
		Asserts.assertNotNull(apiType, "apiType parameter can not be null");
		Asserts.assertNotNull(binding, "binding parameter can not be null");

		ManagerImpl manager = ManagerImpl.getManager();

		boolean currentBinding = false;
		boolean returnAll = false;

		if (binding.length == 0)
		{
			binding = new Annotation[1];
			binding[0] = new CurrentLiteral();
			currentBinding = true;
		}

		Set<Bean<T>> results = new HashSet<Bean<T>>();
		Set<Bean<?>> deployedComponents = manager.getBeans();

		if (apiType.equals(Object.class) && currentBinding)
		{
			returnAll = true;
		}

		Iterator<Bean<?>> it = deployedComponents.iterator();

		while (it.hasNext())
		{
			Bean<?> component = it.next();

			if (returnAll)
			{
				results.add((Bean<T>) component);
				continue;
			}

			else
			{
				Set<Class<?>> componentApiTypes = component.getTypes();
				Iterator<Class<?>> itComponentApiTypes = componentApiTypes.iterator();
				while (itComponentApiTypes.hasNext())
				{
					Class<?> componentApiType = itComponentApiTypes.next();

					if (actualTypeArguments.length > 0)
					{
						Type[] actualArgs = null;
						if (ClassUtil.isAssignable(apiType, componentApiType))
						{
							/*
							 * Annotated Producer method or XML Defined Producer
							 * Method
							 */
							if (ProducerComponentImpl.class.isAssignableFrom(component.getClass()))
							{
								actualArgs = ((ProducerComponentImpl<?>) component).getActualTypeArguments();
								if (Arrays.equals(actualArgs, actualTypeArguments))
								{
									results.add((Bean<T>) component);
									break;
								}

							}

							else
							{
								actualArgs = ClassUtil.getGenericSuperClassTypeArguments(componentApiType);
								if (Arrays.equals(actualArgs, actualTypeArguments))
								{
									results.add((Bean<T>) component);
									break;
								} else
								{
									List<Type[]> listActualArgs = ClassUtil.getGenericSuperInterfacesTypeArguments(componentApiType);
									Iterator<Type[]> itListActualArgs = listActualArgs.iterator();
									while (itListActualArgs.hasNext())
									{
										actualArgs = itListActualArgs.next();

										if (Arrays.equals(actualArgs, actualTypeArguments))
										{
											results.add((Bean<T>) component);
											break;
										}

									}
								}

							}

						}
					} else
					{
						if (apiType instanceof Class)
						{
							if (ClassUtil.isAssignable((Class<?>) apiType, componentApiType))
							{
								results.add((Bean<T>) component);
								break;
							}
						}
					}
				}
			}
		}

		results = findByBindingType(results, binding);

		if (results != null && !results.isEmpty())
		{
			results = findByPrecedence(results);
		}

		return results;
	}

	private <T> Set<Bean<T>> findByPrecedence(Set<Bean<T>> result)
	{
		Bean<T> resolvedComponent = null;
		Iterator<Bean<T>> it = result.iterator();
		Set<Bean<T>> res = new HashSet<Bean<T>>();

		while (it.hasNext())
		{
			Bean<T> component = it.next();

			if (resolvedComponent == null)
			{
				resolvedComponent = component;
				res.add(resolvedComponent);
			} else
			{
				DeploymentTypeManager typeManager = DeploymentTypeManager.getInstance();

				if (typeManager.comparePrecedences(component.getDeploymentType(), resolvedComponent.getDeploymentType()) < 0)
				{
					continue;
				} else if (typeManager.comparePrecedences(component.getDeploymentType(), resolvedComponent.getDeploymentType()) > 0)
				{
					res.clear();
					resolvedComponent = component;
					res.add(resolvedComponent);

				} else
				{
					res.add(component);
				}
			}
		}

		return res;
	}

	private <T> Set<Bean<T>> findByBindingType(Set<Bean<T>> remainingSet, Annotation... annotations)
	{
		Iterator<Bean<T>> it = remainingSet.iterator();
		Set<Bean<T>> result = new HashSet<Bean<T>>();

		while (it.hasNext())
		{
			Bean<T> component = it.next();
			Set<Annotation> bTypes = component.getBindingTypes();

			int i = 0;
			for (Annotation annot : annotations)
			{
				Iterator<Annotation> itBindingTypes = bTypes.iterator();
				while (itBindingTypes.hasNext())
				{
					Annotation bindingType = itBindingTypes.next();
					if (annot.annotationType().equals(bindingType.annotationType()))
					{
						if (AnnotationUtil.isAnnotationMemberExist(bindingType.annotationType(), bindingType, annot))
						{
							i++;
						}
					}

				}
			}

			if (i == annotations.length)
			{
				result.add(component);
				i = 0;
			}

		}

		remainingSet = null;

		return result;
	}
}