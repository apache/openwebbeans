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

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.context.creational.CreationalContextImpl;
import org.apache.webbeans.inject.InjectableMethod;
import org.apache.webbeans.spi.plugins.OpenWebBeansEjbPlugin;
import org.apache.webbeans.util.Asserts;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Set;

public abstract class BaseProducerProducer<T, P> extends AbstractProducer<T>
{

    protected Bean<P> owner;
    protected WebBeansContext webBeansContext;
    protected Method disposalMethod;
    protected Set<InjectionPoint> disposalIPs;
    protected boolean isAnyDisposal;
    protected AnnotatedMethod<? super P> disposerMethod;

    public BaseProducerProducer(Bean<P> owner,
                                AnnotatedMethod<? super P> disposerMethod,
                                Set<InjectionPoint> points,
                                Set<InjectionPoint> disposalIPs,
                                WebBeansContext webBeansContext)
    {
        super(points);
        Asserts.assertNotNull(webBeansContext, Asserts.PARAM_NAME_WEBBEANSCONTEXT);
        this.owner = owner;
        this.webBeansContext = webBeansContext;
        this.disposalIPs = disposalIPs;

        final OpenWebBeansEjbPlugin ejbPlugin = webBeansContext.getPluginLoader().getEjbPlugin();
        if (ejbPlugin != null)
        {
            if (disposerMethod != null)
            {
                disposalMethod = ejbPlugin.resolveViewMethod(owner, disposerMethod.getJavaMember());
            }
        }
        else
        {
            if (disposerMethod != null)
            {
                disposalMethod = disposerMethod.getJavaMember();
            }
        }
        if (disposerMethod != null)
        {
            for (final AnnotatedParameter<?> param : disposerMethod.getParameters())
            {
                if (param.isAnnotationPresent(Disposes.class))
                {
                    isAnyDisposal = param.isAnnotationPresent(Any.class);
                    break;
                }
            }
        }
        this.disposerMethod = disposerMethod;
    }

    public AnnotatedMethod<? super P> getDisposerMethod()
    {
        return disposerMethod;
    }

    @Override
    public void defineInterceptorStack(final Bean<T> bean, final AnnotatedType<T> annotatedType, final WebBeansContext webBeansContext)
    {
        if (webBeansContext.getOpenWebBeansConfiguration().supportsInterceptionOnProducers())
        {
            super.defineInterceptorStack(bean, annotatedType, webBeansContext);
        }
    }

    @Override
    public void dispose(T instance)
    {
        if (disposalMethod != null)
        {
            P parentInstance = null;
            CreationalContext<P> parentCreationalContext = null;
            InjectableMethod<T> m;
            try
            {
                parentCreationalContext = webBeansContext.getBeanManagerImpl().createCreationalContext(owner);

                if (!Modifier.isStatic(disposalMethod.getModifiers()))
                {
                    parentInstance = (P)webBeansContext.getBeanManagerImpl().getReference(owner, owner.getBeanClass(), parentCreationalContext);
                }

                m = new InjectableMethod<T>(disposalMethod, parentInstance, this, (CreationalContextImpl<T>) parentCreationalContext, disposalIPs);
                m.setDisposable(true);
                m.setProducerMethodInstance(instance);

                m.doInjection();

            }
            finally
            {
                parentCreationalContext.release();
            }
        }
    }

    public Set<InjectionPoint> getDisposalIPs()
    {
        return disposalIPs;
    }

    public boolean isAnyDisposal()
    {
        return isAnyDisposal;
    }
}
