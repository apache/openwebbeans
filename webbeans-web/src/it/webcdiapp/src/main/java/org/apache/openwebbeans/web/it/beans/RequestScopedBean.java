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
package org.apache.openwebbeans.web.it.beans;

import javax.annotation.PostConstruct;
import javax.enterprise.context.Initialized;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Named;

@RequestScoped
@Named
public class RequestScopedBean
{

    private static int requestInstanceCounter = 0;
    private static int requestContextCounter = 0;

    private @Inject NonAnnotatedDependentBean nonAnnotatedBean;

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
    }

    public static String info()
    {
        return String.valueOf(requestInstanceCounter) + ',' + String.valueOf(requestContextCounter);
    }

    public int getMeaningOfLife()
    {
        return nonAnnotatedBean.meaningOfLife();
    }
}
