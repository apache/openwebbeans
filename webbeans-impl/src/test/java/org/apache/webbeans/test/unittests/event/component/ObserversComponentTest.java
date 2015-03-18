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
package org.apache.webbeans.svtest.unittests.event.component;

import javax.enterprise.util.AnnotationLiteral;

import junit.framework.Assert;

import org.apache.webbeans.annotation.AnyLiteral;
import org.apache.webbeans.test.AbstractUnitTest;
import org.apache.webbeans.test.annotation.binding.Check;
import org.apache.webbeans.test.annotation.binding.NotAny;
import org.apache.webbeans.test.annotation.binding.Role;
import org.apache.webbeans.test.component.CheckWithCheckPayment;
import org.apache.webbeans.test.component.CheckWithMoneyPayment;
import org.apache.webbeans.test.component.PaymentProcessorComponent;
import org.apache.webbeans.test.component.event.normal.ComponentWithObservable1;
import org.apache.webbeans.test.component.event.normal.ComponentWithObserves1;
import org.apache.webbeans.test.component.event.normal.ComponentWithObserves2;
import org.apache.webbeans.test.component.event.normal.ComponentWithObserves3;
import org.apache.webbeans.test.component.event.normal.ComponentWithObserves4;
import org.apache.webbeans.test.component.event.normal.ComponentWithObserves5;
import org.apache.webbeans.test.component.event.normal.ComponentWithObserves6;
import org.apache.webbeans.test.component.event.normal.ComponentWithObserves7;
import org.apache.webbeans.test.component.event.normal.TransactionalInterceptor;
import org.apache.webbeans.test.event.LoggedInEvent;
import org.junit.Test;

import java.lang.annotation.Annotation;

public class ObserversComponentTest extends AbstractUnitTest
{
    @Test
    public void testObserves()
    {
        startContainer(ComponentWithObserves1.class);

        LoggedInEvent event = new LoggedInEvent("Gurkan");
        getBeanManager().fireEvent(event, AnyLiteral.INSTANCE);

        ComponentWithObserves1 instance = getInstance(ComponentWithObserves1.class);

        Assert.assertEquals("Gurkan", instance.getUserName());

        event = new LoggedInEvent("Mark");
        getBeanManager().fireEvent(event, AnyLiteral.INSTANCE);
        Assert.assertEquals("Mark", instance.getUserName());
    }

    @Test
    public void testWithObservable()
    {
        startContainer(ComponentWithObserves1.class, ComponentWithObservable1.class);

        ComponentWithObserves1 instance = getInstance(ComponentWithObserves1.class);
        ComponentWithObservable1 observable = getInstance(ComponentWithObservable1.class);

        observable.afterLoggedIn();

        Assert.assertEquals("Gurkan", instance.getUserName());
    }

    @Test
    public void testObservesIfExists()
    {
        startContainer(ComponentWithObserves3.class, ComponentWithObserves4.class, ComponentWithObserves5.class, ComponentWithObserves6.class);

        ComponentWithObserves5 instanceIE = getInstance(ComponentWithObserves5.class);

        Annotation notAnyQualifier = new AnnotationLiteral<NotAny>(){};

        LoggedInEvent event = new LoggedInEvent("Gurkan");
        getBeanManager().fireEvent(event, notAnyQualifier);
        Assert.assertNull(instanceIE.getUserName());

        // do it again, Sam
        getBeanManager().fireEvent(event, notAnyQualifier);
        Assert.assertEquals("Gurkan", instanceIE.getUserName());
    }

    @Test
    public void testObservesWithBindingMember()
    {
        startContainer(ComponentWithObserves1.class);

        LoggedInEvent event = new LoggedInEvent("Gurkan");

        class CheckLiteral extends AnnotationLiteral<Check> implements Check
        {
            @Override
            public String type()
            {
                return "CHECK";
            }
        }
        getBeanManager().fireEvent(event, new CheckLiteral());

        ComponentWithObserves1 instance = getInstance(ComponentWithObserves1.class);

        Assert.assertNotNull(instance.getUserName());
        Assert.assertEquals("Gurkan", instance.getUserNameWithMember());
    }

    @Test
    public void testFireWithAtAnyQualifier()
    {
        startContainer(ComponentWithObserves1.class);

        LoggedInEvent event = new LoggedInEvent("Mark");
        getBeanManager().fireEvent(event, AnyLiteral.INSTANCE);

        ComponentWithObserves1 instance = getInstance(ComponentWithObserves1.class);

        Assert.assertEquals("Mark", instance.getUserName());
        Assert.assertNull(instance.getUserNameWithMember());
    }


    @Test
    public void testObservesWithBindingMember2()
    {
        addInterceptor(TransactionalInterceptor.class);
        startContainer(CheckWithCheckPayment.class, CheckWithMoneyPayment.class, PaymentProcessorComponent.class, ComponentWithObserves2.class);

        LoggedInEvent event = new LoggedInEvent("USER");
        class RoleUser extends AnnotationLiteral<Role> implements Role
        {
            @Override
            public String value()
            {
                return "USER";
            }
        }

        class RoleAdmin extends AnnotationLiteral<Role> implements Role
        {
            @Override
            public String value()
            {
                return "ADMIN";
            }
        }

        ComponentWithObserves2.hasBeenIntercepted = false;
        
        getBeanManager().fireEvent(event, new RoleUser());
        ComponentWithObserves2 instance = getInstance(ComponentWithObserves2.class);

        Assert.assertFalse(ComponentWithObserves2.hasBeenIntercepted);
        
        Assert.assertNotNull(instance.getPayment());
        Assert.assertEquals("USER", instance.getUser());

        event = new LoggedInEvent("ADMIN");
        getBeanManager().fireEvent(event, new RoleAdmin());

        Assert.assertTrue(ComponentWithObserves2.hasBeenIntercepted);
        Assert.assertNotNull(instance.getPayment());
        Assert.assertEquals("ADMIN", instance.getUser());

        // lessons learned: do it again sam! ;)
        ComponentWithObserves2.hasBeenIntercepted = false;
        getBeanManager().fireEvent(event, new RoleAdmin());

        Assert.assertTrue(ComponentWithObserves2.hasBeenIntercepted);
        Assert.assertNotNull(instance.getPayment());
        Assert.assertEquals("ADMIN", instance.getUser());
    }
    
    @Test
    public void testObservesWithEventInjection()
    {
        startContainer(ComponentWithObserves7.class, ComponentWithObservable1.class);

        ComponentWithObserves7 instance = getInstance(ComponentWithObserves7.class);
        ComponentWithObservable1 observable = getInstance(ComponentWithObservable1.class);

        observable.afterLoggedIn();

        Assert.assertEquals("Gurkan", instance.getUserName());
        Assert.assertEquals("Rohit_Kelapure", instance.getEventString());
    }    
}
