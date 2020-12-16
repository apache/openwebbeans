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

import static org.apache.webbeans.util.InjectionExceptionUtil.createUnproxyableResolutionException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Set;

import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.TransientReference;
import javax.enterprise.inject.UnproxyableResolutionException;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.Decorator;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.Interceptor;
import javax.enterprise.inject.spi.PassivationCapable;

import org.apache.webbeans.component.EnterpriseBeanMarker;
import org.apache.webbeans.component.OwbBean;
import org.apache.webbeans.component.ProducerMethodBean;
import org.apache.webbeans.exception.WebBeansDeploymentException;
import org.apache.webbeans.exception.helper.ViolationMessageBuilder;
import org.apache.webbeans.intercept.InterceptorResolutionService.BeanInterceptorInfo;
import org.apache.webbeans.logger.WebBeansLoggerFacade;
import org.apache.webbeans.portable.InjectionTargetImpl;
import org.apache.webbeans.util.SecurityUtil;

public class DeploymentValidationService
{
    private WebBeansContext webBeansContext;

    /**
     * Classes which are allowed to be proxies despite having a non-private, non-static final method
     */
    private Set<String> allowProxyingClasses;


    public DeploymentValidationService(WebBeansContext webBeansContext)
    {
        this.webBeansContext = webBeansContext;
    }

    /**
     * Checks the unproxyable condition.
     * @throws org.apache.webbeans.exception.WebBeansConfigurationException if bean is not proxied by the container
     * @return exception TCKs validate at runtime
     */
    public UnproxyableResolutionException validateProxyable(OwbBean<?> bean, boolean ignoreFinalMethods)
    {
        if (allowProxyingClasses == null)
        {
            allowProxyingClasses = webBeansContext.getOpenWebBeansConfiguration().getConfigListValues(OpenWebBeansConfiguration.ALLOW_PROXYING_PARAM);
        }

        // Unproxyable test for NormalScoped beans
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

                    if (!ignoreFinalMethods)
                    {
                        String finalMethodName = hasNonPrivateFinalMethod(beanClass);
                        if (finalMethodName != null)
                        {
                            if (allowProxyingClasses.contains(beanClass.getName()))
                            {
                                WebBeansLoggerFacade.getLogger(DeploymentValidationService.class)
                                        .info(beanClass.getName() + " has final method " +
                                                finalMethodName + ". CDI doesn't allow to proxy that." +
                                                " Continuing because the class is explicitly configured " +
                                                "to be treated as proxyable." +
                                                " Final methods shall not get invoked on this proxy!");
                            }
                            else
                            {
                                violationMessage.addLine(beanClass.getName(), " has final method " + finalMethodName + " CDI doesn't allow to proxy that.");
                            }
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
                        boolean containsViolation = violationMessage.containsViolation();
                        violationMessage.addLine(beanClass.getName(), " has a >private< no-arg constructor! CDI doesn't allow to proxy that.");
                        if (!containsViolation)
                        { // lazy
                            return createUnproxyableResolutionException(violationMessage);
                        }
                    }
                }

                if(violationMessage.containsViolation())
                {
                    return createUnproxyableResolutionException(violationMessage);
                }
            }
        }
        return null;
    }

    /**
     * check if the given class has any non-private, non-static final method
     * @return the method name or <code>null</code> if there is no such method.
     */
    private String hasNonPrivateFinalMethod(Class<?> beanClass)
    {
        if (beanClass == Object.class)
        {
            return null;
        }

        // we also need to check the methods of the parent classes
        String finalMethodName = hasNonPrivateFinalMethod(beanClass.getSuperclass());
        if (finalMethodName != null)
        {
            return finalMethodName;
        }

        Method[] methods = SecurityUtil.doPrivilegedGetDeclaredMethods(beanClass);
        for (Method m : methods)
        {
            int modifiers = m.getModifiers();
            if (Modifier.isFinal(modifiers) && !Modifier.isPrivate(modifiers) && !Modifier.isStatic(modifiers) &&
                !m.isSynthetic() && !m.isBridge())
            {
                return m.getName();
            }
        }

        return null;
    }

    /**
     * If bean is passivation capable, it validate all of its dependencies.
     * @throws org.apache.webbeans.exception.WebBeansConfigurationException if not satisfy passivation dependencies
     */
    public <T> void validatePassivationCapable(OwbBean<T> bean)
    {
        if (isPassivationCapable(bean))
        {
            if (EnterpriseBeanMarker.class.isInstance(bean))
            {
                validatePassivationCapableDependencies(bean, bean.getInjectionPoints());
                if (BeanInterceptorInfoProvider.class.isInstance(bean))
                {
                    validatePassivationCapableInterceptorInfo(bean, BeanInterceptorInfoProvider.class.cast(bean).interceptorInfo());
                }
                return;
            }

            if (!(bean instanceof ProducerMethodBean))
            {
                validatePassivationCapableDependencies(bean, bean.getInjectionPoints());
            }
            if (bean.getProducer() instanceof InjectionTargetImpl)
            {
                InjectionTargetImpl<T> injectionTarget = (InjectionTargetImpl<T>)bean.getProducer();
                BeanInterceptorInfo interceptorInfo = injectionTarget.getInterceptorInfo();
                validatePassivationCapableInterceptorInfo(bean, interceptorInfo);
            }
        }
    }

    private <T> void validatePassivationCapableInterceptorInfo(OwbBean<T> bean, BeanInterceptorInfo interceptorInfo)
    {
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

    private <T> void validatePassivationCapableDependency(Bean<T> bean, Bean<?> dependentBean)
    {
        if (!isPassivationCapable(dependentBean))
        {
            String type = dependentBean instanceof Interceptor? "Interceptor ": "Decorator "; 
            throw new WebBeansDeploymentException(
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
                    Annotated annotated = injectionPoint.getAnnotated();
                    if(annotated.isAnnotationPresent(Disposes.class) || annotated.isAnnotationPresent(TransientReference.class))
                    {
                        continue;
                    }
                    throw new WebBeansDeploymentException(
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

    public interface BeanInterceptorInfoProvider
    {
        BeanInterceptorInfo interceptorInfo();
    }
}
