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
package org.apache.webbeans.proxy;

import java.lang.reflect.Method;

import javax.enterprise.context.spi.Context;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;

import org.apache.webbeans.config.WebBeansContext;

import javassist.util.proxy.MethodHandler;

public class ProvidedBeansProxyHandler implements MethodHandler
{
    private Bean<?> bean  = null;
    
    public ProvidedBeansProxyHandler(Bean<?> bean)
    {
        this.bean = bean;
    }

    @SuppressWarnings("unchecked")
    public Object invoke(Object self, Method thisMethod, Method proceed, Object[] args) throws Throwable
    {
        BeanManager beanManager = WebBeansContext.getInstance().getBeanManagerImpl();
        
        Context context = beanManager.getContext(bean.getScope());
        
        return context.get((Bean<Object>)bean, (CreationalContext<Object>)beanManager.createCreationalContext(bean));
        
    }

}
