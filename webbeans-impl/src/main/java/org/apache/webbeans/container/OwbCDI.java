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
package org.apache.webbeans.container;

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.config.WebBeansFinder;
import org.apache.webbeans.context.creational.CreationalContextImpl;
import org.apache.webbeans.corespi.DefaultSingletonService;
import org.apache.webbeans.inject.instance.InstanceImpl;
import org.apache.webbeans.spi.SingletonService;

import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.enterprise.util.TypeLiteral;
import java.lang.annotation.Annotation;
import java.util.Iterator;

public class OwbCDI extends CDI<Object>
{
    private WebBeansContext getWebBeansContext()
    {
        // DON'T: return WebBeansContext.currentInstance();, it can trigger an implicit start
        final SingletonService<WebBeansContext> singletonService = WebBeansFinder.getSingletonService();
        if (DefaultSingletonService.class.isInstance(singletonService)
                && !DefaultSingletonService.class.cast(singletonService).exists(WebBeansFinder.getCurrentKey()))
        {
            throw new IllegalStateException("No CDI container started");
        }
        // else let's default
        return WebBeansContext.currentInstance();
    }

    protected Instance<Object> instance()
    {
        WebBeansContext webBeansContext = getWebBeansContext();
        BeanManagerImpl bm = webBeansContext.getBeanManagerImpl();
        CreationalContextImpl<Instance<Object>> creationalContext = bm.createCreationalContext(null);
        return webBeansContext.getWebBeansUtil().getInstanceBean().create(creationalContext);
    }

    @Override
    public BeanManager getBeanManager()
    {
        return new InjectableBeanManager(getWebBeansContext().getBeanManagerImpl());
    }

    @Override
    public Instance<Object> select(Annotation... qualifiers)
    {
        return instance().select(qualifiers);
    }

    @Override
    public boolean isUnsatisfied()
    {
        return instance().isUnsatisfied();
    }

    @Override
    public boolean isAmbiguous()
    {
        return instance().isAmbiguous();
    }

    @Override
    public void destroy(Object instance)
    {
        InstanceImpl.class.cast(instance).destroy(instance);
    }

    @Override
    public Handle<Object> getHandle()
    {
        //X TODO implement!
        return null;
    }

    @Override
    public Iterable<? extends Handle<Object>> handles()
    {
        //X TODO implement!
        return null;
    }

    @Override
    public <U> Instance<U> select(TypeLiteral<U> subtype, Annotation... qualifiers)
    {
        return instance().select(subtype, qualifiers);
    }

    @Override
    public <U> Instance<U> select(Class<U> subtype, Annotation... qualifiers)
    {
        return instance().select(subtype, qualifiers);
    }

    @Override
    public Iterator<Object> iterator()
    {
        return instance().iterator();
    }

    @Override
    public Object get()
    {
        return instance().get();
    }
}
