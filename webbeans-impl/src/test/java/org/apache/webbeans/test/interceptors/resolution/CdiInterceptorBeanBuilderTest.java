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
package org.apache.webbeans.test.interceptors.resolution;


import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.InterceptionType;
import jakarta.enterprise.inject.spi.Interceptor;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.webbeans.component.BeanAttributesImpl;
import org.apache.webbeans.component.InterceptorBean;
import org.apache.webbeans.component.creation.BeanAttributesBuilder;
import org.apache.webbeans.component.creation.CdiInterceptorBeanBuilder;
import org.apache.webbeans.test.AbstractUnitTest;
import org.apache.webbeans.test.interceptors.factory.beans.ClassInterceptedClass;
import org.apache.webbeans.test.interceptors.resolution.beans.UtilitySampleBean;
import org.apache.webbeans.test.interceptors.resolution.interceptors.TestIntercepted1;
import org.apache.webbeans.test.interceptors.resolution.interceptors.TestInterceptor1;
import org.apache.webbeans.test.interceptors.resolution.interceptors.TestInterceptorParent;
import org.apache.webbeans.test.component.intercept.webbeans.SecureAndTransactionalInterceptor;
import org.apache.webbeans.test.component.intercept.webbeans.TransactionalInterceptor;
import org.apache.webbeans.test.component.intercept.webbeans.bindings.Transactional;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for the various InterceptorBeanBuilder implementations.
 */
public class CdiInterceptorBeanBuilderTest extends AbstractUnitTest
{
    @Test
    public void testClassLevelSingleInterceptor() throws Exception
    {
        Collection<String> beanXmls = new ArrayList<String>();
        beanXmls.add(getXmlPath(this.getClass().getPackage().getName(), InterceptorResolutionServiceTest.class.getSimpleName()));

        Collection<Class<?>> beanClasses = new ArrayList<Class<?>>();
        beanClasses.add(ClassInterceptedClass.class);
        beanClasses.add(Transactional.class);
        beanClasses.add(TransactionalInterceptor.class);
        beanClasses.add(TestIntercepted1.class);
        beanClasses.add(TestInterceptor1.class);
        beanClasses.add(TestInterceptorParent.class);
        beanClasses.add(UtilitySampleBean.class);

        startContainer(beanClasses, beanXmls);

        {
            // take an Interceptor class which is not listed in beans.xml and verify that is is not enabled
            AnnotatedType<SecureAndTransactionalInterceptor> annotatedType = getBeanManager().createAnnotatedType(SecureAndTransactionalInterceptor.class);
            BeanAttributesImpl<SecureAndTransactionalInterceptor> beanAttributes = BeanAttributesBuilder.forContext(getWebBeansContext()).newBeanAttibutes(annotatedType).build();

            CdiInterceptorBeanBuilder<SecureAndTransactionalInterceptor> ibb
                    = new CdiInterceptorBeanBuilder<SecureAndTransactionalInterceptor>(getWebBeansContext(), annotatedType, beanAttributes);
            Assert.assertFalse(ibb.isInterceptorEnabled());
        }

        {
            AnnotatedType<TransactionalInterceptor> annotatedType = getBeanManager().createAnnotatedType(TransactionalInterceptor.class);
            BeanAttributesImpl<TransactionalInterceptor> beanAttributes = BeanAttributesBuilder.forContext(getWebBeansContext()).newBeanAttibutes(annotatedType).build();

            CdiInterceptorBeanBuilder<TransactionalInterceptor> ibb
                    = new CdiInterceptorBeanBuilder<TransactionalInterceptor>(getWebBeansContext(), annotatedType, beanAttributes);
            ibb.defineCdiInterceptorRules();
            Interceptor<TransactionalInterceptor> bean = ibb.getBean();
            Assert.assertNotNull(bean);

            Assert.assertNotNull(bean.getInterceptorBindings());
            Assert.assertEquals(1, bean.getInterceptorBindings().size());

            Assert.assertTrue(bean.intercepts(InterceptionType.AROUND_INVOKE));
            Assert.assertFalse(bean.intercepts(InterceptionType.AROUND_TIMEOUT));
            Assert.assertFalse(bean.intercepts(InterceptionType.POST_CONSTRUCT));

        }

        shutDownContainer();
    }

    @Test
    public void testClassLevelParentInterceptor() throws Exception
    {
        Collection<String> beanXmls = new ArrayList<String>();
        beanXmls.add(getXmlPath(this.getClass().getPackage().getName(), InterceptorResolutionServiceTest.class.getSimpleName()));

        Collection<Class<?>> beanClasses = new ArrayList<Class<?>>();
        beanClasses.add(TestIntercepted1.class);
        beanClasses.add(TestInterceptor1.class);
        beanClasses.add(TestInterceptorParent.class);
        beanClasses.add(UtilitySampleBean.class);

        startContainer(beanClasses, beanXmls);

        AnnotatedType<TestInterceptor1> annotatedType = getBeanManager().createAnnotatedType(TestInterceptor1.class);
        BeanAttributesImpl<TestInterceptor1> beanAttributes = BeanAttributesBuilder.forContext(getWebBeansContext()).newBeanAttibutes(annotatedType).build();

        CdiInterceptorBeanBuilder<TestInterceptor1> ibb
                = new CdiInterceptorBeanBuilder<TestInterceptor1>(getWebBeansContext(), annotatedType, beanAttributes);
        ibb.defineCdiInterceptorRules();
        InterceptorBean<TestInterceptor1> bean = ibb.getBean();
        Assert.assertNotNull(bean);

        Assert.assertTrue(bean.intercepts(InterceptionType.AROUND_INVOKE));
        Assert.assertTrue(bean.intercepts(InterceptionType.AROUND_TIMEOUT));
        Assert.assertTrue(bean.intercepts(InterceptionType.PRE_DESTROY));
        Assert.assertTrue(bean.intercepts(InterceptionType.POST_CONSTRUCT));

        Assert.assertFalse(bean.intercepts(InterceptionType.PRE_PASSIVATE));
        Assert.assertFalse(bean.intercepts(InterceptionType.POST_ACTIVATE));
        Assert.assertEquals(1, bean.getInterceptorBindings().size());

        Assert.assertEquals(1, bean.getInterceptorMethods(InterceptionType.AROUND_INVOKE).length);
        Assert.assertEquals(1, bean.getInterceptorMethods(InterceptionType.AROUND_TIMEOUT).length);
        Assert.assertEquals(2, bean.getInterceptorMethods(InterceptionType.POST_CONSTRUCT).length);
        Assert.assertEquals(2, bean.getInterceptorMethods(InterceptionType.PRE_DESTROY).length);

        CreationalContext<TestInterceptor1> cc = getBeanManager().createCreationalContext(bean);
        TestInterceptor1 interceptorInstance = bean.create(cc);
        Assert.assertNotNull(interceptorInstance);

        shutDownContainer();
    }
}
