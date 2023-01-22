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
package org.apache.webbeans.test.interceptors.lifecycle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;

import org.junit.Assert;

import org.apache.webbeans.test.AbstractUnitTest;
import org.junit.Test;

public class LifecycleTest extends AbstractUnitTest
{
    private static final String PACKAGE_NAME = LifecycleTest.class.getPackage().getName(); 
    
    public LifecycleTest()
    {
        
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testLifecycle()
    {
        Collection<String> beanXmls = new ArrayList<String>();
        beanXmls.add(getXmlPath(PACKAGE_NAME, "LifecycleTest"));
        
        Collection<Class<?>> beanClasses = new ArrayList<Class<?>>();
        beanClasses.add(LifecycleInterceptor.class);
        beanClasses.add(LifecycleBean.class);
        beanClasses.add(LifecycleBean.FooProducer.class);

        addExtension(new InterceptorExtension());

        startContainer(beanClasses, beanXmls);        
        
        Set<Bean<?>> beans = getBeanManager().getBeans(LifecycleBean.class);
        Assert.assertNotNull(beans);
        Bean<LifecycleBean> lifecycleBean = (Bean<LifecycleBean>)beans.iterator().next();

        CreationalContext<LifecycleBean> creationalContext = getBeanManager().createCreationalContext(lifecycleBean);
        
        Object reference = getBeanManager().getReference(lifecycleBean, LifecycleBean.class, creationalContext);
        Assert.assertNotNull(reference);
        
        Assert.assertTrue(reference instanceof LifecycleBean);

        Assert.assertTrue(LifecycleInterceptor.POST_CONSTRUCT);
        
        Assert.assertEquals(LifecycleBean.FOO, LifecycleBean.CONSTRUCTOR_INJECTED);
        
        Assert.assertTrue(!LifecycleInterceptor.PRE_DESTROY);
        
        lifecycleBean.destroy((LifecycleBean)reference, creationalContext);
        
        shutDownContainer();
        
        Assert.assertTrue(LifecycleInterceptor.PRE_DESTROY);
        
    }
    
    @Test
    public void testNotannotated()
    {
        Collection<String> beanXmls = new ArrayList<String>();
        beanXmls.add(getXmlPath(PACKAGE_NAME, "LifecycleTest"));
        
        Collection<Class<?>> beanClasses = new ArrayList<Class<?>>();
        beanClasses.add(NotAnnotatedBean.class);
        beanClasses.add(LifecycleInterceptor.class);
        
        
        addExtension(new InterceptorExtension());
        
        startContainer(beanClasses, beanXmls);        
        
        Set<Bean<?>> beans = getBeanManager().getBeans(NotAnnotatedBean.class.getName());
        Assert.assertNotNull(beans);        
        Bean<NotAnnotatedBean> notAnnotatedBean = (Bean<NotAnnotatedBean>)beans.iterator().next();
        
        CreationalContext<NotAnnotatedBean> ctx = getBeanManager().createCreationalContext(notAnnotatedBean);
        
        Object reference = getBeanManager().getReference(notAnnotatedBean, NotAnnotatedBean.class, ctx);
        Assert.assertNotNull(reference);
        
        NotAnnotatedBean nab = (NotAnnotatedBean)reference;
        nab.sayHello();
        Assert.assertTrue(NotAnnotatedBean.PC);
        
        shutDownContainer();
    }

    /**
     * Test an interceptor with no annotations but instead dynamically
     * add the InterceptorBinding and stuff via AnnotatedType.
     * Bbd stands for BeforeBeanDiscovery
     */
    @Test
    public void testDynamicInterceptorBeforeBeanDiscovery()
    {
        Collection<String> beanXmls = new ArrayList<String>();
        beanXmls.add(getXmlPath(PACKAGE_NAME, "LifecycleTestBbd"));

        Collection<Class<?>> beanClasses = new ArrayList<Class<?>>();
        beanClasses.add(NotAnnotatedBean.class);
        beanClasses.add(LifecycleInterceptor.class);


        addExtension(new InterceptorExtension());

        startContainer(beanClasses, beanXmls);

        shutDownContainer();
    }

    /**
     * Test an interceptor with no annotations but instead dynamically
     * add the InterceptorBinding and stuff via AnnotatedType.
     * Bbd stands for BeforeBeanDiscovery
     */
    @Test
    public void testDynamicInterceptorProcessAnnotatedType()
    {
        Collection<String> beanXmls = new ArrayList<String>();
        beanXmls.add(getXmlPath(PACKAGE_NAME, "LifecycleTestPat"));

        Collection<Class<?>> beanClasses = new ArrayList<Class<?>>();
        beanClasses.add(NotAnnotatedBean.class);
        beanClasses.add(LifecycleInterceptor.class);


        addExtension(new InterceptorExtension());

        startContainer(beanClasses, beanXmls);

        shutDownContainer();
    }

}
