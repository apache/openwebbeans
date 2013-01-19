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

import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;

import org.apache.webbeans.component.OwbBean;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.context.creational.CreationalContextImpl;
import org.apache.webbeans.inject.InjectableMethod;
import org.apache.webbeans.util.Asserts;

/**
 * A {@link javax.enterprise.inject.spi.Producer} for producer-method beans.
 */
public class ProducerMethodProducer<T, P> extends AbstractProducer<T>
{

    private Bean<P> owner;
    private WebBeansContext webBeansContext;
    private AnnotatedMethod<P> producerMethod;
    private AnnotatedMethod<P> disposalMethod;

    public ProducerMethodProducer(OwbBean<P> owner, AnnotatedMethod<P> producerMethod, AnnotatedMethod<P> disposerMethod, Set<InjectionPoint> points)
    {
        super(points);
        Asserts.assertNotNull(producerMethod);
        this.owner = owner;
        this.webBeansContext = owner.getWebBeansContext();
        this.producerMethod = producerMethod;
        this.disposalMethod = disposerMethod;
    }
    
    public void setDisposalMethod(AnnotatedMethod<P> disposalMethod)
    {
        this.disposalMethod = disposalMethod;
    }

    @Override
    public T produce(CreationalContext<T> creationalContext)
    {
        CreationalContextImpl<T> context = (CreationalContextImpl<T>)creationalContext;
        P ownerInstance = (P)webBeansContext.getBeanManagerImpl().getReference(owner, owner.getBeanClass(), creationalContext);
        return new InjectableMethod<T>(producerMethod.getJavaMember(), ownerInstance, this, context).doInjection();
    }

    @Override
    public void dispose(T instance)
    {
        if (disposalMethod != null)
        {
            P parentInstance = null;
            CreationalContext<P> parentCreational = null;
            InjectableMethod<T> m = null;
            try
            {
                parentCreational = webBeansContext.getBeanManagerImpl().createCreationalContext(owner);
                
                if (!Modifier.isStatic(disposalMethod.getJavaMember().getModifiers()))
                {
                    parentInstance = (P)webBeansContext.getBeanManagerImpl().getReference(owner, owner.getBeanClass(), parentCreational);
                }

                m = new InjectableMethod<T>(disposalMethod.getJavaMember(), parentInstance, this, (CreationalContextImpl<T>) parentCreational);
                m.setDisposable(true);
                m.setProducerMethodInstance(instance);

                m.doInjection();

            }
            finally
            {
                if (owner.getScope().equals(Dependent.class))
                {
                    owner.destroy(parentInstance, parentCreational);
                }

                //Destroy dependent parameters
                Map<Bean<?>, Object> dependents = m.getDependentBeanParameters();
                if(dependents != null)
                {
                    Set<Bean<?>> beans = dependents.keySet();
                    for(Bean<?> bean : beans)
                    {
                        Bean<Object> beanTt = (Bean<Object>)bean;
                        beanTt.destroy(dependents.get(beanTt), (CreationalContext<Object>) parentCreational);
                    }
                }
            }
        }
    }
}
