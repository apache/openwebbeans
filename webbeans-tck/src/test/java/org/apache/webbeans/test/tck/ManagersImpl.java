/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.apache.webbeans.test.tck;

import java.lang.annotation.Annotation;
import java.util.List;

import javax.inject.manager.Manager;

import org.apache.webbeans.context.ContextFactory;
import org.apache.webbeans.deployment.DeploymentTypeManager;
import org.apache.webbeans.test.mock.MockHttpSession;
import org.apache.webbeans.test.mock.MockManager;
import org.jboss.jsr299.tck.spi.Managers;

public class ManagersImpl implements Managers
{

    public Manager createManager()
    {
        ContextFactory.initApplicationContext(null);
        ContextFactory.initRequestContext(null);
        ContextFactory.initSessionContext(new MockHttpSession());
        ContextFactory.initConversationContext(null);
        return MockManager.getInstance();
    }

    public List<Class<? extends Annotation>> getEnabledDeploymentTypes()
    {
        return DeploymentTypeManager.getInstance().getEnabledDeploymentTypes();
    }

    public void setEnabledDeploymentTypes(List<Class<? extends Annotation>> enabledDeploymentTypes)
    {
        for (Class<? extends Annotation> deploymentType : enabledDeploymentTypes)
        {
            DeploymentTypeManager.getInstance().addNewDeploymentType(deploymentType, 1);
        }
    }


    public Manager getManager() {
        Manager manager = createManager();
        return manager;
    }

}
