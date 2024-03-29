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
package org.apache.webbeans.test.contexts;


import static org.junit.Assert.assertTrue;

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.container.SerializableBean;
import org.apache.webbeans.context.SessionContext;
import org.apache.webbeans.service.ClassLoaderProxyService;
import org.apache.webbeans.spi.DefiningClassService;
import org.apache.webbeans.test.AbstractUnitTest;
import org.apache.webbeans.test.contexts.serialize.AppScopedBean;
import org.apache.webbeans.test.contexts.serialize.SessScopedBean;
import org.apache.webbeans.test.contexts.session.common.PersonalDataBean;
import org.apache.webbeans.test.decorators.multiple.Decorator1;
import org.apache.webbeans.test.decorators.multiple.OutputProvider;
import org.apache.webbeans.test.decorators.multiple.RequestStringBuilder;
import org.apache.webbeans.test.injection.circular.beans.CircularApplicationScopedBean;
import org.apache.webbeans.test.injection.circular.beans.CircularConstructorOrProducerMethodParameterBean;
import org.apache.webbeans.test.injection.circular.beans.CircularDependentScopedBean;
import org.apache.webbeans.test.injection.circular.beans.CircularNormalInConstructor;
import org.apache.webbeans.proxy.OwbNormalScopeProxy;
import org.apache.webbeans.test.component.CheckWithCheckPayment;
import org.apache.webbeans.test.component.CheckWithMoneyPayment;
import org.apache.webbeans.test.component.IPayment;
import org.apache.webbeans.test.component.PaymentProcessorComponent;
import org.apache.webbeans.test.component.event.normal.ComponentWithObserves1;
import org.apache.webbeans.test.component.event.normal.ComponentWithObserves2;
import org.apache.webbeans.test.component.event.normal.TransactionalInterceptor;
import org.apache.webbeans.util.WebBeansUtil;

import org.junit.Assert;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;


import jakarta.enterprise.context.SessionScoped;
import jakarta.enterprise.context.spi.Context;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;


/**
 *  Tests for various serialization issues
 */
public class SerializationTest extends AbstractUnitTest
{
    @Rule
    public final TestName testName = new TestName();

    @SuppressWarnings("unchecked")
    @Test
    public void testCreationalContextSerialization() throws Exception
    {
        Collection<Class<?>> classes = new ArrayList<Class<?>>();

        // add a few random classes
        classes.add(PersonalDataBean.class);
        classes.add(CircularDependentScopedBean.class);
        classes.add(CircularApplicationScopedBean.class);
        startContainer(classes);

        BeanManager bm = getBeanManager();
        Set<Bean<?>> beans = getBeanManager().getBeans(PersonalDataBean.class);
        Assert.assertNotNull(beans);
        Assert.assertTrue(beans.size() == 1);
        Bean pdbBean = beans.iterator().next();
        CreationalContext<PersonalDataBean> pdbCreational = bm.createCreationalContext(pdbBean);
        Assert.assertNotNull(pdbCreational);

        // oki, now let's serializeBean the CreationalContext
        byte[] serial = serializeObject(pdbCreational);
        CreationalContext<?> cc2 = (CreationalContext<?>) deSerializeObject(serial);
        Assert.assertNotNull(cc2);
    }

    @Test
    public void testPersonalDataBean() throws ClassNotFoundException, IOException
    {
        Collection<Class<?>> classes = new ArrayList<Class<?>>();

        // add a few random classes
        classes.add(PersonalDataBean.class);
        classes.add(OutputProvider.class);
        classes.add(Decorator1.class);
        classes.add(CircularApplicationScopedBean.class);
        classes.add(CircularDependentScopedBean.class);
        classes.add(RequestStringBuilder.class);
        classes.add(CircularConstructorOrProducerMethodParameterBean.class);
        classes.add(CircularDependentScopedBean.class);
        classes.add(CircularNormalInConstructor.class);
        classes.add(TransactionalInterceptor.class);
        classes.add(ComponentWithObserves1.class);
        classes.add(ComponentWithObserves2.class);
        classes.add(PaymentProcessorComponent.class);
        classes.add(IPayment.class);
        classes.add(CheckWithCheckPayment.class);
        classes.add(CheckWithMoneyPayment.class);

        startContainer(classes);

        Set<Bean<?>> beans = getBeanManager().getBeans(Object.class);
        Assert.assertNotNull(beans);
        Assert.assertTrue(beans.size() > 7);

        WebBeansContext webBeansContext = WebBeansContext.getInstance();
        for (Bean<?> bean : beans)
        {
            String id = null;
            if((id = WebBeansUtil.getPassivationId(bean)) != null)
            {
                bean = (Bean<?>) webBeansContext.getSerializableBeanVault().getSerializableBean(bean);
                
                byte[] serial = serializeBean(bean);
                Bean<?> b2 = deSerializeBean(serial);

                Assert.assertEquals(((SerializableBean<?>)bean).getBean(), ((SerializableBean<?>)b2).getBean());
                
            }
        }
        
        // and now we are keen and try to serialize the whole passivatable Contexts!
        PersonalDataBean pdb = getInstance(PersonalDataBean.class);
        pdb.business();

        Bean<PersonalDataBean> pdbBean = getBean(PersonalDataBean.class);

        Context sessionContext = webBeansContext.getBeanManagerImpl().getContext(SessionScoped.class);
        Assert.assertNotNull(sessionContext);
        Assert.assertNotNull(sessionContext.get(pdbBean));
        byte[] ba = serializeObject(sessionContext);
        Assert.assertNotNull(ba);
        Context sessContext2 = (Context) deSerializeObject(ba);
        Assert.assertNotNull(sessContext2);
        ((SessionContext) sessContext2).setActive(true);
        Assert.assertNotNull(sessContext2.get(pdbBean));
    }

    @Test
    public void testProxySerialization() throws Exception
    {
        doProxySerialization();
    }

    @Test
    public void testProxySerializationWithClassLoaderProxy() throws Exception
    {
        addService(DefiningClassService.class, ClassLoaderProxyService.class);
        doProxySerialization();
        assertTrue(ClassLoaderProxyService.class.isInstance(
                getWebBeansContext().getService(DefiningClassService.class)));
    }

    private void doProxySerialization() throws IOException, ClassNotFoundException
    {
        Collection<Class<?>> classes = new ArrayList<Class<?>>();

        // add a few random classes
        classes.add(SessScopedBean.class);
        classes.add(AppScopedBean.class);

        startContainer(classes);

        Set<Bean<?>> beans = getBeanManager().getBeans(SessScopedBean.class);
        Assert.assertNotNull(beans);
        Assert.assertTrue(beans.size() == 1);

        @SuppressWarnings("unchecked")
        Bean<SessScopedBean> bean = (Bean<SessScopedBean>) beans.iterator().next();
        CreationalContext<SessScopedBean> ssbCreational = getBeanManager().createCreationalContext(bean);
        Assert.assertNotNull(ssbCreational);

        SessScopedBean reference = (SessScopedBean) getBeanManager().getReference(bean, SessScopedBean.class, ssbCreational);
        Assert.assertNotNull(reference);
        Assert.assertTrue(reference instanceof OwbNormalScopeProxy);

        reference.getApp().setI(4711);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(reference);
        byte[] ba = baos.toByteArray();

        ByteArrayInputStream bais = new ByteArrayInputStream(ba);
        ObjectInputStream ois = new ObjectInputStream(bais);
        SessScopedBean ssb2 =  (SessScopedBean) ois.readObject();
        Assert.assertNotNull(ssb2);

        Assert.assertNotNull(ssb2.getApp());
        Assert.assertTrue(ssb2.getApp().getI() == 4711);
    }

    public static byte[] serializeBean(Bean<?> bean) throws IOException
    {
        return serializeObject(bean);
    }
    
    public static byte[] serializeObject(Object o) throws IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(o);
        return baos.toByteArray();
    }

    public static Bean<?> deSerializeBean(byte[] serial) throws IOException, ClassNotFoundException
    {
        return (Bean<?>) deSerializeObject(serial);
    }
    
    public static Object deSerializeObject(byte[] serial) throws IOException, ClassNotFoundException
    {
        ByteArrayInputStream bais = new ByteArrayInputStream(serial);
        ObjectInputStream ois = new ObjectInputStream(bais);
        return ois.readObject();
    }

}
