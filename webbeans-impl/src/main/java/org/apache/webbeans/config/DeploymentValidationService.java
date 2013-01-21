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
package org.apache.webbeans.config;

import java.util.Set;

import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.Decorator;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.Interceptor;
import javax.enterprise.inject.spi.PassivationCapable;

import org.apache.webbeans.component.OwbBean;
import org.apache.webbeans.component.ProducerMethodBean;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.intercept.InterceptorResolutionService.BeanInterceptorInfo;
import org.apache.webbeans.portable.InjectionTargetImpl;

public class DeploymentValidationService
{

    private WebBeansContext webBeansContext;

    public DeploymentValidationService(WebBeansContext webBeansContext)
    {
        this.webBeansContext = webBeansContext;
    }

    /**
     * If bean is passivation capable, it validate all of its dependencies.
     * @throws org.apache.webbeans.exception.WebBeansConfigurationException if not satisfy passivation dependencies
     */
    public <T> void validatePassivationCapable(OwbBean<T> bean)
    {
        if (isPassivationCapable(bean))
        {
            if (!(bean instanceof ProducerMethodBean))
            {
                validatePassivationCapableDependencies(bean, bean.getInjectionPoints());
            }
            if (bean.getProducer() instanceof InjectionTargetImpl)
            {
                InjectionTargetImpl<T> injectionTarget = (InjectionTargetImpl<T>)bean.getProducer();
                BeanInterceptorInfo interceptorInfo = injectionTarget.getInterceptorInfo();
                if (interceptorInfo != null)
                {
                    for (Interceptor<?> ejbInterceptor: interceptorInfo.getEjbInterceptors())
                    {
                        validatePassivationCapableDependency(bean, ejbInterceptor);
                    }
                    for (Interceptor<?> cdiInterceptor: interceptorInfo.getCdiInterceptors())
                    {
                        validatePassivationCapableDependency(bean, cdiInterceptor);
                    }
                    for (Decorator<?> decorators: interceptorInfo.getDecorators())
                    {
                        validatePassivationCapableDependency(bean, decorators);
                    }
                }
            }
        }
    }
    
    private <T> void validatePassivationCapableDependency(Bean<T> bean, Bean<?> dependentBean)
    {
        if (!isPassivationCapable(dependentBean))
        {
            String type = dependentBean instanceof Interceptor? "Interceptor ": "Decorator "; 
            throw new WebBeansConfigurationException(
                    "Passivation capable beans must satisfy passivation capable dependencies. " +
                    "Bean : " + bean.toString() + " does not satisfy. " + type + dependentBean.toString() + " is not passivation capable");
        }
        validatePassivationCapableDependencies(bean, dependentBean.getInjectionPoints());
    }

    private <T> void validatePassivationCapableDependencies(Bean<T> bean, Set<InjectionPoint> injectionPoints)
    {
        for (InjectionPoint injectionPoint: injectionPoints)
        {
            if(!injectionPoint.isTransient())
            {
                if(!webBeansContext.getWebBeansUtil().isPassivationCapableDependency(injectionPoint))
                {
                    if(injectionPoint.getAnnotated().isAnnotationPresent(Disposes.class))
                    {
                        continue;
                    }
                    throw new WebBeansConfigurationException(
                            "Passivation capable beans must satisfy passivation capable dependencies. " +
                            "Bean : " + bean.toString() + " does not satisfy. Details about the Injection-point: " +
                                    injectionPoint.toString());
                }
            }
        }
    }
    
    private boolean isPassivationCapable(Bean<?> bean)
    {
        return bean instanceof OwbBean? ((OwbBean<?>)bean).isPassivationCapable(): bean instanceof PassivationCapable;
    }
}
