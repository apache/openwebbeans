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
package org.apache.webbeans.newtests.interceptors.business.tests;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InterceptionType;
import javax.enterprise.inject.spi.Interceptor;
import javax.enterprise.util.AnnotationLiteral;

import org.apache.webbeans.intercept.DefaultInterceptorHandler;
import org.apache.webbeans.newtests.AbstractUnitTest;
import org.apache.webbeans.newtests.interceptors.business.common.RuntimeExceptionBindingTypeBean;
import org.apache.webbeans.newtests.interceptors.common.RuntimeExceptionsInterceptor;
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
        
        RuntimeExceptionBindingTypeBean target = (RuntimeExceptionBindingTypeBean) beanManager.getContext(RequestScoped.class).get(bean, creationalContext);
        RuntimeExceptionsInterceptor interceptor = (RuntimeExceptionsInterceptor) beanManager.getReference(interceptorBean, RuntimeExceptionsInterceptor.class, creationalContext);

        List<Method> interceptedMethods = Arrays.asList(RuntimeExceptionBindingTypeBean.class.getMethod("business"));
        Map<Method, List<Interceptor<RuntimeExceptionBindingTypeBean>>> interceptors = new HashMap<Method, List<Interceptor<RuntimeExceptionBindingTypeBean>>>();
        interceptors.put(interceptedMethods.iterator().next(), Arrays.<Interceptor<RuntimeExceptionBindingTypeBean>> asList(interceptorBean));
        Map instances = new HashMap();
        instances.put(interceptorBean, interceptor);
        InterceptorHandler interceptorHandler = new DefaultInterceptorHandler<RuntimeExceptionBindingTypeBean>(target, interceptedMethods, interceptors, (Map<Interceptor<RuntimeExceptionBindingTypeBean>, RuntimeExceptionBindingTypeBean>) instances);
        
        InterceptorDecoratorProxyFactory factory = new InterceptorDecoratorProxyFactory();
        Class<RuntimeExceptionBindingTypeBean> proxyClass = factory.createProxyClass(Thread.currentThread().getContextClassLoader(), RuntimeExceptionBindingTypeBean.class, interceptedMethods, new ArrayList<Method>());
        RuntimeExceptionBindingTypeBean instance = factory.createProxyInstance(proxyClass, target, interceptorHandler);
        int result = instance.business();
        Assert.assertEquals(42, result);
        
        shutDownContainer();
        
    }
}
