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

import javax.enterprise.context.spi.Context;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;

import org.apache.webbeans.component.AbstractBean;
import org.apache.webbeans.container.BeanManagerImpl;

public class NormalScopedBeanInterceptorHandler extends InterceptorHandler
{
    private static final long serialVersionUID = -7169354477951284657L;

    private CreationalContext<?> creationalContext;
    
    
    public NormalScopedBeanInterceptorHandler(AbstractBean<?> bean, CreationalContext<?> creationalContext)
    {
        super(bean);
        this.creationalContext = creationalContext;
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public Object invoke(Object instance, Method method, Method proceed, Object[] arguments) throws Exception
    {
        //Context of the bean
        Context webbeansContext = BeanManagerImpl.getManager().getContext(bean.getScope());
        
        //Get bean instance from context
        Object webbeansInstance = webbeansContext.get((Contextual<Object>)this.bean, (CreationalContext<Object>)this.creationalContext);
        
        // TODO Auto-generated method stub
        return super.invoke(webbeansInstance, method, proceed, arguments);
    }
    
    protected <T> Object callAroundInvokes(Method proceed, Object[] arguments, List<InterceptorData> stack) throws Exception
    {
        InvocationContextImpl impl = new InvocationContextImpl(this.bean, null,proceed, arguments, stack, InterceptorType.AROUND_INVOKE);

        return impl.proceed();

    }
    
    
}
