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

import org.apache.webbeans.test.AbstractUnitTest;
import org.apache.webbeans.test.interceptors.business.common.RuntimeExceptionBindingTypeBean;
import org.apache.webbeans.test.interceptors.common.RuntimeExceptionsInterceptor;
import org.junit.Test;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.Collection;

@SuppressWarnings("unchecked")
public class ExceptionInterceptorTest extends AbstractUnitTest
{
    private static final String PACKAGE_NAME = ExceptionInterceptorTest.class.getPackage().getName();
    
    @Test
    public void testAroundInvokeWithoutThrowsException() throws Exception
    {
        Collection<String> beanXmls = new ArrayList<String>();
        beanXmls.add(getXmlPath(PACKAGE_NAME, "RuntimeExceptionsInterceptorTest"));
        
        Collection<Class<?>> beanClasses = new ArrayList<Class<?>>();
        beanClasses.add(RuntimeExceptionsInterceptor.class);
        beanClasses.add(RuntimeExceptionBindingTypeBean.class);

        startContainer(beanClasses, beanXmls);

        RuntimeExceptionBindingTypeBean instance = getInstance(RuntimeExceptionBindingTypeBean.class);

        Assert.assertNotNull(instance);

        int result = instance.business();
        Assert.assertEquals(42, result);
        
        shutDownContainer();
        
    }
}
