/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.webbeans.newtests.managed.instance;

import junit.framework.Assert;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.newtests.AbstractUnitTest;
import org.apache.webbeans.newtests.managed.instance.beans.InstanceInjectedComponent;
import org.apache.webbeans.test.component.CheckWithCheckPayment;
import org.apache.webbeans.test.component.CheckWithMoneyPayment;
import org.apache.webbeans.test.component.IPayment;
import org.apache.webbeans.test.component.PaymentProcessorComponent;
import org.junit.Test;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.util.TypeLiteral;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

public class InjectedInstanceComponentTest extends AbstractUnitTest {

        @Test
    public void testInstanceInjectedComponent()
    {
        Collection<Class<?>> beanClasses = new ArrayList<Class<?>>();
        beanClasses.add(PaymentProcessorComponent.class);
        beanClasses.add(InstanceInjectedComponent.class);
        beanClasses.add(CheckWithCheckPayment.class);
        beanClasses.add(CheckWithMoneyPayment.class);
        beanClasses.add(IPayment.class);

        startContainer(beanClasses, null);

        InstanceInjectedComponent instance = getInstance(InstanceInjectedComponent.class);

        org.junit.Assert.assertNotNull(instance);
        org.junit.Assert.assertNotNull(instance.getInstance());
        org.junit.Assert.assertNotNull(instance.getPaymentComponent());

        Instance<PaymentProcessorComponent> ins = instance.getInstance();

        boolean ambigious = ins.isAmbiguous();

        Assert.assertFalse(ambigious);

        boolean unsatisfied = ins.isUnsatisfied();

        Assert.assertFalse(unsatisfied);

        shutDownContainer();
    }

    @Test
    public void testManualInstanceBeanResolving() {
        Collection<Class<?>> beanClasses = new ArrayList<Class<?>>();

        startContainer(beanClasses, null);

        //X TODO doesn't work Set<Bean<?>> beans = getBeanManager().getBeans(Instance.class);
        Type instanceType = new TypeLiteral<Instance<Object>>(){}.getType();
        Set<Bean<?>> beans = getBeanManager().getBeans(instanceType);
        Assert.assertNotNull(beans);
        Assert.assertTrue(beans.size() > 0);

        Bean<Instance> bean = (Bean<Instance>) getBeanManager().resolve(beans);
        Assert.assertNotNull(bean);
        
        CreationalContext<Instance> ctx = getBeanManager().createCreationalContext(bean);
        
        Object reference = getBeanManager().getReference(bean, instanceType, ctx);
        Assert.assertNotNull(reference);
        
        shutDownContainer();
    }
}
