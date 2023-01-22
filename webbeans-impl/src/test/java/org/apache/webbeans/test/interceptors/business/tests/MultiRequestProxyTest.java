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

import jakarta.enterprise.context.RequestScoped;
import java.util.ArrayList;
import java.util.Collection;

import org.junit.Assert;
import org.apache.webbeans.test.AbstractUnitTest;
import org.apache.webbeans.test.interceptors.beans.ApplicationScopedBean;
import org.apache.webbeans.test.interceptors.beans.DependentScopedBean;
import org.apache.webbeans.test.interceptors.beans.RequestScopedBean;
import org.apache.webbeans.test.interceptors.common.TransactionInterceptor;
import org.junit.Test;

@SuppressWarnings("unchecked")
public class MultiRequestProxyTest extends AbstractUnitTest
{
    private static final String PACKAGE_NAME = MultiRequestProxyTest.class.getPackage().getName();
    
    @Test
    public void testMultiRequestProxying()
    {
        Collection<String> beanXmls = new ArrayList<String>();
        beanXmls.add(getXmlPath(PACKAGE_NAME, "DependingInterceptorTest"));
        
        Collection<Class<?>> beanClasses = new ArrayList<Class<?>>();
        beanClasses.add(TransactionInterceptor.class);
        beanClasses.add(ApplicationScopedBean.class);
        beanClasses.add(RequestScopedBean.class);
        beanClasses.add(DependentScopedBean.class);

        startContainer(beanClasses, beanXmls);

        for (int i = 1; i < 100; i++)
        {
            RequestScopedBean rb1 = getInstance("requestScopedBean");
            Assert.assertNotNull(rb1);
            Assert.assertNotNull(rb1.getInstance());

            getLifecycle().getContextService().endContext(RequestScoped.class, null);

            // and now the 2nd request
            getLifecycle().getContextService().startContext(RequestScoped.class, null);
        }

        shutDownContainer();
        
    }
    
}
