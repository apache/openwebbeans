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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.inject.spi.Decorator;

import org.apache.webbeans.intercept.InterceptorResolutionService.BeanInterceptorInfo;
import org.apache.webbeans.intercept.InterceptorResolutionService.BusinessMethodInterceptorInfo;
import org.apache.webbeans.proxy.InterceptorHandler;
import org.apache.webbeans.util.ExceptionUtil;

public class DecoratorHandler implements InterceptorHandler {

    private BeanInterceptorInfo interceptorInfo;
    private List<Decorator<?>> decorators;
    private Map<Decorator<?>, ?> instances;
    private int index;
    private Object target;

    public DecoratorHandler(BeanInterceptorInfo interceptorInfo, List<Decorator<?>> decorators, Map<Decorator<?>, ?> instances, int index, Object target) {
        this.interceptorInfo = interceptorInfo;
        this.decorators = decorators;
        this.instances = instances;
        this.index = index;
        this.target = target;
    }

    @Override
    public Object invoke(Method method, Object[] args) {
        BusinessMethodInterceptorInfo methodInterceptorInfo = interceptorInfo.getBusinessMethodsInfo().get(method);
        LinkedHashMap<Decorator<?>, Method> methodDecorators = methodInterceptorInfo.getMethodDecorators();
        for (int i = index; i < decorators.size(); i++)
        {
            Decorator<?> decorator = decorators.get(i);
            Method decoratingMethod = methodDecorators.get(decorator);
            if (decoratingMethod != null)
            {
                try {
                    return decoratingMethod.invoke(instances.get(decorator), args);
                } catch (InvocationTargetException e) {
                    return ExceptionUtil.throwAsRuntimeException(e.getTargetException());
                } catch (Exception e) {
                    return ExceptionUtil.throwAsRuntimeException(e);
                }
            }
        }
        try {
            return method.invoke(target, args);
        } catch (InvocationTargetException e) {
            return ExceptionUtil.throwAsRuntimeException(e.getTargetException());
        } catch (Exception e) {
            return ExceptionUtil.throwAsRuntimeException(e);
        }
    }
}
