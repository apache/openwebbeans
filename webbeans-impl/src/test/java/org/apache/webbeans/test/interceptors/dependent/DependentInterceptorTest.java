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
package org.apache.webbeans.test.interceptors.dependent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;

import org.junit.Assert;

import org.apache.webbeans.test.AbstractUnitTest;
import org.apache.webbeans.test.interceptors.beans.DependentScopedBean;
import org.apache.webbeans.test.interceptors.common.DependentInterceptor;
import org.junit.Test;

public class DependentInterceptorTest extends AbstractUnitTest
{
    private static final String PACKAGE_NAME = DependentInterceptorTest.class.getPackage().getName();
    
    public DependentInterceptorTest()
    {
        
    }
    
    @Test
    @SuppressWarnings("unchecked")
    public void testLifecycle()
    {
        Collection<String> beanXmls = new ArrayList<String>();
        beanXmls.add(getXmlPath(PACKAGE_NAME, "DependentInterceptorTest"));
        
        Collection<Class<?>> beanClasses = new ArrayList<Class<?>>();
        beanClasses.add(DependentInterceptor.class);
        beanClasses.add(DependentScopedBean.class);

        DependentScopedBean.PRE_DESTROY = false;
        DependentScopedBean.POST_CONSTRUCT = false;

        startContainer(beanClasses, beanXmls);        
        
        Set<Bean<?>> beans = getBeanManager().getBeans("org.apache.webbeans.test.interceptors.beans.DependentScopedBean");
        Assert.assertNotNull(beans);        
        Bean<DependentScopedBean> bean = (Bean<DependentScopedBean>)beans.iterator().next();
        
        CreationalContext<DependentScopedBean> ctx = getBeanManager().createCreationalContext(bean);

        DependentInterceptor.refCount = 0;

        Object reference = getBeanManager().getReference(bean, DependentScopedBean.class, ctx);
        Assert.assertNotNull(reference);
        
        Assert.assertTrue(reference instanceof DependentScopedBean);

        Assert.assertTrue(DependentScopedBean.POST_CONSTRUCT);
        
        Assert.assertTrue(!DependentScopedBean.PRE_DESTROY);
        
        DependentScopedBean dbean = (DependentScopedBean)reference;
        dbean.sayHello();

        Assert.assertTrue(DependentInterceptor.refCount == 1);
        
        Assert.assertTrue(DependentScopedBean.SAY_HELLO);

        try { 
            dbean.throwException();
        }
        catch (Exception e) { 
            Assert.assertTrue(DependentInterceptor.exceptionTarget.equals(DependentScopedBean.class));
        }
            
                
        bean.destroy(dbean, ctx);
        
        shutDownContainer();
        
        Assert.assertTrue(DependentScopedBean.PRE_DESTROY);
        
    }
    
}
