/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.webbeans.intercept;

import java.lang.reflect.Method;
import java.util.List;

import javax.enterprise.context.spi.CreationalContext;

import org.apache.webbeans.component.OwbBean;
import org.apache.webbeans.context.creational.CreationalContextImpl;

/**
 * Dependent scoped beans interceptor handler.
 * @version $Rev$ $Date$
 *
 */
public class DependentScopedBeanInterceptorHandler extends InterceptorHandler
{
    /**default servial id*/
    private static final long serialVersionUID = 1L;
    
    /**Bean instance*/
    private Object actualInstance;
    
    /**Creaitonal context*/
    private CreationalContext<?> creationalContext;
    
    /**
     * Creates a new instance of handler.
     * @param bean dependent bean 
     * @param instance bean instance
     * @param creationalContext creational context
     */
    public DependentScopedBeanInterceptorHandler(OwbBean<?> bean, Object instance, CreationalContext<?> creationalContext)
    {
        super(bean);        
        this.actualInstance = instance;
        this.creationalContext = creationalContext;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object invoke(Object instance, Method method, Method proceed, Object[] arguments) throws Exception
    {
        return super.invoke(this.actualInstance, method, proceed, arguments, (CreationalContextImpl<?>)creationalContext);
    }
    
    /**
     * {@inheritDoc}
     */
    protected Object callAroundInvokes(Method proceed, Object[] arguments, List<InterceptorData> stack) throws Exception
    {
        InvocationContextImpl impl = new InvocationContextImpl(this.bean, this.actualInstance ,proceed, arguments, stack, InterceptorType.AROUND_INVOKE);
        impl.setCreationalContext(creationalContext);
        
        return impl.proceed();
    }
}
