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
package org.apache.webbeans.intercept;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.spi.Context;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;
import javax.interceptor.InvocationContext;

import org.apache.webbeans.component.EnterpriseBeanMarker;
import org.apache.webbeans.component.OwbBean;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.context.creational.CreationalContextImpl;
import org.apache.webbeans.util.ClassUtil;

/**
 * Implementation of the {@link InvocationContext} interface.
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
    
    /**Bean creational context*/
    private CreationalContext<?> creationalContext;
    
    private OwbBean<?> owbBean;
    private InvocationContext ejbInvocationContext;
    private WebBeansContext webBeansContext;

    
    /** alternate key to be used for dependent creational contexts */
    private Object ccKey;
    
    /**
     * Initializes the invocation context.
     *
     * @param bean the Bean meta info
     * @param instance target object
     * @param method method
     * @param parameters method parameters
     * @param datas interceptor stack
     * @param type interceptor type
     */
    public InvocationContextImpl(WebBeansContext webBeansContext, OwbBean<?> bean, Object instance, Method method,
                                 Object[] parameters, List<InterceptorData> datas, InterceptorType type)
    {
        this.webBeansContext = webBeansContext;
        this.owbBean = bean;
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
    
    /**
     * Sets owner bean creational context.
     * @param ownerCreationalContext owner creational context
     */
    public void setCreationalContext(CreationalContext<?> ownerCreationalContext)
    {
        this.creationalContext = ownerCreationalContext;
    }

    /**
     * Sets EJB invocation context
     * @param c EJB containers invocation context
     */
    public void setEJBInvocationContext(InvocationContext c)
    {
        this.ejbInvocationContext = c;
    }

    
    /**
     * Gets target instance for given bean.
     * @param bean bean instance
     */
    @SuppressWarnings("unchecked")
    private void configureTarget(OwbBean<?> bean)
    {
        Context webbeansContext = bean.getWebBeansContext().getBeanManagerImpl().getContext(bean.getScope());
        
        this.target = webbeansContext.get((Contextual<Object>)bean, (CreationalContext<Object>)this.creationalContext);        
        
    }
    
    /**
     * {@inheritDoc}
     */
    public Map<String, Object> getContextData()
    {
        return this.contextData;
    }
    
    /**
     * {@inheritDoc}
     */
    public Method getMethod()
    {
        return this.method;
    }
    
    /**
     * {@inheritDoc}
     */
    public Object[] getParameters()
    {
        return this.parameters;
    }
    
    /**
     * {@inheritDoc}
     */
    public Object getTarget()
    {
        return this.target;
    }
    
    /**
     * {@inheritDoc}
     */
    public Object proceed() throws Exception
    {
        try
        {
            if (type.equals(InterceptorType.AROUND_INVOKE))
            {
                return proceedAroundInvokes(this.interceptorDatas);
            }
            else if (type.equals(InterceptorType.AROUND_TIMEOUT))
            {
                return proceedAroundTimeouts(this.interceptorDatas);
            }
            return proceedCommonAnnots(this.interceptorDatas, this.type);

        }
        catch (InvocationTargetException ite)
        {
            this.target = null; // destroy target instance
            
            // Try to provide the original exception to the interceptor stack, 
            // not the InvocationTargetException from Method.invoke
            Throwable t = ite.getCause();
            if (t instanceof Exception)
            {
                throw (Exception) t;
            }
            throw ite;
        }
        catch (Exception e)
        {
            this.target = null; // destroy target instance

            throw e;
        }
    }
    
    /**
     * AroundInvoke operations on stack.
     * @param datas interceptor stack
     * @return final result
     * @throws Exception for exceptions
     */
    private Object proceedAroundInvokes(List<InterceptorData> datas) throws Exception
    {
        Object result = null;

        if (currentMethod <= datas.size())
        {
            InterceptorData intc = datas.get(currentMethod - 1);

            Method aroundInvokeMethod = intc.getAroundInvoke();
            boolean accessible = aroundInvokeMethod.isAccessible();
            
            if (!accessible)
            {
                owbBean.getWebBeansContext().getSecurityService().doPrivilegedSetAccessible(aroundInvokeMethod, true);
            }
            
            Object t = intc.createNewInstance(this.ccKey != null ? this.ccKey : this.target,
                    (CreationalContextImpl<?>)this.creationalContext);     
            
            if (t == null)
            {
                t = target;
            }

            currentMethod++;
            
            result = aroundInvokeMethod.invoke(t, new Object[] { this });
            
            if(!accessible)
            {
                owbBean.getWebBeansContext().getSecurityService().doPrivilegedSetAccessible(aroundInvokeMethod, false);
            }

        }
        else
        {
            if(!(this.owbBean instanceof EnterpriseBeanMarker))
            {
                boolean accessible = this.method.isAccessible();
                if(!accessible)
                {                
                    owbBean.getWebBeansContext().getSecurityService().doPrivilegedSetAccessible(method, true);
                }
                
                result = this.method.invoke(target, parameters);
                
                if(!accessible)
                {
                    owbBean.getWebBeansContext().getSecurityService().doPrivilegedSetAccessible(method, false);
                }                
            }
            else 
            { 
                if (this.ejbInvocationContext != null)
                {
                    result = ejbInvocationContext.proceed();
                }
            }
        }

        return result;
    }

    /**
     * AroundTimeout operations on stack.
     * @param datas interceptor stack
     * @return final result
     * @throws Exception for exceptions
     */
    private Object proceedAroundTimeouts(List<InterceptorData> datas) throws Exception
    {
        Object result = null;

        if (currentMethod <= datas.size())
        {
            InterceptorData intc = datas.get(currentMethod - 1);

            Method aroundTimeoutMethod = intc.getAroundTimeout();
            boolean accessible = aroundTimeoutMethod.isAccessible();
            
            if (!accessible)
            {
                owbBean.getWebBeansContext().getSecurityService().doPrivilegedSetAccessible(aroundTimeoutMethod, true);
            }
            
            Object t = intc.createNewInstance(this.ccKey != null ? this.ccKey : this.target,
                    (CreationalContextImpl<?>)this.creationalContext);      
            
            if (t == null)
            {
                t = target;
            }

            currentMethod++;
            
            result = aroundTimeoutMethod.invoke(t, new Object[] { this });
            
            if(!accessible)
            {
                owbBean.getWebBeansContext().getSecurityService().doPrivilegedSetAccessible(aroundTimeoutMethod, false);
            }

        }
        else
        {
            if(!(this.owbBean instanceof EnterpriseBeanMarker))
            {
                boolean accessible = method.isAccessible();
                if(!accessible)
                {                
                    owbBean.getWebBeansContext().getSecurityService().doPrivilegedSetAccessible(method, true);
                }
                
                result = this.method.invoke(target, parameters);
                
                if(!accessible)
                {
                    owbBean.getWebBeansContext().getSecurityService().doPrivilegedSetAccessible(method, false);
                }                
            }
            else 
            { 
                if (this.ejbInvocationContext != null)
                {
                    result = ejbInvocationContext.proceed();
                }
            }
        }

        return result;
    }

    /**
     * Post construct and predestroy 
     * callback operations.
     * @param datas interceptor stack
     * @param type interceptor type
     * @return final result
     * @throws Exception for any exception
     */
    private Object proceedCommonAnnots(List<InterceptorData> datas, InterceptorType type) throws Exception
    {
        Object result = null;

        if (currentMethod <= datas.size())
        {
            InterceptorData intc = datas.get(currentMethod - 1);
            Method commonAnnMethod = null;

            if (type.equals(InterceptorType.POST_CONSTRUCT))
            {
                commonAnnMethod = intc.getPostConstruct();
            }
            else if (type.equals(InterceptorType.POST_ACTIVATE))
            {
                commonAnnMethod = intc.getPostActivate();
            }
            else if (type.equals(InterceptorType.PRE_PASSIVATE))
            {
                commonAnnMethod = intc.getPrePassivate();
            }
            else if (type.equals(InterceptorType.PRE_DESTROY))
            {
                commonAnnMethod = intc.getPreDestroy();
            }
            else
            {
                throw new IllegalArgumentException("Unsupportet InterceptorType: " + type);
            }

            if (commonAnnMethod == null)
            {
                throw new IllegalArgumentException("Could not find intercepted Method!");
            }

            if (!commonAnnMethod.isAccessible())
            {
                webBeansContext.getSecurityService().doPrivilegedSetAccessible(commonAnnMethod, true);
            }

            currentMethod++;

            Object t = intc.createNewInstance(this.ccKey != null ? this.ccKey : this.target,
                    (CreationalContextImpl<?>)this.creationalContext);      
            
            //In bean class
            if (t == null)
            {
                if(!(this.owbBean instanceof EnterpriseBeanMarker))
                {
                    t = target;                
                    result = commonAnnMethod.invoke(t, new Object[] {});
                    
                    //Continue to call others
                    proceedCommonAnnots(datas, type);                                    
                }                
            }
            //In interceptor class
            else
            {
                result = commonAnnMethod.invoke(t, new Object[] { this });
            }

        }
        else 
        {
            /* For EJB's, we do not call the "in bean class" interceptors --the container does, and only if
             * our last 299 interceptor called proceed (which takes us here).
             */
            if ((this.owbBean instanceof EnterpriseBeanMarker) && (this.ejbInvocationContext != null))
            {
                result = ejbInvocationContext.proceed();
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public void setParameters(Object[] params)
    {
        if (getMethod() != null)
        {
            if (params == null)
            {
                if (this.parameters.length >= 0)
                {
                    throw new IllegalArgumentException("Gvien parameters is null but expected not null parameters");
                }
            }
            else
            {
                if (params.length != this.parameters.length)
                {
                    throw new IllegalArgumentException("Expected " + this.parameters.length + " " +
                            "parameters, but only got " + params.length + " parameters");
                }

                Class<?>[] methodParameters = this.method.getParameterTypes();
                int i = 0;
                for (Object obj : params)
                {
                    Class<?> parameterType = methodParameters[i++];
                    if (obj == null)
                    {
                        if (parameterType.isPrimitive())
                        {
                            throw new IllegalArgumentException("Expected parameter " + i + " to be primitive type " + parameterType.getName() +
                                    ", but got a parameter that is null");
                        }
                    }
                    else
                    {
                        //Primitive check
                        if(parameterType.isPrimitive())
                        {
                            //First change to wrapper for comparision
                            parameterType = ClassUtil.getPrimitiveWrapper(parameterType);
                        }

                        //Actual check
                        if (!parameterType.isInstance(obj))
                        {
                            throw new IllegalArgumentException("Expected parameter " + i + " to be of type " + parameterType.getName() +
                                    ", but got a parameter of type " + obj.getClass().getName());
                        }
                    }
                }

                System.arraycopy(params, 0, this.parameters, 0, params.length);

            }

        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getTimer()
    {
        // TODO Auto-generated method stub
        return null;
    }    
    
    /**
     * Sets the alternate key (alternate owner instance) to be used within 
     * the passed CreationalContext for dependent interceptors.
     * 
     * @param ccKey a unique key used to index dependent interceptors
     */
    public void setCcKey(Object ccKey)
    {
        this.ccKey = ccKey;
    }
}