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

import org.apache.webbeans.component.AbstractOwbBean;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.context.creational.CreationalContextImpl;
import org.apache.webbeans.exception.WebBeansException;
import org.apache.webbeans.intercept.DefaultInterceptorHandler;
import org.apache.webbeans.proxy.InterceptorHandler;
import org.apache.webbeans.proxy.OwbInterceptorProxy;
import org.apache.webbeans.util.Asserts;
import org.apache.webbeans.util.WebBeansUtil;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.AnnotatedField;
import jakarta.enterprise.inject.spi.AnnotatedMethod;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.enterprise.inject.spi.Interceptor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class ProducerFieldProducer<T, P> extends BaseProducerProducer<T, P>
{
    private AnnotatedField<? super P> producerField;

    public ProducerFieldProducer(Bean<P> owner,
                                 AnnotatedField<? super P> producerField,
                                 AnnotatedMethod<? super P> disposerMethod,
                                 Set<InjectionPoint> disposalIPs,
                                 WebBeansContext webBeansContext)
    {
        super(owner, disposerMethod, Collections.<InjectionPoint>emptySet(), disposalIPs, webBeansContext);
        if (owner == null && !producerField.isStatic())
        {
            throw new IllegalArgumentException("owner may not be null");
        }
        Asserts.assertNotNull(producerField, "producerField");
        this.producerField = producerField;
    }

    @Override
    protected T produce(Map<Interceptor<?>, ?> interceptors, CreationalContextImpl<T> creationalContext)
    {
        T instance = null;
        P parentInstance = null;
        CreationalContext<P> parentCreational = null;
        try
        {
            parentCreational = webBeansContext.getBeanManagerImpl().createCreationalContext(owner);
            
            Field field = producerField.getJavaMember();
            if (!field.isAccessible())
            {
                webBeansContext.getSecurityService().doPrivilegedSetAccessible(field, true);
            }

            if (Modifier.isStatic(field.getModifiers()))
            {
                instance = (T) field.get(null);
            }
            else
            { 
                parentInstance = getParentInstanceFromContext(parentCreational);

                if (OwbInterceptorProxy.class.isInstance(parentInstance))
                {
                    InterceptorHandler handler = webBeansContext.getInterceptorDecoratorProxyFactory()
                        .getInterceptorHandler(OwbInterceptorProxy.class.cast(parentInstance));
                    if (DefaultInterceptorHandler.class.isInstance(handler))
                    {
                        parentInstance = (P) DefaultInterceptorHandler.class.cast(handler).getTarget();
                    }
                }
                instance = (T) field.get(parentInstance);
            }
        }
        catch(Exception e)
        {
            throw new WebBeansException(e);
        }
        finally
        {
            if (owner != null && owner.getScope().equals(Dependent.class))
            {
                owner.destroy(parentInstance, parentCreational);
            }
        }

        return instance;

    }
    
    @SuppressWarnings("unchecked")
    protected P getParentInstanceFromContext(CreationalContext<?> creationalContext)
    {
        P  parentInstance;

        Bean<?> specialize = WebBeansUtil.getMostSpecializedBean(webBeansContext.getBeanManagerImpl(), (AbstractOwbBean<T>) owner);

        if (specialize != null)
        {
            parentInstance = (P) webBeansContext.getBeanManagerImpl().getContext(specialize.getScope()).
                    get((Bean<Object>)specialize,(CreationalContext<Object>) creationalContext);
        }
        else
        {
            parentInstance = (P) webBeansContext.getBeanManagerImpl().getContext(
                    owner.getScope()).get((Bean<Object>)owner, (CreationalContext<Object>) creationalContext);
        }

        return parentInstance;

    }
}
