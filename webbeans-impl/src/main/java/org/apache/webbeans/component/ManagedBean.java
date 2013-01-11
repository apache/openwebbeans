/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.webbeans.component;

import java.io.Serializable;
import java.lang.reflect.Constructor;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Decorator;

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.context.creational.CreationalContextImpl;
import org.apache.webbeans.decorator.AbstractDecoratorMethodHandler;
import org.apache.webbeans.inject.InjectableConstructor;
import org.apache.webbeans.intercept.InterceptorData;
import org.apache.webbeans.portable.InjectionTargetImpl;

/**
 * Managed bean implementation of the {@link javax.enterprise.inject.spi.Bean}.
 * 
 * @version $Rev$ $Date$
 */
public class ManagedBean<T> extends AbstractInjectionTargetBean<T> implements InterceptedMarker
{
    /** Constructor of the web bean component */
    private Constructor<T> constructor;
    
    protected boolean isAbstractDecorator;


    public ManagedBean(WebBeansContext webBeansContext, Class<T> returnType, AnnotatedType<T> annotatedType)
    {
        this(webBeansContext, returnType, WebBeansType.MANAGED, annotatedType);
    }

    /**
     * Creates a new instance.
     * 
     * @param returnType bean class
     * @param type webbeans type
     * @param webBeansContext
     */
    public ManagedBean(WebBeansContext webBeansContext, Class<T> returnType, WebBeansType type, AnnotatedType<T> annotatedType)
    {
        super(webBeansContext, type, returnType, annotatedType);
        
        //Setting inherited meta data instance
        setInheritedMetaData();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected T createComponentInstance(CreationalContext<T> creationalContext)
    {
        Constructor<T> con = getConstructor();
        InjectionTargetImpl<T> injectionTarget = new InjectionTargetImpl<T>(getAnnotatedType(), getInjectionPoints(), getWebBeansContext());
        InjectableConstructor<T> ic = new InjectableConstructor<T>(con, injectionTarget, (CreationalContextImpl<T>) creationalContext);

        T instance = ic.doInjection();
        
        //If this is an abstract Decorator, we need to set the handler on the Proxy instance
        if(isAbstractDecorator)
        {
            webBeansContext.getProxyFactory().setHandler(instance, new AbstractDecoratorMethodHandler());
        }
        return instance;
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
    
    public boolean isPassivationCapable()
    {
        if (isPassivationCapable != null)
        {
            return isPassivationCapable.booleanValue();
        }
        if(Serializable.class.isAssignableFrom(returnType))
        {
            for(Decorator<?> dec : decorators)
            {
                if(dec.getBeanClass() != null && !Serializable.class.isAssignableFrom(dec.getBeanClass()))
                {
                    isPassivationCapable = Boolean.FALSE;
                    return false;
                }
            }

            for(InterceptorData interceptorData : interceptorStack)
            {
                if(interceptorData.isDefinedInInterceptorClass())
                {
                    Class<?> interceptor = interceptorData.getInterceptorClass();
                    if(!Serializable.class.isAssignableFrom(interceptor))
                    {
                        isPassivationCapable = Boolean.FALSE;
                        return false;
                    }
                }
            }

            isPassivationCapable = Boolean.TRUE;
            return true;
        }

        isPassivationCapable = Boolean.FALSE;
        return false;
    }

    /** cache previously calculated result */
    private Boolean isPassivationCapable = null;
    
    public void setIsAbstractDecorator(boolean flag)
    {
        isAbstractDecorator = flag;
    }
}
