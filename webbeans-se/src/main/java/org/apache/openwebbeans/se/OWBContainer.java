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
package org.apache.openwebbeans.se;

import org.apache.webbeans.annotation.DefaultLiteral;
import org.apache.webbeans.component.InstanceBean;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.container.BeanManagerImpl;
import org.apache.webbeans.container.InjectableBeanManager;
import org.apache.webbeans.context.creational.CreationalContextImpl;
import org.apache.webbeans.inject.instance.InstanceImpl;
import org.apache.webbeans.spi.ContainerLifecycle;

import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.se.SeContainer;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.util.TypeLiteral;
import java.lang.annotation.Annotation;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;

public class OWBContainer implements SeContainer
{
    protected final WebBeansContext context;
    protected final Object startEvent;
    private AtomicBoolean running = new AtomicBoolean(true);

    // let's it be public in case we extend it
    public OWBContainer(WebBeansContext context, Object startObj)
    {
        this.context = context;
        this.startEvent = startObj;
    }

    protected void doClose()
    {
        context.getService(ContainerLifecycle.class).stopApplication(startEvent);
    }

    @Override
    public void close()
    {
        if (running.compareAndSet(true, false))
        {
            doClose();
        }
    }

    @Override
    public boolean isRunning()
    {
        return running.get();
    }

    @Override
    public BeanManager getBeanManager()
    {
        return new InjectableBeanManager(context.getBeanManagerImpl());
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
    public void destroy(Object instance) // not sure it is the right impl
    {
        InstanceImpl.class.cast(instance).destroy(instance);
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

    private Instance<Object> instance()
    {
        BeanManagerImpl bm = context.getBeanManagerImpl();
        CreationalContextImpl<Instance<Object>> creationalContext = bm.createCreationalContext(null);
        return new InstanceBean<>(context).create(creationalContext).select(DefaultLiteral.INSTANCE);
    }
}
