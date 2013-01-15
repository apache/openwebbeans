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

import java.lang.reflect.Constructor;
import java.util.Collections;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.AnnotatedMethod;

import org.apache.webbeans.component.ManagedBean;
import org.apache.webbeans.context.creational.CreationalContextImpl;
import org.apache.webbeans.decorator.AbstractDecoratorMethodHandler;
import org.apache.webbeans.inject.InjectableConstructor;
import org.apache.webbeans.proxy.ProxyFactory;

public class AbstractDecoratorInjectionTarget<T> extends InjectionTargetImpl<T>
{

    private Constructor<T> constructor;
    private ProxyFactory proxyFactory;
    
    //TODO: handle postconstruct and predestroy methods...
    public AbstractDecoratorInjectionTarget(ManagedBean<T> bean)
    {
        super(bean.getAnnotatedType(), bean.getInjectionPoints(), bean.getWebBeansContext(),
              Collections.<AnnotatedMethod<?>>emptyList(), Collections.<AnnotatedMethod<?>>emptyList());
        proxyFactory = bean.getWebBeansContext().getProxyFactoryRemove();
        Class<T> clazz = (Class<T>) proxyFactory.createAbstractDecoratorProxyClass(bean);
        constructor = bean.getWebBeansContext().getWebBeansUtil().defineConstructor(clazz);
    }

    @Override
    public T produce(CreationalContext<T> creationalContext)
    {
        InjectableConstructor<T> ic = new InjectableConstructor<T>(constructor, this, (CreationalContextImpl<T>) creationalContext);
        T instance = ic.doInjection();
        proxyFactory.setHandler(instance, new AbstractDecoratorMethodHandler());
        return instance;
    }
}
