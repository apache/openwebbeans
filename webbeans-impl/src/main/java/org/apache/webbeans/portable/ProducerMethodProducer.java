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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.Interceptor;

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.context.creational.CreationalContextImpl;
import org.apache.webbeans.inject.InjectableMethod;
import org.apache.webbeans.spi.plugins.OpenWebBeansEjbPlugin;
import org.apache.webbeans.util.Asserts;

/**
 * A {@link javax.enterprise.inject.spi.Producer} for producer-method beans.
 */
public class ProducerMethodProducer<T, P> extends BaseProducerProducer<T, P>
{
    private Method producerMethod;

    public ProducerMethodProducer(Bean<P> owner,
                                  AnnotatedMethod<? super P> producerMethod,
                                  AnnotatedMethod<? super P> disposerMethod,
                                  Set<InjectionPoint> points,
                                  Set<InjectionPoint> disposalIPs,
                                  WebBeansContext webBeansContext)
    {
        super(owner, disposerMethod, points, disposalIPs, webBeansContext);
        Asserts.assertNotNull(producerMethod, "producerMethod");
        if (!producerMethod.isStatic())
        {
            if (owner == null)
            {
                throw new IllegalArgumentException("owner may not be null for non-static producer method " + producerMethod);
            }
        }
        final OpenWebBeansEjbPlugin ejbPlugin = webBeansContext.getPluginLoader().getEjbPlugin();
        if (ejbPlugin != null)
        {
            this.producerMethod = ejbPlugin.resolveViewMethod(owner, producerMethod.getJavaMember());
        }
        else
        {
            this.producerMethod = producerMethod.getJavaMember();
        }
    }

    @Override
    protected T produce(Map<Interceptor<?>, ?> interceptors, CreationalContextImpl<T> creationalContext)
    {
        P parentInstance = null;
        CreationalContext<P> parentCreationalContext = null;
        InjectableMethod<T> m;
        try
        {
            parentCreationalContext = webBeansContext.getBeanManagerImpl().createCreationalContext(owner);

            if (!Modifier.isStatic(producerMethod.getModifiers()))
            {
                parentInstance = (P)webBeansContext.getBeanManagerImpl().getReference(owner, owner.getBeanClass(), parentCreationalContext);
            }
            
            m = new InjectableMethod<T>(producerMethod, parentInstance, this, (CreationalContextImpl<T>) creationalContext);
            
            return m.doInjection();

        }
        finally
        {
            if (parentCreationalContext != null)
            {
                parentCreationalContext.release();
            }
        }
    }
}
