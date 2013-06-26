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

import static org.apache.webbeans.util.InjectionExceptionUtil.throwUnproxyableResolutionException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
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
import org.apache.webbeans.exception.helper.ViolationMessageBuilder;
import org.apache.webbeans.intercept.InterceptorResolutionService.BeanInterceptorInfo;
import org.apache.webbeans.portable.InjectionTargetImpl;
import org.apache.webbeans.util.SecurityUtil;

public class DeploymentValidationService
{

    private WebBeansContext webBeansContext;

    public DeploymentValidationService(WebBeansContext webBeansContext)
    {
        this.webBeansContext = webBeansContext;
    }

    /**
     * Checks the unproxyable condition.
     * @throws org.apache.webbeans.exception.WebBeansConfigurationException if bean is not proxied by the container
     */
    public void validateProxyable(OwbBean<?> bean)
    {
        //Unproxiable test for NormalScoped beans
        if (webBeansContext.getBeanManagerImpl().isNormalScope(bean.getScope()))
        {
            ViolationMessageBuilder violationMessage = ViolationMessageBuilder.newViolation();

            Class<?> beanClass = bean.getReturnType();
            
            if(!beanClass.isInterface() && beanClass != Object.class)
            {
                if(beanClass.isPrimitive())
                {
                    violationMessage.addLine("It isn't possible to proxy a primitive type (" + beanClass.getName(), ")");
                }

                if(beanClass.isArray())
                {
                    violationMessage.addLine("It isn't possible to proxy an array type (", beanClass.getName(), ")");
                }

                if(!violationMessage.containsViolation())
                {
                    if (Modifier.isFinal(beanClass.getModifiers()))
                    {
                        violationMessage.addLine(beanClass.getName(), " is a final class! CDI doesn't allow to proxy that.");
                    }

                    Method[] methods = SecurityUtil.doPrivilegedGetDeclaredMethods(beanClass);
                    for (Method m : methods)
                    {
                        int modifiers = m.getModifiers();
                        if (Modifier.isFinal(modifiers) && !Modifier.isPrivate(modifiers) &&
                            !m.isSynthetic() && !m.isBridge())
                        {
                            violationMessage.addLine(beanClass.getName(), " has final method "+ m + " CDI doesn't allow to proxy that.");
                        }
                    }

                    Constructor<?> cons = webBeansContext.getWebBeansUtil().getNoArgConstructor(beanClass);
                    if (cons == null)
                    {
                        violationMessage.addLine(beanClass.getName(), " has no explicit no-arg constructor!",
                                "A public or protected constructor without args is required!");
                    }
                    else if (Modifier.isPrivate(cons.getModifiers()))
                    {
                        violationMessage.addLine(beanClass.getName(), " has a >private< no-arg constructor! CDI doesn't allow to proxy that.");
                    }
                }

                //Throw Exception
                if(violationMessage.containsViolation())
                {
                    throwUnproxyableResolutionException(violationMessage);
                }
            }
        }
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
