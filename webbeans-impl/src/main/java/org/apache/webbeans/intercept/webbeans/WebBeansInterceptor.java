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
package org.apache.webbeans.intercept.webbeans;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.webbeans.InterceptorBindingType;
import javax.webbeans.NonBinding;
import javax.webbeans.manager.InterceptionType;
import javax.webbeans.manager.Interceptor;

import org.apache.webbeans.component.AbstractComponent;
import org.apache.webbeans.component.ComponentImpl;
import org.apache.webbeans.container.ManagerImpl;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.inject.InjectableField;
import org.apache.webbeans.inject.InjectableMethods;
import org.apache.webbeans.intercept.InterceptorUtil;
import org.apache.webbeans.intercept.WebBeansInterceptorConfig;
import org.apache.webbeans.proxy.JavassistProxyFactory;
import org.apache.webbeans.util.AnnotationUtil;
import org.apache.webbeans.util.WebBeansUtil;
import org.apache.webbeans.xml.XMLAnnotationTypeManager;

/**
 * Defines the webbeans specific interceptors.
 * <p>
 * WebBeans interceotor classes has at least one {@link InterceptorBindingType}
 * annotation. It can be defined on the class or method level at the component.
 * WebBeans interceptors are called after the EJB related interceptors are
 * called in the chain. Semantics of the interceptors are specified by the EJB
 * specificatin.
 * </p>
 * 
 * @author <a href="mailto:gurkanerdogdu@yahoo.com">Gurkan Erdogdu</a>
 * @since 1.0
 */
public class WebBeansInterceptor extends Interceptor
{
	/** InterceptorBindingTypes exist on the interceptor class */
	private Map<Class<? extends Annotation>, Annotation> interceptorBindingSet = new HashMap<Class<? extends Annotation>, Annotation>();

	/** Interceptor class */
	private Class<?> clazz;

	/** Simple Web Beans component */
	private AbstractComponent<Object> delegateComponent;

	public WebBeansInterceptor(AbstractComponent<Object> delegateComponent)
	{
		super(ManagerImpl.getManager());
		this.delegateComponent = delegateComponent;
		this.clazz = getDelegate().getReturnType();

	}

	public AbstractComponent<Object> getDelegate()
	{
		return this.delegateComponent;
	}

	/**
	 * Add new binding type to the interceptor.
	 * 
	 * @param bindingType interceptor binding type annot. class
	 * @param annot binding type annotation
	 */
	public void addInterceptorBindingType(Class<? extends Annotation> bindingType, Annotation annot)
	{
		Method[] methods = bindingType.getDeclaredMethods();

		for (Method method : methods)
		{
			Class<?> clazz = method.getReturnType();
			if (clazz.isArray() || clazz.isAnnotation())
			{
				if (!AnnotationUtil.isAnnotationExist(method.getAnnotations(), NonBinding.class))
				{
					throw new WebBeansConfigurationException("Interceptor definition class : " + getClazz().getName() + " @InterceptorBindingType : " + bindingType.getName() + " must have @NonBinding valued members for its array-valued and annotation valued members");
				}
			}
		}

		interceptorBindingSet.put(bindingType, annot);
	}

	/**
	 * Checks whether this interceptor has given binding types with
	 * {@link NonBinding} member values.
	 * 
	 * @param bindingTypes binding types
	 * @param annots binding types annots.
	 * @return true if binding types exist ow false
	 */
	public boolean isBindingTypesExist(List<Class<? extends Annotation>> bindingTypes, List<Annotation> annots)
	{
		boolean result = false;

		if (bindingTypes != null && annots != null && (bindingTypes.size() == annots.size()))
		{
			int i = 0;
			for (Class<? extends Annotation> bindingType : bindingTypes)
			{
				if (this.interceptorBindingSet.containsKey(bindingType))
				{
					Annotation target = this.interceptorBindingSet.get(bindingType);
					if (AnnotationUtil.isAnnotationMemberExist(bindingType, annots.get(i), target))
					{
						result = true;
					} else
					{
						return false;
					}
				} else
				{
					return false;
				}

				i++;
			}
		}

		return result;
	}

	/**
	 * Gets the interceptor class.
	 * 
	 * @return interceptor class
	 */
	public Class<?> getClazz()
	{
		return clazz;
	}

	public Set<Interceptor> getMetaInceptors()
	{
		Set<Interceptor> set = new HashSet<Interceptor>();

		Set<Class<? extends Annotation>> keys = interceptorBindingSet.keySet();
		Iterator<Class<? extends Annotation>> it = keys.iterator();

		while (it.hasNext())
		{
			Class<? extends Annotation> clazzAnnot = it.next();
			Set<Annotation> declared = null;
			Annotation[] anns = null;

			if (XMLAnnotationTypeManager.getInstance().isInterceptorBindingTypeExist(clazzAnnot))
			{
				declared = XMLAnnotationTypeManager.getInstance().getInterceptorBindingTypeInherites(clazzAnnot);
				anns = new Annotation[declared.size()];
				anns = declared.toArray(anns);
			}

			else if (AnnotationUtil.isInterceptorBindingMetaAnnotationExist(clazzAnnot.getAnnotations()))
			{
				anns = AnnotationUtil.getInterceptorBindingMetaAnnotations(clazzAnnot.getAnnotations());
			}

			/*
			 * For example: @InterceptorBindingType @Transactional @Action
			 * public @interface ActionTransactional @ActionTransactional
			 * @Production { }
			 */

			if (anns != null && anns.length > 0)
			{
				// For example : @Transactional @Action Interceptor
				Set<Interceptor> metas = WebBeansInterceptorConfig.findDeployedWebBeansInterceptor(anns);
				set.addAll(metas);

				// For each @Transactional and @Action Interceptor
				for (Annotation ann : anns)
				{
					Annotation[] simple = new Annotation[1];
					simple[0] = ann;
					metas = WebBeansInterceptorConfig.findDeployedWebBeansInterceptor(simple);
					set.addAll(metas);
				}

			}

		}

		return set;
	}

	/**
	 * Sets interceptor class.
	 * 
	 * @param clazz class instance
	 */
	public void setClazz(Class<?> clazz)
	{
		this.clazz = clazz;
	}

	@Override
	public Set<Annotation> getInterceptorBindingTypes()
	{
		Set<Annotation> set = new HashSet<Annotation>();
		Set<Class<? extends Annotation>> keySet = this.interceptorBindingSet.keySet();
		Iterator<Class<? extends Annotation>> itSet = keySet.iterator();

		while (itSet.hasNext())
		{
			set.add(this.interceptorBindingSet.get(itSet.next()));
		}

		return set;
	}

	@Override
	public Method getMethod(InterceptionType type)
	{
		Class<? extends Annotation> interceptorTypeAnnotationClazz = InterceptorUtil.getInterceptorAnnotationClazz(type);
		Method method = WebBeansUtil.checkCommonAnnotationCriterias(getClazz(), interceptorTypeAnnotationClazz, true);

		return method;
	}

	@Override
	public Object create()
	{
		Object proxy = JavassistProxyFactory.createNewProxyInstance(this);

		return proxy;

		// return delegateComponent.create();
	}

	public void setInjections(Object proxy)
	{
		// Set injected fields
		ComponentImpl<Object> delegate = (ComponentImpl<Object>) this.delegateComponent;

		Set<Field> injectedFields = delegate.getInjectedFields();
		for (Field injectedField : injectedFields)
		{
			InjectableField ife = new InjectableField(injectedField, proxy, this.delegateComponent);
			ife.doInjection();
		}

		Set<Method> injectedMethods = delegate.getInjectedMethods();
		for (Method injectedMethod : injectedMethods)
		{
			@SuppressWarnings("unchecked")
			InjectableMethods<?> ife = new InjectableMethods(injectedMethod, proxy, this.delegateComponent);
			ife.doInjection();
		}

	}

	@Override
	public void destroy(Object instance)
	{
		delegateComponent.destroy(instance);
	}

	@Override
	public Set<Annotation> getBindingTypes()
	{
		return delegateComponent.getBindingTypes();
	}

	@Override
	public Class<? extends Annotation> getDeploymentType()
	{
		return delegateComponent.getDeploymentType();
	}

	@Override
	public String getName()
	{
		return delegateComponent.getName();
	}

	@Override
	public Class<? extends Annotation> getScopeType()
	{
		return delegateComponent.getScopeType();
	}

	@Override
	public Set<Class<?>> getTypes()
	{
		return delegateComponent.getTypes();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;

		WebBeansInterceptor o = null;

		if (obj instanceof WebBeansInterceptor)
		{
			o = (WebBeansInterceptor) obj;

			if (o.clazz != null && this.clazz != null)
			{
				return o.clazz.equals(this.clazz);
			}

		}

		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode()
	{
		return this.clazz != null ? clazz.hashCode() : 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		return "WebBeans Interceptor with class : " + "[" + this.clazz.getName() + "]";
	}

	@Override
	public boolean isNullable()
	{
		return delegateComponent.isNullable();
	}

	@Override
	public boolean isSerializable()
	{
		return delegateComponent.isSerializable();
	}
}