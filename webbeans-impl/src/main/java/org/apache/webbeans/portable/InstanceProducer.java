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
import java.util.Map;
import java.util.Set;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.Interceptor;

import org.apache.webbeans.annotation.DefaultLiteral;
import org.apache.webbeans.component.InstanceBean;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.context.creational.CreationalContextImpl;
import org.apache.webbeans.inject.instance.InstanceImpl;

public class InstanceProducer<T> extends AbstractProducer<Instance<T>>
{
    private WebBeansContext webBeansContext;


    public InstanceProducer(WebBeansContext webBeansContext)
    {
        this.webBeansContext = webBeansContext;
    }

    @Override
    protected Instance<T> produce(Map<Interceptor<?>, ?> interceptors, CreationalContextImpl<Instance<T>> creationalContext)
    {
        CreationalContextImpl<Instance<T>> creationalContextImpl = null;
        try
        {
            InjectionPoint injectionPoint = null;
            if (creationalContext instanceof CreationalContextImpl)
            {
                creationalContextImpl = creationalContext;
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
                // an 'empty' directly resolved Instance always is for Default qualified Objects
                qualifiers = DefaultLiteral.SET;
                type = Object.class;
            }

            return new InstanceImpl<>(type, injectionPoint, webBeansContext, creationalContextImpl, qualifiers.toArray(new Annotation[qualifiers.size()]));
        }
        finally
        {
            if (creationalContextImpl != null)
            {
                creationalContextImpl.removeInjectionPoint();
            }
        }
    }

    @Override
    public void dispose(Instance<T> instance)
    {
        super.dispose(instance);

        ((InstanceImpl<T>) instance).release();
    }
}
