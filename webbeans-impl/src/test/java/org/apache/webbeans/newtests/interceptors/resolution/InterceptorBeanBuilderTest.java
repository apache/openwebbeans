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
import java.util.ArrayList;
import java.util.Collection;

import org.apache.webbeans.component.creation.CdiInterceptorBeanBuilder;
import org.apache.webbeans.newtests.AbstractUnitTest;
import org.apache.webbeans.newtests.interceptors.factory.beans.ClassInterceptedClass;
import org.apache.webbeans.test.component.intercept.webbeans.SecureAndTransactionalInterceptor;
import org.apache.webbeans.test.component.intercept.webbeans.TransactionalInterceptor;
import org.apache.webbeans.test.component.intercept.webbeans.bindings.Transactional;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for the various InterceptorBeanBuilder implementations.
 */
public class InterceptorBeanBuilderTest extends AbstractUnitTest
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

        startContainer(beanClasses, beanXmls);

        {
            AnnotatedType<SecureAndTransactionalInterceptor> annotatedType = getBeanManager().createAnnotatedType(SecureAndTransactionalInterceptor.class);

            CdiInterceptorBeanBuilder<SecureAndTransactionalInterceptor> ibb
                    = new CdiInterceptorBeanBuilder<SecureAndTransactionalInterceptor>(getWebBeansContext(), annotatedType);
            Assert.assertFalse(ibb.isInterceptorEnabled());
        }

        {
            AnnotatedType<TransactionalInterceptor> annotatedType = getBeanManager().createAnnotatedType(TransactionalInterceptor.class);

            CdiInterceptorBeanBuilder<TransactionalInterceptor> ibb
                    = new CdiInterceptorBeanBuilder<TransactionalInterceptor>(getWebBeansContext(), annotatedType);
            ibb.defineCdiInterceptorRules();
            Bean<TransactionalInterceptor> bean = ibb.getBean();
            Assert.assertNotNull(bean);
        }

        shutDownContainer();
    }
}
