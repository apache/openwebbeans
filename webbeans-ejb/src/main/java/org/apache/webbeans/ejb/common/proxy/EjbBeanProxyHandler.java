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
package org.apache.webbeans.ejb.common.proxy;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;
import java.util.List;

import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.Context;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.SessionBeanType;

import org.apache.webbeans.component.OwbBean;
import org.apache.webbeans.config.OWBLogConst;
import org.apache.webbeans.container.BeanManagerImpl;
import org.apache.webbeans.context.AbstractContext;
import org.apache.webbeans.context.creational.CreationalContextFactory;
import org.apache.webbeans.ejb.common.component.BaseEjbBean;
import org.apache.webbeans.ejb.common.interceptor.OpenWebBeansEjbInterceptor;
import org.apache.webbeans.logger.WebBeansLogger;
import org.apache.webbeans.util.ClassUtil;
import org.apache.webbeans.util.SecurityUtil;
import org.apache.webbeans.util.WebBeansUtil;

import javassist.util.proxy.MethodHandler;

/**
 * EJB beans proxy handler.
 * @version $Rev: 889852 $ $Date: 2009-12-12 01:11:53 +0200 (Sat, 12 Dec 2009) $
 *
 */
@SuppressWarnings("unchecked")
public class EjbBeanProxyHandler implements MethodHandler
{
    //Logger instance
    private final WebBeansLogger logger = WebBeansLogger.getLogger(EjbBeanProxyHandler.class);
    
    /**Proxy ejb bean instance*/
    private BaseEjbBean<?> ejbBean;
    
    /**Dependent ejb instance*/
    private Object dependentEJB;
    
    /**Scope is dependent*/
    private boolean isDependent = false;
    
    /**Creational Context*/
    private CreationalContext<?> creationalContext;
    
    /**
     * Creates a new instance.
     * @param ejbBean ejb bean instance
     */
    public EjbBeanProxyHandler(BaseEjbBean<?> ejbBean, CreationalContext<?> creationalContext)
    {
        this.ejbBean = ejbBean;
        
        if(WebBeansUtil.isScopeTypeNormal(ejbBean.getScope()))
        {
            initiateBeanBag((OwbBean<Object>)ejbBean, (CreationalContext<Object>)creationalContext);
        }
        else
        {
            this.creationalContext = creationalContext;   
        }
        
        if (ejbBean.getScope().equals(Dependent.class)) 
        {
            isDependent = true;
            dependentEJB = null;
        }
    }    
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Object invoke(Object proxyInstance, Method method, Method proceed, Object[] arguments) throws Exception
    {
        Object result = null;
        
        //Calling method name on Proxy
        String methodName = method.getName();
        
        if(ClassUtil.isObjectMethod(methodName) && !methodName.equals("toString"))
        {
            logger.trace("Calling method on proxy is restricted except Object.toString(), but current method is Object. [{0}]", methodName);
            
            boolean access = method.isAccessible();
            SecurityUtil.doPrivilegedSetAccessible(method, true);
            try
            {
                return proceed.invoke(proxyInstance, arguments);
                
            }
            finally
            {
                SecurityUtil.doPrivilegedSetAccessible(method, access);
            }            
        }
                
        try
        {
            Object webbeansInstance = null;

            // Set Ejb bean on thread local
            OpenWebBeansEjbInterceptor.setThreadLocal(this.ejbBean, getContextualCreationalContext());

            // Context of the bean
            Context webbeansContext = BeanManagerImpl.getManager().getContext(this.ejbBean.getScope());

            // Don't go into a _dependent_ context on subsequent method calls in
            // this proxy!
            if (isDependent && this.dependentEJB != null)
            {
                webbeansInstance = this.dependentEJB;
            }
            else
            {
                // try looking in the context without
                // getContextualCreationalContext() first
                webbeansInstance = webbeansContext.get(this.ejbBean);
                if (webbeansInstance == null)
                {
                    webbeansInstance = webbeansContext.get((Contextual<Object>) this.ejbBean, getContextualCreationalContext());
                }

                // We just got a new dependent EJB, save it in this the 
                // method handler.
                if (isDependent && webbeansInstance != null)
                {
                    this.dependentEJB = webbeansInstance;

                    if (this.ejbBean.getEjbType().equals(SessionBeanType.STATEFUL))
                    {
                        // It's an SFSB, so we need to track when it's removed
                        this.ejbBean.addDependentSFSB(webbeansInstance, proxyInstance);
                    }
                }
            }
            
            //Check ejb remove method for dependent SFSB (
            if (this.ejbBean.isDependent() && this.ejbBean.getEjbType().equals(SessionBeanType.STATEFUL))
            {
                if (checkEjbRemoveMethod(method))
                {
                    // Stop tracking the EJB associated with this proxy
                    this.ejbBean.removeDependentSFSB(proxyInstance);
                    
                    /*
                     * Keep the local reference to the dependent SFSB. If the
                     * user calls a contextual EJB with a removed EJB associated
                     * with it we should let the EJB container complain, not
                     * give them a new EJB under the covers.
                     */
                }
            }

            //Call actual method on proxy
            //Actually it is called from OWB Proxy --> EJB Proxy --> Actual Bean Instance
            boolean access = method.isAccessible();
            SecurityUtil.doPrivilegedSetAccessible(method, true);
            try
            {
                result = method.invoke(webbeansInstance, arguments);
                
            }
            finally
            {
                SecurityUtil.doPrivilegedSetAccessible(method, access);
            }            
            
        }
        finally
        {
            OpenWebBeansEjbInterceptor.unsetThreadLocal();   
        }                
        
        return result;
    }    
    
    /**
     * Check stateful bean remove method control.
     * @param method called method
     * @throws UnsupportedOperationException if not valid call
     */
    private boolean checkEjbRemoveMethod(Method method)
    {
        List<Method> removeMethods = this.ejbBean.getRemoveMethods();
        if(removeMethods.contains(method))
        {
            if(this.ejbBean.getScope() != Dependent.class)
            {
                throw new UnsupportedOperationException("Can not call EJB Statefull Bean Remove Method without scoped @Dependent");
            }
            else
            {
                return true;
            }
        }
        
        return false;
    }
       
    protected CreationalContext<Object> getContextualCreationalContext()
    {
        CreationalContext<Object> creationalContext = null;
        
        if(this.creationalContext != null)
        {
            return (CreationalContext<Object>)this.creationalContext;
        }
        
        OwbBean<Object> contextual = (OwbBean<Object>)this.ejbBean;
        //Context of the bean
        Context webbeansContext = BeanManagerImpl.getManager().getContext(this.ejbBean.getScope());
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
                owbContext.initContextualBag((OwbBean<Object>)this.ejbBean, creationalContext);
            }            
        }
                
        return creationalContext;
    }
    
    private void initiateBeanBag(OwbBean<Object> bean, CreationalContext<Object> creationalContext)
    {
        Context webbeansContext =  BeanManagerImpl.getManager().getContext(bean.getScope());
        if (webbeansContext instanceof AbstractContext)
        {
            AbstractContext owbContext = (AbstractContext)webbeansContext;
            owbContext.initContextualBag(bean, creationalContext);
        }
    }    
    
    /**
     * Write to stream.
     * @param s stream
     * @throws IOException
     */
    private  void writeObject(ObjectOutputStream s) throws IOException
    {
        // we have to write the ids for all beans, not only PassivationCapable
        // since this gets serialized along with the Bean proxy.
        String passivationId = this.ejbBean.getId();
        if (passivationId!= null)
        {
            s.writeObject(passivationId);
        }
        else
        {
            s.writeObject(null);
            logger.warn(OWBLogConst.WARN_0015, this.ejbBean);
        }
        
        s.writeBoolean(this.isDependent);
        s.writeObject(this.creationalContext);
        s.writeObject(this.dependentEJB);
    }
    
    /**
     * Read from stream.
     * @param s stream
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private  void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException
    {
        String passivationId = (String) s.readObject();
        if (passivationId != null)
        {
            this.ejbBean = (BaseEjbBean<?>)BeanManagerImpl.getManager().getPassivationCapableBean(passivationId);
        }
        
        this.isDependent = s.readBoolean();
        this.creationalContext = (CreationalContext<?>)s.readObject();
        this.dependentEJB = s.readObject();
    }
    
}
