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
import java.util.ArrayList;
import java.util.Collection;

import org.apache.webbeans.component.InterceptorBean;
import org.apache.webbeans.component.creation.SelfInterceptorBeanBuilder;
import org.apache.webbeans.newtests.AbstractUnitTest;
import org.apache.webbeans.newtests.interceptors.resolution.interceptors.SelfInterceptedClass;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test for building EJB-style interceptor beans
 */
public class SelfInterceptorBeanBuilderTest extends AbstractUnitTest
{

    @Test
    public void testEjbInterceptorBeanCreation()
    {
        Collection<Class<?>> beanClasses = new ArrayList<Class<?>>();
        beanClasses.add(SelfInterceptedClass.class);

        startContainer(beanClasses, null);

        AnnotatedType<SelfInterceptedClass> annotatedType = getBeanManager().createAnnotatedType(SelfInterceptedClass.class);

        SelfInterceptorBeanBuilder<SelfInterceptedClass> ibb
                = new SelfInterceptorBeanBuilder<SelfInterceptedClass>(getWebBeansContext(), annotatedType);
        ibb.defineSelfInterceptorRules();
        InterceptorBean<SelfInterceptedClass> bean = ibb.getBean();
        Assert.assertNotNull(bean);


        shutDownContainer();
    }

}
