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

import javax.context.CreationalContext;

import org.apache.webbeans.context.creational.CreationalContextFactory;
import org.apache.webbeans.exception.WebBeansException;
import org.apache.webbeans.inject.InjectableConstructor;
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

    public ComponentImpl(Class<T> returnType)
    {
        this(returnType, WebBeansType.SIMPLE);
    }

    public ComponentImpl(Class<T> returnType, WebBeansType type)
    {
        super(type, returnType);
        
        //Setting inherited meta data instance
        setInheritedMetaData();
    }

    /*
     * (non-Javadoc)
     * @see org.apache.webbeans.component.AbstractComponent#createInstance()
     */
    @Override
    protected T createComponentInstance(CreationalContext<T> creationalContext)
    {
        Constructor<T> con = getConstructor();
        InjectableConstructor<T> ic = new InjectableConstructor<T>(con, this,creationalContext);

        T instance = ic.doInjection();
        
//        if(WebBeansUtil.isScopeTypeNormal(getScopeType()))
//        {
//            creationalContext.push(instance);   
//        }
        
        return instance;
    }

 
    @Override
    @SuppressWarnings("unchecked")
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
                throw new WebBeansException(e);
            }

        }
        
        CreationalContextFactory.getInstance().removeCreationalContext(this);
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

    
    public String toString()
    {
        return super.toString();
    }

}
