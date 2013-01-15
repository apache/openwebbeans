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
package org.apache.webbeans.ejb.common.component;

import java.util.List;
import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.enterprise.inject.spi.ObserverMethod;

import org.apache.webbeans.component.InjectionTargetBean;
import org.apache.webbeans.component.creation.AbstractInjectionTargetBeanBuilder;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.ejb.common.util.EjbValidator;
import org.apache.webbeans.portable.AbstractEjbInjectionTarget;

/**
 * EjbBeanCreatorImpl.
 * 
 * @version $Rev$ $Date$
 *
 * @param <T> ejb class type
 */
public abstract class EjbBeanBuilder<T, E extends BaseEjbBean<T>> extends AbstractInjectionTargetBeanBuilder<T, E>
{
    public EjbBeanBuilder(WebBeansContext webBeansContext, AnnotatedType<T> annotatedType)
    {
        super(webBeansContext, annotatedType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void checkCreateConditions()
    {        
        EjbValidator.validateDecoratorOrInterceptor(getBeanType());
    }

    public void defineScopeType(String errorMessage)
    {
        super.defineScopeType(errorMessage);

        EjbValidator.validateEjbScopeType(getBean());
        EjbValidator.validateGenericBeanType(getBeanType(), getScope());
    }
    
    /* (non-Javadoc)
     * @see org.apache.webbeans.component.creation.AbstractInjectedTargetBeanCreator#defineObserverMethods()
     */
    @Override
    public Set<ObserverMethod<?>> defineObserverMethods(InjectionTargetBean<T> bean)
    {
        Set<ObserverMethod<?>> observerMethods = super.defineObserverMethods(bean);
        EjbValidator.validateObserverMethods((BaseEjbBean<?>) bean, observerMethods);
        
        return observerMethods;
    }

    @Override
    protected InjectionTarget<T> buildInjectionTarget(AnnotatedType<T> annotatedType,
                                                      Set<InjectionPoint> points,
                                                      WebBeansContext webBeansContext,
                                                      List<AnnotatedMethod<?>> postConstructMethods,
                                                      List<AnnotatedMethod<?>> preDestroyMethods)
    {
        return new AbstractEjbInjectionTarget<T>(annotatedType, points, webBeansContext)
        {
            public T produce(CreationalContext<T> creationalContext)
            {
                return getInstance(creationalContext);
            }
        };
    }
    
    protected abstract T getInstance(CreationalContext<T> creationalContext);
}
