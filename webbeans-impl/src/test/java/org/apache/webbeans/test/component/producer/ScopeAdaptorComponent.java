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
package org.apache.webbeans.test.component.producer;

import java.io.Serializable;

import jakarta.enterprise.context.SessionScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;

import org.apache.webbeans.test.annotation.binding.Binding2;
import org.apache.webbeans.test.annotation.binding.Check;
import org.apache.webbeans.test.component.IPayment;

@SessionScoped
@Named
public class ScopeAdaptorComponent implements Serializable
{
    @Produces
    @SessionScoped
    @Binding2
    @Named
    public IPayment scope(@Check(type = "CHECK") IPayment payment)
    {
        return payment;
    }
}
