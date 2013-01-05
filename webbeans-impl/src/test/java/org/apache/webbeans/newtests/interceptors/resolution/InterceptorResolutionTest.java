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
package org.apache.webbeans.newtests.interceptors.resolution;

import javax.enterprise.inject.spi.AnnotatedType;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.apache.webbeans.intercept.InterceptorResolution;
import org.apache.webbeans.newtests.AbstractUnitTest;
import org.apache.webbeans.newtests.interceptors.factory.beans.ClassInterceptedClass;

import org.apache.webbeans.newtests.interceptors.factory.beans.ClassMultiInterceptedClass;
import org.apache.webbeans.test.component.intercept.webbeans.ActionInterceptor;
import org.apache.webbeans.test.component.intercept.webbeans.SecureInterceptor;
import org.apache.webbeans.test.component.intercept.webbeans.TransactionalInterceptor;

import org.apache.webbeans.test.component.intercept.webbeans.bindings.Action;
import org.apache.webbeans.test.component.intercept.webbeans.bindings.Secure;
import org.apache.webbeans.test.component.intercept.webbeans.bindings.Transactional;


import org.junit.Assert;
import org.junit.Test;

/**
 * Test interceptor resolution.
 */
public class InterceptorResolutionTest  extends AbstractUnitTest
{

    @Test
    public void testClassLevelSingleInterceptor() throws Exception
    {
        Collection<String> beanXmls = new ArrayList<String>();
        beanXmls.add(getXmlPath(this.getClass().getPackage().getName(), this.getClass().getSimpleName()));

        Collection<Class<?>> beanClasses = new ArrayList<Class<?>>();
        beanClasses.add(ClassInterceptedClass.class);
        beanClasses.add(Transactional.class);
        beanClasses.add(TransactionalInterceptor.class);

        startContainer(beanClasses, beanXmls);

        InterceptorResolution ir = new InterceptorResolution(getWebBeansContext());
        AnnotatedType<ClassInterceptedClass> annotatedType = getBeanManager().createAnnotatedType(ClassInterceptedClass.class);

        InterceptorResolution.BeanInterceptorInfo interceptorInfo = ir.calculateInterceptorInfo(annotatedType);
        Assert.assertNotNull(interceptorInfo);

        Assert.assertNotNull(interceptorInfo.getInterceptors());
        Assert.assertEquals(1, interceptorInfo.getInterceptors().size());

        Assert.assertNull(interceptorInfo.getDecorators());

        Map<Method, InterceptorResolution.MethodInterceptorInfo> methodInterceptorInfos = interceptorInfo.getMethodsInfo();
        Assert.assertNotNull(methodInterceptorInfos);
        Assert.assertEquals(6, methodInterceptorInfos.size());

        for (InterceptorResolution.MethodInterceptorInfo mi : methodInterceptorInfos.values())
        {
            Assert.assertEquals(1, mi.getMethodCdiInterceptors().size());
        }

        shutDownContainer();
    }

    @Test
    public void testClassLevelMultipleInterceptor() throws Exception
    {
        Collection<String> beanXmls = new ArrayList<String>();
        beanXmls.add(getXmlPath(this.getClass().getPackage().getName(), this.getClass().getSimpleName()));

        Collection<Class<?>> beanClasses = new ArrayList<Class<?>>();
        beanClasses.add(ClassMultiInterceptedClass.class);
        beanClasses.add(Transactional.class);
        beanClasses.add(Secure.class);
        beanClasses.add(Action.class);
        beanClasses.add(ActionInterceptor.class);
        beanClasses.add(SecureInterceptor.class);
        beanClasses.add(TransactionalInterceptor.class);

        startContainer(beanClasses, beanXmls);

        InterceptorResolution ir = new InterceptorResolution(getWebBeansContext());
        AnnotatedType<ClassMultiInterceptedClass> annotatedType = getBeanManager().createAnnotatedType(ClassMultiInterceptedClass.class);

        InterceptorResolution.BeanInterceptorInfo interceptorInfo = ir.calculateInterceptorInfo(annotatedType);
        Assert.assertNotNull(interceptorInfo);

        Assert.assertNotNull(interceptorInfo.getInterceptors());
        Assert.assertEquals(3, interceptorInfo.getInterceptors().size());

        Assert.assertNull(interceptorInfo.getDecorators());

        Map<Method, InterceptorResolution.MethodInterceptorInfo> methodInterceptorInfos = interceptorInfo.getMethodsInfo();
        Assert.assertNotNull(methodInterceptorInfos);
        Assert.assertEquals(6, methodInterceptorInfos.size());

        for (InterceptorResolution.MethodInterceptorInfo mi : methodInterceptorInfos.values())
        {
            Assert.assertEquals(3, mi.getMethodCdiInterceptors().size());
        }

        shutDownContainer();
    }


}
