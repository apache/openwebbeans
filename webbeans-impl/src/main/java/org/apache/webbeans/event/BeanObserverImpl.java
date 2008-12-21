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
package org.apache.webbeans.event;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import javax.webbeans.Dependent;
import javax.webbeans.Observer;
import javax.webbeans.Observes;
import javax.webbeans.manager.Context;

import org.apache.webbeans.annotation.CurrentLiteral;
import org.apache.webbeans.component.AbstractComponent;
import org.apache.webbeans.component.ObservesMethodsOwner;
import org.apache.webbeans.container.InjectionResolver;
import org.apache.webbeans.container.ManagerImpl;
import org.apache.webbeans.context.DependentContext;
import org.apache.webbeans.util.AnnotationUtil;

public class BeanObserverImpl<T> implements Observer<T>
{
	private ObservesMethodsOwner<?> bean;
	
	private Method observerMethod;
	
	private boolean ifExist;
	
	private TransactionalObserverType type;
	
	private ManagerImpl manager;
		
	public BeanObserverImpl(ObservesMethodsOwner<?> bean, Method observerMethod, boolean ifExist, TransactionalObserverType type)
	{
		this.bean = bean;
		this.observerMethod = observerMethod;
		this.ifExist = ifExist;
		this.type = type;
		this.manager = ManagerImpl.getManager();
	}

	public void notify(T event)
	{
		AbstractComponent<?> baseComponent = (AbstractComponent<?>) bean;
		Object object = null;

		boolean isActiveSet = false;
		DependentContext dependentContext = (DependentContext)ManagerImpl.getManager().getContext(Dependent.class);
		
		try
		{
			if(baseComponent.getScopeType().equals(Dependent.class))
			{
				dependentContext = (DependentContext)ManagerImpl.getManager().getContext(Dependent.class);
				
				if(!dependentContext.isActive())
				{
					isActiveSet = true;
					dependentContext.setActive(true);
					
				}
				
				object = ManagerImpl.getManager().getInstance(baseComponent);
			}
			
			Context context = manager.getContext(baseComponent.getScopeType());		
			if(ifExist)
			{
				object = context.get(baseComponent, false);
			}
			else
			{
				object = context.get(baseComponent, true);
			}
			
			if(object != null)
			{
				Object[] args = null;
				List<Object> argsObjects = getMethodArguments(event);
				
				args = new Object[argsObjects.size()];
				args = argsObjects.toArray(args);
				
				try
				{
					observerMethod.invoke(object, args);
					
				} catch (IllegalArgumentException e)
				{
					throw e;
					
				} catch (IllegalAccessException e)
				{
					throw new RuntimeException(e);
					
				} catch (InvocationTargetException e)
				{
					throw new RuntimeException(e);
				}finally
				{
					if(isActiveSet)
					{
						object = null;
					}
				}
				
			}
			
		}finally
		{
			if(isActiveSet)
			{
				dependentContext.setActive(false);
			}
		}
		
	}
	
	protected List<Object> getMethodArguments(Object event)
	{
		Type[] types = this.observerMethod.getGenericParameterTypes();
		Annotation[][] annots = this.observerMethod.getParameterAnnotations();
		List<Object> list = new ArrayList<Object>();
		
		if (types.length > 0)
		{
			int i = 0;
			for (Type type : types)
			{
				Annotation[] annot = annots[i];
				
				boolean observesAnnotation = false;
				
				if (annot.length == 0)
				{
					annot = new Annotation[1];
					annot[0] = new CurrentLiteral();
				}
				else
				{
					for(Annotation observersAnnot : annot)
					{
						if(observersAnnot.annotationType().equals(Observes.class))
						{
							list.add(event);
							observesAnnotation = true;
							break;
						}
					}
				}
				
				if(!observesAnnotation)
				{
					Type[] args = new Type[0];
					Class<?> clazz = null;
					if (type instanceof ParameterizedType)
					{
						ParameterizedType pt = (ParameterizedType) type;
						args = new Type[1];
						args = pt.getActualTypeArguments();

						clazz = (Class<?>) pt.getRawType();
					} else
					{
						clazz = (Class<?>) type;
					}
					
					Annotation[] bindingTypes = AnnotationUtil.getBindingAnnotations(annot);
					
					if(bindingTypes.length > 0)
					{
						list.add(manager.getInstance(InjectionResolver.getInstance().implResolveByType(clazz, args, bindingTypes).iterator().next()));	
					}
					else
					{
						list.add(null);
					}
					
				}				
				
				i++;
			}
		}
		
		return list;		
	}

	/**
	 * @return the bean
	 */
	public ObservesMethodsOwner<?> getBean()
	{
		return bean;
	}

	/**
	 * @return the type
	 */
	public TransactionalObserverType getType()
	{
		return type;
	}
	
	
	
}
