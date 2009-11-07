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
package org.apache.webbeans.decorator;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

import javax.decorator.Delegate;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Decorator;
import javax.enterprise.inject.spi.InjectionPoint;

import org.apache.webbeans.component.AbstractBean;
import org.apache.webbeans.component.ManagedBean;
import org.apache.webbeans.component.WebBeansType;
import org.apache.webbeans.exception.WebBeansException;
import org.apache.webbeans.inject.InjectableField;
import org.apache.webbeans.inject.InjectableMethods;
import org.apache.webbeans.logger.WebBeansLogger;
import org.apache.webbeans.proxy.JavassistProxyFactory;
import org.apache.webbeans.util.AnnotationUtil;
import org.apache.webbeans.util.ClassUtil;

/**
 * Defines decorators. It wraps the bean instance related
 * with decorator class. Actually, each decorator is an instance
 * of the {@link ManagedBean}.
 * 
 * @version $Rev$ $Date$
 *
 * @param <T> decorator type info
 */
public class WebBeansDecorator<T> extends AbstractBean<T> implements Decorator<T>
{
    private static WebBeansLogger logger = WebBeansLogger.getLogger(WebBeansDecorator.class);

    /** Decorator class */
    private Class<?> clazz;

    /** Decorates api types */
    private Set<Type> decoratedTypes = new HashSet<Type>();

    /** Delegate field class type */
    protected Type delegateType;

    /** Delegate field bindings */
    protected Set<Annotation> delegateBindings = new HashSet<Annotation>();

    /** Wrapped bean*/
    private AbstractBean<T> wrappedBean;
    
    /**Creational context*/
    private CreationalContext<Object> creationalContext;

    /**
     * Creates a new decorator bean instance with the given wrapped bean.
     * @param delegateComponent delegate bean instance
     */
    public WebBeansDecorator(AbstractBean<T> wrappedBean)
    {
        super(WebBeansType.DECORATOR,wrappedBean.getReturnType());
        
        this.wrappedBean = wrappedBean;
        this.clazz = wrappedBean.getReturnType();

        init();
    }

    protected void init()
    {
        ClassUtil.setInterfaceTypeHierarchy(this.decoratedTypes, this.clazz);

        if (this.decoratedTypes.contains(Serializable.class))
        {
            this.decoratedTypes.remove(Serializable.class);
        }

        initDelegate();
    }

    protected void initDelegate()
    {
        Field field = ClassUtil.getFieldWithAnnotation(this.clazz, Delegate.class);
        
        if(field == null)
        {
            initDelegateRecursively(this.clazz);
        }
        else
        {
            initDelegateInternal(field);
        }
    }
    
    private void initDelegateRecursively(Class<?> delegateClazz)
    {
        Class<?> superClazz = delegateClazz.getSuperclass();
        if(!superClazz.equals(Object.class))
        {
            Field field = ClassUtil.getFieldWithAnnotation(superClazz, Delegate.class);
            if(field != null)
            {
                initDelegateInternal(field);
            }
            else
            {
                initDelegateRecursively(superClazz);
            }
        }
    }
    
    private void initDelegateInternal(Field field)
    {
        this.delegateType = field.getGenericType();

        Annotation[] anns = field.getAnnotations();

        for (Annotation ann : anns)
        {
            if (AnnotationUtil.isQualifierAnnotation(ann.annotationType()))
            {
                this.delegateBindings.add(ann);
            }
        }
        
    }
    
    private boolean bindingMatchesAnnotations(Annotation bindingType, Set<Annotation> annotations)
    {

        for (Annotation annot : annotations)
        {
            if (AnnotationUtil.hasAnnotationMember(bindingType.annotationType(), annot, bindingType))
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Helper method to check if any of a list of Types are assignable to the
     * delegate type.
     * 
     * @param apiTypes Set of apiTypes to check against the delegate type
     * @return true if one of the types is assignable to the delegate type
     */
    private boolean apiTypesMatchDelegateType(Set<Type> apiTypes)
    {
        for (Type apiType : apiTypes)
        {
            if (ClassUtil.isAssignable(apiType, this.delegateType))
            {
                return true;
            }
        }

        return false;
    }

    public boolean isDecoratorMatch(Set<Type> apiTypes, Set<Annotation> annotations)
    {

        if (!apiTypesMatchDelegateType(apiTypes))
        {
            return false;
        }

        for (Annotation bindingType : delegateBindings)
        {
            if (!bindingMatchesAnnotations(bindingType, annotations))
            {
                return false;
            }
        }

        return true;
    }

    @Override
    public Set<Annotation> getDelegateQualifiers()
    {
        return delegateBindings;
    }

    @Override
    public Type getDelegateType()
    {
        return delegateType;
    }

    public void setDelegate(Object instance, Object delegate)
    {
        Field field = ClassUtil.getFieldWithAnnotation(getClazz(), Delegate.class);
        if (!field.isAccessible())
        {
            field.setAccessible(true);
        }

        try
        {
            field.set(instance, delegate);

        }
        catch (IllegalArgumentException e)
        {
            logger.error("Delegate field is not found on the given decorator class : " + instance.getClass().getName(), e);
            throw new WebBeansException(e);

        }
        catch (IllegalAccessException e)
        {
            logger.error("Illegal access exception for field " + field.getName() + " in decorator class : " + instance.getClass().getName(), e);
        }

    }

    
    @SuppressWarnings("unchecked")    
    protected  T createInstance(CreationalContext<T> creationalContext)
    {
        T proxy = (T)JavassistProxyFactory.createNewProxyInstance(this);
        
        this.wrappedBean.setCreationalContext(creationalContext);

        return proxy;
        
    }

    public void setInjections(Object proxy)
    {
        // Set injected fields
        ManagedBean<T> delegate = (ManagedBean<T>) this.wrappedBean;

        Set<Field> injectedFields = delegate.getInjectedFromSuperFields();
        for (Field injectedField : injectedFields)
        {
            boolean isDecorates = injectedField.isAnnotationPresent(Delegate.class);

            if (!isDecorates)
            {
                injectField(injectedField, proxy);
            }
        }
        
        Set<Method> injectedMethods = delegate.getInjectedFromSuperMethods();
        for (Method injectedMethod : injectedMethods)
        {
            injectMethod(injectedMethod, proxy);
        }        

        injectedFields = delegate.getInjectedFields();
        for (Field injectedField : injectedFields)
        {
            boolean isDecorates = injectedField.isAnnotationPresent(Delegate.class);

            if (!isDecorates)
            {
                injectField(injectedField, proxy);
            }
        }
        
        injectedMethods = delegate.getInjectedMethods();
        for (Method injectedMethod : injectedMethods)
        {
            injectMethod(injectedMethod, proxy);
        }        
    }
    
    private void injectField(Field field, Object instance)
    {
        InjectableField f = new InjectableField(field, instance, this.wrappedBean, this.creationalContext);
        f.doInjection();        
    }

    @SuppressWarnings("unchecked")
    private void injectMethod(Method method, Object instance)
    {
        InjectableMethods m = new InjectableMethods(method, instance, this.wrappedBean, this.creationalContext);
        m.doInjection();        
    }
    
    
    public void destroy(T instance,CreationalContext<T> context)
    {
        wrappedBean.destroy(instance,context);
    }

    @Override
    public Set<Annotation> getQualifiers()
    {
        return wrappedBean.getQualifiers();
    }

    @Override
    public Class<? extends Annotation> getDeploymentType()
    {
        return wrappedBean.getDeploymentType();
    }

    @Override
    public String getName()
    {
        return wrappedBean.getName();
    }

    @Override
    public Class<? extends Annotation> getScope()
    {
        return wrappedBean.getScope();
    }

    
    public Set<Type> getTypes()
    {
        return wrappedBean.getTypes();
    }

    @Override
    public boolean isNullable()
    {
        return wrappedBean.isNullable();
    }

    @Override
    public boolean isSerializable()
    {
        return wrappedBean.isSerializable();
    }

    /**
     * @return the delegateComponent
     */
    public AbstractBean<T> getDelegateComponent()
    {
        return wrappedBean;
    }
    
    public Set<InjectionPoint> getInjectionPoints()
    {
        return wrappedBean.getInjectionPoints();
    }

    /**
     * @return the clazz
     */
    public Class<?> getClazz()
    {
        return clazz;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((clazz == null) ? 0 : clazz.hashCode());
        return result;
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
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final WebBeansDecorator<?> other = (WebBeansDecorator<?>) obj;
        if (clazz == null)
        {
            if (other.clazz != null)
                return false;
        }
        else if (!clazz.equals(other.clazz))
            return false;
        return true;
    }

    @Override
    public Class<?> getBeanClass()
    {
        return this.wrappedBean.getBeanClass();
    }

	@Override
	public Set<Class<? extends Annotation>> getStereotypes() 
	{
		return this.wrappedBean.getStereotypes();
	}

	@Override
	public Set<Type> getDecoratedTypes() {
		return this.wrappedBean.getTypes();
	}

    @Override
    public boolean isAlternative()
    {
        return this.wrappedBean.isAlternative();
    }


}