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
package org.apache.openwebbeans.junit5.extension;

import org.apache.webbeans.context.AbstractContext;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.BeforeBeanDiscovery;
import jakarta.enterprise.inject.spi.Extension;
import java.util.concurrent.ConcurrentHashMap;

// register a custom scope we can manipulate injecting the extension (getScope())
public class MyScope implements Extension
{
    private AbstractContext scope;

    public AbstractContext getScope()
    {
        return scope;
    }

    void addScope(@Observes final BeforeBeanDiscovery beforeBeanDiscovery)
    {
        beforeBeanDiscovery.addScope(DummyScoped.class, true, true);
    }

    void addScope(@Observes final AfterBeanDiscovery afterBeanDiscovery)
    {
        afterBeanDiscovery.addContext(scope = new AbstractContext(DummyScoped.class)
        {
            @Override
            protected void setComponentInstanceMap()
            {
                componentInstanceMap = new ConcurrentHashMap<>();
            }
        });
    }
}
