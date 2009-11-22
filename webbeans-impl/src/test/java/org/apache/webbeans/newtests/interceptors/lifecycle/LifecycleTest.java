/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.apache.webbeans.newtests.interceptors.lifecycle;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;

import junit.framework.Assert;

import org.apache.webbeans.newtests.AbstractUnitTest;
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
        Collection<URL> beanXmls = new ArrayList<URL>();
        beanXmls.add(getXMLUrl(PACKAGE_NAME, "LifecycleTest"));
        
        Collection<Class<?>> beanClasses = new ArrayList<Class<?>>();
        beanClasses.add(LifecycleInterceptor.class);
        beanClasses.add(LifecycleBean.class);
        
        startContainer(beanClasses, beanXmls);        
        
        Set<Bean<?>> beans = getBeanManager().getBeans("org.apache.webbeans.newtests.interceptors.lifecycle.LifecycleBean");
        Assert.assertNotNull(beans);        
        Bean<LifecycleBean> lifecycleBean = (Bean<LifecycleBean>)beans.iterator().next();
        
        CreationalContext<LifecycleBean> ctx = getBeanManager().createCreationalContext(lifecycleBean);
        
        Object reference = getBeanManager().getReference(lifecycleBean, LifecycleBean.class, ctx);
        Assert.assertNotNull(reference);
        
        Assert.assertTrue(reference instanceof LifecycleBean);

        Assert.assertTrue(LifecycleInterceptor.POST_CONSTRUCT);
        
        Assert.assertNotNull(LifecycleBean.CONSTRUCTOR_INJECTED);
        
        Assert.assertTrue(!LifecycleInterceptor.PRE_DESTROY);
        
        lifecycleBean.destroy((LifecycleBean)reference, ctx);
        
        shutDownContainer();
        
        Assert.assertTrue(LifecycleInterceptor.PRE_DESTROY);
        
    }
    
}
