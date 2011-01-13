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
package org.apache.webbeans.ejb.common.util;

import javax.enterprise.context.spi.CreationalContext;

import javassist.util.proxy.ProxyFactory;
import javassist.util.proxy.ProxyObject;

import org.apache.webbeans.ejb.common.component.BaseEjbBean;
import org.apache.webbeans.ejb.common.component.EjbBeanCreatorImpl;
import org.apache.webbeans.ejb.common.proxy.EjbBeanProxyHandler;
import org.apache.webbeans.exception.WebBeansException;
import org.apache.webbeans.util.ClassUtil;

/**
 * @version $Rev: 917060 $ $Date: 2010-02-28 00:14:47 +0200 (Sun, 28 Feb 2010) $
 */
public final class EjbDefinitionUtility
{
    private EjbDefinitionUtility()
    {
        
    }

    @SuppressWarnings("unchecked")
    public static void defineApiType(BaseEjbBean<?> ejbComponent)
    {        
        EjbBeanCreatorImpl<?> creator = new EjbBeanCreatorImpl(ejbComponent);
        creator.defineApiType();
    }
    
    /**
     * 
     * @param <T> EJB bean implementation class
     * @param bean The Enterprise bean to create a proxy for
     * @param iface The @Local interface the proxy should implement or the 
     *              @Localbean class the proxy should extend.
     * @param creationalContext 
     * @return
     */
    @SuppressWarnings({"unchecked"})
    public static <T> T defineEjbBeanProxy(BaseEjbBean<T> bean, Class<?> iface, CreationalContext<?> creationalContext)
    {
        try
        {
            T proxyInstance = null;
            Class<?> clazz = bean.getWebBeansContext().getJavassistProxyFactory().getEjbBeanProxyClass(bean, iface);
            if(clazz == null)
            {
                ProxyFactory factory = new ProxyFactory();
                if (iface.isInterface())
                {
                    factory.setInterfaces(new Class[]{iface});
                }
                else 
                {
                    // @LocalBean no-interface local view requested
                    factory.setSuperclass(iface);
                }

                clazz = bean.getWebBeansContext().getJavassistProxyFactory().defineEjbBeanProxyClass(bean, iface, factory);
            }
            
            proxyInstance = (T) ClassUtil.newInstance(clazz);
            
            EjbBeanProxyHandler handler = new EjbBeanProxyHandler(bean, creationalContext);
            ((ProxyObject)proxyInstance).setHandler(handler);
            
            return proxyInstance;
            
        }
        catch(Exception e)
        {
            throw new WebBeansException(e);
        }
    }
}
