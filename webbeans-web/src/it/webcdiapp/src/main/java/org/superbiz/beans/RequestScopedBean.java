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
package org.superbiz.beans;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ContextNotActiveException;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@RequestScoped
@Named
public class RequestScopedBean
{

    private static volatile int requestInstanceCounter = 0;
    private static volatile int requestContextCounter = 0;

    private @Inject NonAnnotatedDependentBean nonAnnotatedBean;
    private @Inject SessionScopedCounter sessionScopedCounter;

    private String name = "Super name";


    @PostConstruct
    protected void init()
    {
        requestInstanceCounter++;
    }


    public String getName()
    {
        return name;
    }

    public int getRequestInstanceCount()
    {
        return requestInstanceCounter;
    }

    public int getRequestContextCount()
    {
        return requestContextCounter;
    }

    public static void resetCounter()
    {
        requestContextCounter = 0;
        requestInstanceCounter = 0;
    }

    public void onRequestInit(@Observes @Initialized(RequestScoped.class) Object payload)
    {
        requestContextCounter++;
        try
        {
            sessionScopedCounter.increment();
        }
        catch (ContextNotActiveException cnae)
        {
            // ignore if on app startup.
        }
    }

    public static String info()
    {
        return String.valueOf(requestInstanceCounter) + "," + String.valueOf(requestContextCounter);
    }

    public int getMeaningOfLife()
    {
        return nonAnnotatedBean.meaningOfLife();
    }
}
