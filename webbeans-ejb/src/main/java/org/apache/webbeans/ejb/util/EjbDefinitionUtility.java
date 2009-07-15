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
package org.apache.webbeans.ejb.util;

import java.util.List;

import javassist.util.proxy.ProxyFactory;

import org.apache.webbeans.ejb.component.EjbBean;
import org.apache.webbeans.ejb.component.creation.EjbBeanCreatorImpl;
import org.apache.webbeans.ejb.proxy.EjbBeanProxyHandler;
import org.apache.webbeans.exception.WebBeansException;

/**
 * @version $Rev$ $Date$
 */
public final class EjbDefinitionUtility
{
    private EjbDefinitionUtility()
    {
        
    }

    @SuppressWarnings("unchecked")
    public static void defineApiType(EjbBean<?> ejbComponent)
    {        
        EjbBeanCreatorImpl<?> creator = new EjbBeanCreatorImpl(ejbComponent);
        creator.defineApiType();
    }
    
    @SuppressWarnings("unchecked")
    public static <T> T defineEjbBeanProxy(EjbBean<T> bean)
    {
        try
        {
            ProxyFactory factory = new ProxyFactory();
            
            EjbBeanProxyHandler handler = new EjbBeanProxyHandler(bean);
            
            factory.setHandler(handler);
            List<Class> interfaces = bean.getDeploymentInfo().getBusinessLocalInterfaces();            
            factory.setInterfaces(interfaces.toArray(new Class[0]));  
         
            return (T)factory.createClass().newInstance();
            
        }catch(Exception e)
        {
            throw new WebBeansException(e);
        }
    }
}
