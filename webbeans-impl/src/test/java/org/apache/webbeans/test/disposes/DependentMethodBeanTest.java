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
package org.apache.webbeans.test.disposes;

import java.util.ArrayList;
import java.util.Collection;

import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;

import org.junit.Assert;

import org.apache.webbeans.test.AbstractUnitTest;
import org.apache.webbeans.test.disposes.beans.AppScopedBean;
import org.apache.webbeans.test.disposes.beans.DependentModelProducer;
import org.apache.webbeans.test.disposes.beans.InjectedIntoBean;
import org.apache.webbeans.test.disposes.beans.IntermediateDependentBean;
import org.apache.webbeans.test.disposes.beans.RequestModelProducer;
import org.apache.webbeans.test.disposes.common.DependentModel;
import org.apache.webbeans.test.disposes.common.RequestModel;
import org.junit.Test;

public class DependentMethodBeanTest extends AbstractUnitTest
{
    @Test
    @SuppressWarnings("unchecked")
    public void testDisposerMethod()
    {
        Collection<String> beanXmls = new ArrayList<String>();
        
        Collection<Class<?>> beanClasses = new ArrayList<Class<?>>();
        beanClasses.add(AppScopedBean.class);
        beanClasses.add(RequestModelProducer.class);
        
        startContainer(beanClasses, beanXmls);        

        Bean<RequestModel> bean = (Bean<RequestModel>)getBeanManager().getBeans("rproduce").iterator().next();
        CreationalContext<RequestModel> cc = getBeanManager().createCreationalContext(bean);
        RequestModel model = (RequestModel) getBeanManager().getReference(bean, RequestModel.class, cc);
        System.out.println(model.getID());
        
        shutDownContainer();
        
        Assert.assertTrue(AppScopedBean.OK);
        
    }
    
    @Test
    @SuppressWarnings("unchecked")
    public void testDisposerMethodWithIntermediateDependent()
    {
        Collection<String> beanXmls = new ArrayList<String>();
        
        Collection<Class<?>> beanClasses = new ArrayList<Class<?>>();
        beanClasses.add(DependentModelProducer.class);
        beanClasses.add(InjectedIntoBean.class);
        beanClasses.add(IntermediateDependentBean.class);
        
        startContainer(beanClasses, beanXmls);
        Bean<InjectedIntoBean> bean = (Bean<InjectedIntoBean>)getBeanManager().getBeans("injectedIntoBean").iterator().next();

        CreationalContext<InjectedIntoBean> cc = getBeanManager().createCreationalContext(bean);

        InjectedIntoBean model = (InjectedIntoBean) getBeanManager().getReference(bean, InjectedIntoBean.class, cc);
        
        Assert.assertFalse(model.isBeanNull());

        shutDownContainer();
        
        //Disposer should only be called once
        Assert.assertEquals(1, DependentModelProducer.disposerCount);
    }

    //X @Test temporarily disabled
    @SuppressWarnings("unchecked")
    public void testDisposerMethodWithRequestScoped()
    {
        Collection<String> beanXmls = new ArrayList<String>();

        Collection<Class<?>> beanClasses = new ArrayList<Class<?>>();
        beanClasses.add(DependentModelProducer.class);
        beanClasses.add(DependentModel.class);
        beanClasses.add(RequestModel.class);
        beanClasses.add(RequestModelProducer.class);

        startContainer(beanClasses, beanXmls);

        RequestModelProducer.producerGotDestroyed = false;
        DependentModelProducer.producerGotDestroyed = false;
        DependentModelProducer.disposerCount = 0;

        RequestModel model = getInstance(RequestModel.class);

        Assert.assertEquals(0, model.getID());

        getLifecycle().getContextService().endContext(RequestScoped.class, null);

        Assert.assertFalse(DependentModelProducer.producerGotDestroyed);
        Assert.assertFalse(RequestModelProducer.producerGotDestroyed);

        shutDownContainer();

        //Disposer should only be called once
        Assert.assertEquals(1, DependentModelProducer.disposerCount);
    }
}
