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

import org.apache.webbeans.component.AbstractOwbBean;
import org.apache.webbeans.container.BeanManagerImpl;
import org.apache.webbeans.context.creational.CreationalContextImpl;

public class NormalScopedBeanInterceptorHandler extends InterceptorHandler 
{
    private static final long serialVersionUID = -7169354477951284657L;

    // A creationalContext has a very short lifespan. So we can use a ThreadLocal to pass it over
    // if we make sure that it is cleaned up properly!
    private static ThreadLocal<CreationalContext<Object>> creationalContxt = new ThreadLocal<CreationalContext<Object>>();

    @SuppressWarnings("unchecked")
    public NormalScopedBeanInterceptorHandler(AbstractOwbBean<?> bean, CreationalContext<?> cc)
    {
        super(bean);
        creationalContxt.set((CreationalContext<Object>) cc);
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public Object invoke(Object instance, Method method, Method proceed, Object[] arguments) throws Exception
    {
        BeanManagerImpl beanManager = BeanManagerImpl.getManager();

        //Context of the bean
        Context webbeansContext = beanManager.getContext(bean.getScope());
        Object webbeansInstance = webbeansContext.get(this.bean);

        if (webbeansInstance == null)
        {
            CreationalContext<Object> cc = creationalContxt.get();

            if (cc == null)
            {
                // we need to create the CreationalContext ourself and store it
                try
                {
                    cc = (CreationalContext<Object>) beanManager.createCreationalContext(bean);
                    creationalContxt.set(cc);
                    //Get bean instance from context
                    webbeansInstance = webbeansContext.get((Contextual<Object>)this.bean, cc);
                }
                finally
                {
                    // make sure that we remove the cc from the thread in the handler who created it
                    creationalContxt.remove();
                }
            }
            else
            {
                //Get bean instance from context
                webbeansInstance = webbeansContext.get((Contextual<Object>)this.bean, cc);
            }

        }


        return super.invoke(webbeansInstance, method, proceed, arguments, (CreationalContextImpl<?>)creationalContxt.get());
    }
    
    protected <T> Object callAroundInvokes(Method proceed, Object[] arguments, List<InterceptorData> stack) throws Exception
    {
        InvocationContextImpl impl = new InvocationContextImpl(this.bean, null,proceed, arguments, stack, InterceptorType.AROUND_INVOKE);

        return impl.proceed();

    }
    
    
}
