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
package org.apache.webbeans.inject;

import java.io.Serializable;
import java.lang.reflect.Member;
import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.context.spi.Contextual;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.IllegalProductException;
import jakarta.enterprise.inject.TransientReference;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.enterprise.inject.spi.Producer;

import org.apache.webbeans.component.AbstractProducerBean;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.container.BeanManagerImpl;
import org.apache.webbeans.context.creational.CreationalContextImpl;
import org.apache.webbeans.util.ClassUtil;
import org.apache.webbeans.util.WebBeansUtil;

/**
 * Abstract implementation of all components which are injectable.
 * 
 * <p>
 * Do actual injection via {@link AbstractInjectable#inject(InjectionPoint)}
 * </p>
 * 
 * @see InjectableField
 * @see InjectableConstructor
 * @see InjectableMethod
 */
public abstract class AbstractInjectable<T>
{

    protected Producer<?> owner;
    
    protected final CreationalContextImpl<?> creationalContext;
    
    protected CreationalContextImpl<?> transientCreationalContext;

    protected AbstractInjectable(Producer<?> owner, CreationalContextImpl<?> creationalContext)
    {
        this.owner = owner;
        this.creationalContext = creationalContext;
        this.transientCreationalContext = creationalContext.getWebBeansContext().getBeanManagerImpl().createCreationalContext(creationalContext.getContextual());
    }

    /**
     * Gets the injected bean instance in its scoped context. 
     * @param injectionPoint injection point definition  
     * @return current bean instance in the resolved bean scope
     */
    public T inject(InjectionPoint injectionPoint)
    {
        T injected;
        BeanManagerImpl beanManager = creationalContext.getWebBeansContext().getBeanManagerImpl();

        //Injected contextual bean
        Bean<?> injectedBean = beanManager.getInjectionResolver().getInjectionPointBean(injectionPoint);
        
        //Injection for dependent instance InjectionPoint fields
        boolean dependentProducer = false;
        if(WebBeansUtil.isDependent(injectedBean) && !injectionPoint.isTransient() && injectedBean instanceof AbstractProducerBean
                    && creationalContext.getBean() != null && beanManager.isPassivatingScope(creationalContext.getBean().getScope()))
        {
            dependentProducer = true;
        }
        
        CreationalContext<?> injectionPointContext;
        if (injectionPoint.getAnnotated().isAnnotationPresent(TransientReference.class))
        {
            injectionPointContext = transientCreationalContext;
        }
        else
        {
            injectionPointContext = creationalContext;
        }

        //Gets injectable reference for injected bean
        injected = (T) beanManager.getInjectableReference(injectionPoint, injectionPointContext);

        if (injected == null)
        {
            if (beanManager.isNormalScope(injectedBean.getScope()))
            {
                throw new IllegalStateException("InjectableReference is 'null' for " + injectionPoint.toString());
            }
            Class<?> type = ClassUtil.getClass(injectionPoint.getType());
            if (type.isPrimitive())
            {
                injected = (T) ClassUtil.getDefaultValue(type);
            }
        }

        /*X TODO see spec issue CDI-140 */
        if(dependentProducer)
        {
            if(injected != null && !Serializable.class.isAssignableFrom(injected.getClass()))
            {
                throw new IllegalProductException("A producer method or field of scope @Dependent returns an unserializable object for injection " +
                        "into an injection point "+ injectionPoint +" that requires a passivation capable dependency");
            }
        }

        return injected;
    }
    
    protected Contextual<?> getBean()
    {
        return creationalContext.getBean();
    }

    protected WebBeansContext getWebBeansContext()
    {
        return creationalContext.getWebBeansContext();
    }
        
    /**
     * Returns injection points related with given member type of the bean.
     * @param member java member
     * @return injection points related with given member type
     */
    protected List<InjectionPoint> getInjectionPoints(Member member)
    {
        return createInjectionPoints(owner, member);
    }

    protected static List<InjectionPoint> createInjectionPoints(Producer<?> owner, Member member)
    {
        List<InjectionPoint> injectionPoints = new ArrayList<>();
        for (InjectionPoint injectionPoint : owner.getInjectionPoints())
        {
            if (injectionPoint.getMember().equals(member))
            {
                injectionPoints.add(injectionPoint);
            }
        }
        return injectionPoints;
    }
}
