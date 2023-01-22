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
package org.apache.webbeans.test.interceptors.business.tests;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.InterceptionType;
import jakarta.enterprise.inject.spi.Interceptor;
import jakarta.enterprise.util.AnnotationLiteral;

import org.apache.webbeans.intercept.DefaultInterceptorHandler;
import org.apache.webbeans.test.AbstractUnitTest;
import org.apache.webbeans.test.interceptors.business.common.RuntimeExceptionBindingTypeBean;
import org.apache.webbeans.test.interceptors.common.RuntimeExceptionsInterceptor;
import org.apache.webbeans.proxy.InterceptorDecoratorProxyFactory;
import org.apache.webbeans.proxy.InterceptorHandler;
import org.apache.webbeans.test.component.intercept.webbeans.bindings.RuntimeExceptions;
import org.junit.Assert;
import org.junit.Test;

@SuppressWarnings("unchecked")
public class NewProxyTest extends AbstractUnitTest
{
    private static final String PACKAGE_NAME = NewProxyTest.class.getPackage().getName();
    
    @Test
    public void testAroundInvokeWithoutThrowsException() throws Exception
    {
        Collection<String> beanXmls = new ArrayList<String>();
        beanXmls.add(getXmlPath(PACKAGE_NAME, "RuntimeExceptionsInterceptorTest"));
        
        Collection<Class<?>> beanClasses = new ArrayList<Class<?>>();
        beanClasses.add(RuntimeExceptionsInterceptor.class);
        beanClasses.add(RuntimeExceptionBindingTypeBean.class);

        startContainer(beanClasses, beanXmls);

        BeanManager beanManager = getBeanManager();
        Interceptor interceptorBean = beanManager.resolveInterceptors(InterceptionType.AROUND_INVOKE, new AnnotationLiteral<RuntimeExceptions>() {}).iterator().next();
        Bean bean = beanManager.resolve(beanManager.getBeans(RuntimeExceptionBindingTypeBean.class));
        CreationalContext creationalContext = beanManager.createCreationalContext(bean);
        
        // we cannot use the container to create the proxy as it already proxies the internal instance
        RuntimeExceptionBindingTypeBean target = new RuntimeExceptionBindingTypeBean();

        RuntimeExceptionsInterceptor interceptor = (RuntimeExceptionsInterceptor) interceptorBean.create(creationalContext);

        Method[] interceptedMethods = {RuntimeExceptionBindingTypeBean.class.getMethod("business")};
        Map<Method, List<Interceptor<?>>> interceptors = new HashMap<Method, List<Interceptor<?>>>();
        interceptors.put(interceptedMethods[0], Arrays.<Interceptor<?>> asList(interceptorBean));
        Map instances = new HashMap();
        instances.put(interceptorBean, interceptor);
        InterceptorHandler interceptorHandler
                = new DefaultInterceptorHandler<RuntimeExceptionBindingTypeBean>(target, target, interceptors, instances, null);
        
        InterceptorDecoratorProxyFactory factory = new InterceptorDecoratorProxyFactory(getWebBeansContext());
        Class<RuntimeExceptionBindingTypeBean> proxyClass
                = factory.createProxyClass(bean, Thread.currentThread().getContextClassLoader(), RuntimeExceptionBindingTypeBean.class, interceptedMethods, null);

        RuntimeExceptionBindingTypeBean instance = factory.createProxyInstance(proxyClass, target, interceptorHandler);
        int result = instance.business();
        Assert.assertEquals(42, result);

        Assert.assertEquals(1, interceptor.invoked);
        
        shutDownContainer();
        
    }
}
