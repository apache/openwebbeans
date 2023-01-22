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
package org.apache.webbeans.test.component.decorator.clean;

import jakarta.decorator.Decorator;
import jakarta.decorator.Delegate;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.inject.Inject;

import org.apache.webbeans.test.annotation.binding.Binding1;
import org.apache.webbeans.test.component.service.IService;

/**
 * The same as {@see ServiceDecorator} but with the @Delegate in a ct and not as a field
 */
@Decorator
public  class ServiceDecoratorWithCtInjectionPoint implements IService
{
    private IService delegate;

    public static String delegateAttr = null;
    public static InjectionPoint ip;

    @Inject
    public ServiceDecoratorWithCtInjectionPoint(@Delegate @Binding1 IService delegate, InjectionPoint ip)
    {
        ServiceDecoratorWithCtInjectionPoint.ip = ip;
        this.delegate = delegate;
    }

    @Override
    public String service()
    {
        this.delegateAttr = delegate.service();

        return "ServiceDecoratorWithCtInjectionPoint";
    }

    public String getDelegateAttr()
    {
        return delegateAttr;
    }
}
