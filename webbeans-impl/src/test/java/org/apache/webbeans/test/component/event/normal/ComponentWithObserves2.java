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
package org.apache.webbeans.test.component.event.normal;

import javax.webbeans.Observes;
import javax.webbeans.Production;
import javax.webbeans.RequestScoped;

import org.apache.webbeans.test.annotation.binding.Role;
import org.apache.webbeans.test.component.IPayment;
import org.apache.webbeans.test.component.PaymentProcessorComponent;
import org.apache.webbeans.test.event.LoggedInEvent;

@Production
@RequestScoped
public class ComponentWithObserves2
{
    private IPayment payment;

    private String user;

    public void afterLogin(@Observes @Role(value = "USER") LoggedInEvent event, PaymentProcessorComponent payment)
    {
        this.payment = payment.getPaymentCheck();
        this.user = event.getUserName();
    }

    public void afterAdminLogin(@Observes @Role(value = "ADMIN") LoggedInEvent event, PaymentProcessorComponent payment)
    {
        this.payment = payment.getPaymentCheck();
        this.user = event.getUserName();
    }

    public String getUser()
    {
        return user;
    }

    public IPayment getPayment()
    {
        return this.payment;
    }
}
