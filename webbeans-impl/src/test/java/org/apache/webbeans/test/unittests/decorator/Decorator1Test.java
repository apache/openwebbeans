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
package org.apache.webbeans.test.unittests.decorator;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.enterprise.inject.spi.Decorator;

import org.junit.Assert;

import org.apache.webbeans.annotation.DefaultLiteral;
import org.apache.webbeans.test.AbstractUnitTest;
import org.apache.webbeans.test.annotation.binding.Binding1;
import org.apache.webbeans.test.annotation.binding.Binding1Literal;
import org.apache.webbeans.test.component.CheckWithCheckPayment;
import org.apache.webbeans.test.component.decorator.clean.Account;
import org.apache.webbeans.test.component.decorator.clean.AccountComponent;
import org.apache.webbeans.test.component.decorator.clean.LargeTransactionDecorator;
import org.apache.webbeans.test.component.decorator.clean.ServiceDecorator;
import org.apache.webbeans.test.component.decorator.clean.ServiceDecoratorWithCtInjectionPoint;
import org.apache.webbeans.test.component.service.IService;
import org.apache.webbeans.test.component.service.ServiceImpl1;
import org.junit.Test;

public class Decorator1Test extends AbstractUnitTest
{
    @Test
    public void test1()
    {
        addDecorator(ServiceDecorator.class);
        addDecorator(LargeTransactionDecorator.class);
        
        startContainer(ServiceDecorator.class, CheckWithCheckPayment.class, ServiceImpl1.class, Binding1.class);

        ServiceDecorator.delegateAttr = null;
        ServiceDecorator.ip = null;

        ServiceImpl1 serviceImpl = getInstance(ServiceImpl1.class, new Annotation[]{new Binding1Literal()});
        String s = serviceImpl.service();

        Assert.assertEquals("ServiceDecorator", s);

        Set<Type> apiTyeps = new HashSet<Type>();
        apiTyeps.add(IService.class);

        List<Decorator<?>> decs = getBeanManager().resolveDecorators(apiTyeps, new Annotation[]{new Binding1Literal()});
        Assert.assertNotNull(decs);
        Assert.assertTrue(decs.size() > 0);

        Assert.assertEquals(ServiceDecorator.delegateAttr, "ServiceImpl1");

        // actually the following is NOT defined in the spec!
        // Assert.assertNotNull(ServiceDecorator.ip);
        // It is _currently_ totally fine that it is null.
    }

    @Test
    public void test2()
    {
        addDecorator(LargeTransactionDecorator.class);
        
        startContainer(LargeTransactionDecorator.class, AccountComponent.class);

        AccountComponent account = getInstance(AccountComponent.class);

        LargeTransactionDecorator.depositeAmount = null;
        LargeTransactionDecorator.withDrawAmount = null;

        account.deposit(new BigDecimal(1500));
        account.withdraw(new BigDecimal(3000));

        Set<Type> apiTyeps = new HashSet<Type>();
        apiTyeps.add(Account.class);

        List<Decorator<?>> decs = getBeanManager().resolveDecorators(apiTyeps, new Annotation[] { DefaultLiteral.INSTANCE });
        Assert.assertNotNull(decs);
        Assert.assertTrue(decs.size() > 0);

        Assert.assertEquals(1500, LargeTransactionDecorator.depositeAmount.intValue());
        Assert.assertEquals(3000, LargeTransactionDecorator.withDrawAmount.intValue());

    }


    @Test
    public void testDecoratorWithCtDelegate()
    {
        addDecorator(ServiceDecoratorWithCtInjectionPoint.class);
        addDecorator(LargeTransactionDecorator.class);

        startContainer(ServiceDecoratorWithCtInjectionPoint.class, CheckWithCheckPayment.class, ServiceImpl1.class, Binding1.class);

        ServiceDecoratorWithCtInjectionPoint.delegateAttr = null;
        ServiceDecoratorWithCtInjectionPoint.ip = null;

        ServiceImpl1 serviceImpl = getInstance(ServiceImpl1.class, new Annotation[]{new Binding1Literal()});
        String s = serviceImpl.service();

        Assert.assertEquals("ServiceDecoratorWithCtInjectionPoint", s);

        Set<Type> apiTyeps = new HashSet<Type>();
        apiTyeps.add(IService.class);

        List<Decorator<?>> decs = getBeanManager().resolveDecorators(apiTyeps, new Annotation[]{new Binding1Literal()});
        Assert.assertNotNull(decs);
        Assert.assertTrue(decs.size() > 0);

        Assert.assertEquals("ServiceImpl1", ServiceDecoratorWithCtInjectionPoint.delegateAttr);
    }

}
