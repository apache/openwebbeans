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
package org.apache.webbeans.inject;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Set;

import javax.webbeans.Dependent;
import javax.webbeans.New;
import javax.webbeans.Fires;
import javax.webbeans.manager.Bean;

import org.apache.webbeans.component.AbstractComponent;
import org.apache.webbeans.container.InjectionResolver;
import org.apache.webbeans.container.ManagerImpl;
import org.apache.webbeans.container.ResolutionUtil;
import org.apache.webbeans.context.DependentContext;
import org.apache.webbeans.context.WebBeansContext;
import org.apache.webbeans.event.EventImpl;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.util.ClassUtil;
import org.apache.webbeans.util.WebBeansUtil;

/**
 * Abstract implementation of the {@link Injectable} contract.
 * 
 * @author <a href="mailto:gurkanerdogdu@yahoo.com">Gurkan Erdogdu</a>
 * @since 1.0
 */
@SuppressWarnings("unchecked")
public abstract class AbstractInjectable implements Injectable
{
	/** Owner component */
	private AbstractComponent<?> injectionOwnerComponent;
	
	protected AbstractInjectable(AbstractComponent<?> component)
	{
		this.injectionOwnerComponent = component;
	}

	/**
	 * Gets the injected component instance in its scoped context.
	 * 
	 * @param type
	 *            type of the injection point, maybe parametrized type
	 * @param annotations
	 *            binding annotations at the injection point
	 * @return current component instance in the resolved component scope
	 */
	public <T> Object inject(Class<T> type, Type[] args, Annotation... annotations)
	{
		DependentContext context = (DependentContext)ManagerImpl.getManager().getContext(Dependent.class);
		boolean isSetOnThis = false;
		
		try
		{
			if(!context.isActive())
			{
				context.setActive(true);
				isSetOnThis = true;
			}
			
			if (isNewBinding(annotations))
			{
				return injectForNew(type, annotations);
			}
			
			if(isObservableBinding(annotations))
			{
				return injectForObservable(args, annotations);
			}
			
			Set<Bean<T>> componentSet = InjectionResolver.getInstance().implResolveByType(type, args, annotations);
			ResolutionUtil.checkResolvedBeans(componentSet, type);
			
			AbstractComponent<?> component = (AbstractComponent<?>)componentSet.iterator().next();
			
			/*Nullable check*/
			WebBeansUtil.checkNullable(type, component);

			if (component.getScopeType().equals(Dependent.class))
			{
				return injectForDependent(component);
				
			} else
			{
				return injectForComponent(component);
			}
			
		}finally
		{
			if(isSetOnThis)
			{
				context.setActive(false);	
			}
		}
		
	}
	
	private boolean isNewBinding(Annotation...annotations)
	{
		if (annotations.length == 1)
		{
			if (annotations[0].annotationType().equals(New.class))
			{
				return true;
			}
		}
		
		return false;
	}
	
	private boolean isObservableBinding(Annotation...annotations)
	{
		for(Annotation ann : annotations)
		{
			if(ann.annotationType().equals(Fires.class))
			{
				return true;
			}
		}
		
		return false;
	}
	

	private Object injectForNew(Type type, Annotation... annotations)
	{
		Class<?> clazz = null;

		if (type instanceof ParameterizedType)
		{
			clazz = (Class<?>) ((ParameterizedType) type).getRawType();
		} else if (type instanceof Class)
		{
			clazz = (Class<?>) type;
		}

		return injectForDependent(WebBeansUtil.createNewComponent(clazz));
	}
	
	
	private <T> Object injectForObservable(Type[] args, Annotation... annotations)
	{
		Class<?> eventType = (Class<?>) args[0];
		
		return injectForDependent(WebBeansUtil.createObservableImplicitComponent(EventImpl.class, eventType, annotations));
	}

	private Object injectForDependent(AbstractComponent<?> component)
	{
		Object object = null;
		object = this.injectionOwnerComponent.getDependent(component);

		return object;
	}

	private Object injectForComponent(AbstractComponent<?> component)
	{
		WebBeansContext context = null;
		Object object = null;
		
		context = (WebBeansContext) ManagerImpl.getManager().getContext(component.getScopeType());
		object = context.get(component, true);

			return object;

	}
	
	protected void checkParametrizedTypeForInjectionPoint(ParameterizedType pType)
	{
		if(!ClassUtil.checkParametrizedType(pType))
		{
			throw new WebBeansConfigurationException("Injection point with parametrized type : " + pType + " can not define Type variable or Wildcard type");
		}
	}

	/**
	 * Gets the component.
	 * 
	 * @return the component
	 */
	public AbstractComponent<?> getInjectionOwnerComponent()
	{
		return injectionOwnerComponent;
	}

}