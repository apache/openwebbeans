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
package org.apache.webbeans.ee.beans;

import jakarta.validation.Validator;

import org.apache.webbeans.component.BuiltInOwbBean;
import org.apache.webbeans.component.SimpleProducerFactory;
import org.apache.webbeans.component.WebBeansType;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.portable.ProviderBasedProducer;

//used by BeansDeployer (created via reflection)
public class ValidatorBean extends BuiltInOwbBean<Validator>
{

    public ValidatorBean(WebBeansContext webBeansContext)
    {
        super(webBeansContext,
              WebBeansType.VALIDATION,
              Validator.class,
            new SimpleProducerFactory<>(new ProviderBasedProducer<>(webBeansContext, Validator.class, new ValidatorProvider(webBeansContext), true)));
    }

    @Override
    public boolean isPassivationCapable()
    {
        return true;
    }

    @Override
    public Class<?> proxyableType()
    {
        return null;
    }
}
