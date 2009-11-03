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
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.NonBinding;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InterceptionType;
import javax.enterprise.inject.spi.Interceptor;
import javax.interceptor.InvocationContext;

import org.apache.webbeans.component.AbstractBean;
import org.apache.webbeans.component.ManagedBean;
import org.apache.webbeans.component.WebBeansType;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.exception.WebBeansException;
import org.apache.webbeans.inject.InjectableField;
import org.apache.webbeans.inject.InjectableMethods;
import org.apache.webbeans.intercept.InterceptorUtil;
import org.apache.webbeans.intercept.WebBeansInterceptorConfig;
import org.apache.webbeans.logger.WebBeansLogger;
import org.apache.webbeans.proxy.JavassistProxyFactory;
import org.apache.webbeans.util.AnnotationUtil;
import org.apache.webbeans.util.WebBeansUtil;
import org.apache.webbeans.xml.XMLAnnotationTypeManager;

/**
 * Defines the webbeans specific interceptors.
 * <p>
 * WebBeans interceotor classes has at least one {@link javax.interceptor.InterceptorBinding}
 * annotation. It can be defined on the class or method level at the component.
 * WebBeans interceptors are called after the EJB related interceptors are
 * called in the chain. Semantics of the interceptors are specified by the EJB
 * specificatin.
 * </p>
 * 
 * @version $Rev$ $Date$
 */
public class WebBeansInterceptor<T> extends AbstractBean<T> implements Interceptor<T>
{
	private static final WebBeansLogger logger = WebBeansLogger.getLogger(WebBeansInterceptor.class);
	
    /** InterceptorBindingTypes exist on the interceptor class */
    private Map<Class<? extends Annotation>, Annotation> interceptorBindingSet = new HashMap<Class<? extends Annotation>, Annotation>();

    /** Interceptor class */
    private Class<?> clazz;

    /** Simple Web Beans component */
    private AbstractBean<T> delegateComponent;
    
    private CreationalContext<T> creationalContext;

    public WebBeansInterceptor(AbstractBean<T> delegateComponent)
    {
        super(WebBeansType.INTERCEPTOR,delegateComponent.getReturnType());
        
        this.delegateComponent = delegateComponent;
        this.clazz = getDelegate().getReturnType();

    }

    public AbstractBean<T> getDelegate()
    {
        return this.delegateComponent;
    }

    /**
     * Add new binding type to the interceptor.
     * 
     * @param binding interceptor binding annotation. class
     * @param annot binding type annotation
     */
    public void addInterceptorBinding(Class<? extends Annotation> binding, Annotation annot)
    {
        Method[] methods = binding.getDeclaredMethods();

        for (Method method : methods)
        {
            Class<?> clazz = method.getReturnType();
            if (clazz.isArray() || clazz.isAnnotation())
            {
                if (!AnnotationUtil.hasAnnotation(method.getAnnotations(), NonBinding.class))
                {
                    throw new WebBeansConfigurationException("Interceptor definition class : " + getClazz().getName() + " @InterceptorBinding : " + binding.getName() + " must have @NonBinding valued members for its array-valued and annotation valued members");
                }
            }
        }

        interceptorBindingSet.put(binding, annot);
    }

    /**
     * Checks whether all of this interceptors binding types are present on the bean, with 
     * {@link NonBinding} member values.
     * 
     * @param bindingTypes binding types of bean
     * @param annots binding types annots of bean
     * @return true if all binding types of this interceptor exist ow false
     */
    public boolean hasBinding(List<Class<? extends Annotation>> bindingTypes, List<Annotation> annots)
    {
        if (bindingTypes == null || annots == null)
        {
            return false;
        }
        if (bindingTypes.size() != annots.size())
        {
            return false;
        }
        if (bindingTypes.size() == 0)
        {
            return false;
        }

        /* This interceptor is enabled if all of its interceptor bindings are present on the bean */
        for (Class<? extends Annotation> bindingType : this.interceptorBindingSet.keySet())
        {
        	int index = bindingTypes.indexOf(bindingType);
        	if (index < 0) 
        	{
        	    return false; /* at least one of this interceptors types is not in the beans bindingTypes */	
        	}
        	
        	if (!AnnotationUtil.hasAnnotationMember(bindingTypes.get(index), annots.get(index), this.interceptorBindingSet.get(bindingType)))
        	
        	{
        		return false;
        	}
        }
        
        return true;
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

    public Set<Interceptor<?>> getMetaInceptors()
    {
        Set<Interceptor<?>> set = new HashSet<Interceptor<?>>();

        Set<Class<? extends Annotation>> keys = interceptorBindingSet.keySet();
        Iterator<Class<? extends Annotation>> it = keys.iterator();

        while (it.hasNext())
        {
            Class<? extends Annotation> clazzAnnot = it.next();
            Set<Annotation> declared = null;
            Annotation[] anns = null;

            if (XMLAnnotationTypeManager.getInstance().hasInterceptorBindingType(clazzAnnot))
            {
                declared = XMLAnnotationTypeManager.getInstance().getInterceptorBindingTypeInherites(clazzAnnot);
                anns = new Annotation[declared.size()];
                anns = declared.toArray(anns);
            }

            else if (AnnotationUtil.hasInterceptorBindingMetaAnnotation(clazzAnnot.getDeclaredAnnotations()))
            {
                anns = AnnotationUtil.getInterceptorBindingMetaAnnotations(clazzAnnot.getDeclaredAnnotations());
            }

            /*
             * For example: @InterceptorBinding @Transactional @Action
             * public @interface ActionTransactional @ActionTransactional
             * @Production { }
             */

            if (anns != null && anns.length > 0)
            {
                // For example : @Transactional @Action Interceptor
                Set<Interceptor<?>> metas = WebBeansInterceptorConfig.findDeployedWebBeansInterceptor(anns);
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
    public Set<Annotation> getInterceptorBindings()
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

    public Method getMethod(InterceptionType type)
    {
        Class<? extends Annotation> interceptorTypeAnnotationClazz = InterceptorUtil.getInterceptorAnnotationClazz(type);
        Method method = WebBeansUtil.checkCommonAnnotationCriterias(getClazz(), interceptorTypeAnnotationClazz, true);

        return method;
    }

    
    @SuppressWarnings("unchecked")
    protected T createInstance(CreationalContext<T> creationalContext)
    {
        T proxy = (T)JavassistProxyFactory.createNewProxyInstance(this);
        
        this.delegateComponent.setCreationalContext(creationalContext);

        return proxy;
        
    }

    public void setInjections(Object proxy)
    {
        // Set injected fields
        ManagedBean<T> delegate = (ManagedBean<T>) this.delegateComponent;

        Set<Field> injectedFields = delegate.getInjectedFromSuperFields();
        for (Field injectedField : injectedFields)
        {
            injectField(injectedField, proxy);
        }

        Set<Method> injectedMethods = delegate.getInjectedFromSuperMethods();
        for (Method injectedMethod : injectedMethods)
        {
            injectMethod(injectedMethod, proxy);
        }
        
        injectedFields = delegate.getInjectedFields();
        for (Field injectedField : injectedFields)
        {
            injectField(injectedField, proxy);            
        }
        

        injectedMethods = delegate.getInjectedMethods();
        for (Method injectedMethod : injectedMethods)
        {
            injectMethod(injectedMethod, proxy);            
        }        
    }
    
    private void injectField(Field field, Object instance)
    {
        InjectableField f = new InjectableField(field, instance, this.delegateComponent, this.creationalContext);
        f.doInjection();        
    }

    @SuppressWarnings("unchecked")
    private void injectMethod(Method method, Object instance)
    {
        InjectableMethods m = new InjectableMethods(method, instance, this.delegateComponent, this.creationalContext);
        m.doInjection();        
    }
    

    public void destroy(T instance,CreationalContext<T> context)
    {
        delegateComponent.destroy(instance,context);
    }

    @Override
    public Set<Annotation> getQualifiers()
    {
        return delegateComponent.getQualifiers();
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
    public Class<? extends Annotation> getScope()
    {
        return delegateComponent.getScope();
    }

    public Set<Type> getTypes()
    {
        return delegateComponent.getTypes();
    }
    
    public Set<InjectionPoint> getInjectionPoints()
    {
        return delegateComponent.getInjectionPoints();
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;

        WebBeansInterceptor<?> o = null;

        if (obj instanceof WebBeansInterceptor)
        {
            o = (WebBeansInterceptor<?>) obj;

            if (o.clazz != null && this.clazz != null)
            {
                return o.clazz.equals(this.clazz);
            }

        }

        return false;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode()
    {
        return this.clazz != null ? clazz.hashCode() : 0;
    }

    /*
     * (non-Javadoc)
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

    @Override
    public Class<?> getBeanClass()
    {
        return this.delegateComponent.getBeanClass();
    }

	@Override
	public Set<Class<? extends Annotation>> getStereotypes() 
	{ 
		return this.delegateComponent.getStereotypes();
	}

	@Override
	public Object intercept(InterceptionType type, T instance,InvocationContext ctx) 
	{
		Method method = getMethod(type);
		try 
		{
			method.invoke(instance,new Object[]{ctx});
		} 
		catch (Exception e)
		{
			logger.error(e);
			throw new WebBeansException(e);
		}
		
		return null;
	}

	@Override
	public boolean intercepts(InterceptionType type) 
	{
		Method method = getMethod(type);
		
		return method != null ? true : false;
	}

    @Override
    public boolean isAlternative()
    {
        return this.delegateComponent.isAlternative();
    }
}