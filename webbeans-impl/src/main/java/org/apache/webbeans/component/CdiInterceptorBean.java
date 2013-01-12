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

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.AnnotatedType;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.util.Set;

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.context.creational.CreationalContextImpl;
import org.apache.webbeans.inject.InjectableConstructor;
import org.apache.webbeans.portable.InjectionTargetImpl;

/**
 * <p>{@link javax.enterprise.inject.spi.Interceptor}
 * Bean implementation for CDI-style Beans.
 * This is Interceptors which got defined using
 * &#064;{@link javax.interceptor.InterceptorBinding}.</p>
 */
public class CdiInterceptorBean<T> extends InterceptorBean<T>
{
    /**
     * Constructor of the web bean component
     */
    private Constructor<T> constructor;

    /**
     *
     * @param annotatedType AnnotatedType will be returned by some methods in the SPI
     * @param webBeansContext
     */
    public CdiInterceptorBean(WebBeansContext webBeansContext, AnnotatedType<T> annotatedType)
    {
        super(webBeansContext, annotatedType);
    }


    private Set<Annotation> interceptorBindings;

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
     * {@inheritDoc}
     */
    @Override
    protected T createComponentInstance(CreationalContext<T> creationalContext)
    {
        Constructor<T> con = getConstructor();
        InjectionTargetImpl<T> injectionTarget = new InjectionTargetImpl<T>(getAnnotatedType(), getInjectionPoints(), getWebBeansContext());
        InjectableConstructor<T> ic = new InjectableConstructor<T>(con, injectionTarget, (CreationalContextImpl<T>) creationalContext);

        T instance = ic.doInjection();

        return instance;
    }


    public void setInterceptorBindings(Set<Annotation> interceptorBindings)
    {
        this.interceptorBindings = interceptorBindings;
    }

    @Override
    public Set<Annotation> getInterceptorBindings()
    {
        return interceptorBindings;
    }


}
