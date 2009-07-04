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

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;

import org.apache.webbeans.context.creational.CreationalContextImpl;
import org.apache.webbeans.exception.WebBeansException;
import org.apache.webbeans.inject.InjectableConstructor;
import org.apache.webbeans.intercept.InterceptorType;
import org.apache.webbeans.intercept.InvocationContextImpl;
import org.apache.webbeans.util.WebBeansUtil;

/**
 * Managed bean implementation of the {@link Bean}.
 * 
 * @version $Rev$ $Date$
 */
public class ManagedBean<T> extends AbstractObservesBean<T>
{
    /** Constructor of the web bean component */
    private Constructor<T> constructor;

    public ManagedBean(Class<T> returnType)
    {
        this(returnType, WebBeansType.SIMPLE);
    }

    /**
     * Creates a new instance.
     * 
     * @param returnType bean class
     * @param type webbeans type
     */
    public ManagedBean(Class<T> returnType, WebBeansType type)
    {
        super(type, returnType);
        
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
        InjectableConstructor<T> ic = new InjectableConstructor<T>(con, this,creationalContext);

        T instance = ic.doInjection();
        
        return instance;
    }

 
    /**
     * {@inheritDoc}
     */
    @Override
    protected void destroyComponentInstance(T instance)
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
        
        //Remove it from creational context, if any
        CreationalContextImpl<T> cc = (CreationalContextImpl<T>)this.creationalContext;
        cc.remove();
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
}