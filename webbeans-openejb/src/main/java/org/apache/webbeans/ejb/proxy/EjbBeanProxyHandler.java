/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.webbeans.ejb.proxy;

import java.lang.reflect.Method;
import java.util.List;

import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.Context;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.SessionBeanType;

import org.apache.webbeans.container.BeanManagerImpl;
import org.apache.webbeans.ejb.component.EjbBean;
import org.apache.webbeans.ejb.interceptor.OpenWebBeansEjbInterceptor;

import javassist.util.proxy.MethodHandler;

/**
 * EJB beans proxy handler.
 * @version $Rev$ $Date$
 *
 */
@SuppressWarnings("unchecked")
public class EjbBeanProxyHandler implements MethodHandler
{
    /**Proxy ejb bean instance*/
    private EjbBean<?> ejbBean;
    
    private CreationalContext<?> creationalContext;
    
    /**
     * Creates a new instance.
     * @param ejbBean ejb bean instance
     */
    public EjbBeanProxyHandler(EjbBean<?> ejbBean, CreationalContext<?> creationalContext)
    {
        this.ejbBean = ejbBean;
        this.creationalContext = creationalContext;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Object invoke(Object instance, Method method, Method proceed, Object[] arguments) throws Exception
    {
        //Check ejb remove method
        if(this.ejbBean.getEjbType().equals(SessionBeanType.STATEFUL))
        {
            if(checkEjbRemoveMethod(method))
            {
                this.ejbBean.setRemoveStatefulInstance(true);
            }
        }
        
        OpenWebBeansEjbInterceptor.setThreadLocal(this.ejbBean, this.creationalContext);
        
        //Context of the bean
        Context webbeansContext = BeanManagerImpl.getManager().getContext(ejbBean.getScope());
        
        //Get bean instance from context
        Object webbeansInstance = webbeansContext.get((Contextual<Object>)this.ejbBean, (CreationalContext<Object>)this.creationalContext);
        
        Object result = method.invoke(webbeansInstance, arguments);
        
        OpenWebBeansEjbInterceptor.unsetThreadLocal();
        
        return result;
    }
    
    /**
     * Check stateful bean remove method control.
     * @param method called method
     * @throws UnsupportedOperationException if not valid call
     */
    private boolean checkEjbRemoveMethod(Method method)
    {
        List<Method> removeMethods = this.ejbBean.getDeploymentInfo().getRemoveMethods();
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
}
