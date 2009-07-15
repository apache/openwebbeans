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
package org.apache.webbeans.ejb.interceptor;

import javax.annotation.PostConstruct;
import javax.interceptor.InvocationContext;

import org.apache.webbeans.ejb.component.EjbBean;

public class OpenWebBeansEjbInterceptor
{
    private static ThreadLocal<EjbBean<?>> threadLocal = new ThreadLocal<EjbBean<?>>();
    
    public static void setThreadLocal(EjbBean<?> ejbBean)
    {
        threadLocal.set(ejbBean);
    }
    
    public static void unsetThreadLocal()
    {
        threadLocal.remove();
    }
    
    @PostConstruct
    @SuppressWarnings("unchecked")
    public void afterConstruct(InvocationContext context) throws Exception
    {
        Object instance = context.getTarget();
        EjbBean<Object> bean = (EjbBean<Object>)threadLocal.get();
        
        threadLocal.get().injectFieldInInterceptor(instance, bean.getCreationalContext());
        
        
    }
    
}
