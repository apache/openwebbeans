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

import javax.enterprise.context.spi.Context;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;

import org.apache.webbeans.container.BeanManagerImpl;
import org.apache.webbeans.context.creational.CreationalContextFactory;
import org.apache.webbeans.ejb.component.EjbBean;
import org.apache.webbeans.ejb.interceptor.OpenWebBeansEjbInterceptor;

import javassist.util.proxy.MethodHandler;

@SuppressWarnings("unchecked")
public class EjbBeanProxyHandler implements MethodHandler
{
    private EjbBean<?> ejbBean;
    
    public EjbBeanProxyHandler(EjbBean<?> ejbBean)
    {
        this.ejbBean = ejbBean;
    }
    
    @Override
    public Object invoke(Object instance, Method method, Method proceed, Object[] arguments) throws Exception
    {
        OpenWebBeansEjbInterceptor.setThreadLocal(this.ejbBean);
        
        //Context of the bean
        Context webbeansContext = BeanManagerImpl.getManager().getContext(ejbBean.getScopeType());
        
        //Get bean instance from context
        Object webbeansInstance = webbeansContext.get((Contextual<Object>)this.ejbBean, (CreationalContext<Object>)CreationalContextFactory.getInstance().getCreationalContext(this.ejbBean));
        
        Object result = method.invoke(webbeansInstance, arguments);
        
        OpenWebBeansEjbInterceptor.unsetThreadLocal();
        
        return result;
    }
    

}
