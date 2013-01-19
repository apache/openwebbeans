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
package org.apache.webbeans.intercept;

import org.apache.webbeans.component.InjectionTargetBean;
import org.apache.webbeans.component.InterceptedMarker;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.logger.WebBeansLoggerFacade;
import org.apache.webbeans.portable.InjectionTargetImpl;
import org.apache.webbeans.proxy.InterceptorDecoratorProxyFactory;
import javax.enterprise.inject.spi.InterceptionType;
import javax.enterprise.inject.spi.Interceptor;
import javax.interceptor.AroundInvoke;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static org.apache.webbeans.intercept.InterceptorResolutionService.BeanInterceptorInfo;
import static org.apache.webbeans.intercept.InterceptorResolutionService.BusinessMethodInterceptorInfo;


/**
 * Configures the Web Beans related interceptors.
 *
 * TODO most of the stuff in this class can most probably get removed. All important logic is contained in {@link InterceptorResolutionService}
 */
public final class WebBeansInterceptorConfig
{
    /** Logger instance */
    private static Logger logger = WebBeansLoggerFacade.getLogger(WebBeansInterceptorConfig.class);

    private WebBeansContext webBeansContext;

    public WebBeansInterceptorConfig(WebBeansContext webBeansContext)
    {
        this.webBeansContext = webBeansContext;
    }

    /**
     * This method gets invoked in the ValidateBean phase and will fill all the
     * interceptor information into the given InjectionTargetBean
     *
     * TODO: move this method to some other, better place!
     * Configure bean instance interceptor stack.
     * @param bean bean instance
     */
    public void defineBeanInterceptorStack(InjectionTargetBean<?> bean)
    {
        if (bean instanceof InterceptedMarker && bean.getInjectionTarget() instanceof InjectionTargetImpl)
        {
            InjectionTargetImpl<?> injectionTarget = (InjectionTargetImpl<?>) bean.getInjectionTarget();
            BeanInterceptorInfo interceptorInfo = webBeansContext.getInterceptorResolutionService().
                    calculateInterceptorInfo(bean.getTypes(), bean.getQualifiers(), bean.getAnnotatedType());

            //X TODO decorator stack

            Map<Method, List<Interceptor<?>>> methodInterceptors = new HashMap<Method, List<Interceptor<?>>>();
            List<Method> nonBusinessMethods = new ArrayList<Method>();
            for (Map.Entry<Method, BusinessMethodInterceptorInfo> miEntry : interceptorInfo.getBusinessMethodsInfo().entrySet())
            {
                Method interceptedMethod = miEntry.getKey();
                BusinessMethodInterceptorInfo mii = miEntry.getValue();
                List<Interceptor<?>> activeInterceptors = new ArrayList<Interceptor<?>>();

                if (mii.getEjbInterceptors() != null)
                {
                    for (Interceptor<?> i : mii.getEjbInterceptors())
                    {
                        activeInterceptors.add(i);
                    }
                }
                if (mii.getCdiInterceptors() != null)
                {
                    for (Interceptor<?> i : mii.getCdiInterceptors())
                    {
                        activeInterceptors.add(i);
                    }
                }
                if (interceptorInfo.getSelfInterceptorBean() != null)
                {
                    if (interceptedMethod.getAnnotation(AroundInvoke.class) == null) // this check is a dirty hack for now to prevent infinite loops
                    {
                        // add self-interception as last interceptor in the chain.
                        activeInterceptors.add(interceptorInfo.getSelfInterceptorBean());
                    }
                }

                if (activeInterceptors.size() > 0)
                {
                    methodInterceptors.put(interceptedMethod, activeInterceptors);
                }
                else if (mii.getMethodDecorators() != null)
                {
                    methodInterceptors.put(interceptedMethod, Collections.EMPTY_LIST);
                }

                // empty InterceptionType -> AROUND_INVOKE
                if (!mii.getInterceptionTypes().isEmpty())
                {
                    nonBusinessMethods.add(interceptedMethod);
                }
            }

            List<Interceptor<?>> postConstructInterceptors
                    = getLifecycleInterceptors(interceptorInfo.getEjbInterceptors(), interceptorInfo.getCdiInterceptors(), InterceptionType.POST_CONSTRUCT);

            List<Interceptor<?>> preDestroyInterceptors
                    = getLifecycleInterceptors(interceptorInfo.getEjbInterceptors(), interceptorInfo.getCdiInterceptors(), InterceptionType.PRE_DESTROY);

            if (methodInterceptors.size() > 0 || postConstructInterceptors.size() > 0 || preDestroyInterceptors.size() > 0)
            {
                // we only need to create a proxy class for intercepted or decorated Beans
                InterceptorDecoratorProxyFactory pf = webBeansContext.getInterceptorDecoratorProxyFactory();

                ClassLoader classLoader = this.getClass().getClassLoader();

                Method[] businessMethods = methodInterceptors.keySet().toArray(new Method[methodInterceptors.size()]);
                Method[] nonInterceptedMethods = interceptorInfo.getNonInterceptedMethods().toArray(new Method[interceptorInfo.getNonInterceptedMethods().size()]);

                Class proxyClass = pf.createProxyClass(classLoader, bean.getReturnType(), businessMethods, nonInterceptedMethods);

                // now we collect the post-construct and pre-destroy interceptors

                injectionTarget.setInterceptorInfo(interceptorInfo, proxyClass, methodInterceptors, postConstructInterceptors, preDestroyInterceptors);
            }
            else // we have no interceptors so we have to define lifecycle methods
            {
                
            }
        }

    }


    private List<Interceptor<?>> getLifecycleInterceptors(LinkedHashSet<Interceptor<?>> ejbInterceptors, List<Interceptor<?>> cdiInterceptors, InterceptionType interceptionType)
    {
        List<Interceptor<?>> lifecycleInterceptors = new ArrayList<Interceptor<?>>();

        for (Interceptor<?> ejbInterceptor : ejbInterceptors)
        {
            if (ejbInterceptor.intercepts(interceptionType))
            {
                lifecycleInterceptors.add(ejbInterceptor);
            }
        }
        for (Interceptor<?> cdiInterceptor : cdiInterceptors)
        {
            if (cdiInterceptor.intercepts(interceptionType))
            {
                lifecycleInterceptors.add(cdiInterceptor);
            }
        }

        return lifecycleInterceptors;
    }



}
