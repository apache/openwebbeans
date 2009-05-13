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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import javax.context.CreationalContext;
import javax.decorator.Decorates;

import org.apache.webbeans.config.DefinitionUtil;
import org.apache.webbeans.exception.WebBeansException;
import org.apache.webbeans.inject.InjectableField;
import org.apache.webbeans.inject.InjectableMethods;
import org.apache.webbeans.intercept.InterceptorType;
import org.apache.webbeans.intercept.InvocationContextImpl;
import org.apache.webbeans.util.WebBeansUtil;

public abstract class AbstractObservesComponent<T> extends AbstractComponent<T> implements ObservesMethodsOwner<T>
{
    private Set<Method> observableMethods = new HashSet<Method>();
    
    /** Injected fields of the component */
    private Set<Field> injectedFields = new HashSet<Field>();

    /** Injected methods of the component */
    private Set<Method> injectedMethods = new HashSet<Method>();    
    
    protected boolean fromRealizes;

    protected AbstractObservesComponent(WebBeansType webBeansType, Class<T> returnType)
    {
        super(webBeansType, returnType);
    }

    protected T createInstance(CreationalContext<T> creationalContext)
    {
        beforeConstructor();
        
        T instance = createComponentInstance(creationalContext);
        
        afterConstructor(instance, creationalContext);
        
        return instance;
    }
    
    protected void destroyInstance(T instance)
    {
        destroyComponentInstance(instance);
    }
    
    abstract protected T createComponentInstance(CreationalContext<T> creationalContext);
    
    abstract protected void destroyComponentInstance(T instance);
    
    protected void beforeConstructor()
    {
        
    }
    
    protected void afterConstructor(T instance,CreationalContext<T> creationalContext)
    {   
        //Inject fields
        injectFields(instance,creationalContext);
        
        //Inject methods
        injectMethods(instance,creationalContext);
        
        //Interceptor and decorator stack
        if (getWebBeansType().equals(WebBeansType.SIMPLE))
        {
            DefinitionUtil.defineSimpleWebBeanInterceptorStack(this);
            DefinitionUtil.defineWebBeanDecoratorStack(this, instance);
        }        

        //Call Post Construct
        if (WebBeansUtil.isContainsInterceptorMethod(getInterceptorStack(), InterceptorType.POST_CONSTRUCT))
        {
            InvocationContextImpl impl = new InvocationContextImpl(null,instance, null, null, WebBeansUtil.getInterceptorMethods(getInterceptorStack(), InterceptorType.POST_CONSTRUCT), InterceptorType.POST_CONSTRUCT);
            
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
    protected void injectFields(T instance, CreationalContext<T> creationalContext)
    {
        Set<Field> fields = getInjectedFields();
        for (Field field : fields)
        {
            if (field.getAnnotation(Decorates.class) == null)
            {
                InjectableField f = new InjectableField(field, instance, this, creationalContext);
                f.doInjection();
            }
        }
    }

    /*
     * Injectable methods
     */
    @SuppressWarnings("unchecked")
    protected void injectMethods(T instance, CreationalContext<T> creationalContext)
    {
        Set<Method> methods = getInjectedMethods();

        for (Method method : methods)
        {
            InjectableMethods m = new InjectableMethods(method, instance, this,creationalContext);
            m.doInjection();
        }
    }
    
    
    /*
     * (non-Javadoc)
     * @see
     * org.apache.webbeans.component.ObservableComponent#addObservableMethod
     * (java.lang.reflect.Method)
     */
    public void addObservableMethod(Method observerMethod)
    {
        this.observableMethods.add(observerMethod);
    }

    /*
     * (non-Javadoc)
     * @see
     * org.apache.webbeans.component.ObservableComponent#getObservableMethods()
     */
    public Set<Method> getObservableMethods()
    {
        return this.observableMethods;
    }



    /**
     * @return the fromRealizes
     */
    public boolean isFromRealizes()
    {
        return fromRealizes;
    }



    /**
     * @param fromRealizes the fromRealizes to set
     */
    public void setFromRealizes(boolean fromRealizes)
    {
        this.fromRealizes = fromRealizes;
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
    

}
