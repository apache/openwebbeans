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
package org.apache.webbeans.test.component.newcomp;

import javax.webbeans.Dependent;
import javax.webbeans.Named;
import javax.webbeans.New;
import javax.webbeans.Produces;
import javax.webbeans.Production;
import javax.webbeans.RequestScoped;

import org.apache.webbeans.test.component.CheckWithCheckPayment;
import org.apache.webbeans.test.component.IPayment;

@Production
@RequestScoped
@Named
public class ProducerNewComponent
{
    private IPayment component;

    @Produces
    @Dependent
    @Named("paymentProducer")
    public IPayment create(@New CheckWithCheckPayment comp)
    {
        this.component = comp;
        return comp;
    }

    public IPayment getComponent()
    {
        return this.component;
    }
}
