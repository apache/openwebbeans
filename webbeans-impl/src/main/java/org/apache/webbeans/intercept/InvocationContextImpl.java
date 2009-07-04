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
package org.apache.webbeans.intercept;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.spi.Context;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.interceptor.InvocationContext;

import org.apache.webbeans.container.BeanManagerImpl;
import org.apache.webbeans.context.creational.CreationalContextFactory;

/**
 * Implementation of the {@link InvocationContext} interface.
 * 
 * @author <a href="mailto:gurkanerdogdu@yahoo.com">Gurkan Erdogdu</a>
 * @since 1.0
 */
public class InvocationContextImpl implements InvocationContext
{
    /** Context data for passing between interceptors */
    private Map<String, Object> contextData = new HashMap<String, Object>();

    /** Invoked method */
    private Method method;

    /** Method parameters */
    private Object[] parameters;

    /** Interceptor stack */
    private List<InterceptorData> interceptorDatas;

    /** Target object */
    private Object target;

    /** Interceptor type */
    private InterceptorType type;

    /** Used for numbering interceptors */
    private int currentMethod = 1;
    
    
    /**
     * Initializes the context.
     * 
     * @param target target object
     * @param method method
     * @param parameters method parameters
     * @param datas interceptor stack
     * @param type interceptor type
     */
    public InvocationContextImpl(Bean<?> bean, Object instance, Method method, Object[] parameters, List<InterceptorData> datas, InterceptorType type)
    {
        this.method = method;
        this.parameters = parameters;
        this.interceptorDatas = datas;
        this.type = type;
        
        if(instance == null)
        {
            configureTarget(bean);    
        }
        else
        {
            this.target = instance;
        }
    }

    
    @SuppressWarnings("unchecked")
    private void configureTarget(Bean<?> bean)
    {
        Context webbeansContext = BeanManagerImpl.getManager().getContext(bean.getScopeType());
        
        this.target = webbeansContext.get((Contextual<Object>)bean, (CreationalContext<Object>)CreationalContextFactory.getInstance().getCreationalContext(bean));        
        
    }
    
    /*
     * (non-Javadoc)
     * @see javax.interceptor.InvocationContext#getContextData()
     */
    public Map<String, Object> getContextData()
    {
        return this.contextData;
    }

    /*
     * (non-Javadoc)
     * @see javax.interceptor.InvocationContext#getMethod()
     */
    public Method getMethod()
    {
        return this.method;
    }

    /*
     * (non-Javadoc)
     * @see javax.interceptor.InvocationContext#getParameters()
     */
    public Object[] getParameters()
    {
        return this.parameters;
    }

    /*
     * (non-Javadoc)
     * @see javax.interceptor.InvocationContext#getTarget()
     */
    public Object getTarget()
    {
        return this.target;
    }

    /*
     * (non-Javadoc)
     * @see javax.interceptor.InvocationContext#proceed()
     */
    public Object proceed() throws Exception
    {
        try
        {
            if (type.equals(InterceptorType.AROUND_INVOKE))
            {
                return proceedAroundInvokes(this.interceptorDatas);
            }
            else
            {
                return proceedCommonAnnots(this.interceptorDatas, this.type);
            }

        }
        catch (Exception e)
        {
            this.target = null; // destroy target instance

            throw e;
        }
    }

    /*
     * Around invoke chain.
     */
    private Object proceedAroundInvokes(List<InterceptorData> datas) throws Exception
    {
        Object result = null;

        if (currentMethod <= datas.size())
        {
            InterceptorData intc = datas.get(currentMethod - 1);

            Method method = intc.getAroundInvoke();

            if (!method.isAccessible())
            {
                method.setAccessible(true);
            }

            Object t = intc.getInterceptorInstance();
            
            if (t == null)
            {
                t = target;
            }

            currentMethod++;
            
            result = method.invoke(t, new Object[] { this });
        }
        else
        {
            result = this.method.invoke(target, parameters);
        }

        return result;
    }

    /*
     * PreDestroy and PostConstruct chain
     */
    private Object proceedCommonAnnots(List<InterceptorData> datas, InterceptorType type) throws Exception
    {
        Object result = null;

        if (currentMethod <= datas.size())
        {
            InterceptorData intc = datas.get(currentMethod - 1);
            Method method = null;

            if (type.equals(InterceptorType.POST_CONSTRUCT))
            {
                method = intc.getPostConstruct();
            }
            else if (type.equals(InterceptorType.PRE_DESTROY))
            {
                method = intc.getPreDestroy();
            }

            if (!method.isAccessible())
            {
                method.setAccessible(true);
            }

            currentMethod++;

            Object t = intc.getInterceptorInstance();
            if (t == null)
            {
                t = target;
                
                result = method.invoke(t, new Object[] {});
            }
            else
            {
                result = method.invoke(t, new Object[] { this });
            }

        }

        return result;
    }

    /*
     * (non-Javadoc)
     * @see
     * javax.interceptor.InvocationContext#setParameters(java.lang.Object[])
     */
    public void setParameters(Object[] params)
    {
        if (getMethod() != null)
        {
            if (params == null)
            {
                if (this.parameters.length > 0)
                {
                    throw new IllegalArgumentException("Parameters is null");
                }
            }
            else
            {
                List<Class<?>> src = new ArrayList<Class<?>>();

                if (params.length != this.parameters.length)
                {
                    throw new IllegalArgumentException("Parameters length not match");
                }

                for (Object param : params)
                {
                    src.add(param.getClass());
                }

                for (Class<?> param : this.method.getParameterTypes())
                {
                    if (!src.contains(param))
                        throw new IllegalArgumentException("Parameters types not match");
                }

                System.arraycopy(params, 0, this.parameters, 0, params.length);

            }

        }
    }

}