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
package org.apache.webbeans.test.contexts.session.common;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Destroyed;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.context.SessionScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class AppScopedBean {
    
    private @Inject PersonalDataBean pdb;

    public static List<Object> appContextInitializedEvent = new ArrayList<Object>();
    public static List<Object> appContextDestroyedEvent = new ArrayList<Object>();

    public static List<Object> sessionContextInitializedEvent = new ArrayList<Object>();
    public static List<Object> sessionContextDestroyedEvent = new ArrayList<Object>();

    public static List<Object> requestContextInitializedEvent = new ArrayList<Object>();
    public static List<Object> requestContextDestroyedEvent = new ArrayList<Object>();

    public PersonalDataBean getPdb() {
        return pdb;
    }

    public void appContextInitialized(@Observes @Initialized(ApplicationScoped.class) Object payload)
    {
        appContextInitializedEvent.add(payload);
    }

    public void appContextDestroyed(@Observes @Destroyed(ApplicationScoped.class) Object payload)
    {
        appContextDestroyedEvent.add(payload);
    }

    public void sessionContextInitialized(@Observes @Initialized(SessionScoped.class) Object payload)
    {
        sessionContextInitializedEvent.add(payload);
    }

    public void sessionContextDestroyed(@Observes @Destroyed(SessionScoped.class) Object payload)
    {
        sessionContextDestroyedEvent.add(payload);
    }

    public void requestContextInitialized(@Observes @Initialized(RequestScoped.class) Object payload)
    {
        requestContextInitializedEvent.add(payload);
    }

    public void requestContextDestroyed(@Observes @Destroyed(RequestScoped.class) Object payload)
    {
        requestContextDestroyedEvent.add(payload);
    }

    
}
