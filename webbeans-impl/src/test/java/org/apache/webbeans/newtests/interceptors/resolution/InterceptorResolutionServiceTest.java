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
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.util.AnnotationLiteral;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.apache.webbeans.intercept.InterceptorResolutionService;
import org.apache.webbeans.newtests.AbstractUnitTest;
import org.apache.webbeans.newtests.interceptors.factory.beans.ClassInterceptedClass;
import org.apache.webbeans.newtests.interceptors.factory.beans.ClassMultiInterceptedClass;
import org.apache.webbeans.newtests.interceptors.factory.beans.DecoratedClass;
import org.apache.webbeans.newtests.interceptors.factory.beans.MethodInterceptedClass;
import org.apache.webbeans.test.annotation.binding.Binding1;
import org.apache.webbeans.test.component.decorator.clean.ServiceDecorator;
import org.apache.webbeans.test.component.intercept.webbeans.ActionInterceptor;
import org.apache.webbeans.test.component.intercept.webbeans.SecureInterceptor;
import org.apache.webbeans.test.component.intercept.webbeans.TransactionalInterceptor;
import org.apache.webbeans.test.component.intercept.InterceptedComponent;
import org.apache.webbeans.test.component.intercept.Interceptor1;

import org.apache.webbeans.test.component.intercept.webbeans.bindings.Action;
import org.apache.webbeans.test.component.intercept.webbeans.bindings.Secure;
import org.apache.webbeans.test.component.intercept.webbeans.bindings.Transactional;


import org.junit.Assert;
import org.junit.Test;

/**
 * Test interceptor resolution.
 */
public class InterceptorResolutionServiceTest extends AbstractUnitTest
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

        InterceptorResolutionService ir = new InterceptorResolutionService(getWebBeansContext());
        AnnotatedType<ClassInterceptedClass> annotatedType = getBeanManager().createAnnotatedType(ClassInterceptedClass.class);
        Bean<ClassInterceptedClass> bean = (Bean<ClassInterceptedClass>) getBeanManager().resolve(getBeanManager().getBeans(ClassInterceptedClass.class));

        InterceptorResolutionService.BeanInterceptorInfo interceptorInfo = ir.calculateInterceptorInfo(bean, annotatedType);
        Assert.assertNotNull(interceptorInfo);

        Assert.assertNotNull(interceptorInfo.getInterceptors());
        Assert.assertEquals(1, interceptorInfo.getInterceptors().size());

        Assert.assertNull(interceptorInfo.getDecorators());

        Map<Method, InterceptorResolutionService.MethodInterceptorInfo> methodInterceptorInfos = interceptorInfo.getBusinessMethodsInfo();
        Assert.assertNotNull(methodInterceptorInfos);
        Assert.assertEquals(7, methodInterceptorInfos.size());

        for (InterceptorResolutionService.MethodInterceptorInfo mi : methodInterceptorInfos.values())
        {
            Assert.assertEquals(1, mi.getCdiInterceptors().length);
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

        InterceptorResolutionService ir = new InterceptorResolutionService(getWebBeansContext());
        AnnotatedType<ClassMultiInterceptedClass> annotatedType = getBeanManager().createAnnotatedType(ClassMultiInterceptedClass.class);
        Bean<ClassMultiInterceptedClass> bean = (Bean<ClassMultiInterceptedClass>) getBeanManager().resolve(getBeanManager().getBeans(ClassMultiInterceptedClass.class));

        InterceptorResolutionService.BeanInterceptorInfo interceptorInfo = ir.calculateInterceptorInfo(bean, annotatedType);
        Assert.assertNotNull(interceptorInfo);

        Assert.assertNotNull(interceptorInfo.getInterceptors());
        Assert.assertEquals(3, interceptorInfo.getInterceptors().size());

        Assert.assertNull(interceptorInfo.getDecorators());

        Map<Method, InterceptorResolutionService.MethodInterceptorInfo> methodInterceptorInfos = interceptorInfo.getBusinessMethodsInfo();
        Assert.assertNotNull(methodInterceptorInfos);
        Assert.assertEquals(6, methodInterceptorInfos.size());

        for (InterceptorResolutionService.MethodInterceptorInfo mi : methodInterceptorInfos.values())
        {
            Assert.assertEquals(3, mi.getCdiInterceptors().length);
        }

        shutDownContainer();
    }

    @Test
    public void testMethodLevelInterceptor() throws Exception
    {
        Collection<String> beanXmls = new ArrayList<String>();
        beanXmls.add(getXmlPath(this.getClass().getPackage().getName(), this.getClass().getSimpleName()));

        Collection<Class<?>> beanClasses = new ArrayList<Class<?>>();
        beanClasses.add(MethodInterceptedClass.class);
        beanClasses.add(Transactional.class);
        beanClasses.add(Secure.class);
        beanClasses.add(SecureInterceptor.class);
        beanClasses.add(TransactionalInterceptor.class);

        startContainer(beanClasses, beanXmls);

        InterceptorResolutionService ir = new InterceptorResolutionService(getWebBeansContext());
        AnnotatedType<MethodInterceptedClass> annotatedType = getBeanManager().createAnnotatedType(MethodInterceptedClass.class);
        Bean<MethodInterceptedClass> bean = (Bean<MethodInterceptedClass>) getBeanManager().resolve(getBeanManager().getBeans(MethodInterceptedClass.class));

        InterceptorResolutionService.BeanInterceptorInfo interceptorInfo = ir.calculateInterceptorInfo(bean, annotatedType);
        Assert.assertNotNull(interceptorInfo);

        Assert.assertNotNull(interceptorInfo.getInterceptors());
        Assert.assertEquals(2, interceptorInfo.getInterceptors().size());

        Assert.assertNull(interceptorInfo.getDecorators());

        Map<Method, InterceptorResolutionService.MethodInterceptorInfo> methodInterceptorInfos = interceptorInfo.getBusinessMethodsInfo();
        Assert.assertNotNull(methodInterceptorInfos);
        Assert.assertEquals(2, methodInterceptorInfos.size());

        for (Map.Entry<Method, InterceptorResolutionService.MethodInterceptorInfo> mi : methodInterceptorInfos.entrySet())
        {
            if (mi.getKey().getName().equals("getMeaningOfLife"))
            {
                Assert.assertEquals(1, mi.getValue().getCdiInterceptors().length);
            }
            else if (mi.getKey().getName().equals("setMeaningOfLife"))
            {
                Assert.assertEquals(2, mi.getValue().getCdiInterceptors().length);
            }
        }

        shutDownContainer();
    }


    @Test
    public void testDecoratorResolution() throws Exception
    {
        Collection<String> beanXmls = new ArrayList<String>();
        beanXmls.add(getXmlPath(this.getClass().getPackage().getName(), this.getClass().getSimpleName()));

        Collection<Class<?>> beanClasses = new ArrayList<Class<?>>();
        beanClasses.add(DecoratedClass.class);
        beanClasses.add(ServiceDecorator.class);

        startContainer(beanClasses, beanXmls);

        InterceptorResolutionService ir = new InterceptorResolutionService(getWebBeansContext());
        AnnotatedType<DecoratedClass> annotatedType = getBeanManager().createAnnotatedType(DecoratedClass.class);
        Bean<DecoratedClass> bean = (Bean<DecoratedClass>) getBeanManager().resolve(
                getBeanManager().getBeans(DecoratedClass.class, new AnnotationLiteral<Binding1>() {}));

        InterceptorResolutionService.BeanInterceptorInfo interceptorInfo = ir.calculateInterceptorInfo(bean, annotatedType);
        Assert.assertNotNull(interceptorInfo);

        Assert.assertNotNull(interceptorInfo.getBusinessMethodsInfo());
        Assert.assertEquals(1, interceptorInfo.getBusinessMethodsInfo().size());

        Assert.assertNotNull(interceptorInfo.getDecorators());
        Assert.assertEquals(1, interceptorInfo.getDecorators().size());

        shutDownContainer();
    }

    @Test
    public void testEjbStyleInterceptorResolution() throws Exception
    {
        Collection<String> beanXmls = new ArrayList<String>();
        beanXmls.add(getXmlPath(this.getClass().getPackage().getName(), this.getClass().getSimpleName()));

        Collection<Class<?>> beanClasses = new ArrayList<Class<?>>();
        beanClasses.add(InterceptedComponent.class);
        beanClasses.add(Interceptor1.class);

        startContainer(beanClasses, beanXmls);

        InterceptorResolutionService ir = new InterceptorResolutionService(getWebBeansContext());
        AnnotatedType<InterceptedComponent> annotatedType = getBeanManager().createAnnotatedType(InterceptedComponent.class);
        Bean<InterceptedComponent> bean = (Bean<InterceptedComponent>) getBeanManager().resolve(getBeanManager().getBeans(InterceptedComponent.class));

        InterceptorResolutionService.BeanInterceptorInfo interceptorInfo = ir.calculateInterceptorInfo(bean, annotatedType);
        Assert.assertNotNull(interceptorInfo);
        Assert.assertNotNull(interceptorInfo.getBusinessMethodsInfo());
        Assert.assertEquals(1, interceptorInfo.getBusinessMethodsInfo().size());

        for (Map.Entry<Method, InterceptorResolutionService.MethodInterceptorInfo> mi : interceptorInfo.getBusinessMethodsInfo().entrySet())
        {
            Assert.assertNotNull(mi.getValue().getEjbInterceptors());
            Assert.assertEquals(1, mi.getValue().getEjbInterceptors().length);
        }

        shutDownContainer();
    }

}
