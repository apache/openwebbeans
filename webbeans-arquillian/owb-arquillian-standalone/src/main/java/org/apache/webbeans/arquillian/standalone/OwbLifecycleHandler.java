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
package org.apache.webbeans.arquillian.standalone;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.ConversationScoped;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.context.SessionScoped;

import org.apache.webbeans.spi.ContainerLifecycle;
import org.apache.webbeans.spi.ContextsService;
import org.jboss.arquillian.container.spi.event.container.AfterDeploy;
import org.jboss.arquillian.container.spi.event.container.BeforeUnDeploy;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.test.spi.event.suite.After;
import org.jboss.arquillian.test.spi.event.suite.Before;

/**
 * Starts and stops the application and the contexts in it.
 */
public class OwbLifecycleHandler
{
    @Inject
    private Instance<ContainerLifecycle> lifecycle;

    public void afterDeployment(@Observes AfterDeploy event)
    {
        // the application context should be started already
    }

    public void beforeUndeployment(@Observes BeforeUnDeploy event)
    {
        ContextsService service = lifecycle.get().getContextService();

        service.endContext(ApplicationScoped.class, null);
    }

    public void beforeMethod(@Observes Before event)
    {
        ContainerLifecycle lc = lifecycle.get();
        if (lc == null)
        {
            // this may happen if there was a DeploymentError during CDI boot
            return;
        }

        ContextsService service = lc.getContextService();

        service.startContext(RequestScoped.class, null);
        service.startContext(SessionScoped.class, null);
        service.startContext(ConversationScoped.class, null);
    }

    public void afterMethod(@Observes After event)
    {
        ContainerLifecycle lc = lifecycle.get();
        if (lc == null)
        {
            // this may happen if there was a DeploymentError during CDI boot
            return;
        }

        ContextsService service = lc.getContextService();

        service.endContext(ConversationScoped.class, null);
        service.endContext(SessionScoped.class, null);
        service.endContext(RequestScoped.class, null);
    }


}
