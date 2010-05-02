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

import javax.enterprise.context.ContextNotActiveException;
import javax.enterprise.context.spi.Context;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;

import org.apache.webbeans.component.OwbBean;
import org.apache.webbeans.context.AbstractContext;
import org.apache.webbeans.context.creational.CreationalContextFactory;
import org.apache.webbeans.context.creational.CreationalContextImpl;


/**
 * Normal scoped beans interceptor handler.
 * @version $Rev$ $Date$
 *
 */
@SuppressWarnings("unchecked")
public class NormalScopedBeanInterceptorHandler extends InterceptorHandler 
{
    /**Serial id*/
    private static final long serialVersionUID = 1L;
    
    /**
     * Creates a new bean instance
     * @param bean bean 
     * @param creationalContext creational context
     */
    public NormalScopedBeanInterceptorHandler(OwbBean<?> bean, CreationalContext<?> creationalContext)
    {
        super(bean);    
        
        //Initiate bean for saving creational context instance
        initiateBeanBag((OwbBean<Object>)bean, (CreationalContext<Object>)creationalContext);
    }
    
    private void initiateBeanBag(OwbBean<Object> bean, CreationalContext<Object> creationalContext)
    {
        try
        {
            Context webbeansContext = getBeanManager().getContext(bean.getScope());
            if (webbeansContext instanceof AbstractContext)
            {
                AbstractContext owbContext = (AbstractContext)webbeansContext;
                owbContext.initContextualBag(bean, creationalContext);
            }            
        }catch(ContextNotActiveException e)
        {
            //Nothing
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Object invoke(Object instance, Method method, Method proceed, Object[] arguments) throws Exception
    {
        if (method.getName().equals("finalize") &&
            method.getParameterTypes().length == 0
        	&& method.getReturnType().equals(Void.TYPE)) 
        {
        	// we should NOT invoke the bean's finalize() from proxied 
        	// finalize() method since JVM will invoke it directly. 
        	// OWB-366
        	return null;
        }

        //Get instance from context
        Object webbeansInstance = getContextualInstance();
        
        //Call super
        return super.invoke(webbeansInstance, method, proceed, arguments, (CreationalContextImpl<?>) getContextualCreationalContext());
    }
        
    /**
     * {@inheritDoc}
     */
    protected Object callAroundInvokes(Method proceed, Object[] arguments, List<InterceptorData> stack) throws Exception
    {
        InvocationContextImpl impl = new InvocationContextImpl(this.bean, getContextualInstance(),
                                                               proceed, arguments, stack, InterceptorType.AROUND_INVOKE);
        impl.setCreationalContext(getContextualCreationalContext());

        return impl.proceed();

    }
    
    
    /**
     * Gets instance from context.
     * @param bean bean instance
     * @return the underlying contextual instance, either cached or resolved from the context 
     */
    protected Object getContextualInstance()
    {
        Object webbeansInstance = null;

        //Context of the bean
        Context webbeansContext = getBeanManager().getContext(this.bean.getScope());
        
        //Already saved in context?
        webbeansInstance=webbeansContext.get(this.bean);
        if (webbeansInstance != null)
        {
            // voila, we are finished if we found an existing contextual instance
            return webbeansInstance;
        }
        else
        {
            // finally, we create a new contextual instance
            webbeansInstance = webbeansContext.get((Contextual<Object>)this.bean, getContextualCreationalContext());   
        }
                
        return webbeansInstance;
    }
    
    protected CreationalContext<Object> getContextualCreationalContext()
    {
        CreationalContext<Object> creationalContext = null;
        
        OwbBean<Object> contextual = (OwbBean<Object>)this.bean;
        //Context of the bean
        Context webbeansContext = getBeanManager().getContext(bean.getScope());
        if (webbeansContext instanceof AbstractContext)
        {
            AbstractContext owbContext = (AbstractContext)webbeansContext;
            creationalContext = owbContext.getCreationalContext(contextual);

            //No creational context means that no BeanInstanceBag
            //Actually this can be occurs like scenarions
            //@SessionScoped bean injected into @ApplicationScopedBean
            //And session is destroyed and restarted but proxy still
            //contained in @ApplicationScopedBean
            if(creationalContext == null)
            {
                creationalContext = CreationalContextFactory.getInstance().getCreationalContext(contextual);
                owbContext.initContextualBag((OwbBean<Object>)this.bean, creationalContext);
            }            
        }
                
        return creationalContext;
    }
}
