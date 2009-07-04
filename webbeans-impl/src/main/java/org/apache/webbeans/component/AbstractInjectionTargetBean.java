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

import javax.decorator.Decorates;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Initializer;

import org.apache.webbeans.config.DefinitionUtil;
import org.apache.webbeans.context.creational.CreationalContextImpl;
import org.apache.webbeans.exception.WebBeansException;
import org.apache.webbeans.inject.InjectableField;
import org.apache.webbeans.inject.InjectableMethods;
import org.apache.webbeans.intercept.InterceptorType;
import org.apache.webbeans.intercept.InvocationContextImpl;
import org.apache.webbeans.logger.WebBeansLogger;
import org.apache.webbeans.util.WebBeansUtil;

/**
 * Abstract class for owning observer methods.
 * 
 * @version $Rev$ $Date$
 *
 * @param <T> bean class
 */
public abstract class AbstractInjectionTargetBean<T> extends AbstractBean<T> implements InjectionTargetBean<T>
{
    /**Logger instance*/
    private final WebBeansLogger logger = WebBeansLogger.getLogger(getClass());
    
    /**Bean observable method*/
    private Set<Method> observableMethods = new HashSet<Method>();
    
    /** Injected fields of the component */
    private Set<Field> injectedFields = new HashSet<Field>();

    /** Injected methods of the component */
    private Set<Method> injectedMethods = new HashSet<Method>();    
    
    /**From realization*/
    protected boolean fromRealizes;

    /**
     * Creates a new observer owner component.
     * 
     * @param webBeansType webbean type
     * @param returnType bean class type
     */
    protected AbstractInjectionTargetBean(WebBeansType webBeansType, Class<T> returnType)
    {
        super(webBeansType, returnType);
    }

    /**
     * {@inheritDoc}
     */
    protected T createInstance(CreationalContext<T> creationalContext)
    {
        beforeConstructor();
        
        T instance = createComponentInstance(creationalContext);
        
        afterConstructor(instance, creationalContext);
        
        return instance;
    }
    
    /**
     * {@inheritDoc}
     */
    protected void destroyInstance(T instance)
    {
        destroyComponentInstance(instance);
    }
    
    /**
     * Sub-classes must override this method to create bean instance.
     * 
     * @param creationalContext creational context
     * @return bean instance
     */
    abstract protected T createComponentInstance(CreationalContext<T> creationalContext);
    
    /**
     * Sub-classes must override this method to destroy bean instance.
     * 
     * @param instance object instance.
     */
    protected void destroyComponentInstance(T instance)
    {
        preDestroy(instance);
        
        //Remove it from creational context, if any
        CreationalContextImpl<T> cc = (CreationalContextImpl<T>)this.creationalContext;
        cc.remove();        
    }
    
    /**
     * Called before constructor
     */
    protected void beforeConstructor()
    {
        
    }
    
    /**
     * Called after bean instance is created.
     * 
     * @param instance bean instance
     * @param creationalContext cretional context object
     */
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

        postConstruct(instance);
    }
    
    /**
     * {@inheritDoc}
     */
    public void postConstruct(T instance)
    {
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
                logger.error("Error is occured while executing @PostConstruct",e);
                throw new WebBeansException(e);
            }
        }        
    }
    
    /**
     * {@inheritDoc}
     */
    public void preDestroy(T instance)
    {
        if (WebBeansUtil.isContainsInterceptorMethod(getInterceptorStack(), InterceptorType.PRE_DESTROY))
        {
            InvocationContextImpl impl = new InvocationContextImpl(null,instance, null, null, WebBeansUtil.getInterceptorMethods(getInterceptorStack(), InterceptorType.PRE_DESTROY), InterceptorType.PRE_DESTROY);
            try
            {
                impl.proceed();
            }
            catch (Exception e)
            {
                getLogger().error("Error is occıred while executing @PreDestroy method",e);
                throw new WebBeansException(e);
            }

        }                
    }
    
    
    /**
     * Injects fields of the bean after constructing.
     * 
     * @param instance bean instance
     * @param creationalContext creational context
     */
    public void injectFields(T instance, CreationalContext<T> creationalContext)
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
    
    /**
     * Injects all {@link Initializer} methods of the bean
     * instance.
     * 
     * @param instance bean instance
     * @param creationalContext creational context instance
     */
    @SuppressWarnings("unchecked")
    public void injectMethods(T instance, CreationalContext<T> creationalContext)
    {
        Set<Method> methods = getInjectedMethods();

        for (Method method : methods)
        {
            InjectableMethods m = new InjectableMethods(method, instance, this,creationalContext);
            m.doInjection();
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public void injectResources(T instance, CreationalContext<T> creationalContext)
    {
        //TODO Java EE injections
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
    
    /**
     * Returns bean logger instance.
     * 
     * @return logger
     */
    protected WebBeansLogger getLogger()
    {
        return this.logger;
    }
    
}