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
package org.apache.webbeans.test.unittests.event.component;

import java.lang.annotation.Annotation;

import javax.enterprise.util.AnnotationLiteral;

import junit.framework.Assert;

import org.apache.webbeans.annotation.AnyLiteral;
import org.apache.webbeans.component.AbstractBean;
import org.apache.webbeans.context.ContextFactory;
import org.apache.webbeans.test.TestContext;
import org.apache.webbeans.test.annotation.binding.Check;
import org.apache.webbeans.test.annotation.binding.Role;
import org.apache.webbeans.test.component.CheckWithCheckPayment;
import org.apache.webbeans.test.component.CheckWithMoneyPayment;
import org.apache.webbeans.test.component.PaymentProcessorComponent;
import org.apache.webbeans.test.component.event.normal.ComponentWithObservable1;
import org.apache.webbeans.test.component.event.normal.ComponentWithObserves1;
import org.apache.webbeans.test.component.event.normal.ComponentWithObserves2;
import org.apache.webbeans.test.event.LoggedInEvent;
import org.apache.webbeans.util.WebBeansUtil;
import org.junit.Before;
import org.junit.Test;

public class ObserversComponentTest extends TestContext
{
    public ObserversComponentTest()
    {
        super(ObserversComponentTest.class.getName());
    }

    @Before
    public void init()
    {
        super.init();
    }

    @Test
    public void testObserves()
    {
        clear();

        AbstractBean<ComponentWithObserves1> component = defineManagedBean(ComponentWithObserves1.class);
        ContextFactory.initRequestContext(null);

        LoggedInEvent event = new LoggedInEvent("Gurkan");

        Annotation[] anns = new Annotation[1];
        anns[0] = new AnyLiteral();        

        getManager().fireEvent(event, anns);

        ComponentWithObserves1 instance = getManager().getInstance(component);

        Assert.assertEquals("Gurkan", instance.getUserName());
    }

    @Test
    public void testWithObservable()
    {
        clear();
        
        getManager().addBean(WebBeansUtil.getEventBean());

        AbstractBean<ComponentWithObserves1> component = defineManagedBean(ComponentWithObserves1.class);
        AbstractBean<ComponentWithObservable1> componentObservable = defineManagedBean(ComponentWithObservable1.class);

        ContextFactory.initRequestContext(null);

        ComponentWithObserves1 instance = getManager().getInstance(component);
        ComponentWithObservable1 observable = getManager().getInstance(componentObservable);

        observable.afterLoggedIn();

        Assert.assertEquals("Gurkan", instance.getUserName());
    }

    @Test
    public void testObservesWithBindingMember()
    {
        clear();

        getManager().addBean(WebBeansUtil.getEventBean());
        
        AbstractBean<ComponentWithObserves1> component = defineManagedBean(ComponentWithObserves1.class);
        ContextFactory.initRequestContext(null);

        LoggedInEvent event = new LoggedInEvent("Gurkan");

        class CheckLiteral extends AnnotationLiteral<Check> implements Check
        {

            public String type()
            {
                return "CHECK";
            }

        }

        Annotation[] anns = new Annotation[1];
        anns[0] = new CheckLiteral();

        getManager().fireEvent(event, anns);

        ComponentWithObserves1 instance = getManager().getInstance(component);

        Assert.assertNotNull(instance.getUserName());

        Assert.assertEquals("Gurkan", instance.getUserNameWithMember());
    }

    @Test
    public void testFireWithAtAnyQualifier()
    {
        clear();

        getManager().addBean(WebBeansUtil.getEventBean());
        
        AbstractBean<ComponentWithObserves1> component = defineManagedBean(ComponentWithObserves1.class);
        ContextFactory.initRequestContext(null);

        LoggedInEvent event = new LoggedInEvent("Mark");

        Annotation[] anns = new Annotation[1];
        anns[0] = new AnyLiteral();

        getManager().fireEvent(event, anns);

        ComponentWithObserves1 instance = getManager().getInstance(component);

        Assert.assertEquals("Mark", instance.getUserName());
        Assert.assertNull(instance.getUserNameWithMember());
    }


    @Test
    public void testObservesWithBindingMember2()
    {
        clear();

        defineManagedBean(CheckWithCheckPayment.class);
        defineManagedBean(CheckWithMoneyPayment.class);
        defineManagedBean(PaymentProcessorComponent.class);
        AbstractBean<ComponentWithObserves2> component = defineManagedBean(ComponentWithObserves2.class);
        ContextFactory.initRequestContext(null);

        LoggedInEvent event = new LoggedInEvent("USER");

        class RoleUser extends AnnotationLiteral<Role> implements Role
        {

            public String value()
            {
                return "USER";
            }

        }

        class RoleAdmin extends AnnotationLiteral<Role> implements Role
        {

            public String value()
            {
                return "ADMIN";
            }

        }

        Annotation[] anns = new Annotation[1];
        anns[0] = new RoleUser();

        getManager().fireEvent(event, anns);
        ComponentWithObserves2 instance = getManager().getInstance(component);

        Assert.assertNotNull(instance.getPayment());
        Assert.assertEquals("USER", instance.getUser());

        anns[0] = new RoleAdmin();
        event = new LoggedInEvent("ADMIN");

        getManager().fireEvent(event, anns);
        instance = getManager().getInstance(component);

        Assert.assertNotNull(instance.getPayment());
        Assert.assertEquals("ADMIN", instance.getUser());

    }

}
