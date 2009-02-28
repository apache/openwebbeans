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
package org.apache.webbeans.component;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import javax.context.CreationalContext;
import javax.decorator.Decorates;

import org.apache.webbeans.config.DefinitionUtil;
import org.apache.webbeans.exception.WebBeansException;
import org.apache.webbeans.inject.InjectableConstructor;
import org.apache.webbeans.inject.InjectableField;
import org.apache.webbeans.inject.InjectableMethods;
import org.apache.webbeans.intercept.InterceptorType;
import org.apache.webbeans.intercept.InvocationContextImpl;
import org.apache.webbeans.util.WebBeansUtil;

/**
 * Concrete implementation of the {@link AbstractComponent}.
 * <p>
 * It is defined as bean implementation class component.
 * </p>
 * 
 * @author <a href="mailto:gurkanerdogdu@yahoo.com">Gurkan Erdogdu</a>
 * @since 1.0
 */
public class ComponentImpl<T> extends AbstractObservesComponent<T>
{
    /** Constructor of the web bean component */
    private Constructor<T> constructor;

    /** Injected fields of the component */
    private Set<Field> injectedFields = new HashSet<Field>();

    /** Injected methods of the component */
    private Set<Method> injectedMethods = new HashSet<Method>();

    public ComponentImpl(Class<T> returnType)
    {
        this(returnType, WebBeansType.SIMPLE);
    }

    public ComponentImpl(Class<T> returnType, WebBeansType type)
    {
        super(type, returnType);
    }

    /*
     * (non-Javadoc)
     * @see org.apache.webbeans.component.AbstractComponent#createInstance()
     */
    @Override
    protected T createInstance(CreationalContext<T> creationalContext)
    {
        beforeConstructor();

        Constructor<T> con = getConstructor();
        InjectableConstructor<T> ic = new InjectableConstructor<T>(con, this);

        T instance = ic.doInjection();
        
        afterConstructor(instance);

        return instance;
    }

    /*
     * Call before object constructor
     */
    protected void beforeConstructor()
    {

    }

    /*
     * Call after object construction
     */
    protected void afterConstructor(T instance)
    {
        injectFields(instance);
        injectMethods(instance);

        if (getWebBeansType().equals(WebBeansType.SIMPLE))
        {
            DefinitionUtil.defineSimpleWebBeanInterceptorStack(this);
            DefinitionUtil.defineWebBeanDecoratorStack(this, instance);
        }

        if (WebBeansUtil.isContainsInterceptorMethod(getInterceptorStack(), InterceptorType.POST_CONSTRUCT))
        {
            InvocationContextImpl impl = new InvocationContextImpl(instance, null, null, WebBeansUtil.getInterceptorMethods(getInterceptorStack(), InterceptorType.POST_CONSTRUCT), InterceptorType.POST_CONSTRUCT);
            try
            {
                impl.proceed();

            }
            catch (Exception e)
            {
                throw new WebBeansException(e);
            }
        }

    }

    /*
     * Injectable fields
     */
    protected void injectFields(T instance)
    {
        Set<Field> fields = getInjectedFields();
        for (Field field : fields)
        {
            if (field.getAnnotation(Decorates.class) == null)
            {
                InjectableField f = new InjectableField(field, instance, this);
                f.doInjection();
            }
        }
    }

    /*
     * Injectable methods
     */
    @SuppressWarnings("unchecked")
    protected void injectMethods(T instance)
    {
        Set<Method> methods = getInjectedMethods();

        for (Method method : methods)
        {
            InjectableMethods m = new InjectableMethods(method, instance, this);
            m.doInjection();
        }
    }

    /*
     * (non-Javadoc)
     * @see
     * org.apache.webbeans.component.AbstractComponent#destroyInstance(java.
     * lang.Object)
     */
    @Override
    protected void destroyInstance(T instance)
    {
        if (WebBeansUtil.isContainsInterceptorMethod(getInterceptorStack(), InterceptorType.PRE_DESTROY))
        {
            InvocationContextImpl impl = new InvocationContextImpl(instance, null, null, WebBeansUtil.getInterceptorMethods(getInterceptorStack(), InterceptorType.PRE_DESTROY), InterceptorType.PRE_DESTROY);
            try
            {
                impl.proceed();

            }
            catch (Exception e)
            {
                throw new WebBeansException(e);
            }

        }
    }

    /**
     * Get constructor.
     * 
     * @return constructor
     */
    public Constructor<T> getConstructor()
    {
        return constructor;
    }

    /**
     * Set constructor.
     * 
     * @param constructor constructor instance
     */
    public void setConstructor(Constructor<T> constructor)
    {
        this.constructor = constructor;
    }

    /**
     * Gets injected fields.
     * 
     * @return injected fields
     */
    public Set<Field> getInjectedFields()
    {
        return this.injectedFields;
    }

    /**
     * Add new injected field.
     * 
     * @param field new injected field
     */
    public void addInjectedField(Field field)
    {
        this.injectedFields.add(field);
    }

    /**
     * Gets injected methods.
     * 
     * @return injected methods
     */
    public Set<Method> getInjectedMethods()
    {
        return this.injectedMethods;
    }

    /**
     * Add new injected method.
     * 
     * @param field new injected method
     */
    public void addInjectedMethod(Method method)
    {
        this.injectedMethods.add(method);
    }
    
    public String toString()
    {
        return super.toString();
    }

}
