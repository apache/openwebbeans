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
package org.apache.webbeans.container;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.Referenceable;
import javax.naming.StringRefAddr;
import javax.webbeans.AmbiguousDependencyException;
import javax.webbeans.ContextNotActiveException;
import javax.webbeans.Dependent;
import javax.webbeans.Observer;
import javax.webbeans.TypeLiteral;
import javax.webbeans.manager.Bean;
import javax.webbeans.manager.Context;
import javax.webbeans.manager.Decorator;
import javax.webbeans.manager.InterceptionType;
import javax.webbeans.manager.Interceptor;
import javax.webbeans.manager.Manager;

import org.apache.webbeans.component.AbstractComponent;
import org.apache.webbeans.config.WebBeansFinder;
import org.apache.webbeans.context.ContextFactory;
import org.apache.webbeans.context.DependentContext;
import org.apache.webbeans.decorator.DecoratorComparator;
import org.apache.webbeans.decorator.WebBeansDecorator;
import org.apache.webbeans.decorator.WebBeansDecoratorConfig;
import org.apache.webbeans.event.NotificationManager;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.intercept.InterceptorComparator;
import org.apache.webbeans.intercept.WebBeansInterceptorConfig;
import org.apache.webbeans.intercept.webbeans.WebBeansInterceptor;
import org.apache.webbeans.util.Asserts;
import org.apache.webbeans.util.ClassUtil;
import org.apache.webbeans.util.WebBeansUtil;


/**
 * Implementation of the {@link WebBeansManager} contract of the web beans
 * container.
 * 
 * @author <a href="mailto:gurkanerdogdu@yahoo.com">Gurkan Erdogdu</a>
 * @since 1.0
 * 
 * @see java.webbeans.WebBeansManager
 */
@SuppressWarnings("unchecked")
public class ManagerImpl implements Manager, Referenceable
{
	private Map<Class<? extends Annotation>, Context> contextMap = new ConcurrentHashMap<Class<? extends Annotation>, Context>();
	
	private Set<Bean<?>> components = new CopyOnWriteArraySet<Bean<?>>();
	
	private Set<Interceptor> webBeansInterceptors = new CopyOnWriteArraySet<Interceptor>();
	
	private Set<Decorator> webBeansDecorators = new CopyOnWriteArraySet<Decorator>();

	private NotificationManager notificationManager = null;
	
	private InjectionResolver injectionResolver = null;
	
	
	public ManagerImpl()
	{		
		injectionResolver = InjectionResolver.getInstance();
		notificationManager = NotificationManager.getInstance();
	}
	
	public static ManagerImpl getManager()
	{
		ManagerImpl instance = (ManagerImpl) WebBeansFinder.getSingletonInstance(WebBeansFinder.SINGLETON_MANAGER);
		
		return instance;
	}
	
	public Context getContext(Class<? extends Annotation> scopType)
	{
		Asserts.assertNotNull(scopType, "scopeType paramter can not be null");		
		
		Context ctx = null;
		
		ctx = ContextFactory.getStandartContext(scopType);
		
		if(ctx == null)
		{
			ctx = getManager().contextMap.get(scopType);
		}

		//Still null
		if(ctx == null)
		{
			throw new ContextNotActiveException("WebBeans context with scope type annotation @" + scopType.getSimpleName() +  " is not exist within current thread");	
		}			

		return ctx;
	}

	public Manager addBean(Bean<?> component)
	{
		getManager().components.add(component);
		
		return this;
	}

	public Manager addContext(Context context)
	{
		addContext(context.getScopeType(), context);
		
		return this;
		
	}

	public void fireEvent(Object event, Annotation... bindings)
	{
		if(ClassUtil.isParametrized(event.getClass()))
		{
			throw new WebBeansConfigurationException("Event class : " + event.getClass().getName() + " can not be defined as generic type");
		}
		
		this.notificationManager.fireEvent(event, bindings);
	}

	public Object getInstanceByName(String name)
	{
		AbstractComponent<?> component = null;
		Object object = null;

		Set<Bean<?>> set = this.injectionResolver.implResolveByName(name);
		if(set.isEmpty())
		{
			return null;
		}
		
		if(set.size() > 1)
		{
			throw new AmbiguousDependencyException("There are more than one WebBeans with name : " + name);
		}			
		
		component = (AbstractComponent<?>) set.iterator().next();
		
		object = getInstance(component);
			
		return object;
	}

	public <T> T getInstanceByType(Class<T> type, Annotation... bindingTypes)
	{	
		ResolutionUtil.getInstanceByTypeConditions(bindingTypes);
		Set<Bean<T>> set = resolveByType(type, bindingTypes);
		
		ResolutionUtil.checkResolvedBeans(set, type);
		
		return getInstance(set.iterator().next());
	}

	public <T> T getInstanceByType(TypeLiteral<T> type, Annotation... bindingTypes)
	{
		ResolutionUtil.getInstanceByTypeConditions(bindingTypes);
		Set<Bean<T>> set = resolveByType(type, bindingTypes);
		
		ResolutionUtil.checkResolvedBeans(set, type.getRawType());
		
		return getInstance(set.iterator().next());
	}

	public Set<Bean<?>> resolveByName(String name)
	{
		return this.injectionResolver.implResolveByName(name);
	}

	public <T> Set<Bean<T>> resolveByType(Class<T> apiType, Annotation... bindingTypes)
	{
		return this.injectionResolver.implResolveByType(apiType, new Type[0], bindingTypes);
	}

	public <T> Set<Bean<T>> resolveByType(TypeLiteral<T> apiType, Annotation... bindingTypes)
	{
		ParameterizedType ptype = (ParameterizedType)apiType.getType();
		ResolutionUtil.resolveByTypeConditions(ptype);
		
		Type[] args = ptype.getActualTypeArguments();
		return this.injectionResolver.implResolveByType(apiType.getRawType(),args , bindingTypes);
	}

	public <T> Set<Observer<T>> resolveObservers(T event, Annotation... bindings)
	{
		return this.notificationManager.resolveObservers(event, bindings);
	}

	public  Set<Bean<?>> getComponents()
	{
		return getManager().components;
	}

	public Manager addDecorator(Decorator decorator)
	{
		getManager().webBeansDecorators.add(decorator);
		return this;
	}

	public Manager addInterceptor(Interceptor interceptor)
	{
		getManager().webBeansInterceptors.add(interceptor);
		return this;
	}

	public <T> Manager addObserver(Observer<T> observer, Class<T> eventType, Annotation... bindings)
	{
		this.notificationManager.addObserver(observer, eventType, bindings);
		return this;
	}

	public <T> Manager addObserver(Observer<T> observer, TypeLiteral<T> eventType, Annotation... bindings)
	{	
		this.notificationManager.addObserver(observer, eventType, bindings);
		return this;
	}

	public <T> T getInstance(Bean<T> bean)
	{
		Context context = null;
		DependentContext dependentContext = null;
		T instance = null;
		boolean isSetOnThis = false;
		
		try
		{
			dependentContext = (DependentContext)getContext(Dependent.class);
			if(!dependentContext.isActive())
			{
				dependentContext.setActive(true);	
				isSetOnThis = true;
			}
			
			context = getContext(bean.getScopeType());
			instance = context.get(bean, true);
			
		}finally
		{
			if(isSetOnThis)
			{
				dependentContext.setActive(false);	
			}
		}
		
		return instance;
	}

	public <T> Manager removeObserver(Observer<T> observer, Class<T> eventType, Annotation... bindings)
	{
		this.notificationManager.removeObserver(observer, eventType, bindings);
		return this;
	}

	public <T> Manager removeObserver(Observer<T> observer, TypeLiteral<T> eventType, Annotation... bindings)
	{
		this.notificationManager.removeObserver(observer, eventType, bindings);
		return this;
	}

	public List<Decorator> resolveDecorators(Set<Class<?>> types, Annotation... bindingTypes)
	{
		WebBeansUtil.checkDecoratorResolverParams(types, bindingTypes);
		Set<Decorator> intsSet = WebBeansDecoratorConfig.findDeployedWebBeansDecorator(types, bindingTypes);
		Iterator<Decorator> itSet = intsSet.iterator();
		
		List<Decorator> decoratorList = new ArrayList<Decorator>();
		while(itSet.hasNext())
		{
			WebBeansDecorator decorator = (WebBeansDecorator)itSet.next();
			decoratorList.add(decorator);
			
		}
		
		Collections.sort(decoratorList, new DecoratorComparator());
		
		return decoratorList;
		
	}

	public List<Interceptor> resolveInterceptors(InterceptionType type, Annotation... interceptorBindings)
	{
		WebBeansUtil.checkInterceptorResolverParams(interceptorBindings);
		
		Set<Interceptor> intsSet = WebBeansInterceptorConfig.findDeployedWebBeansInterceptor(interceptorBindings);
		Iterator<Interceptor> itSet = intsSet.iterator();
		
		List<Interceptor> interceptorList = new ArrayList<Interceptor>();
		while(itSet.hasNext())
		{
			WebBeansInterceptor interceptor = (WebBeansInterceptor)itSet.next();
			
			if(interceptor.getMethod(type) != null)
			{
				interceptorList.add(interceptor);	
			}
			
		}
		
		Collections.sort(interceptorList, new InterceptorComparator());
		
		return interceptorList;
	}
		
	public Set<Bean<?>> getBeans()
	{
		return getManager().components;
	}
	
	public Set<Interceptor> getInterceptors()
	{
		return getManager().webBeansInterceptors;
	}
	
	public Set<Decorator> getDecorators()
	{
		return getManager().webBeansDecorators;
	}
	
	private void addContext(Class<? extends Annotation> scopeType, javax.webbeans.manager.Context context)
	{
		Asserts.assertNotNull(scopeType, "scopeType parameter can not be null");
		Asserts.assertNotNull(context, "context parameter can not be null");
		
		getManager().contextMap.put(scopeType, context);
	}

	public Reference getReference() throws NamingException
	{
		return new Reference(ManagerImpl.class.getName(),new StringRefAddr("ManagerImpl","ManagerImpl"),ManagerObjectFactory.class.getName(),null);
	}
	
}