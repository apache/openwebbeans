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
package org.apache.webbeans.portable;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.InjectionPoint;

import org.apache.webbeans.component.InstanceBean;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.context.creational.CreationalContextImpl;
import org.apache.webbeans.inject.instance.InstanceImpl;

public class InstanceProducer<T> extends AbstractProducer<Instance<T>>
{
    private Class<Instance<T>> returnType;
    private Set<Annotation> qualifiers;
    private WebBeansContext webBeansContext;
    
    public InstanceProducer(Class<Instance<T>> returnType, Set<Annotation> qualifiers, WebBeansContext webBeansContext)
    {
        this.returnType = returnType;
        this.qualifiers = qualifiers;
        this.webBeansContext = webBeansContext;
    }

    @Override
    public Instance<T> produce(CreationalContext<Instance<T>> creationalContext)
    {
        try
        {
            InjectionPoint injectionPoint = null;
            CreationalContextImpl<Instance<T>> creationalContextImpl = null;
            if (creationalContext instanceof CreationalContextImpl)
            {
                creationalContextImpl = (CreationalContextImpl<Instance<T>>)creationalContext;
            }
            else
            {
                InstanceBean<Object> instanceBean = webBeansContext.getWebBeansUtil().getInstanceBean();
                creationalContextImpl = webBeansContext.getCreationalContextFactory().wrappedCreationalContext(creationalContext, instanceBean);
            }
            injectionPoint = creationalContextImpl.getInjectionPoint();
            Set<Annotation> qualifiers;
            Type type;

            if (injectionPoint != null)
            {
                ParameterizedType injectedType = (ParameterizedType)injectionPoint.getType();
                qualifiers = injectionPoint.getQualifiers();
                type = injectedType.getActualTypeArguments()[0];
            }
            else
            {
                qualifiers = this.qualifiers;
                type = this.returnType;
            }

            Instance<T> instance = new InstanceImpl<T>(type, injectionPoint, webBeansContext, creationalContextImpl, qualifiers.toArray(new Annotation[qualifiers.size()]));
            
            return instance;
        }
        finally
        {
            if (creationalContext instanceof CreationalContextImpl)
            {
                ((CreationalContextImpl<Instance<T>>)creationalContext).removeInjectionPoint();
            }
        }
    }
}
