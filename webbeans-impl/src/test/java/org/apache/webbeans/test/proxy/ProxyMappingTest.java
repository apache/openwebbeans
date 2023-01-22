/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.webbeans.test.proxy;


import jakarta.inject.Provider;

import org.apache.webbeans.intercept.ApplicationScopedBeanInterceptorHandler;
import org.apache.webbeans.intercept.NormalScopedBeanInterceptorHandler;
import org.apache.webbeans.test.AbstractUnitTest;
import org.apache.webbeans.test.proxy.beans.ApplicationBean;
import org.apache.webbeans.test.proxy.beans.ConversationBean;
import org.apache.webbeans.test.proxy.beans.DummyScopedExtension;
import org.apache.webbeans.proxy.OwbNormalScopeProxy;
import org.junit.Test;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Test the mapping of scopes to proxy MethodHandler implementations.
 */
public class ProxyMappingTest extends AbstractUnitTest
{

    @Test
    public void testProxyMappingConfig()
    {
        Collection<Class<?>> beanClasses = new ArrayList<Class<?>>();

        addExtension(new DummyScopedExtension());
        beanClasses.add(ConversationBean.class);
        beanClasses.add(ApplicationBean.class);
        startContainer(beanClasses, null);

        ConversationBean conversationBean = getInstance(ConversationBean.class);
        Assert.assertNotNull(conversationBean);
        Assert.assertTrue(conversationBean instanceof OwbNormalScopeProxy);

        Provider instanceProvider = getWebBeansContext().getNormalScopeProxyFactory().getInstanceProvider((OwbNormalScopeProxy) conversationBean);
        Assert.assertNotNull(instanceProvider);
        Assert.assertEquals(instanceProvider.getClass(), NormalScopedBeanInterceptorHandler.class);


        ApplicationBean applicationBean = getInstance(ApplicationBean.class);
        Assert.assertNotNull(applicationBean);
        Assert.assertTrue(applicationBean instanceof OwbNormalScopeProxy);
        instanceProvider = getWebBeansContext().getNormalScopeProxyFactory().getInstanceProvider((OwbNormalScopeProxy) applicationBean);
        Assert.assertNotNull(instanceProvider);
        Assert.assertEquals(instanceProvider.getClass(), ApplicationScopedBeanInterceptorHandler.class);

    }

}
