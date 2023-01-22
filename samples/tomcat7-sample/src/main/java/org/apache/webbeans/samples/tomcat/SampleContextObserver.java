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
package org.apache.webbeans.samples.tomcat;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Destroyed;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.context.SessionScoped;
import jakarta.enterprise.event.Observes;
import java.util.logging.Logger;

/**
 * A sample bean which observes the new CDI-1.1 &#064;Initialized and &#064;Destroyed events
 */
@ApplicationScoped
public class SampleContextObserver
{
    private static final Logger log = Logger.getLogger(SampleContextObserver.class.getName());

    public void onAppInit(@Observes @Initialized(ApplicationScoped.class) Object payload)
    {
        log.info("ApplicationContext got started. Init Object: " + payload.toString());
    }

    public void onAppDestroy(@Observes @Destroyed(ApplicationScoped.class) Object payload)
    {
        // this is unlikely to ever fire - we just did destroy our applicationscoped bean ;)
        log.info("ApplicationContext got destroyed. Init Object: " + payload.toString());
    }
    
    public void onSessionInit(@Observes @Initialized(SessionScoped.class) Object payload)
    {
        log.info("SessionContext got started. Init Object: " + payload.toString());
    }

    public void onSessionDestroy(@Observes @Destroyed(SessionScoped.class) Object payload)
    {
        log.info("SessionContext got destroyed. Init Object: " + payload.toString());
    }
    
    public void onRequestInit(@Observes @Initialized(RequestScoped.class) Object payload)
    {
        log.info("RequestContext got started. Init Object: " + payload.toString());
    }

    public void onRequestDestroy(@Observes @Destroyed(RequestScoped.class) Object payload)
    {
        log.info("RequestContext got destroyed. Init Object: " + payload.toString());
    }
    
    
}
