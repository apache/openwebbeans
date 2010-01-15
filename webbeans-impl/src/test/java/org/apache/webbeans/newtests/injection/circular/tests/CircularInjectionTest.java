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
package org.apache.webbeans.newtests.injection.circular.tests;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;

import junit.framework.Assert;

import org.apache.webbeans.newtests.AbstractUnitTest;
import org.apache.webbeans.newtests.injection.circular.beans.CircularApplicationScopedBean;
import org.apache.webbeans.newtests.injection.circular.beans.CircularDependenScopeBean;
import org.junit.Test;

public class CircularInjectionTest extends AbstractUnitTest
{
    public CircularInjectionTest()
    {
        
    }
    
    @Test
    @SuppressWarnings("unchecked")
    public void testOneNormalOneDependent()
    {
        Collection<URL> beanXmls = new ArrayList<URL>();
        
        Collection<Class<?>> beanClasses = new ArrayList<Class<?>>();
        beanClasses.add(CircularDependenScopeBean.class);
        beanClasses.add(CircularApplicationScopedBean.class);
        
        startContainer(beanClasses, beanXmls);        
        
        Set<Bean<?>> beans = getBeanManager().getBeans("org.apache.webbeans.newtests.injection.circular.beans.CircularApplicationScopedBean");
        Assert.assertNotNull(beans);        
        
        Bean<CircularApplicationScopedBean> dependentBean = (Bean<CircularApplicationScopedBean>)beans.iterator().next();        
        CreationalContext<CircularApplicationScopedBean> ctx = getBeanManager().createCreationalContext(dependentBean);
        
        Object reference = getBeanManager().getReference(dependentBean, CircularApplicationScopedBean.class, ctx);
        
        Assert.assertTrue(reference instanceof CircularApplicationScopedBean);
        
        CircularApplicationScopedBean beanInstance = (CircularApplicationScopedBean)reference;
        beanInstance.hello();
        
        Assert.assertTrue(CircularDependenScopeBean.success);
        Assert.assertTrue(CircularApplicationScopedBean.success);
        
        shutDownContainer();
    }
    
    @Test
    @SuppressWarnings("unchecked")
    public void testOneDependentOneNormal()
    {
        Collection<URL> beanXmls = new ArrayList<URL>();
        
        Collection<Class<?>> beanClasses = new ArrayList<Class<?>>();
        beanClasses.add(CircularDependenScopeBean.class);
        beanClasses.add(CircularApplicationScopedBean.class);
        
        startContainer(beanClasses, beanXmls);        
        
        Set<Bean<?>> beans = getBeanManager().getBeans("org.apache.webbeans.newtests.injection.circular.beans.CircularDependenScopeBean");
        Assert.assertNotNull(beans);        
        
        Bean<CircularDependenScopeBean> dependentBean = (Bean<CircularDependenScopeBean>)beans.iterator().next();        
        CreationalContext<CircularDependenScopeBean> ctx = getBeanManager().createCreationalContext(dependentBean);
        
        Object reference = getBeanManager().getReference(dependentBean, CircularDependenScopeBean.class, ctx);
        
        Assert.assertTrue(reference instanceof CircularDependenScopeBean);
                
        Assert.assertTrue(CircularDependenScopeBean.success);
        Assert.assertTrue(CircularApplicationScopedBean.success);
        
        shutDownContainer();
    }
}
