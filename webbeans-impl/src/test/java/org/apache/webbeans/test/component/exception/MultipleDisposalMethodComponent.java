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
package org.apache.webbeans.test.component.exception;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;

import org.apache.webbeans.test.annotation.binding.Binding1;
import org.apache.webbeans.test.component.service.IService;

public class MultipleDisposalMethodComponent
{
    private IService service = null;

    @Produces
    @ApplicationScoped
    @Default
    public IService getService(@Binding1 IService service)
    {
        this.service = service;

        return service;
    }

    public IService service()
    {
        return this.service;
    }

    public void dispose(@Disposes @Default IService service)
    {
        service = null;
        this.service = null;
    }

    public void dispose2(@Disposes @Default IService service)
    {
        service = null;
        this.service = null;
    }

}
